-- ============================================================================
-- MovieMatch (KMP) — 0002_group_join_by_code.sql
-- ============================================================================
-- O QUE ESTA MIGRAÇÃO RESOLVE
--   Adiciona o caminho "entrar por código": um convidado AUTENTICADO digita o
--   invite_code de um grupo ativo e ENTRA nele numa única chamada, ocupando o
--   slot 2 e gravando o pair_key — via a RPC public.join_group_by_code(p_code).
--
-- DIVERGÊNCIA DA REFERÊNCIA
--   O schema original (0001_init.sql) só sabia RESOLVER um código
--   (resolve_invite_code) e ENTRAR a partir de um convite já existente
--   (accept_invite). Faltava a porta em que o próprio convidado entra sozinho a
--   partir do código; é exatamente isso que esta migração acrescenta. O
--   0001_init.sql NÃO é alterado (segue cópia verbatim da referência).
--
-- COMO APLICAR
--   Aplicar POR CIMA do 0001 (que precisa já estar aplicado). Cole este arquivo
--   inteiro no SQL Editor do Supabase (Dashboard > SQL Editor > New query >
--   Run). É IDEMPOTENTE: `create or replace function` permite reexecutá-lo sem
--   quebrar o banco.
--
-- CONTRATO DE ERROS (nomes estáveis; o app mapeia para mensagens de UI)
--   * not_authenticated       (28000) — sem sessão;
--   * already_in_active_group (23505) — o usuário já tem vínculo ativo;
--   * invalid_invite_code             — código inexistente ou grupo não 'active';
--   * group_full              (23505) — slot 2 ocupado.
--   Reusa public.accept_invite() para pending/expiração/grupo ativo/slot
--   livre/pair_key, de modo que a regra de entrada fica num lugar só.
-- ============================================================================

-- ------------------------------------------------- join_group_by_code --------
create or replace function public.join_group_by_code(p_code text)
returns public.groups
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid    uuid := auth.uid();
  v_group  public.groups;
  v_invite public.group_invites;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- Um usuário participa de no máximo um grupo ativo por vez (mesma regra de
  -- create_group/accept_invite).
  if exists (
    select 1 from public.group_members
    where profile_id = v_uid and left_at is null
  ) then
    raise exception 'already_in_active_group' using errcode = '23505';
  end if;

  -- Resolve o código em um grupo ATIVO (mesma regra de resolve_invite_code).
  select * into v_group
    from public.groups
   where invite_code = upper(trim(coalesce(p_code, '')))
     and status = 'active';

  if not found then
    raise exception 'invalid_invite_code';
  end if;

  -- AUTO-SOLICITAÇÃO: o convidado cria um convite `pending` para SI MESMO
  -- (invited_by = invited_profile_id = auth.uid()) e o aceita logo abaixo.
  -- Assim a regra de entrada (pending/expiração/grupo ativo/slot livre/pair_key)
  -- fica num único lugar: public.accept_invite().
  -- Antes de inserir, reaproveita um convite `pending` já existente para mim
  -- neste grupo — o meu próprio (evita estourar no índice parcial único
  -- group_invites_pending_profile_uidx) ou um convite que um membro do grupo
  -- tenha me feito — tornando a operação idempotente.
  select * into v_invite
    from public.group_invites
   where group_id = v_group.id
     and status = 'pending'
     and (
       invited_profile_id = v_uid
       or (
         invitee_handle is not null
         and exists (
           select 1 from public.profiles p
           where p.id = v_uid
             and p.handle is not null
             and lower(p.handle) = lower(invitee_handle)
         )
       )
     )
   order by created_at desc
   limit 1
   for update;

  if v_invite.id is null then
    begin
      insert into public.group_invites (group_id, invited_by, invited_profile_id, status)
      values (v_group.id, v_uid, v_uid, 'pending')
      returning * into v_invite;
    exception
      -- Corrida com outra sessão: relê o convite pending que acabou de nascer.
      when unique_violation then
        select * into v_invite
          from public.group_invites
         where group_id = v_group.id
           and invited_profile_id = v_uid
           and status = 'pending'
         for update;
    end;
  end if;

  -- Renova a janela da solicitação: um pedido pending vencido não pode impedir
  -- a entrada (accept_invite o marcaria como expired e erraria).
  update public.group_invites
     set expires_at = now() + interval '7 days'
   where id = v_invite.id
     and status = 'pending';

  -- valida pending/expiração/grupo ativo/slot livre, ocupa o slot 2, grava o
  -- pair_key e marca o convite como accepted.
  return public.accept_invite(v_invite.id);
end;
$$;

comment on function public.join_group_by_code(text) is 'Entrar por código: auto-solicita convite pending e o aceita (reusa accept_invite), ocupando o slot 2 do grupo ativo.';

revoke all on function public.join_group_by_code(text) from public;
grant execute on function public.join_group_by_code(text) to authenticated;
