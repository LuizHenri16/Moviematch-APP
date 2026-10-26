-- ============================================================================
-- MovieMatch (KMP) — 0001_init.sql
-- ============================================================================
-- CÓPIA VERBATIM do schema do app de referência
-- (`docs/native-app/supabase/0001_init.sql` do repositório pessoal).
--
-- DIVERGÊNCIAS: nenhuma. O backend do projeto Supabase já foi criado a partir
-- exatamente deste arquivo, por isso copiá-lo sem alterações garante que o que
-- está versionado aqui é o que está aplicado no servidor — não há nada novo a
-- reaplicar. O cabeçalho original logo abaixo continua sendo a documentação
-- válida da ordem de execução, das policies de RLS e do bloco de realtime.
--
-- O app nativo consome este schema via supabase-kt (`from()`/`rpc()`):
--   * leitura  -> movies, series, sessions, series_sessions, ratings,
--                 series_ratings, profiles, groups, group_members;
--   * escrita  -> sessions/series_sessions e ratings/series_ratings (RLS por
--                 grupo) e os RPCs upsert_movie()/upsert_series() (SECURITY
--                 DEFINER, única porta de escrita do catálogo global).
-- ============================================================================

-- ============================================================================
-- MovieMatch (KMP + Supabase novo) — Migração inicial de schema (0001_init.sql)
-- ============================================================================
-- PROPÓSITO
--   Schema completo do backend Supabase do novo app NATIVO (Kotlin Multiplatform).
--   Este arquivo é auto-suficiente: pode ser colado INTEIRO no SQL Editor do
--   Supabase (Dashboard > SQL Editor > New query > Run) ou executado como uma
--   migração única (supabase migration / psql -f). Ele é IDEMPOTENTE sempre que
--   possível (`create table if not exists`, `create or replace function`,
--   `drop policy if exists ...; create policy ...`, `do $$ ... exception ...`),
--   de modo que reexecutá-lo não quebra o banco.
--
-- ORDEM DE EXECUÇÃO (não reordene — dependências são resolvidas nesta sequência)
--   01. Extensões (pgcrypto / gen_random_uuid).
--   02. public.profiles  (+ índice lower(handle) + trigger updated_at).
--   03. Função/trigger handle_new_user() em auth.users (auto-provisão).
--   04. Catálogo global: public.movies e public.series.
--   05. Enum group_status + public.groups (+ índice parcial único pair_key).
--   06. public.group_members (+ uniques + índice).
--   07. Enum invite_status + public.group_invites (+ índices parciais).
--   08. Enum watch_status + public.sessions (+ índices).
--   09. public.ratings (+ índices).
--   10. Espelhos de séries: public.series_sessions e public.series_ratings.
--   11. Funções auxiliares de RLS: is_group_member(), shares_group_with().
--   12. GRANTs de tabela (privilégios base por papel).
--   13. ENABLE ROW LEVEL SECURITY em todas as tabelas de dados.
--   14. POLICIES de RLS (select/insert/update/delete) usando as funções acima.
--   15. RPCs (SECURITY DEFINER) que concentram as escritas sensíveis.
--   16. Habilitação de Realtime (bloco final, idempotente).
--
-- SEGURANÇA / CONVENÇÕES
--   * Toda escrita "de regra de negócio" passa por RPC SECURITY DEFINER
--     (revoke public / grant execute to authenticated). As RPCs rodam como o
--     dono das tabelas (postgres) e por isso NÃO sofrem RLS — é assim que
--     evitamos recursão de policies e concentramos validação/erros claros.
--   * As funções auxiliares usadas DENTRO das policies também são
--     SECURITY DEFINER (com `set search_path = public`), impedindo recursão
--     infinita de RLS ao consultarem group_members/groups.
--   * Identificadores, tabelas, colunas e RPCs em INGLÊS (como no plano);
--     comentários e mensagens em PT-BR.
--   * Datas sempre `timestamptz`.
--
-- REALTIME
--   O bloco final adiciona as tabelas relevantes à publicação
--   `supabase_realtime` (de forma idempotente) para que o app receba convites,
--   mudanças de membros, sessões e avaliações em tempo real.
-- ============================================================================


-- ============================================================================
-- 01. EXTENSÕES
-- ============================================================================
-- pgcrypto fornece gen_random_uuid() (e gen_random_bytes() usado nos códigos de
-- convite). Em projetos Supabase recentes a extensão já costuma estar ativa.
create extension if not exists pgcrypto;


-- ============================================================================
-- 02. PROFILES
-- ============================================================================
-- Espelho 1:1 de auth.users. É a identidade pública do usuário no app.
create table if not exists public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  handle       text unique,
  display_name text,
  avatar_url   text,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

comment on table  public.profiles            is 'Perfil público do usuário; espelha auth.users 1:1 (id = auth.uid()).';
comment on column public.profiles.id         is 'Mesmo UUID de auth.users.id (1:1, cascade na remoção da conta).';
comment on column public.profiles.handle     is 'Apelido único usado para busca/convite por handle (Fase 2).';
comment on column public.profiles.display_name is 'Nome exibido (default derivado do e-mail/metadata no signup).';
comment on column public.profiles.avatar_url is 'URL do avatar (muitas vezes vindo do provedor OAuth).';
comment on column public.profiles.updated_at is 'Atualizado automaticamente pelo trigger set_updated_at().';

-- Busca case-insensitive por handle.
create index if not exists profiles_handle_lower_idx
  on public.profiles (lower(handle));

-- Trigger genérico para manter updated_at coerente.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

comment on function public.set_updated_at() is 'Trigger BEFORE UPDATE genérico: carimba updated_at = now().';

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();


-- ============================================================================
-- 03. AUTO-PROVISÃO DE PROFILE (trigger em auth.users)
-- ============================================================================
-- Cria automaticamente a linha em public.profiles quando um usuário é criado.
-- SECURITY DEFINER + search_path fixo (evita hijack de schema).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name, avatar_url)
  values (
    new.id,
    coalesce(
      new.raw_user_meta_data ->> 'full_name',
      new.raw_user_meta_data ->> 'name',
      split_part(coalesce(new.email, ''), '@', 1)
    ),
    new.raw_user_meta_data ->> 'avatar_url'
  )
  on conflict (id) do nothing;   -- idempotente: nunca duplica perfil
  return new;
end;
$$;

comment on function public.handle_new_user() is 'Provisiona public.profiles após INSERT em auth.users (SECURITY DEFINER, idempotente).';

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();


-- ============================================================================
-- 04. CATÁLOGO GLOBAL (movies / series)
-- ============================================================================
-- Catálogo COMPARTILHADO por todos os usuários (não é por grupo). Espelha os
-- campos usados pelo app de referência. Escrita somente via RPC
-- upsert_movie()/upsert_series(); leitura liberada para authenticated.
create table if not exists public.movies (
  id              uuid primary key default gen_random_uuid(),
  tmdb_id         bigint unique,
  title           text not null,
  release_year    integer,
  runtime_minutes integer,
  overview        text,
  poster_path     text,
  main_genre      text,
  subgenres       text[],
  created_by      uuid references public.profiles (id) on delete set null,
  created_at      timestamptz not null default now()
);

comment on table  public.movies              is 'Catálogo global de filmes (compartilhado entre todos os grupos).';
comment on column public.movies.tmdb_id      is 'ID no TMDB — chave natural idempotente para upsert.';
comment on column public.movies.main_genre   is 'Gênero principal (rótulo do TMDB).';
comment on column public.movies.subgenres    is 'Gêneros secundários.';
comment on column public.movies.created_by   is 'Quem inseriu o registro (primeiro que "descobriu" o título).';

create table if not exists public.series (
  id                       uuid primary key default gen_random_uuid(),
  tmdb_id                  bigint unique,
  title                    text not null,
  release_year             integer,
  overview                 text,
  poster_path              text,
  main_genre               text,
  subgenres                text[],
  number_of_seasons        integer,
  number_of_episodes       integer,
  episode_run_time_minutes integer,
  created_by               uuid references public.profiles (id) on delete set null,
  created_at               timestamptz not null default now()
);

comment on table  public.series                            is 'Catálogo global de séries (espelho de movies para TV).';
comment on column public.series.number_of_seasons          is 'Quantidade de temporadas (TMDB).';
comment on column public.series.number_of_episodes         is 'Quantidade de episódios (TMDB).';
comment on column public.series.episode_run_time_minutes   is 'Duração típica de episódio em minutos (TMDB).';


-- ============================================================================
-- 05. GRUPOS
-- ============================================================================
-- Um grupo representa um PAR (no máx. 2 membros). Soft-delete via status.
do $$
begin
  create type public.group_status as enum ('active', 'dissolved');
exception
  when duplicate_object then null;   -- já existe
end $$;

comment on type public.group_status is 'Estado do grupo: active (em uso) ou dissolved (soft-delete).';

create table if not exists public.groups (
  id           uuid primary key default gen_random_uuid(),
  status       public.group_status not null default 'active',
  invite_code  text not null unique,
  pair_key     text,
  created_by   uuid not null references public.profiles (id),
  created_at   timestamptz not null default now(),
  dissolved_at timestamptz,
  dissolved_by uuid references public.profiles (id)
);

comment on table  public.groups              is 'Grupo (par de até 2 pessoas). Soft-delete via status=dissolved.';
comment on column public.groups.invite_code  is 'Código curto de convite (compartilhável) — único.';
comment on column public.groups.pair_key     is 'Chave canônica do par "uid_menor:uid_maior"; garante par único entre grupos ativos.';
comment on column public.groups.dissolved_at is 'Quando o grupo foi dissolvido (soft-delete).';
comment on column public.groups.dissolved_by is 'Quem dissolveu o grupo.';

-- Par único: o índice parcial só considera grupos ATIVOS com pair_key preenchido.
create unique index if not exists groups_active_pair_key
  on public.groups (pair_key)
  where status = 'active' and pair_key is not null;


-- ============================================================================
-- 06. MEMBROS DO GRUPO
-- ============================================================================
create table if not exists public.group_members (
  id         uuid primary key default gen_random_uuid(),
  group_id   uuid not null references public.groups (id) on delete cascade,
  profile_id uuid not null references public.profiles (id) on delete cascade,
  slot       smallint not null check (slot in (1, 2)),
  joined_at  timestamptz not null default now(),
  left_at    timestamptz,
  unique (group_id, slot),
  unique (group_id, profile_id)
);

comment on table  public.group_members           is 'Vínculo membro<->grupo. Máx. 2 membros por grupo (slot 1 e 2).';
comment on column public.group_members.slot      is 'Posição no par: 1 (criador) ou 2 (convite aceito).';
comment on column public.group_members.left_at   is 'Soft-delete do vínculo; NULL = membro ativo.';

-- Localizar rapidamente os grupos ATIVOS de um usuário.
create index if not exists group_members_active_profile_idx
  on public.group_members (profile_id)
  where left_at is null;


-- ============================================================================
-- 07. CONVITES
-- ============================================================================
do $$
begin
  create type public.invite_status as enum ('pending', 'accepted', 'declined', 'expired', 'cancelled');
exception
  when duplicate_object then null;
end $$;

comment on type public.invite_status is 'Estado do convite: pending -> accepted|declined|expired|cancelled.';

create table if not exists public.group_invites (
  id                 uuid primary key default gen_random_uuid(),
  group_id           uuid not null references public.groups (id) on delete cascade,
  invited_by         uuid not null references public.profiles (id),
  invited_profile_id uuid references public.profiles (id) on delete cascade,
  invitee_handle     text,
  status             public.invite_status not null default 'pending',
  created_at         timestamptz not null default now(),
  responded_at       timestamptz,
  expires_at         timestamptz not null default (now() + interval '7 days'),
  -- precisa haver um alvo: por perfil OU por handle
  constraint group_invites_target_chk
    check (invited_profile_id is not null or invitee_handle is not null)
);

comment on table  public.group_invites                    is 'Convites para entrada em um grupo (por perfil ou por handle).';
comment on column public.group_invites.invited_profile_id is 'Alvo já cadastrado (quando convidado por id/busca).';
comment on column public.group_invites.invitee_handle     is 'Alvo por handle (usuário pode ainda não existir).';
comment on column public.group_invites.expires_at         is 'Expira em 7 dias por padrão.';

-- Índices parciais: consultas quentes sobre convites PENDENTES.
create unique index if not exists group_invites_pending_profile_uidx
  on public.group_invites (group_id, invited_profile_id)
  where status = 'pending' and invited_profile_id is not null;

create index if not exists group_invites_pending_handle_idx
  on public.group_invites (lower(invitee_handle))
  where status = 'pending';


-- ============================================================================
-- 08. SESSÕES DE FILME (o "o que vamos assistir")
-- ============================================================================
do $$
begin
  create type public.watch_status as enum ('PENDING', 'WATCHED');
exception
  when duplicate_object then null;
end $$;

comment on type public.watch_status is 'Status de uma sessão: PENDING ou WATCHED (espelha os rótulos usados no app).';

create table if not exists public.sessions (
  id                 uuid primary key default gen_random_uuid(),
  group_id           uuid not null references public.groups (id) on delete cascade,
  movie_id           uuid not null references public.movies (id) on delete cascade,
  status             public.watch_status not null default 'PENDING',
  watched_at         timestamptz,
  streaming_provider text,
  sofa_comment       text,
  suggested_by       uuid references public.profiles (id) on delete set null,
  created_at         timestamptz not null default now(),
  unique (group_id, movie_id)
);

comment on table  public.sessions                    is 'Sessão de filme dentro de um grupo (1 filme por grupo).';
comment on column public.sessions.streaming_provider is 'Onde assistir (Netflix, Prime, etc.).';
comment on column public.sessions.sofa_comment       is 'Comentário pós-sessão ("comentário do sofá").';
comment on column public.sessions.suggested_by       is 'Quem sugeriu o filme.';

create index if not exists sessions_group_idx        on public.sessions (group_id);
create index if not exists sessions_movie_idx        on public.sessions (movie_id);
create index if not exists sessions_group_status_idx on public.sessions (group_id, status);


-- ============================================================================
-- 09. AVALIAÇÕES DE FILME
-- ============================================================================
create table if not exists public.ratings (
  id         uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.sessions (id) on delete cascade,
  group_id   uuid not null references public.groups (id) on delete cascade,
  profile_id uuid not null references public.profiles (id) on delete cascade,
  score      numeric(3,1) not null check (score >= 0 and score <= 10),
  comment    text,
  created_at timestamptz not null default now(),
  unique (session_id, profile_id)
);

comment on table  public.ratings            is 'Nota de um membro para uma sessão de filme (0.0 a 10.0).';
comment on column public.ratings.group_id   is 'Denormalizado a partir de sessions.group_id (facilita RLS/agregações).';
comment on column public.ratings.score      is 'Nota de 0.0 a 10.0 (numeric(3,1)).';

create index if not exists ratings_group_idx   on public.ratings (group_id);
create index if not exists ratings_profile_idx on public.ratings (profile_id);


-- ============================================================================
-- 10. ESPELHOS DE SÉRIES (series_sessions / series_ratings)
-- ============================================================================
create table if not exists public.series_sessions (
  id                 uuid primary key default gen_random_uuid(),
  group_id           uuid not null references public.groups (id) on delete cascade,
  series_id          uuid not null references public.series (id) on delete cascade,
  status             public.watch_status not null default 'PENDING',
  watched_at         timestamptz,
  streaming_provider text,
  sofa_comment       text,
  suggested_by       uuid references public.profiles (id) on delete set null,
  created_at         timestamptz not null default now(),
  unique (group_id, series_id)
);

comment on table public.series_sessions is 'Sessão de série dentro de um grupo (espelho de sessions).';

create index if not exists series_sessions_group_idx        on public.series_sessions (group_id);
create index if not exists series_sessions_series_idx       on public.series_sessions (series_id);
create index if not exists series_sessions_group_status_idx on public.series_sessions (group_id, status);

create table if not exists public.series_ratings (
  id                uuid primary key default gen_random_uuid(),
  series_session_id uuid not null references public.series_sessions (id) on delete cascade,
  group_id          uuid not null references public.groups (id) on delete cascade,
  profile_id        uuid not null references public.profiles (id) on delete cascade,
  score             numeric(3,1) not null check (score >= 0 and score <= 10),
  comment           text,
  created_at        timestamptz not null default now(),
  unique (series_session_id, profile_id)
);

comment on table public.series_ratings is 'Nota de um membro para uma sessão de série (espelho de ratings).';

create index if not exists series_ratings_group_idx   on public.series_ratings (group_id);
create index if not exists series_ratings_profile_idx on public.series_ratings (profile_id);


-- ============================================================================
-- 11. FUNÇÕES AUXILIARES DE RLS
-- ============================================================================
-- SECURITY DEFINER: rodam como o dono (postgres) e, por isso, NÃO sofrem RLS.
-- É isso que evita a recursão infinita ao referenciar group_members/groups
-- dentro das policies. search_path fixo evita schema hijack.
create or replace function public.is_group_member(p_group_id uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1
    from public.group_members gm
    join public.groups g on g.id = gm.group_id
    where gm.group_id  = p_group_id
      and gm.profile_id = auth.uid()
      and gm.left_at   is null
      and g.status      = 'active'   -- grupos dissolvidos ficam invisíveis
  );
$$;

comment on function public.is_group_member(uuid) is 'TRUE se auth.uid() é membro ativo de um grupo ATIVO. Usada nas policies (SECURITY DEFINER).';

create or replace function public.shares_group_with(p_profile_id uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1
    from public.group_members me
    join public.group_members other on other.group_id = me.group_id
    join public.groups g           on g.id = me.group_id
    where me.profile_id    = auth.uid()
      and me.left_at      is null
      and other.profile_id = p_profile_id
      and other.left_at   is null
      and g.status         = 'active'
  );
$$;

comment on function public.shares_group_with(uuid) is 'TRUE se auth.uid() e p_profile_id compartilham um grupo ativo (para ver perfis do par).';

-- Sem acesso por padrão; só authenticated executa.
revoke all on function public.is_group_member(uuid)  from public;
revoke all on function public.shares_group_with(uuid) from public;
grant execute on function public.is_group_member(uuid)  to authenticated;
grant execute on function public.shares_group_with(uuid) to authenticated;


-- ============================================================================
-- 12. GRANTs DE TABELA (privilégios base por papel)
-- ============================================================================
-- RLS continua sendo a barreira real; estes grants só definem o que cada papel
-- pode TENTAR. anon não acessa dados (app exige login).
revoke all on all tables in schema public from anon;

grant usage on schema public to anon, authenticated;

grant select on public.movies, public.series to authenticated;

grant select, insert, update, delete on public.profiles        to authenticated;
grant select, insert, update, delete on public.sessions        to authenticated;
grant select, insert, update, delete on public.ratings         to authenticated;
grant select, insert, update, delete on public.series_sessions to authenticated;
grant select, insert, update, delete on public.series_ratings  to authenticated;

-- Escrita em grupos/membros/convites ocorre SOMENTE via RPC (sem INSERT direto).
grant select on public.groups, public.group_members, public.group_invites to authenticated;


-- ============================================================================
-- 13. ROW LEVEL SECURITY (habilitar em TODAS as tabelas de dados)
-- ============================================================================
alter table public.profiles        enable row level security;
alter table public.movies          enable row level security;
alter table public.series          enable row level security;
alter table public.groups          enable row level security;
alter table public.group_members   enable row level security;
alter table public.group_invites   enable row level security;
alter table public.sessions        enable row level security;
alter table public.ratings         enable row level security;
alter table public.series_sessions enable row level security;
alter table public.series_ratings  enable row level security;


-- ============================================================================
-- 14. POLICIES DE RLS
-- ============================================================================
-- Regra geral: um usuário só "vê" dados de grupos dos quais é membro ativo, e
-- só vê/edita o próprio perfil (ou o perfil de quem compartilha grupo).
-- Escritas estruturais (grupo/membro/convite/catálogo) vêm de RPCs.

-- ---------------------------------------------------------------- profiles --
drop policy if exists profiles_select_self_or_pair on public.profiles;
create policy profiles_select_self_or_pair on public.profiles
  for select to authenticated
  using (id = auth.uid() or public.shares_group_with(id));

drop policy if exists profiles_insert_self on public.profiles;
create policy profiles_insert_self on public.profiles
  for insert to authenticated
  with check (id = auth.uid());

drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

comment on policy profiles_select_self_or_pair on public.profiles is 'Vê o próprio perfil e o do parceiro de grupo ativo.';

-- ------------------------------------------------------------------ groups --
-- Leitura apenas para membros ativos (convites/dissolução usam RPC).
drop policy if exists groups_select_member on public.groups;
create policy groups_select_member on public.groups
  for select to authenticated
  using (public.is_group_member(id));

drop policy if exists groups_update_member on public.groups;
create policy groups_update_member on public.groups
  for update to authenticated
  using (public.is_group_member(id))
  with check (public.is_group_member(id));

comment on policy groups_select_member on public.groups is 'Membros ativos leem o grupo; sem INSERT/DELETE direto (RPC-only).';

-- ----------------------------------------------------------- group_members --
drop policy if exists group_members_select_member on public.group_members;
create policy group_members_select_member on public.group_members
  for select to authenticated
  using (public.is_group_member(group_id) or profile_id = auth.uid());

comment on policy group_members_select_member on public.group_members is 'Vê os membros do próprio grupo (ou a própria linha). Escrita só via RPC.';

-- ---------------------------------------------------------- group_invites --
-- O convidado (por id OU por handle) e os membros do grupo enxergam o convite.
drop policy if exists group_invites_select_target_or_member on public.group_invites;
create policy group_invites_select_target_or_member on public.group_invites
  for select to authenticated
  using (
    invited_profile_id = auth.uid()
    or public.is_group_member(group_id)
    or (
      invitee_handle is not null
      and exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and p.handle is not null
          and lower(p.handle) = lower(invitee_handle)
      )
    )
  );

comment on policy group_invites_select_target_or_member on public.group_invites is 'Convidado e membros do grupo leem o convite. Escrita só via RPC.';

-- ----------------------------------------------------- catálogo (global) --
-- Leitura para qualquer autenticado; NENHUMA policy de escrita (só RPC).
drop policy if exists movies_select_authenticated on public.movies;
create policy movies_select_authenticated on public.movies
  for select to authenticated
  using (true);

drop policy if exists series_select_authenticated on public.series;
create policy series_select_authenticated on public.series
  for select to authenticated
  using (true);

comment on policy movies_select_authenticated on public.movies is 'Catálogo global: leitura para autenticados; escrita só via upsert_movie().';
comment on policy series_select_authenticated on public.series is 'Catálogo global: leitura para autenticados; escrita só via upsert_series().';

-- ---------------------------------------------------------------- sessions --
drop policy if exists sessions_select_member on public.sessions;
create policy sessions_select_member on public.sessions
  for select to authenticated
  using (public.is_group_member(group_id));

drop policy if exists sessions_insert_member on public.sessions;
create policy sessions_insert_member on public.sessions
  for insert to authenticated
  with check (public.is_group_member(group_id));

drop policy if exists sessions_update_member on public.sessions;
create policy sessions_update_member on public.sessions
  for update to authenticated
  using (public.is_group_member(group_id))
  with check (public.is_group_member(group_id));

drop policy if exists sessions_delete_member on public.sessions;
create policy sessions_delete_member on public.sessions
  for delete to authenticated
  using (public.is_group_member(group_id));

-- ----------------------------------------------------------------- ratings --
drop policy if exists ratings_select_member on public.ratings;
create policy ratings_select_member on public.ratings
  for select to authenticated
  using (public.is_group_member(group_id));

drop policy if exists ratings_insert_own on public.ratings;
create policy ratings_insert_own on public.ratings
  for insert to authenticated
  with check (profile_id = auth.uid() and public.is_group_member(group_id));

drop policy if exists ratings_update_own on public.ratings;
create policy ratings_update_own on public.ratings
  for update to authenticated
  using (profile_id = auth.uid())
  with check (profile_id = auth.uid());

drop policy if exists ratings_delete_own on public.ratings;
create policy ratings_delete_own on public.ratings
  for delete to authenticated
  using (profile_id = auth.uid());

-- --------------------------------------------------------- series_sessions --
drop policy if exists series_sessions_select_member on public.series_sessions;
create policy series_sessions_select_member on public.series_sessions
  for select to authenticated
  using (public.is_group_member(group_id));

drop policy if exists series_sessions_insert_member on public.series_sessions;
create policy series_sessions_insert_member on public.series_sessions
  for insert to authenticated
  with check (public.is_group_member(group_id));

drop policy if exists series_sessions_update_member on public.series_sessions;
create policy series_sessions_update_member on public.series_sessions
  for update to authenticated
  using (public.is_group_member(group_id))
  with check (public.is_group_member(group_id));

drop policy if exists series_sessions_delete_member on public.series_sessions;
create policy series_sessions_delete_member on public.series_sessions
  for delete to authenticated
  using (public.is_group_member(group_id));

-- ---------------------------------------------------------- series_ratings --
drop policy if exists series_ratings_select_member on public.series_ratings;
create policy series_ratings_select_member on public.series_ratings
  for select to authenticated
  using (public.is_group_member(group_id));

drop policy if exists series_ratings_insert_own on public.series_ratings;
create policy series_ratings_insert_own on public.series_ratings
  for insert to authenticated
  with check (profile_id = auth.uid() and public.is_group_member(group_id));

drop policy if exists series_ratings_update_own on public.series_ratings;
create policy series_ratings_update_own on public.series_ratings
  for update to authenticated
  using (profile_id = auth.uid())
  with check (profile_id = auth.uid());

drop policy if exists series_ratings_delete_own on public.series_ratings;
create policy series_ratings_delete_own on public.series_ratings
  for delete to authenticated
  using (profile_id = auth.uid());


-- ============================================================================
-- 15. RPCs (SECURITY DEFINER) — escritas de negócio
-- ============================================================================
-- Todas fazem `set search_path = public`, validam autorização internamente e
-- lançam exceções com nomes estáveis (o app mapeia para mensagens de UI).

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

  -- Um usuário participa de no máximo um grupo ativo por vez.
  if exists (
    select 1 from public.group_members
    where profile_id = v_uid and left_at is null
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

-- -------------------------------------------------------- invite_by_profile --
create or replace function public.invite_by_profile(p_group_id uuid, p_profile_id uuid)
returns public.group_invites
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid    uuid := auth.uid();
  v_invite public.group_invites;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;
  if p_profile_id = v_uid then
    raise exception 'cannot_invite_self';
  end if;
  if not exists (select 1 from public.profiles where id = p_profile_id) then
    raise exception 'profile_not_found';
  end if;
  if exists (
    select 1 from public.group_members
    where group_id = p_group_id and profile_id = p_profile_id and left_at is null
  ) then
    raise exception 'already_a_member';
  end if;
  if (select count(*) from public.group_members where group_id = p_group_id and left_at is null) >= 2 then
    raise exception 'group_full' using errcode = '23505';
  end if;

  insert into public.group_invites (group_id, invited_by, invited_profile_id, status)
  values (p_group_id, v_uid, p_profile_id, 'pending')
  returning * into v_invite;

  return v_invite;
end;
$$;

comment on function public.invite_by_profile(uuid, uuid) is 'Convida um perfil por ID (cria convite pending). Só membro do grupo.';

-- --------------------------------------------------------- invite_by_handle --
create or replace function public.invite_by_handle(p_group_id uuid, p_handle text)
returns public.group_invites
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid    uuid := auth.uid();
  v_handle text := lower(trim(coalesce(p_handle, '')));
  v_target uuid;
  v_invite public.group_invites;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;
  if v_handle = '' then
    raise exception 'handle_required';
  end if;
  if (select count(*) from public.group_members where group_id = p_group_id and left_at is null) >= 2 then
    raise exception 'group_full' using errcode = '23505';
  end if;

  -- Alvo pode já existir (fica com invited_profile_id) ou não (só handle).
  select id into v_target from public.profiles where lower(handle) = v_handle limit 1;

  if v_target = v_uid then
    raise exception 'cannot_invite_self';
  end if;

  insert into public.group_invites (group_id, invited_by, invited_profile_id, invitee_handle, status)
  values (p_group_id, v_uid, v_target, v_handle, 'pending')
  returning * into v_invite;

  return v_invite;
end;
$$;

comment on function public.invite_by_handle(uuid, text) is 'Convida por handle (alvo pode ainda não ter conta). Só membro do grupo.';

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
  v_lo         uuid;
  v_hi         uuid;
  v_count      integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select * into v_invite from public.group_invites where id = p_invite_id for update;
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
    update public.group_invites
       set status = 'expired', responded_at = now()
     where id = p_invite_id;
    raise exception 'invite_expired';
  end if;

  -- Um usuário participa de no máximo um grupo ativo por vez.
  if exists (
    select 1 from public.group_members
    where profile_id = v_uid and left_at is null
  ) then
    raise exception 'already_in_active_group' using errcode = '23505';
  end if;

  select * into v_group from public.groups where id = v_invite.group_id for update;
  if v_group.status <> 'active' then
    raise exception 'group_not_active';
  end if;

  -- Slot 2 precisa estar livre.
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

  insert into public.group_members (group_id, profile_id, slot)
  values (v_group.id, v_uid, 2);

  -- Par completo -> calcula pair_key canônico "uid_lo:uid_hi".
  select min(profile_id), max(profile_id), count(*)
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

comment on function public.accept_invite(uuid) is 'Aceita convite: ocupa slot 2, grava pair_key e marca accepted. Erra se grupo cheio/expirado.';

-- ---------------------------------------------------------- decline_invite --
create or replace function public.decline_invite(p_invite_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  update public.group_invites
     set status = 'declined', responded_at = now()
   where id = p_invite_id
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
     );

  if not found then
    raise exception 'invite_not_found_or_not_pending';
  end if;
end;
$$;

comment on function public.decline_invite(uuid) is 'Marca o convite como declined (apenas o alvo autorizado).';

-- ------------------------------------------------------------- leave_group --
create or replace function public.leave_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid       uuid := auth.uid();
  v_remaining integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  update public.group_members
     set left_at = now()
   where group_id = p_group_id and profile_id = v_uid and left_at is null;

  select count(*) into v_remaining
    from public.group_members
   where group_id = p_group_id and left_at is null;

  if v_remaining = 0 then
    -- Sem par -> dissolve o grupo (soft-delete) e libera o pair_key.
    update public.groups
       set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
     where id = p_group_id and status = 'active';
  elsif v_remaining = 1 then
    -- Grupo segue ativo, mas não é mais um par -> limpa pair_key.
    update public.groups set pair_key = null where id = p_group_id and status = 'active';
  end if;
end;
$$;

comment on function public.leave_group(uuid) is 'Sai do grupo: seta left_at; dissolve se ficar sem membros e limpa pair_key se deixar de ser par.';

-- ---------------------------------------------------------- dissolve_group --
create or replace function public.dissolve_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'not_a_member' using errcode = '42501';
  end if;

  update public.groups
     set status = 'dissolved', dissolved_at = now(), dissolved_by = v_uid, pair_key = null
   where id = p_group_id and status = 'active';

  if not found then
    raise exception 'group_not_active';
  end if;
end;
$$;

comment on function public.dissolve_group(uuid) is 'Dissolve (soft-delete) o grupo. Apenas membro ativo. Dados preservados, invisíveis por RLS.';

-- ------------------------------------------------------ resolve_invite_code --
create or replace function public.resolve_invite_code(p_code text)
returns public.groups
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

  select * into v_group
    from public.groups
   where invite_code = upper(trim(coalesce(p_code, '')))
     and status = 'active';

  if not found then
    raise exception 'invalid_invite_code';
  end if;

  return v_group;
end;
$$;

comment on function public.resolve_invite_code(text) is 'Resolve um invite_code ativo em seu grupo (para convite por código).';

-- ----------------------------------------------------------- join_group_by_code --
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

-- ------------------------------------------------------------- upsert_movie --
create or replace function public.upsert_movie(p_tmdb jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid     uuid := auth.uid();
  v_tmdb_id bigint;
  v_id      uuid;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  v_tmdb_id := nullif(p_tmdb ->> 'tmdb_id', '')::bigint;
  if v_tmdb_id is null then
    raise exception 'tmdb_id_required';
  end if;

  insert into public.movies (
    tmdb_id, title, release_year, runtime_minutes, overview, poster_path,
    main_genre, subgenres, created_by
  )
  values (
    v_tmdb_id,
    coalesce(nullif(p_tmdb ->> 'title', ''), 'Sem título'),
    nullif(p_tmdb ->> 'release_year', '')::integer,
    nullif(p_tmdb ->> 'runtime_minutes', '')::integer,
    p_tmdb ->> 'overview',
    p_tmdb ->> 'poster_path',
    coalesce(nullif(p_tmdb ->> 'main_genre', ''), 'Outro'),
    case
      when jsonb_typeof(p_tmdb -> 'subgenres') = 'array'
        then array(select jsonb_array_elements_text(p_tmdb -> 'subgenres'))
      else null
    end,
    v_uid
  )
  on conflict (tmdb_id) do update set
    title           = excluded.title,
    release_year    = excluded.release_year,
    runtime_minutes = excluded.runtime_minutes,
    overview        = excluded.overview,
    poster_path     = excluded.poster_path,
    main_genre      = excluded.main_genre,
    subgenres       = excluded.subgenres
  returning id into v_id;

  return v_id;
end;
$$;

comment on function public.upsert_movie(jsonb) is 'Find-or-create idempotente de filme por tmdb_id. Retorna o UUID.';

-- ------------------------------------------------------------ upsert_series --
create or replace function public.upsert_series(p_tmdb jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid     uuid := auth.uid();
  v_tmdb_id bigint;
  v_id      uuid;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  v_tmdb_id := nullif(p_tmdb ->> 'tmdb_id', '')::bigint;
  if v_tmdb_id is null then
    raise exception 'tmdb_id_required';
  end if;

  insert into public.series (
    tmdb_id, title, release_year, overview, poster_path, main_genre, subgenres,
    number_of_seasons, number_of_episodes, episode_run_time_minutes, created_by
  )
  values (
    v_tmdb_id,
    coalesce(nullif(p_tmdb ->> 'title', ''), 'Sem título'),
    nullif(p_tmdb ->> 'release_year', '')::integer,
    p_tmdb ->> 'overview',
    p_tmdb ->> 'poster_path',
    coalesce(nullif(p_tmdb ->> 'main_genre', ''), 'Outro'),
    case
      when jsonb_typeof(p_tmdb -> 'subgenres') = 'array'
        then array(select jsonb_array_elements_text(p_tmdb -> 'subgenres'))
      else null
    end,
    nullif(p_tmdb ->> 'number_of_seasons', '')::integer,
    nullif(p_tmdb ->> 'number_of_episodes', '')::integer,
    nullif(p_tmdb ->> 'episode_run_time_minutes', '')::integer,
    v_uid
  )
  on conflict (tmdb_id) do update set
    title                    = excluded.title,
    release_year             = excluded.release_year,
    overview                 = excluded.overview,
    poster_path              = excluded.poster_path,
    main_genre               = excluded.main_genre,
    subgenres                = excluded.subgenres,
    number_of_seasons        = excluded.number_of_seasons,
    number_of_episodes       = excluded.number_of_episodes,
    episode_run_time_minutes = excluded.episode_run_time_minutes
  returning id into v_id;

  return v_id;
end;
$$;

comment on function public.upsert_series(jsonb) is 'Find-or-create idempotente de série por tmdb_id. Retorna o UUID.';

-- ----------------------------------------------------------- search_profiles --
-- Fase 2: busca por handle/display_name (ilike, limitada) para convidar por ID.
create or replace function public.search_profiles(p_query text)
returns table (id uuid, handle text, display_name text, avatar_url text)
language sql
security definer
set search_path = public
stable
as $$
  select p.id, p.handle, p.display_name, p.avatar_url
  from public.profiles p
  where auth.uid() is not null
    and p.id <> auth.uid()
    and length(coalesce(trim(p_query), '')) >= 2
    and (
      p.handle ilike '%' || trim(p_query) || '%'
      or p.display_name ilike '%' || trim(p_query) || '%'
    )
  order by p.display_name nulls last
  limit 20;
$$;

comment on function public.search_profiles(text) is 'Busca perfis (por handle/display_name, ilike, máx 20) para convidar.';

-- ----------------------------------------------------------- RPC grants -----
revoke all on function public.create_group()                        from public;
revoke all on function public.invite_by_profile(uuid, uuid)         from public;
revoke all on function public.invite_by_handle(uuid, text)          from public;
revoke all on function public.accept_invite(uuid)                   from public;
revoke all on function public.decline_invite(uuid)                  from public;
revoke all on function public.leave_group(uuid)                     from public;
revoke all on function public.dissolve_group(uuid)                  from public;
revoke all on function public.resolve_invite_code(text)             from public;
revoke all on function public.join_group_by_code(text)              from public;
revoke all on function public.upsert_movie(jsonb)                   from public;
revoke all on function public.upsert_series(jsonb)                  from public;
revoke all on function public.search_profiles(text)                 from public;

grant execute on function public.create_group()                 to authenticated;
grant execute on function public.invite_by_profile(uuid, uuid)  to authenticated;
grant execute on function public.invite_by_handle(uuid, text)   to authenticated;
grant execute on function public.accept_invite(uuid)            to authenticated;
grant execute on function public.decline_invite(uuid)           to authenticated;
grant execute on function public.leave_group(uuid)              to authenticated;
grant execute on function public.dissolve_group(uuid)           to authenticated;
grant execute on function public.resolve_invite_code(text)      to authenticated;
grant execute on function public.join_group_by_code(text)       to authenticated;
grant execute on function public.upsert_movie(jsonb)            to authenticated;
grant execute on function public.upsert_series(jsonb)           to authenticated;
grant execute on function public.search_profiles(text)          to authenticated;


-- ============================================================================
-- 16. REALTIME
-- ============================================================================
-- Adiciona as tabelas relevantes à publicação supabase_realtime de forma
-- IDEMPOTENTE (rodar o script duas vezes não gera erro de "already member").
-- replica identity full garante payload completo em UPDATE/DELETE.
do $$
declare
  t text;
begin
  foreach t in array array[
    'groups', 'group_members', 'group_invites',
    'sessions', 'ratings', 'series_sessions', 'series_ratings'
  ]
  loop
    if not exists (
      select 1
      from pg_publication_tables
      where pubname = 'supabase_realtime'
        and schemaname = 'public'
        and tablename = t
    ) then
      execute format('alter publication supabase_realtime add table public.%I', t);
    end if;
  end loop;
end $$;

alter table public.groups        replica identity full;
alter table public.group_members replica identity full;
alter table public.group_invites replica identity full;

-- ============================================================================
-- FIM — 0001_init.sql
-- ============================================================================
