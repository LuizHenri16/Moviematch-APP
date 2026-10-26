# MovieMatch — Relatório de QA (Validação de Entrega)

**Data:** 2026-10-18  
**Ambiente:** Linux, JDK 17, Gradle 9.6.1, Android targetSdk 37  
**Commit base:** inspeção do tree de trabalho atual  
**Testes executados:** `:shared:testAndroidHostTest` — **97/98 passaram** (1 falha em *fixture* de teste, não em código de produção)

---

## 1. BUGS ENCONTRADOS (ordenados por severidade)

### 🔴 **CRÍTICO — `ProfileName.fromDisplayName` só reconhece "maria" ou "luiz"**
| Item | Detalhes |
|------|----------|
| **Arquivo:linha** | `shared/src/commonMain/kotlin/app/moviematch/domain/model/Models.kt:26-33` |
| **O que acontece** | `ProfileName.fromDisplayName(displayName)` faz `lowercase().contains("maria")` / `contains("luiz")`. Se o parceiro no banco se chamar "João", "Ana", "Carlos", etc., a função retorna `null`. |
| **Impacto real** | `SupabaseMovieRepository.resolveProfileId()` (linha 243-244) chama `profiles().firstOrNull { ProfileName.fromDisplayName(it.displayName) == profile }`. Se o nome do parceiro não for "maria"/"luiz", o `firstOrNull` retorna `null` → `saveRating` lança `MovieRepositoryException("Não foi possível identificar o perfil de …")` e a nota **não é gravada**. |
| **Passos de reprodução** | 1. Criar grupo com dois usuários reais cujos `display_name` não contenham "maria" nem "luiz" (ex.: "João" e "Ana").<br>2. Tentar salvar uma nota para o parceiro.<br>3. Observar o erro "Não foi possível identificar o perfil de Ana para salvar a nota." |
| **Evidência** | Código: `Models.kt:26-33` + `SupabaseMovieRepository.kt:243-244`. O `MockMovieRepository` funciona porque o seed fixo usa "Maria" e "Luiz" (`MockData.kt`), mas o repositório real falha com nomes reais. |
| **Sugestão de correção** | Opção A (recomendada): remover o `enum ProfileName` e resolver o `profile_id` diretamente pelo `id` do perfil (já disponível no `ContentItem.suggestedBy` ou via busca por `id` no `profiles()`).<br>Opção B: mudar `fromDisplayName` para buscar no banco (`profiles` table) o `id` correspondente ao `display_name` informado, em vez de *hardcode* de dois nomes. |

---

### 🟠 **ALTO — `group()` estoura exceção quando o usuário não tem grupo ativo**
| Item | Detalhes |
|------|----------|
| **Arquivo:linha** | `shared/src/commonMain/kotlin/app/moviematch/data/supabase/SupabaseMovieRepository.kt:73-80` |
| **O que acontece** | `override suspend fun group(): Group` faz `.firstOrNull() ?: throw MovieRepositoryException(NO_GROUP_MESSAGE)`. O contrato `MovieRepository.group()` **não** é `nullable` nem `Result`, então toda chamada sem grupo ativo crasha a corrotina. |
| **Impacto real** | `addToWatchList` (linha 129) chama `val groupId = group().id` **antes** de verificar se há grupo. Se o usuário estiver logado mas sem grupo (estado transitório entre login e `GroupScreen`), a adição à lista falha com "Nenhum grupo ativo encontrado para esta conta." em vez de guiar o usuário para criar/entrar na roda. |
| **Passos de reprodução** | 1. Login com conta nova (sem grupo).<br>2. Antes de passar pelo `GroupGate` (ex.: navegação direta ou race condition), chamar `addToWatchList`.<br>3. Exceção não tratada sobe para a UI. |
| **Evidência** | Teste `groupReadsTheActiveMembersAndThrowsWithoutOne` valida o comportamento atual, mas o contrato da interface não prevê falha — o `MockMovieRepository.group()` **nunca** falha (retorna seed fixo). |
| **Sugestão de correção** | Mudar `MovieRepository.group()` para `suspend fun group(): Group?` (nullable) e tratar o `null` nos callers (`addToWatchList`, `HomeViewModel`, etc.) redirecionando para o fluxo de grupo. Ou lançar uma exceção tipada `NoActiveGroupException` que a UI saiba capturar e navegar. |

---

### 🟡 **MÉDIO — Fixture de teste com JSON inválido (não é bug de produção)**
| Item | Detalhes |
|------|----------|
| **Arquivo:linha** | `shared/src/commonTest/kotlin/app/moviematch/data/supabase/GroupRepositoryTest.kt:296-300` |
| **O que acontece** | O teste `theDuplicatePendingInviteIsRecognisedByItsUniqueViolation` usa `errorBody(duplicateConstraint, "23505")` onde `duplicateConstraint` contém aspas **não escapadas** dentro da string JSON: `"message":"duplicate key value violates unique constraint "group_invites_pending_profile_uidx""` → o parser JSON do supabase-kt falha (`JsonDecodingException: Unexpected JSON token…`) e o erro cai no `fallback` genérico "Não foi possível enviar o convite." em vez da mensagem específica "Já existe um convite pendente para essa pessoa." |
| **Evidência** | Log do teste: `Could not decode {"code":"23505","message":"duplicate key value violates unique constraint "group_invites_pending_profile_uidx"","details":null,"hint":null} as PostgrestErrorResponse.` |
| **Sugestão de correção** | Escapar as aspas internas no fixture: `"""duplicate key value violates unique constraint \"group_invites_pending_profile_uidx\""""` ou usar JSON serializado programaticamente. **Não altera código de produção.** |

---

### 🟡 **MÉDIO — `contentItem(id)` retorna `null` silenciosamente para ID inexistente (comportamento correto, mas merece atenção)**
| Item | Detalhes |
|------|----------|
| **Arquivo:linha** | `SupabaseMovieRepository.kt:103-120` |
| **O que acontece** | `contentItem` tenta `sessions` → `decodeSingleOrNull` → se `null`, tenta `series_sessions` → `decodeSingleOrNull`. Se ambos vazios, retorna `null`. O contrato `MovieRepository.contentItem(id): ContentItem?` já prevê `null`, mas **nenhum caller verifica** o retorno (ex.: `MovieDetailsViewModel` assume que o item existe). |
| **Risco** | Se um deeplink ou navegação passar um ID inválido, a tela de detalhes receberá `null` e provavelmente crashará ou mostrará estado vazio sem mensagem. |
| **Sugestão** | Adicionar validação no `MovieDetailsViewModel` / `ContentSelectionStore` para tratar `null` como "não encontrado" e mostrar erro amigável. |

---

### 🟡 **MÉDIO — `randomPending` filtra por `WatchStatus.PENDING`; KDoc diz "ainda não assistido" (alinhado, mas termo impreciso)**
| Item | Detalhes |
|------|----------|
| **Arquivo:linha** | `SupabaseMovieRepository.kt:122-123` |
| **Verificação** | O enum `watch_status` no SQL tem apenas `PENDING` e `WATCHED`. O filtro `it.status == WatchStatus.PENDING` **está correto**. O KDoc "ainda não assistido" é sinônimo de `PENDING` — sem bug real, só nomenclatura. |
| **Teste** | `randomPendingPicksAQueuedTitleAndIsNullWhenEverythingWasWatched` passa. |

---

### 🟢 **BAIXO — Poster URL: `MovieMath.toMovie` e `SessionRow.toContentItem` ambos chamam `posterUrl(path)` com default `w500` (consistente)**
| Item | Detalhes |
|------|----------|
| **Verificação** | `MovieMath.posterUrl(path, size = "w500")` (linha 23) é usado por `toMovie` (linha 87) e por `SessionRow.toContentItem` (linha 194) / `SeriesSessionRow.toContentItem` (linha 226). `SearchResult` usa `w185` (thumbnail) via `TmdbRows.SEARCH_POSTER_SIZE`. Tudo consistente com o schema (`poster_path` cru no banco, URL montada no cliente). **Sem bug.** |

---

### 🟢 **BAIXO — `deleteContent` apaga só a sessão; ratings caem por cascade (confirmado no schema)**
| Item | Detalhes |
|------|----------|
| **Verificação** | `SupabaseMovieRepository.deleteContent` (linha 195-200) faz `delete` na tabela da sessão. Schema `0001_init.sql:344-353` define `ratings.session_id REFERENCES sessions(id) ON DELETE CASCADE` e `series_ratings.series_session_id REFERENCES series_sessions(id) ON DELETE CASCADE`. O catálogo `movies`/`series` **não** é apagado (correto: compartilhado entre grupos). Teste `deleteContentRemovesOnlyTheGroupSession` passa e afirma que a linha `movies` não é tocada. **Sem bug.** |

---

### 🟢 **BAIXO — Erro de rede/RLS vira mensagem de tela (tratamento existe)**
| Item | Detalhes |
|------|----------|
| **Verificação** | `SupabaseMovieRepository.guarded` / `insertGuarded` (linhas 262-283) capturam qualquer `Exception` e transformam em `MovieRepositoryException` com mensagem PT-BR. `GroupRepository.guarded` + `groupErrorMessage` (linhas 283-290) mapeia códigos estáveis do Postgres (`raise exception 'codigo'`) para cópia em português. Falhas de transporte (DNS, timeout, 401, 404, 429, 5xx) têm mensagens dedicadas em `TmdbClient.ktmdbError` (linhas 128-140). **Cobertura adequada.** |

---

## 2. O QUE FOI VALIDADO E ESTÁ OK ✅

| Área | Evidência |
|------|-----------|
| **Schema ↔ Código (Supabase)** | Todas as colunas, `on_conflict` (`session_id,profile_id` / `series_session_id,profile_id`), enums (`watch_status`, `group_status`, `invite_status`), FKs com cascade, RLS policies, RPCs (`create_group`, `invite_by_profile`, `invite_by_handle`, `accept_invite`, `decline_invite`, `leave_group`, `dissolve_group`, `resolve_invite_code`, `join_group_by_code`, `upsert_movie`, `upsert_series`, `search_profiles`) — conferidos contra `0001_init.sql` e `0002_group_join_by_code.sql`. |
| **Koin binding** | `dataModule` seleciona `SupabaseMovieRepository` quando `Secrets.hasSupabaseConfig`, senão `MockMovieRepository`. `GroupRepository` só registrado com Supabase. `TmdbClient` instanciado uma vez. `AuthStore` + `ProfileRepository` com session persistence via `SettingsSessionManager` (SharedPreferences/NSUserDefaults). |
| **Busca TMDb (AddMovieViewModel + TmdbClient)** | - Query vazia → lista vazia sem request (teste `searchDoesNotHitTheApiForABlankQuery`).<br>- Resposta vazia → lista vazia (`searchReturnsAnEmptyListWhenTmdbHasNoMatches`).<br>- 401 → "Não foi possível autenticar no TMDb…" (`unauthorizedSearchPointsAtTheApiKey`).<br>- 404 → "Conteúdo não encontrado no TMDb." (`missingDetailsReportANotFoundTitle`).<br>- Timeout/engine failure → "Não foi possível conectar ao TMDb. Verifique sua internet." (`aFailingEngineBecomesANetworkError`).<br>- Payload inválido → "O TMDb retornou uma resposta inválida." (`aBrokenPayloadBecomesAReadableError`).<br>- Mapeamento `SearchResult` com pôster `w185`, ano, `voteAverage` corretos (`searchMapsPosterUrlYearAndVoteAverage`). |
| **Navegação / Troca de telas (AppShell)** | - `enterTransition = ExitTransition = popEnter = popExit = None` (linhas 92-95).<br>- Cada destino pinta `Box(Modifier.fillMaxSize().background(Background))` (linha 181) — fundo opaco impede *bleed-through*.<br>- Bottom bar segue `visibleRoute` reportado pelo destino composto (`ReportVisibleRoute`, linhas 194-198), não pelo back stack — evita barra errada durante transição.<br>- `goToTab` usa `popUpTo(HOME){saveState=true}` + `launchSingleTop=true` + `restoreState=true` — abas mantêm scroll/estado. |
| **Login / Logout / Avatar / Sessão persistida** | - `AuthStore` observa `supabase.auth.sessionStatus` e troca `CurrentProfileStore` para perfil real do `profiles` (fallback para claims do token).<br>- `ProfileRepository.updateAvatarUrl` escreve em `profiles.avatar_url` via RLS `profiles_update_self`; sem Supabase, mantém só em memória.<br>- `SettingsScreen` mostra `GoogleAccountCard` com botão Entrar/Sair; `LoginScreen` usa `GoogleSignInProvider` (Credential Manager no Android).<br>- Sessão sobrevive a *process death* (armazenada em `Settings` com chave fixa `moviematch.session`). |
| **Grupo / Convite (GroupRepository + GroupViewModel + GroupScreen + App gate)** | - `activeGroup()` lê `groups` + `group_members` + `profiles` em 1 round-trip (RLS `groups_select_member` garante que só o próprio grupo aparece).<br>- `pendingInvites()` filtra `status=pending` ordenado `created_at desc`; RLS `group_invites_select_target_or_member` expõe só convites do usuário.<br>- `createGroup` → RPC `create_group` (slot 1, invite_code único).<br>- `joinGroupByCode` → RPC `join_group_by_code` (0002) que auto-cria convite *pending* p/ si mesmo e aceita (reusa `accept_invite`).<br>- `inviteByProfile` / `inviteByHandle` → RPCs respectivos com validações (self, already_member, group_full, profile_not_found).<br>- `acceptInvite` / `declineInvite` → RPCs com mapeamento de erros (expired, not_invited, not_pending, already_in_active_group, group_full, group_not_active).<br>- `searchProfiles` → RPC `search_profiles` (ilike, mínimo 2 chars, max 20).<br>- UI (`GroupScreen`) cobre: sem grupo (criar, entrar por código, convites pendentes), com grupo (pair card, invite code enquanto slot 2 livre, invite form por busca ou handle).<br>- Gate em `App.kt`: `SessionGate` → `GroupGate` → `AppShell`; transição por estado, não por navegação. |
| **Testes de contrato (MockEngine)** | 97 testes cobrem: mappers de linha (`SessionRowsTest`, `GroupRowsTest`, `TmdbRowsTest`, `ProfileRow` implícito), `SupabaseMovieRepository` (reads, writes, erros, duplicate key, fallback series), `GroupRepository` (todos RPCs + mapeamento de erros), `TmdbClient` (request shape, mapping, erros 401/404/429/5xx/network), `MovieMath` (todas regras puras). |

---

## 3. O QUE NÃO DEU PARA VALIDAR (e por quê)

| Item | Motivo |
|------|--------|
| **iOS Google Sign-In** | `GoogleSignIn.ios.kt` é stub documentado; falta SDK `GoogleSignIn-iOS` + binding C-interop + Mac para build. O resto (AuthStore, sessão em NSUserDefaults, engine HTTP Darwin) já é compartilhado. |
| **Realtime (supabase_realtime)** | Habilitado no schema (bloco final do 0001), mas não há código cliente consumindo a publicação ainda. |
| **Deep links / Navegação externa** | Não há `Intent filter` / `Universal Links` configurados; validação exigiria build + device. |
| **Performance / Carga** | Testes rodam em MockEngine (sem rede/latência real). Não há testes de carga nem *screenrecord* analisado nesta rodada. |
| **Acessibilidade (TalkBack / VoiceOver)** | Não verificado — requer execução no device com leitor de tela ativo. |
| **Tema escuro / Dinâmico** | `Theme.kt` usa `MaterialTheme` + `Color.kt` com `darkColorScheme`/`lightColorScheme`, mas não testado visualmente. |
| **Internacionalização (i18n)** | Strings em PT-BR hardcoded; `README` menciona que i18n será definido depois. |

---

## 4. RESUMO DE AÇÕES RECOMENDADAS (para o time de desenvolvimento)

| Prioridade | Ação | Arquivo(s) |
|------------|------|------------|
| **P0** | Corrigir `ProfileName.fromDisplayName` para resolver perfil por `id` (não por substring de nome) ou buscar no banco. | `Models.kt`, `SupabaseMovieRepository.kt` |
| **P0** | Tornar `MovieRepository.group()` nullable ou tipar exceção de "sem grupo"; tratar no `addToWatchList` e ViewModels. | `MovieRepository.kt`, `SupabaseMovieRepository.kt`, `AddMovieViewModel.kt`, `HomeViewModel.kt` |
| **P1** | Corrigir fixture de teste `theDuplicatePendingInviteIsRecognisedByItsUniqueViolation` (JSON com aspas escapadas). | `GroupRepositoryTest.kt` |
| **P1** | Adicionar guarda em `MovieDetailsViewModel` / `ContentSelectionStore` para `contentItem(id)` retornar `null`. | `MovieDetailsViewModel.kt`, `ContentSelectionStore.kt` |
| **P2** | Validar iOS Google Sign-In (integração nativa). | `GoogleSignIn.ios.kt`, `iosApp/` |
| **P2** | Implementar consumo de Realtime (convites, sessões, ratings). | Novo código em `presentation/` + `data/supabase/` |
| **P3** | Auditoria de acessibilidade + tema escuro + i18n. | `Theme.kt`, `strings.xml`, Compose semantics. |

---

## 5. ANEXO — Comando de build/teste usado

```bash
# Testes (host JVM, sem device)
JAVA_HOME=/home/luiz/Downloads/android-studio-rabbit1-linux/android-studio/jbr \
  ./gradlew :shared:testAndroidHostTest --console=plain --rerun-tasks

# Build Android (APK debug)
JAVA_HOME=/home/luiz/Downloads/android-studio-rabbit1-linux/android-studio/jbr \
  ./gradlew :androidApp:assembleDebug --console=plain
```

**Resultado dos testes:** 97 passed, 1 failed (fixture de teste — ver seção 1.3).  
**Build Android:** SUCCESS.