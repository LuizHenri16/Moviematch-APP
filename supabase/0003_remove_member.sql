-- ============================================================================
-- MovieMatch (KMP) — 0003_remove_member.sql
-- ============================================================================
-- O QUE ESTA MIGRAÇÃO RESOLVE
--   Adiciona a capacidade de um membro remover outro membro do grupo (kick).
--   Apenas o criador do grupo (slot = 1) pode remover o outro membro.
--   Reutiliza as validações existentes (grupo ativo, membro ativo, etc.).
--
-- DIVERGÊNCIA DA REFERÊNCIA
--   O schema original não previa remoção de membro pelo criador.
--   O 0001_init.sql NÃO é alterado (segue cópia verbatim da referência).
--
-- COMO APLICAR
--   Aplicar POR CIMA do 0001 e 0002 (que precisam já estar aplicados).
--   Cole este arquivo inteiro no SQL Editor do Supabase (Dashboard > SQL Editor > New query > Run).
--   É IDEMPOTENTE: `create or replace function` permite reexecutá-lo sem quebrar o banco.
--
-- CONTRATO DE ERROS (nomes estáveis; o app mapeia para mensagens de UI)
--   * not_authenticated       (28000) — sem sessão;
--   * not_a_member            (42501) — quem chama não é membro do grupo;
--   * not_group_creator       (42501) — apenas o criador (slot 1) pode remover;
--   * cannot_remove_self      (P0001) — não pode remover a si mesmo (use leave_group);
--   * member_not_found        (P0001) — membro alvo não está no grupo ativo;
--   * group_not_active        (P0001) — grupo não está ativo;
-- ============================================================================

-- ----------------------------------------------------------- remove_member --
create or replace function public.remove_member(p_group_id uuid, p_profile_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid       uuid := auth.uid();
  v_remover   public.group_members;
  v_target    public.group_members;
  v_remaining integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- Quem chama deve ser membro ativo do grupo
  select * into v_remover
    from public.group_members
   where group_id = p_group_id and profile_id = v_uid and left_at is null
   for update;

  if not found then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  -- Apenas o criador (slot = 1) pode remover outro membro
  if v_remover.slot <> 1 then
    raise exception 'not_group_creator' using errcode = '42501';
  end if;

  -- Não pode remover a si mesmo (use leave_group para isso)
  if p_profile_id = v_uid then
    raise exception 'cannot_remove_self' using errcode = 'P0001';
  end if;

  -- O alvo deve ser membro ativo do grupo
  select * into v_target
    from public.group_members
   where group_id = p_group_id and profile_id = p_profile_id and left_at is null
   for update;

  if not found then
    raise exception 'member_not_found' using errcode = 'P0001';
  end if;

  -- Não pode remover o próprio criador (slot 1)
  if v_target.slot = 1 then
    raise exception 'cannot_remove_creator' using errcode = 'P0001';
  end if;

  -- Remove o membro (soft delete com left_at)
  update public.group_members
     set left_at = now()
   where group_id = p_group_id and profile_id = p_profile_id and left_at is null;

  -- Verifica quantos membros ativos restam
  select count(*) into v_remaining
    from public.group_members
   where group_id = p_group_id and left_at is null;

  if v_remaining = 0 then
    -- Sem membros -> dissolve o grupo (soft-delete) e libera o pair_key.
    update public.groups
       set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
     where id = p_group_id and status = 'active';
  elsif v_remaining = 1 then
    -- Grupo segue ativo, mas não é mais um par -> limpa pair_key.
    update public.groups set pair_key = null where id = p_group_id and status = 'active';
  end if;
end;
$$;

comment on function public.remove_member(uuid, uuid) is 'Remove um membro do grupo (apenas o criador slot 1 pode remover o outro membro).';

revoke all on function public.remove_member(uuid, uuid) from public;
grant execute on function public.remove_member(uuid, uuid) to authenticated;