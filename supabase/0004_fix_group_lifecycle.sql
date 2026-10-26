-- ============================================================================
-- MovieMatch (KMP) — 0004_fix_group_lifecycle.sql
-- ============================================================================
-- O QUE ESTA MIGRAÇÃO RESOLVE
--   Corrige o ciclo de vida do grupo/convite que deixava o usuário "preso":
--   depois de dissolver uma roda, o vínculo em `group_members` continuava ativo
--   (`left_at is null`) e os convites pendentes continuavam visíveis. Ao tentar
--   aceitar um convite, `accept_invite` via esse vínculo antigo e respondia
--   `already_in_active_group`; ao recriar/entrar por código, o mesmo acontecia.
--
--   1) Dissolução incompleta (`dissolve_group` só mudava `groups.status`):
--      agora fecha TODOS os vínculos ativos (`left_at = now()`) e cancela os
--      convites pendentes do grupo. `leave_group` também cancela os convites
--      quando dissolver o grupo por falta de membros.
--
--   2) Constraints únicas NÃO parciais em `group_members`
--      (`unique (group_id, slot)` e `unique (group_id, profile_id)`):
--      como sair/remover faz soft-delete (`left_at = now()`), a linha antiga
--      continuava ocupando o slot/perfil. O INSERT de `accept_invite` estourava
--      `23505` e o app caía no texto genérico
--      "Não foi possível aceitar o convite.".
--      Trocamos por índices únicos PARCIAIS com `where left_at is null`.
--
--   3) `accept_invite` agora REATIVA uma linha soft-deleted do mesmo
--      `(group_id, profile_id)` em vez de inserir outra (preserva o histórico e
--      funciona com os novos índices parciais).
--
--   4) `create_group`, `accept_invite` e `join_group_by_code` passam a checar
--      "já está em grupo ativo" com JOIN em `groups.status = 'active'`,
--      ignorando vínculos legados de grupos dissolvidos.
--
--   5) Cleanup de dados legados: grupos que já não estão ativos têm os vínculos
--      ativos fechados e os convites pendentes cancelados.
--
--   6) BUG CRÍTICO DO 0001: `accept_invite` chamava `min(profile_id)` /
--      `max(profile_id)` em colunas `uuid`. O PostgreSQL NÃO possui agregados
--      min()/max() para uuid, então TODO aceite falhava com SQLSTATE 42883
--      ("function min(uuid) does not exist") antes de gravar o `pair_key` —
--      mesmo sem vínculo fantasma algum. Aqui o par é calculado por
--      `min(profile_id::text)::uuid` / `max(...)`, cuja ordem canônica (hex
--      minúsculo) é a mesma do uuid.
--
--   7) ORDEM GLOBAL DE LOCKS (grupo -> membros -> convites): `accept_invite` e
--      `join_group_by_code` travavam o convite antes do grupo, enquanto
--      `dissolve_group`/`leave_group` travavam o grupo primeiro. Sob
--      concorrência isso produzia `deadlock detected` e o aceite podia falhar
--      com o texto genérico "Não foi possível aceitar o convite.". Todas as
--      RPCs de ciclo de vida (inclusive `remove_member`, recriada aqui) agora
--      travam o grupo primeiro.
--
-- DIVERGÊNCIA DA REFERÊNCIA
--   O 0001_init.sql NÃO é alterado (segue cópia verbatim da referência).
--   Esta migração é incremental, no mesmo estilo do 0002 e do 0003.
--
-- COMO APLICAR
--   Aplicar POR CIMA do 0001, 0002 e 0003 (que precisam já estar aplicados).
--   Cole este arquivo inteiro no SQL Editor do Supabase
--   (Dashboard > SQL Editor > New query > Run).
--   É IDEMPOTENTE: os `update` de cleanup são condicionais, os `drop ... if
--   exists` e `create ... if not exists` podem ser reexecutados e
--   `create or replace function` preserva os grants existentes.
--
-- CONTRATO DE ERROS (nomes estáveis; o app mapeia para mensagens de UI)
--   * not_authenticated        (28000) — sem sessão;
--   * already_in_active_group  (23505) — o usuário já tem vínculo em grupo ATIVO;
--   * not_a_member             (42501) — quem chama não é membro ativo do grupo;
--   * group_not_active         (P0001) — grupo não está ativo;
--   * group_full               (23505) — slot 2 ocupado;
--   * already_a_member         (P0001) — perfil já está ativo no grupo;
--   * invite_*                 (P0001) — convite inexistente/expirado/não pendente.
-- ============================================================================


-- ============================================================================
-- 01. CLEANUP DE DADOS LEGADOS
-- ============================================================================
-- Grupos dissolvidos não devem manter membros "ativos". Isso é o que impedia
-- aceitar convite / criar / entrar em outra roda depois de excluir a anterior.
update public.group_members gm
   set left_at = now()
  from public.groups g
 where g.id = gm.group_id
   and g.status <> 'active'
   and gm.left_at is null;

-- Convites de grupos dissolvidos não podem mais ser aceitos; deixá-los
-- `pending` só polui a tela do convidado e gera `group_not_active` no aceite.
update public.group_invites gi
   set status = 'cancelled',
       responded_at = now()
  from public.groups g
 where g.id = gi.group_id
   and g.status <> 'active'
   and gi.status = 'pending';


-- ============================================================================
-- 02. UNICIDADE APENAS ENTRE VÍNCULOS ATIVOS
-- ============================================================================
-- Os nomes abaixo são os gerados pelo Postgres para os `unique (...)` inline do
-- 0001. O `drop index` cobre instalações em que o objeto apareça como índice
-- solto em vez de constraint.
alter table public.group_members
  drop constraint if exists group_members_group_id_slot_key;
alter table public.group_members
  drop constraint if exists group_members_group_id_profile_id_key;

drop index if exists public.group_members_group_id_slot_key;
drop index if exists public.group_members_group_id_profile_id_key;

-- Defesa extra: remove qualquer outra constraint única com a mesma assinatura
-- de colunas (caso o banco tenha sido criado com nomes diferentes).
do $$
declare
  v_conname text;
begin
  for v_conname in
    select c.conname
      from pg_constraint c
      join pg_class t      on t.oid = c.conrelid
      join pg_namespace n  on n.oid = t.relnamespace
     where n.nspname = 'public'
       and t.relname = 'group_members'
       and c.contype = 'u'
       and (
         (
           select array_agg(a.attname::text order by a.attname::text)
             from unnest(c.conkey) as k(attnum)
             join pg_attribute a
               on a.attrelid = c.conrelid and a.attnum = k.attnum
         ) in (
           array['group_id', 'profile_id'],
           array['group_id', 'slot']
         )
       )
  loop
    execute format('alter table public.group_members drop constraint %I', v_conname);
  end loop;
end $$;

-- A partir daqui, um slot/perfil só é único enquanto o vínculo está ativo
-- (`left_at is null`). Linhas históricas (soft-deleted) não bloqueiam reentrada.
create unique index if not exists group_members_active_group_slot_uidx
  on public.group_members (group_id, slot)
  where left_at is null;

create unique index if not exists group_members_active_group_profile_uidx
  on public.group_members (group_id, profile_id)
  where left_at is null;


-- ============================================================================
-- 03. RPCs
-- ============================================================================

-- ------------------------------------------------------------- create_group --
create or replace function public.create_group()
returns public.groups
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid    uuid := auth.uid();
  v_code   text;
  v_group  public.groups;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- Um usuário participa de no máximo um grupo ATIVO por vez. O JOIN em
  -- `groups` ignora vínculos legados que ficaram abertos em grupos dissolvidos.
  if exists (
    select 1
      from public.group_members gm
      join public.groups g on g.id = gm.group_id
     where gm.profile_id = v_uid
       and gm.left_at is null
       and g.status = 'active'
  ) then
    raise exception 'already_in_active_group' using errcode = '23505';
  end if;

  -- Gera invite_code sem depender de pgcrypto: md5(random + timestamp) → 8 chars hex.
  loop
    v_code := upper(substring(md5(random()::text || clock_timestamp()::text) from 1 for 8));
    exit when not exists (select 1 from public.groups g where g.invite_code = v_code);
  end loop;

  insert into public.groups (status, invite_code, created_by)
  values ('active', v_code, v_uid)
  returning * into v_group;

  insert into public.group_members (group_id, profile_id, slot)
  values (v_group.id, v_uid, 1);

  return v_group;
end;
$$;

comment on function public.create_group() is 'Cria grupo ativo (invite_code único), insere o criador no slot 1 e retorna o grupo.';


-- ----------------------------------------------------------- accept_invite --
create or replace function public.accept_invite(p_invite_id uuid)
returns public.groups
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid        uuid := auth.uid();
  v_invite     public.group_invites;
  v_group      public.groups;
  v_existing   public.group_members;
  v_lo         uuid;
  v_hi         uuid;
  v_count      integer;
  v_group_id   uuid;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- ORDEM GLOBAL DE LOCKS: grupo -> membros -> convites.
  -- Descobrimos o grupo por uma leitura SEM trava: `group_invites.group_id` é
  -- imutável (nenhuma RPC altera essa coluna), então a leitura serve apenas
  -- para saber QUAL grupo travar primeiro. Antes esta função travava o convite
  -- antes do grupo, enquanto dissolve_group travava o grupo antes dos convites;
  -- isso produzia `deadlock detected` sob concorrência.
  select group_id into v_group_id
    from public.group_invites
   where id = p_invite_id;

  if not found then
    raise exception 'invite_not_found';
  end if;

  select * into v_invite
    from public.group_invites
   where id = p_invite_id;

  if not found then
    raise exception 'invite_not_found';
  end if;

  -- Autorização: convidado por id OU por handle correspondente ao meu perfil.
  if v_invite.invited_profile_id is distinct from v_uid
     and not (
       v_invite.invitee_handle is not null
       and exists (
         select 1 from public.profiles p
         where p.id = v_uid
           and p.handle is not null
           and lower(p.handle) = lower(v_invite.invitee_handle)
       )
     )
  then
    raise exception 'not_invited' using errcode = '42501';
  end if;

  if v_invite.status <> 'pending' then
    raise exception 'invite_not_pending';
  end if;

  if v_invite.expires_at is not null and v_invite.expires_at < now() then
    -- NÃO atualizamos para 'expired': o `raise` aborta a transação e qualquer
    -- UPDATE seria desfeito. O convite permanece 'pending' (comportamento
    -- legado); `join_group_by_code` renova a janela e serve como saída.
    raise exception 'invite_expired';
  end if;

  -- Primeiro lock da ordem global: o GRUPO.
  select * into v_group
    from public.groups
   where id = v_group_id
   for update;

  if not found or v_group.status <> 'active' then
    raise exception 'group_not_active';
  end if;

  -- Relê o convite SOB a trava do grupo: ele pode ter sido aceito/cancelado
  -- entre a leitura inicial e a aquisição do lock.
  select * into v_invite
    from public.group_invites
   where id = p_invite_id
   for update;

  if not found then
    raise exception 'invite_not_found';
  end if;

  -- Revalida autorização/estado/expiração com o lock já adquirido.
  if v_invite.invited_profile_id is distinct from v_uid
     and not (
       v_invite.invitee_handle is not null
       and exists (
         select 1 from public.profiles p
         where p.id = v_uid
           and p.handle is not null
           and lower(p.handle) = lower(v_invite.invitee_handle)
       )
     )
  then
    raise exception 'not_invited' using errcode = '42501';
  end if;

  if v_invite.status <> 'pending' then
    raise exception 'invite_not_pending';
  end if;

  if v_invite.expires_at is not null and v_invite.expires_at < now() then
    raise exception 'invite_expired';
  end if;

  -- Um usuário participa de no máximo um grupo ATIVO por vez. Só conta vínculo
  -- cujo grupo ainda está ativo; vínculos legados de grupos dissolvidos não
  -- podem bloquear o aceite (é exatamente o bug que esta migração corrige).
  if exists (
    select 1
      from public.group_members gm
      join public.groups g on g.id = gm.group_id
     where gm.profile_id = v_uid
       and gm.left_at is null
       and g.status = 'active'
  ) then
    raise exception 'already_in_active_group' using errcode = '23505';
  end if;

  -- Slot 2 precisa estar livre (apenas vínculos ativos contam).
  if exists (
    select 1 from public.group_members
    where group_id = v_group.id and slot = 2 and left_at is null
  ) then
    raise exception 'group_full' using errcode = '23505';
  end if;
  if exists (
    select 1 from public.group_members
    where group_id = v_group.id and profile_id = v_uid and left_at is null
  ) then
    raise exception 'already_a_member';
  end if;

  -- Reaproveita a linha soft-deleted do mesmo (group_id, profile_id) quando
  -- existir. Sem isso, o INSERT colidiria com o índice único parcial de perfil
  -- (ou com o antigo `unique (group_id, profile_id)` em bancos não migrados).
  select * into v_existing
    from public.group_members
   where group_id = v_group.id and profile_id = v_uid
   order by joined_at desc
   limit 1
   for update;

  if found then
    update public.group_members
       set left_at   = null,
           slot      = 2,
           joined_at = now()
     where id = v_existing.id;
  else
    insert into public.group_members (group_id, profile_id, slot)
    values (v_group.id, v_uid, 2);
  end if;

  -- Par completo -> calcula pair_key canônico "uid_lo:uid_hi".
  -- NÃO existe agregado min()/max() para uuid no PostgreSQL (o 0001 usava e por
  -- isso o aceite estourava 42883 antes de gravar o pair_key). O cast para text
  -- é determinístico e preserva a ordem canônica do UUID (hex minúsculo).
  select min(profile_id::text)::uuid, max(profile_id::text)::uuid, count(*)
    into v_lo, v_hi, v_count
    from public.group_members
   where group_id = v_group.id and left_at is null;

  if v_count = 2 then
    update public.groups
       set pair_key = v_lo::text || ':' || v_hi::text
     where id = v_group.id
     returning * into v_group;
  end if;

  update public.group_invites
     set status = 'accepted', responded_at = now()
   where id = p_invite_id;

  return v_group;
end;
$$;

comment on function public.accept_invite(uuid) is 'Aceita convite: reativa/ocupa o slot 2, grava pair_key e marca accepted. Erra se grupo cheio/expirado.';


-- ------------------------------------------------------------- leave_group --
create or replace function public.leave_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid       uuid := auth.uid();
  v_group     public.groups;
  v_member    public.group_members;
  v_remaining integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  -- Ordem global: grupo -> membros -> convites.
  select * into v_group
    from public.groups
   where id = p_group_id
   for update;

  if not found then
    raise exception 'group_not_active';
  end if;

  -- Revalida o vínculo sob a trava (pode ter sido removido concorrentemente).
  select * into v_member
    from public.group_members
   where group_id = p_group_id
     and profile_id = v_uid
     and left_at is null
   for update;

  if not found then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  update public.group_members
     set left_at = now()
   where id = v_member.id;

  -- Grupo já dissolvido (legado): só fecha o vínculo e encerra.
  if v_group.status <> 'active' then
    return;
  end if;

  select count(*) into v_remaining
    from public.group_members
   where group_id = p_group_id and left_at is null;

  if v_remaining = 0 then
    -- Sem par -> dissolve o grupo (soft-delete) e libera o pair_key.
    update public.groups
       set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
     where id = p_group_id and status = 'active';

    -- Grupo dissolvido -> convites pendentes não podem mais ser aceitos.
    update public.group_invites
       set status = 'cancelled', responded_at = now()
     where group_id = p_group_id and status = 'pending';
  elsif v_remaining = 1 then
    -- Grupo segue ativo, mas não é mais um par -> limpa pair_key.
    update public.groups set pair_key = null where id = p_group_id and status = 'active';
  end if;
end;
$$;

comment on function public.leave_group(uuid) is 'Sai do grupo: seta left_at; dissolve se ficar sem membros (cancelando convites) e limpa pair_key se deixar de ser par.';


-- ---------------------------------------------------------- dissolve_group --
create or replace function public.dissolve_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid   uuid := auth.uid();
  v_group public.groups;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- Mantém o contrato: quem chama precisa ser membro ativo do grupo.
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  -- Trava o grupo primeiro (ordem global: grupo -> membros -> convites) para
  -- não competir com accept_invite/leave_group/remove_member concorrentes.
  select * into v_group
    from public.groups
   where id = p_group_id
   for update;

  if not found or v_group.status <> 'active' then
    raise exception 'group_not_active';
  end if;

  -- Revalida o vínculo sob a trava do grupo.
  if not exists (
    select 1
      from public.group_members
     where group_id = p_group_id
       and profile_id = v_uid
       and left_at is null
  ) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  update public.groups
     set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
   where id = p_group_id and status = 'active';

  -- Fecha TODOS os vínculos ativos, inclusive o de quem dissolveu. Sem isto o
  -- usuário ficava com `left_at is null` e era barrado por
  -- `already_in_active_group` ao aceitar/criar/entrar em outra roda.
  update public.group_members
     set left_at = now()
   where group_id = p_group_id and left_at is null;

  -- Convites pendentes de um grupo dissolvido nunca poderão ser aceitos.
  update public.group_invites
     set status = 'cancelled', responded_at = now()
   where group_id = p_group_id and status = 'pending';
end;
$$;

comment on function public.dissolve_group(uuid) is 'Dissolve (soft-delete) o grupo, fecha todos os vínculos ativos e cancela convites pendentes. Apenas membro ativo.';


-- ------------------------------------------------------ join_group_by_code --
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

  -- Um usuário participa de no máximo um grupo ATIVO por vez (mesma regra de
  -- create_group/accept_invite). O JOIN ignora vínculos legados de grupos
  -- dissolvidos.
  if exists (
    select 1
      from public.group_members gm
      join public.groups g on g.id = gm.group_id
     where gm.profile_id = v_uid
       and gm.left_at is null
       and g.status = 'active'
  ) then
    raise exception 'already_in_active_group' using errcode = '23505';
  end if;

  -- Resolve E TRAVA o grupo ANTES de qualquer leitura/escrita em convites.
  -- Esta é a ordem global (grupo -> membros -> convites): sem o `for update`
  -- aqui, o lock do convite era adquirido antes do lock do grupo e podia
  -- inverter a ordem em relação a dissolve_group/leave_group.
  select * into v_group
    from public.groups
   where invite_code = upper(trim(coalesce(p_code, '')))
     and status = 'active'
   for update;

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


-- ------------------------------------------------------------ remove_member --
-- Recriada aqui (o 0003 continua sendo a origem da feature) apenas para
-- respeitar a ordem global de locks: grupo -> membros. A versão do 0003 travava
-- a linha do membro antes de tocar no grupo, o que podia inverter a ordem em
-- relação a accept_invite/dissolve_group e gerar `deadlock detected`.
create or replace function public.remove_member(p_group_id uuid, p_profile_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid       uuid := auth.uid();
  v_group     public.groups;
  v_remover   public.group_members;
  v_target    public.group_members;
  v_remaining integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- Quem chama deve ser membro ativo do grupo (pré-checagem sem lock).
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  -- Ordem global: grupo primeiro.
  select * into v_group
    from public.groups
   where id = p_group_id
   for update;

  if not found or v_group.status <> 'active' then
    raise exception 'group_not_active';
  end if;

  -- Revalida sob a trava: quem chama precisa continuar sendo membro ativo.
  select * into v_remover
    from public.group_members
   where group_id = p_group_id
     and profile_id = v_uid
     and left_at is null
   for update;

  if not found then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  -- Apenas o criador (slot = 1) pode remover outro membro.
  if v_remover.slot <> 1 then
    raise exception 'not_group_creator' using errcode = '42501';
  end if;

  if p_profile_id = v_uid then
    raise exception 'cannot_remove_self' using errcode = 'P0001';
  end if;

  select * into v_target
    from public.group_members
   where group_id = p_group_id
     and profile_id = p_profile_id
     and left_at is null
   for update;

  if not found then
    raise exception 'member_not_found' using errcode = 'P0001';
  end if;

  if v_target.slot = 1 then
    raise exception 'cannot_remove_creator' using errcode = 'P0001';
  end if;

  update public.group_members
     set left_at = now()
   where id = v_target.id;

  select count(*) into v_remaining
    from public.group_members
   where group_id = p_group_id and left_at is null;

  if v_remaining = 0 then
    update public.groups
       set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
     where id = p_group_id and status = 'active';

    -- Grupo dissolvido -> convites pendentes não podem mais ser aceitos.
    update public.group_invites
       set status = 'cancelled', responded_at = now()
     where group_id = p_group_id and status = 'pending';
  elsif v_remaining = 1 then
    update public.groups set pair_key = null where id = p_group_id and status = 'active';
  end if;
end;
$$;

comment on function public.remove_member(uuid, uuid) is 'Remove um membro do grupo (apenas o criador slot 1), respeitando a ordem global de locks.';


-- ============================================================================
-- 04. GRANTS
-- ============================================================================
-- `create or replace function` preserva os grants, mas repetimos de forma
-- idempotente para o caso de o banco ter sido montado manualmente.
revoke all on function public.create_group()               from public;
revoke all on function public.accept_invite(uuid)          from public;
revoke all on function public.leave_group(uuid)            from public;
revoke all on function public.dissolve_group(uuid)         from public;
revoke all on function public.join_group_by_code(text)     from public;
revoke all on function public.remove_member(uuid, uuid)    from public;

grant execute on function public.create_group()            to authenticated;
grant execute on function public.accept_invite(uuid)       to authenticated;
grant execute on function public.leave_group(uuid)         to authenticated;
grant execute on function public.dissolve_group(uuid)      to authenticated;
grant execute on function public.join_group_by_code(text)  to authenticated;
grant execute on function public.remove_member(uuid, uuid) to authenticated;
