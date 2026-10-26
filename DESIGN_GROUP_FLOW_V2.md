# MovieMatch — DESIGN_GROUP_FLOW_V2

**Data:** 19/10/2026
**Autor:** Arquiteto de Software
**Status:** Implementado pelo @desenvolvedor e verificado (ver §9)
**Escopo:** fluxo de login + ciclo de vida do grupo ("roda") + arquitetura de estado compartilhado
**Substitui/complementa:** `DESIGN_GROUP_FLOW.md` (navegação) e `QA_REPORT.md` (bugs)

> Documento de **design/planejamento**. Nenhum arquivo de produção foi alterado para produzi-lo.
> Cada afirmação abaixo foi verificada nos arquivos citados (`arquivo:linha`).

---

## 1. Veredito do fluxo de login

**Está correto e é simples.** O encadeamento é:

```
App()  [App.kt:38]
 └─ if (Secrets.hasSupabaseConfig) SessionGate() else AppShell()   [App.kt:46-50]
      └─ SessionGate() [App.kt:63-72]
           ├─ SignedIn  -> GroupGate() -> AppShell()   [App.kt:67, 83-90]
           └─ (outros)  -> LoginScreen()
```

Pontos fortes:

- A transição é **por estado, não por navegação**: `AuthStore.state` muda para `SignedOut`, `SessionGate` troca `AppShell` por `LoginScreen` sozinho (`App.kt:67-71`). Logout em qualquer lugar não precisa de navegação.
- O modo offline é explícito: sem `.env` de Supabase, nem login nem gate rodam e o app abre em `AppShell` (`App.kt:48-50`), com `AuthStore` em `AuthState.Error` (`AuthStore.kt:56-58`).
- A sessão sobrevive a process death (persistida via `Settings`), então `SignedIn` já chega pronto no primeiro frame.

Riscos confirmados:

| # | Risco | Evidência |
|---|-------|-----------|
| L1 | **`GroupGate` é código morto.** Cria e coleta um `GroupViewModel` e sempre renderiza `AppShell()`. | `App.kt:83-90` |
| L2 | **Crash offline ao abrir Configurações/Roda.** `GroupRepository` só é registrado `if (Secrets.hasSupabaseConfig)` (`Koin.kt:69-71`), mas `SettingsViewModel` (`Koin.kt:111`) e `GroupViewModel` (`Koin.kt:113`) o consomem via `get()`. Sem config, o grafo Koin não resolve → `NoDefinitionFoundException` ao abrir `Routes.SETTINGS` ou `Routes.GROUP`. Como `LoginScreen`/rota GROUP são alcançáveis, é um crash real (não só teórico). | `Koin.kt:69-71,111,113` |
| L3 | Menor: `GroupGate` também dependia de `currentProfile.profile.id` para recarregar; essa recarga hoje vive no `init` do `GroupViewModel` (`GroupViewModel.kt:91-100`), que só existe enquanto a tela estiver montada. | `GroupViewModel.kt:91-100` |

**Decisão:** manter o par `SessionGate` → `AppShell`, **remover `GroupGate`** e resolver L2 no item 4 (GroupStore + interface + `getOrNull`).

---

## 2. Estados da tela de grupo

`GroupScreen` é estado-única (`GroupScreen.kt:129-164`): `isLoading` → `StateError` → `NoGroupContent` (`group == null`) → `WithGroupContent` (`group != null`). Abaixo, todos os estados que o **produto** exige, com ações por estado.

```
                        ┌────────────────────────────┐
                        │ S0  SEM SESSÃO             │
                        │ SessionGate -> LoginScreen │
                        └────────────────────────────┘
                                      │ login ok
                                      ▼
        ┌───────────────────────────────────────────────────────────────┐
        │ S1  SEM GRUPO                                                   │
        │  • Criar grupo (create_group)                                   │
        │  • Entrar com código (join_group_by_code)                       │
        │  • Convites pendentes?  ──► S2                                  │
        └───────────────────────────────────────────────────────────────┘
                                      │ aceitar convite / entrar código / criar
                                      ▼
        ┌───────────────────────────────────────────────────────────────┐
        │ S3  GRUPO COM 1 MEMBRO (vaga aberta, partner == null)           │
        │  • Você sozinho (slot 1, isCreator)  OU                         │
        │    você entrou por código e o outro saiu (slot 2, !isCreator)   │
        │  • Código do convite + formulário de convite                    │
        │  • [Sair da roda]  [Excluir/Dissolver]   ← TODOS                │
        │  • (sem "Remover parceiro": não há parceiro)                    │
        └───────────────────────────────────────────────────────────────┘
                                      │ convite aceito pela outra pessoa
                                      ▼
        ┌───────────────────────────────────────────────────────────────┐
        │ S4  GRUPO COMPLETO (par, partner != null)                       │
        │  • Par (avatares)                                               │
        │  • [Sair da roda]                         ← TODOS               │
        │  • [Excluir/Dissolver]                    ← TODOS               │
        │  • [Remover parceiro]                     ← SÓ criador (slot 1) │
        │  • (opcional) [Cancelar convite]          ← ver §8, fora escopo │
        └───────────────────────────────────────────────────────────────┘

        Ações que zeram o grupo (sair/dissolver) fazem o estado voltar para S1
        (na própria tela), "disponibilizando a criação de outro" — requisito do usuário.
```

### 2.1 Estado atual (defeitos) vs. alvo

- **S3 hoje não existe de verdade.** Em `WithGroupContent` (`GroupScreen.kt:334-394`) quando `partner == null` há `return` antecipado (`GroupScreen.kt:352-378`) e **nenhuma** ação de sair/excluir é renderizada. `GroupManagementCard` (`GroupScreen.kt:697-755`) só aparece quando `partner != null`. → **Defeito 2:** dono sozinho (ou quem entrou por código e ficou só) fica preso sem poder criar outro grupo.
- Alvo: **remover o `return`** de `GroupScreen.kt:377` e passar a renderizar `GroupManagementCard` (com "Remover parceiro" condicional) **em S3 e S4**.

### 2.2 Ações destrutivas exigem confirmação (Defeito 3)

Hoje `onLeaveGroup`/`onDissolveGroup`/`onRemoveMember` disparam direto (`GroupScreen.kt:727-753`). Alvo: diálogo de confirmação por ação, no mesmo padrão visual de `ConfirmDeleteDialog.kt` (`presentation/details/ConfirmDeleteDialog.kt:50-164`), promovido a um componente reutilizável (ver §5).

---

## 3. Matriz de permissões (alinhada ao backend)

Fonte: RPCs em `supabase/0001_init.sql`, `0002_group_join_by_code.sql`, `0003_remove_member.sql`.

| Ação (RPC) | Sem grupo | Membro slot 1 (criador) | Membro slot 2 | Convidado (convidado/pendente) | Erros estáveis → cópia PT-BR |
|---|---|---|---|---|---|
| `create_group` (`0001:675`) | ✅ | ❌ já tem grupo | ❌ já tem grupo | ❌ | `already_in_active_group` → "Você já está em uma roda ativa." |
| `join_group_by_code` (`0002:32`) | ✅ | ❌ | ❌ | ✅ (por código) | `invalid_invite_code`, `already_in_active_group`, `group_full`, `group_not_active`, `invite_expired`, `not_invited`, `invite_not_pending`, `already_a_member` |
| `invite_by_profile` (`0001:718`) | ❌ | ✅ | ✅ | ❌ | `not_a_member`, `cannot_invite_self`, `already_a_member`, `group_full`, `profile_not_found`, duplicado |
| `invite_by_handle` (`0001:761`) | ❌ | ✅ | ✅ | ❌ | `handle_required`, `cannot_invite_self`, `group_full`, `not_a_member` |
| `accept_invite` (`0001:804`) | ✅ | ❌ | ❌ | ✅ (alvo) | `invite_expired`, `not_invited`, `invite_not_pending`, `already_in_active_group`, `group_not_active`, `group_full`, `already_a_member` |
| `decline_invite` (`0001:907`) | ✅ | ❌ | ❌ | ✅ (alvo) | `invite_not_found_or_not_pending`, `invite_not_found` |
| `leave_group(p_group_id)` (`0001:946`) | ❌ | ✅ | ✅ | ❌ | `not_authenticated`, `not_a_member` |
| `dissolve_group(p_group_id)` (`0001:986`) | ❌ | ✅ | ✅ | ❌ | `not_authenticated`, `not_a_member`, `group_not_active` |
| `remove_member(p_group_id,p_profile_id)` (`0003:28`) | ❌ | ✅ (só se `p_profile_id` = slot 2) | ❌ não é criador | ❌ | `not_authenticated`, `not_a_member`, `not_group_creator`, `cannot_remove_self`, `member_not_found`, `cannot_remove_creator`, `group_not_active` |
| `search_profiles` (`0001:1257`) | ✅ | ✅ | ✅ | ✅ | (query < 2 chars → lista vazia) |

**Cópia PT-BR:** todas as chaves já existem em `GroupRepository.kt:310-330` (`not_group_creator`, `cannot_remove_self`, `member_not_found`, `cannot_remove_creator`, `group_not_active` etc.). **Nenhuma string nova é necessária** para sair/dissolver/remover.

**Regras de UI derivadas (espelham o backend, não o substituem):**

- `partner == null` (contador de membros = 1) ⇒ não renderiza "Remover parceiro".
- `isCreator == false` ⇒ não renderiza "Remover parceiro" (evita o `not_group_creator`).
- "Sair" e "Excluir/Dissolver" aparecem para **qualquer** membro ativo (S3 e S4).
- O backend é a autoridade final: a UI só evita o erro óbvio; qualquer corrida ainda cai na mensagem estável.

### 3.1 Identidade criador × membro (Defeito 6)

O domínio `Group` (`Models.kt:44-51`) não carrega identidade, e a UI (`GroupManagementCard`, `GroupScreen.kt:697-755`) mostra tudo para todos. O SQL já guarda `groups.created_by uuid not null` (`0001:73`) e impõe a regra em `remove_member` (`0003:54-57`).

**Correção:** expor `Group.createdBy: String?` e derivar `GroupOverview.isCreator = (group.createdBy == me.id)`. Caso importante: se o criador sair, `groups.created_by` continua apontando para ele, então quem ficou (slot 2) recebe `isCreator = false` — coerente com o backend (não pode remover; pode sair/dissolver).

---

## 4. Arquitetura: estado compartilhado do grupo

### 4.1 Decisão (recomendada): `GroupStore` app-scoped observado pelas duas telas

Extrair um **façade de estado** único, escopo aplicação, e fazer `SettingsScreen` e `GroupScreen` **observarem a mesma fonte**:

```kotlin
// presentation/group/GroupStore.kt  (novo)
class GroupStore(
    private val repository: GroupDataSource?,     // null quando não há Supabase configurado
    private val currentProfile: CurrentProfileStore,
    private val authStore: AuthStore,
) {
    /** Grupo ativo (Content(null) = sem grupo), espaço de erro tipado. */
    val groupState: StateFlow<UiState<Group?>>

    /** Convites pendentes do usuário quando ele NÃO tem grupo. */
    val invites: StateFlow<List<GroupInvite>>

    /** Recarrega grupo + convites. Chamado após toda mutação e em login/troca de perfil. */
    suspend fun refresh()

    // Mesma superfície do repository (leitura e escrita), para os ViewModels não falarem
    // direto com o data source e o modo offline ficar tratado num lugar só:
    suspend fun createGroup()
    suspend fun joinGroupByCode(code: String)
    suspend fun searchProfiles(query: String): List<ProfileSummary>
    suspend fun inviteByProfile(groupId: String, profileId: String)
    suspend fun inviteByHandle(groupId: String, handle: String)
    suspend fun acceptInvite(inviteId: String)
    suspend fun declineInvite(inviteId: String)
    suspend fun leaveGroup(groupId: String)
    suspend fun dissolveGroup(groupId: String)
    suspend fun removeMember(groupId: String, profileId: String)
}
```

- Registro em `appModule` (`Koin.kt:97-103`), **única instância** para todo o app.
- `GroupStore` observa `authStore.state` e `currentProfile.profile.id`: em `SignedIn`/troca de perfil → `refresh()`; fora de sessão → `Content(null)`.
- Os `ViewModel`s deixam de ter estado próprio de grupo:
  - `SettingsViewModel` remove `_groupState`/`loadGroup()` (`SettingsViewModel.kt:45-46,86-93`) e passa a repassar `store.groupState`.
  - `GroupViewModel` monta `GroupOverview` a partir de `store.groupState` + `store.invites`; após cada mutação chama `store.refresh()`.

### 4.2 Justificativa (por que não "recarregar no ON_RESUME")

- **Elimina a defasagem (Defeito 4) por construção.** Hoje `SettingsViewModel` carrega o grupo só no `init` (`SettingsViewModel.kt:64-67`); o `SettingsViewModel` fica vivo no `ViewModelStore` do `NavBackStackEntry` de `Settings` (`koinViewModel()`), então ao voltar de `Group` o `init` **não** roda de novo e o card "Minha roda" fica velho. Com uma `StateFlow` compartilhada, mudar o grupo em `GroupScreen` **empurra** o novo valor para o card em Settings sem nenhum reload manual — inclusive `SettingsScreen.kt:212-344` alterna sozinho entre "criar/entrar" e "código + parceiro".
- **Elimina o código morto (Defeito 5).** `GroupGate` (`App.kt:83-90`) sai; a recarga em login/troca de perfil passa a viver no `GroupStore` (fora do ciclo de vida de qualquer tela), então não há mais VM "coletado e descartado".
- **Resolve o crash offline (L2).** `GroupStore` recebe `repository: GroupDataSource?` via `getOrNull()` e traduz a ausência em estado/erro tipado, em vez de o grafo Koin explodir. Passa a ser seguro abrir `Routes.SETTINGS`/`Routes.GROUP` sem Supabase.
- **Testabilidade.** Com a interface `GroupDataSource` (§5), `GroupStore` e os `ViewModel`s testam com um fake em memória, sem `MockEngine`.

Alternativa considerada e **rejeitada**: recarregar no `ON_RESUME` de cada tela. Resolve o defeito 4 de forma frágil (depende de cada tela lembrar de chamar; duplica round-trips; não resolve o "offline crash" nem o código morto) e mantém duas cópias de estado divergentes.

### 4.3 Fluxo de dados alvo

```
AuthStore.state ─┐
                 ├─► GroupStore (app-scoped, single) ── groupState / invites ──┬─► SettingsViewModel ─► card "Minha roda"
CurrentProfile ──┘                    ▲                                          └─► GroupViewModel  ─► GroupScreen
                                      │ refresh() após cada mutação
                                      └──────────────── GroupStore.leaveGroup/dissolve/remove/invite/accept...
```

---

## 5. Plano por arquivo

### 5.1 Kotlin

#### `domain/GroupDataSource.kt` — **NOVO**
Interface espelhando a API pública de `GroupRepository` (`GroupRepository.kt:142-303`). Permite fake nos testes e `getOrNull()` no Koin.

```kotlin
interface GroupDataSource {
    suspend fun activeGroup(): Group?
    suspend fun pendingInvites(): List<GroupInvite>
    suspend fun createGroup()
    suspend fun joinGroupByCode(code: String)
    suspend fun searchProfiles(query: String): List<ProfileSummary>
    suspend fun inviteByProfile(groupId: String, profileId: String)
    suspend fun inviteByHandle(groupId: String, handle: String)
    suspend fun acceptInvite(inviteId: String)
    suspend fun declineInvite(inviteId: String)
    suspend fun leaveGroup(groupId: String)      // <- id obrigatório (Defeito 1)
    suspend fun dissolveGroup(groupId: String)   // <- id obrigatório (Defeito 1)
    suspend fun removeMember(groupId: String, profileId: String)
}
```

#### `data/supabase/GroupRepository.kt` — corrigir (Defeito 1)
- `class GroupRepository(...) : GroupDataSource` (`GroupRepository.kt:132`).
- **Assinaturas novas (id obrigatório):**
  ```kotlin
  suspend fun leaveGroup(groupId: String) {
      guarded("Não foi possível sair da roda.") {
          supabaseClient.postgrest.rpc(LEAVE_GROUP_FUNCTION, GroupIdParams(groupId))
      }
  }
  suspend fun dissolveGroup(groupId: String) {
      guarded("Não foi possível dissolver a roda.") {
          supabaseClient.postgrest.rpc(DISSOLVE_GROUP_FUNCTION, GroupIdParams(groupId))
      }
  }
  ```
  (hoje `GroupRepository.kt:270` e `:284` chamam `rpc(fn)` sem argumento; a assinatura SQL é `(p_group_id uuid)` em `0001:946,986`, então a chamada sempre falha.)
- **Novo payload privado** (junto dos outros em `GroupRepository.kt:81-106`):
  ```kotlin
  @Serializable
  private data class GroupIdParams(@SerialName("p_group_id") val groupId: String)
  ```
- `removeMember` (`GroupRepository.kt:299-303`) já envia `p_group_id`/`p_profile_id` — sem mudança.
- Contrato de erro: inalterado; `groupErrorMessage` (`GroupRepository.kt:341-371`) já cobre todos os identificadores.

#### `domain/model/Models.kt` — identidade (Defeito 6)
- `data class Group` (`Models.kt:44-51`): adicionar `val createdBy: String? = null`.

#### `data/supabase/GroupRows.kt` — propagar `created_by`
- `GROUP_SELECT` (`GroupRows.kt:26-28`): incluir `created_by` (ex.: `"id,created_at,invite_code,created_by,group_members(...)"`). Permitido pela policy `groups_select_member` (`0001:521-524`).
- `GroupRow` (`GroupRows.kt:40-45`): `@SerialName("created_by") val createdBy: String? = null`.
- `toGroup()` (`GroupRows.kt:54-72`): `createdBy = createdBy`.

#### `presentation/group/GroupStore.kt` — **NOVO** (item 4)
Implementação descrita em §4.1. Encapsula `repository: GroupDataSource?`; quando `null`, `refresh()` publica `UiState.Error("Supabase não configurado: ...")` e as escritas lançam `GroupRepositoryException`.

#### `presentation/group/GroupViewModel.kt` — corrigir (Defeitos 1, 2, 6)
- Construtor: `GroupViewModel(store: GroupStore, currentProfile: CurrentProfileStore, authStore: AuthStore)`.
- `GroupOverview` (`GroupViewModel.kt:36-41`): adicionar `val isCreator: Boolean = false`.
- `load()`/`refresh()` (`:103-108,277-279`): delegar a `store.refresh()` e derivar `overview()` de `store.groupState` + `store.invites`.
- `overview()` (`:281-291`): `group = store.groupState.value.dataOrNull()`, `partner = group?.members?.firstOrNull { it.id != me.id }`, `isCreator = group?.createdBy == me.id`.
- `onLeaveGroup()` (`:206-212`) e `onDissolveGroup()` (`:214-220`): passar o id:
  ```kotlin
  fun onLeaveGroup() {
      val groupId = stateGroupId() ?: return
      launchAction("Não foi possível sair da roda.") {
          authStore.sessionReady.firstOrNull { it } ?: return@launchAction
          store.leaveGroup(groupId)
      }
  }
  ```
  (idem `onDissolveGroup`). Após sucesso, `launchAction` já chama `refresh()` (`:260`) → `group == null` → tela volta a S1 sozinha.
- `onRemoveMember(profileId)` (`:222-229`): passa a usar `store.removeMember(groupId, profileId)`.
- Demais ações de escrita migram para `store.*` mantendo `launchAction`/mensagens atuais.
- Remover o `init { load() ... }` (`:91-100`): a recarga por login/troca de perfil agora é do `GroupStore`.

#### `presentation/settings/SettingsViewModel.kt` — corrigir (Defeito 4)
- Construtor: trocar `groupRepository: GroupRepository` por `groupStore: GroupStore` (`SettingsViewModel.kt:36`).
- Remover `_groupState`/`loadGroup()` (`:45-46,86-93`) e a chamada no `init` (`:66`).
- Expor `val groupState: StateFlow<UiState<Group?>> = groupStore.groupState`.

#### `presentation/group/GroupScreen.kt` — corrigir (Defeitos 2, 3)
- `WithGroupContent` (`:334-394`): **remover o `return` de `:377`**. Estrutura alvo:
  - `PairCard(me, partner)` sempre.
  - `if (partner != null) { "Roda completa" card }`.
  - `if (partner == null) { InviteCodeCard(code); InviteForm(...) }`.
  - `GroupManagementCard(...)` **sempre** (S3 e S4), com `isCreator` e `partner` opcional.
- `GroupManagementCard` (`:697-755`): receber `isCreator: Boolean`, `partner: Profile?`; renderizar "Remover parceiro" somente `if (isCreator && partner != null)`; "Sair" e "Excluir/Dissolver" sempre.
- **Confirmações (Defeito 3):** estado local `var pending by remember { mutableStateOf<GroupAction?>(null) }`; ao confirmar, dispara o callback correspondente. Assinatura dos callbacks inalterada (`onLeaveGroup`, `onDissolveGroup`, `onRemoveMember(id)`).

#### `presentation/components/ConfirmDialog.kt` — **NOVO** (Defeito 3)
Generalizar `ConfirmDeleteDialog.kt:50-164` para `ConfirmDialog(title, message, confirmLabel, danger: Boolean = true, busy: Boolean, onConfirm, onDismiss)`. Manter `ConfirmDeleteDialog` como wrapper fino (ou migrar a chamada em `MovieDetailsScreen`). Reuso evita copy/estilo divergentes.

#### `App.kt` — remover código morto (Defeito 5)
- Remover `GroupGate` (`App.kt:83-90`) e os imports `GroupScreen`, `GroupViewModel`, `koinViewModel`, `dataOrNull` quando não usados.
- `SessionGate` (`App.kt:63-72`): trocar `GroupGate()` por `AppShell()` no ramo `SignedIn`.

#### `di/Koin.kt` — wiring (Defeitos 1, 4, 5)
- `dataModule` (`Koin.kt:69-71`): registrar a interface condicionalmente:
  ```kotlin
  if (Secrets.hasSupabaseConfig) {
      single<GroupDataSource> { GroupRepository(supabaseClient = get()) }
  }
  ```
- `appModule` (`Koin.kt:97-103`): adicionar `single { GroupStore(repository = getOrNull(), currentProfile = get(), authStore = get()) }`.
- `viewModelModule` (`Koin.kt:106-114`):
  - `viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }` (mesma aridade; o 2º `get()` agora resolve `GroupStore`).
  - `viewModel { GroupViewModel(get(), get(), get()) }` (store, currentProfile, authStore).
- `AppShell.kt:132-142`: sem mudança de assinatura; a rota `Routes.GROUP` continua (`Routes.kt:18`, `AppShell.kt:139-141`).

### 5.2 P0 relacionados

#### P0-A — `MovieRepository.group()` tipado/nullable
- `MovieRepository.kt:21`: `suspend fun group(): Group?`.
- `SupabaseMovieRepository.kt:73-80`: retornar `null` (em vez de `throw`) quando não há grupo ativo; remover/renomear `NO_GROUP_MESSAGE` conforme uso.
- `addToWatchList` (`SupabaseMovieRepository.kt:127-148`): tratar `group() == null` **antes** de `group().id` (`:129`) lançando `MovieRepositoryException("Você ainda não tem uma roda. Crie ou entre em uma para adicionar títulos.")` — copy que a UI já exibe via `uiStateOf`/mensagens do `AddMovieViewModel`.
- `MockMovieRepository.kt:37`: continua retornando o seed (não-nulo) — sem mudança de comportamento offline.
- Ajustar testes existentes em `SupabaseMovieRepositoryTest.kt` (`groupReadsTheActiveMembersAndThrowsWithoutOne`).

#### P0-B — resolver perfil por `id` (não por substring de nome)
- Problema: `ProfileName.fromDisplayName` (`Models.kt:26-33`) só reconhece "maria"/"luiz"; `SupabaseMovieRepository.resolveProfileId` (`:243-244`) depende disso → `saveRating` falha com nomes reais (`:170-172`).
- **Escopo P1 (não bloqueia o fluxo de grupo):** trocar o parâmetro `ProfileName` por `profileId: String` em `saveRating` (`MovieRepository.kt:45-51`, `SupabaseMovieRepository.kt:162`, `MovieDetailsViewModel.kt:97`) e derivar o id do membro do grupo (`me.id` ou `partner.id`), eliminando `resolveProfileId`. `MovieMath`/`RatingDialog` continuam usando `ProfileName` apenas para exibição/mapeamento de scores — o mapeamento por nome pode ser mantido onde já é mock (`MovieMath.kt:60-62`).
- Alternativa mínima, se o time preferir menor difusão: manter `ProfileName` mas trocar `resolveProfileId` para casar contra `group().members` por posição (me x partner), com `fromDisplayName` como fallback.

---

## 6. Cobertura de teste

Depois de `GroupDataSource`, os testes de VM ficam triviais (fake em memória). `commonTest` já tem `kotlin.test`, `kotlinx-coroutines-test` e `turbine` (`shared/build.gradle.kts:153-158`).

### 6.1 `GroupRepositoryTest.kt` (MockEngine) — ampliar
Seguindo o harness de `:57-67`:

| Teste | O que verifica |
|---|---|
| `leaveGroupSendsTheGroupId` | body do `/rpc/leave_group` contém `"p_group_id":"group-1"` |
| `dissolveGroupSendsTheGroupId` | body do `/rpc/dissolve_group` contém `"p_group_id":"group-1"` |
| `removeMemberSendsGroupAndProfile` | `/rpc/remove_member` contém `p_group_id` e `p_profile_id` |
| `leaveDissolveAndRemoveMapEveryDocumentedError` | `not_a_member`, `not_group_creator`, `cannot_remove_self`, `member_not_found`, `cannot_remove_creator`, `group_not_active` → copy PT-BR (`GroupRepository.kt:310-330`) |
| (manter) `everyRaisedIdentifierOfTheFlowHasPortugueseCopy` (`:360`) | incluir os 4 identificadores novos na lista |

> Nota: o fixture de duplicidade (`GroupRepositoryTest.kt:296-306`) **já está** com aspas escapadas (`\\\"`), então o P1 do `QA_REPORT.md:36-42` está resolvido — não regredir.

### 6.2 `GroupStoreTest.kt` — **NOVO** (fake `GroupDataSource`)
- `refreshPublishesGroupAndInvitesWhenSignedIn`.
- `refreshPublishesNullGroupWhenThereIsNoActiveOne`.
- `offlineStorePublishesTypedErrorInsteadOfThrowing`.
- `leaveGroupThenRefreshFlipsStateToNoGroup` (fixa o Defeito 1 + a virada S3/S4→S1).

### 6.3 `GroupViewModelTest.kt` — **NOVO** (store + fake, `runTest` + Turbine)
- `leavingCallsStoreWithTheCurrentGroupId` (Defeito 1).
- `dissolvingCallsStoreWithTheCurrentGroupId`.
- `removingPartnerCallsStoreWithGroupAndProfile`.
- `overviewMarksIsCreatorFromCreatedBy` (Defeito 6).
- `aFailedActionBecomesAnErrorMessageAndKeepsTheGroup` (estado de erro tipado).

### 6.4 Casos de borda obrigatórios
- **Dono sozinho (S3):** `GroupScreen` renderiza Sair/Excluir; "Remover parceiro" **ausente** (Defeito 2).
- **Grupo cheio:** `inviteByProfile`/`acceptInvite` → "Esta roda já está completa." (`group_full`) e UI esconde o formulário de convite quando `partner != null`.
- **Convite expirado:** `acceptInvite` → "Este convite expirou. Peça um convite novo."
- **Sem sessão:** escrever com `authStore` fora de `SignedIn` → VM não chama o backend (`sessionReady` gate, `GroupViewModel.kt:119`); `not_authenticated` mapeado.
- **Criador saiu, membro slot 2 ficou:** `isCreator == false` → Sair/Excluir disponíveis; Remover ausente.

---

## 7. Checklist de aceite (verificável)

| # | Defeito / item | Critério de aceite | Como verificar |
|---|---|---|---|
| 1 | CRÍTICO: `leave_group`/`dissolve_group` sem `p_group_id` | Toda chamada envia `p_group_id`; "Sair da roda" e "Excluir/Dissolver" concluem sem erro de função inexistente | `GroupRepositoryTest.leaveGroupSendsTheGroupId` / `dissolveGroupSendsTheGroupId`; teste manual em S4 |
| 2 | CRÍTICO: dono sozinho sem ações | Em S3 (partner == null) a tela mostra Sair e Excluir/Dissolver; após usar, volta a S1 com "Criar grupo" | `GroupScreen.kt` sem `return` em S3; teste de VM/composição S3 |
| 3 | ALTO: ações destrutivas sem confirmação | Sair/Excluir/Remover abrem `ConfirmDialog`; só executam após confirmar | Inspeção + teste manual |
| 4 | ALTO: card "Minha roda" defasado | Sair/dissolver em `GroupScreen` atualiza o card em Settings sem reabrir nada | `GroupStoreTest` + teste manual Settings→Group→voltar |
| 5 | MÉDIO: `GroupGate` morto + crash offline | `App.kt` sem `GroupGate`; abrir Settings/Roda sem Supabase não crasha (estado de erro tipado) | Inspeção `App.kt:63-90`; `GroupStoreTest.offlineStorePublishesTypedError` |
| 6 | IDENTIDADE: criador x membro | `Group.createdBy` exposto; "Remover parceiro" só para `<creator && partner != null>`; Sair/Excluir para todos | `overviewMarksIsCreatorFromCreatedBy`; manual S3/S4 |
| P0-A | `group()` lança sem grupo | `group(): Group?`; `addToWatchList` sem grupo mostra copy guiando para criar/entrar, sem crash | ajuste em `SupabaseMovieRepositoryTest` |
| P0-B | nota do parceiro com nome real | `saveRating` resolve por `profileId`, não por substring de nome | teste de `saveRating` com ids arbitrários |
| — | Testes | `:shared:testAndroidHostTest` verde (novos testes inclusos) | `./gradlew :shared:testAndroidHostTest` |
| — | Sem regressão | `everyRaisedIdentifierOfTheFlowHasPortugueseCopy` cobre todos os identificadores | `GroupRepositoryTest.kt:360-388` |

---

## 8. Riscos, migrações e fora de escopo

### Migrações SQL
**Ideal: ZERO.** Todas as RPCs necessárias já existem e já recebem `p_group_id`:

- `leave_group(p_group_id uuid)` — `0001:946`
- `dissolve_group(p_group_id uuid)` — `0001:986`
- `remove_member(p_group_id uuid, p_profile_id uuid)` — `0003:28`

O defeito 1 é puramente **cliente** (payload faltando). Nenhum `ALTER`/`CREATE FUNCTION` é necessário. **Não** alterar `0001_init.sql` (cópia verbatim da referência) nem `0003`.

### Riscos
| Risco | Mitigação |
|---|---|
| A interface `GroupDataSource` + `GroupStore` mexem em assinaturas de VM e no Koin | Mudança mecânica; compilar `:shared` cedo; testes de VM com fake cobrem o wiring |
| `GroupStore` observando `authStore`/`currentProfile` pode gerar refresh duplicado (login dispara ambos) | `distinctUntilChanged()` em `profile.id` + guarda de `busy`/coalescing no `refresh()` |
| Regressão em `SettingsScreen` (compilação da nova `groupState`) | `SettingsScreen.kt:212-344` só consome `UiState<Group?>`; contrato preservado |
| `saveRating` por `profileId` (P0-B) é cross-cutting | Isolar como PR separado (P1); não bloqueia os defeitos 1–6 |
| Erros de corrida (remover parceiro que já saiu) | Mensagem estável já mapeada (`member_not_found`) cai como `GroupMessage` de erro |

### Fora de escopo (nesta entrega)
- **Cancelar convite enviado (outgoing):** o enum tem `cancelled` (`0001:120`) mas **não há RPC** para cancelar. Exigiria migração nova (`cancel_invite`) + UI de "convites enviados" (hoje `pendingInvites` é só o lado convidado, `GroupRepository.kt:158-166`). **Marcado opcional; recomendado adiar.**
- Realtime (convites/grupo) — `QA_REPORT.md:105`.
- Deep links / push de convite.
- iOS Google Sign-In (`GoogleSignIn.ios.kt` stub) — `QA_REPORT.md:104`.
- Redesenho visual do `GroupScreen` (apenas os defeitos funcionais entram aqui).
- i18n/acessibilidade/tema escuro.

### Ordem de execução sugerida (dependências)
1. `GroupDataSource` (interface) + `GroupRepository` implementando + fix de `leave/dissolve` (§5.1) ⟵ base.
2. `GroupStore` + Koin wiring + remoção do `GroupGate` (Defeitos 4, 5, L2).
3. `SettingsViewModel` migra para `GroupStore`.
4. `GroupViewModel` migra para `GroupStore` + `isCreator` (Defeito 6).
5. `GroupScreen` (remove `return`, ações em S3/S4) + `ConfirmDialog` (Defeitos 2, 3).
6. `Group.createdBy`/`GroupRows` (Defeito 6, leitura).
7. Testes (§6).
8. P0-A e P0-B (PRs seguintes, independentes).

---

## 9. Status da implementação (19/10/2026)

Todos os itens 1–6 do checklist e o P0-A foram implementados; **nenhuma migração SQL foi necessária**.

### Arquivos criados
- `domain/GroupDataSource.kt` — contrato consumido por `GroupStore`/testes.
- `presentation/group/GroupStore.kt` — fonte única de verdade, app-scoped.
- `presentation/components/ConfirmDialog.kt` — confirmação reutilizável.
- `commonTest/.../group/FakeGroupDataSource.kt`, `GroupStoreTest.kt`, `GroupViewModelTest.kt`.

### Arquivos alterados
`GroupRepository`, `GroupRows`, `SupabaseMovieRepository`, `MovieRepository`, `Models`,
`AuthStore`, `Koin`, `GroupViewModel`, `GroupScreen`, `ConfirmDeleteDialog`,
`SettingsViewModel`, `SettingsScreen`, `HomeViewModel`, `App`, `GroupRepositoryTest`,
`GroupRowsTest`, `SupabaseMovieRepositoryTest`.

### Verificação
```
JAVA_HOME=<android-studio>/jbr ./gradlew :shared:testAndroidHostTest --console=plain --rerun-tasks
BUILD SUCCESSFUL
tests=113 failures=0 errors=0 skipped=0
```
Apenas os 2 warnings pré-existentes (`GroupRepository.kt:358`, `SettingsScreen.kt:255`).

### Pendências conscientes (follow-up, fora desta entrega)
- **P0-B:** `saveRating` ainda resolve o autor por `ProfileName`/nome; migrar para `profileId`
  (o dashboard já usa os nomes reais do grupo em `MovieMath`/`HomeViewModel`).
- Cancelar convite enviado (exige RPC nova), realtime, deep links, iOS sign-in.

---

**Fim do documento.**
