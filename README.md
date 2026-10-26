# MovieMatch — Kotlin Multiplatform + Compose Multiplatform

> **Arquitetura atual:** consulte [`ARCHITECTURE.md`](ARCHITECTURE.md) como fonte única.

App Android + iOS compartilhando UI e regras de negócio em Kotlin (Compose Multiplatform).
Este repositório contém **apenas a configuração inicial** (esqueleto que compila e roda) —
as funcionalidades de negócio (login com Google/Supabase, grupos de 2 pessoas, sessões e
avaliações) entram nas próximas etapas.

> O contrato de dados planejado está documentado em `docs/native-app/` do repositório do app
> pessoal (`ER-DIAGRAM.md` e `supabase/0001_init.sql`). O backend **não** faz parte deste repo.

---

## Estrutura

```
MoviematchApp/
├── settings.gradle.kts            # inclui :shared e :androidApp
├── build.gradle.kts               # plugins declarados (apply false)
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml         # version catalog — fonte única de versões
│   └── wrapper/                   # Gradle wrapper 9.6.1
├── androidApp/                    # app Android (Activity + Application)
│   ├── proguard-rules.pro         # regras (ligadas já, minify ainda desligado)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/app/moviematch/{MainActivity,MovieMatchApplication}.kt
│       └── res/                   # tema, cores, string e ícones
├── shared/                        # módulo KMP compartilhado
│   └── src/
│       ├── commonMain/kotlin/app/moviematch/
│       │   ├── App.kt             # raiz da UI Compose
│       │   ├── Platform.kt        # expect platformName()
│       │   ├── auth/              # AuthStore + expect GoogleSignInProvider
│       │   ├── data/supabase/     # cliente supabase-kt (Auth + Postgrest)
│       │   ├── di/Koin.kt         # inicialização do Koin (módulos + Supabase)
│       │   └── presentation/
│       │       ├── common/AppStores.kt   # profile atual / seleção de conteúdo
│       │       ├── home/HomeScreen.kt
│       │       ├── settings/             # tela Ajustes + GoogleAccountCard
│       │       └── theme/{Color,Dimens,Theme}.kt   # tokens de design
│       ├── androidMain/kotlin/app/moviematch/   # Platform.android, GoogleSignIn.android, di/Koin.android
│       ├── iosMain/kotlin/app/moviematch/        # Platform.ios, MainViewController, GoogleSignIn.ios
│       └── commonTest/kotlin/app/moviematch/     # PlatformTest, domain/MovieMathTest
└── iosApp/                        # shell SwiftUI + .xcodeproj
    ├── Configuration/Config.xcconfig
    └── iosApp/{iOSApp.swift,ContentView.swift,Info.plist,Assets.xcassets}
```

Alvos: **Android** (`minSdk 24`, `compileSdk`/`targetSdk` 37) e **iOS** (`iosArm64` +
`iosSimulatorArm64`, framework estático `Shared`). Não há target desktop/web.

---

## Como rodar

### Pré-requisitos

| Ferramenta | Versão |
| :--- | :--- |
| JDK | **17** (obrigatório) |
| Android Studio | versão que suporte AGP **9.1.1** (Panda 3 / 2025.3.3+) |
| Android SDK | Platform **37** + Build-Tools **36.0.0** (mínimo exigido pelo AGP 9) |
| Xcode | 15+ (somente para iOS, requer macOS) |

### Android

1. Abra a pasta `MoviematchApp/` no Android Studio (`File > Open` → selecione a pasta raiz).
2. Confirme **JDK 17** em `Settings > Build, Execution, Deployment > Build Tools > Gradle >
   Gradle JDK` (ou `File > Settings > Build Tools > Gradle`).
3. Aguarde o **Gradle Sync** (baixa Gradle 9.6.1 + dependências).
4. Selecione a configuração de execução **androidApp** e clique em ▶️ Run
   (ou `./gradlew :androidApp:installDebug` com um emulador/dispositivo conectado).

### iOS

1. Abra `iosApp/iosApp.xcodeproj` no **Xcode** (em um Mac).
2. Em `Signing & Capabilities`, selecione o seu **Team** (ou preencha `TEAM_ID` em
   `iosApp/Configuration/Config.xcconfig`). O bundle id já é **`app.moviematch`**.
3. Rode o app. O build do Xcode chama a task Gradle que compila o framework `Shared`
   automaticamente (`embedAndSignAppleFrameworkForXcode`).

### Linha de comando (Android)

```bash
./gradlew :androidApp:assembleDebug       # gera o APK de debug
./gradlew :shared:testAndroidHostTest     # roda commonTest no host JVM (Linux/CI, sem macOS)
./gradlew :shared:allTests                # todos os targets (os testes iOS exigem macOS)
```

---

## Login com Google (Supabase)

O login fica todo no `shared`: o app abre a UI nativa do Google (Credential Manager no Android),
recebe um **ID token** e o troca por uma sessão Supabase com
`supabase.auth.signInWith(IDToken) { provider = Google; nonce = ... }`.

- `auth/AuthStore.kt` — dono da sessão: entra/sai, observa `sessionStatus` e troca o
  `CurrentProfileStore` pelo profile real (`public.profiles`), com fallback para as claims do token.
- `auth/GoogleSignIn.kt` — `expect`/`actual` de `GoogleSignInProvider`; só a obtenção do token é
  específica de plataforma.
- `data/supabase/*` — cliente supabase-kt (Auth + Postgrest) e `Settings` por plataforma
  (`SharedPreferences` no Android, `NSUserDefaults` no iOS).
- `presentation/settings/GoogleAccountCard.kt` — botões "Entrar com Google" / "Sair" na tela Ajustes.

O login é **aditivo**: sem sessão o app continua funcionando com o profile semeado.

### 1. Variáveis no `.env`

| Variável | Onde pegar |
| :--- | :--- |
| `SUPABASE_URL` | Supabase Dashboard → Project Settings → API |
| `SUPABASE_ANON_KEY` | idem (chave pública `sb_publishable_*` / `anon`) |
| `WEB_CLIENT_ID` | Google Cloud Console → Credentials → OAuth client do tipo **Web application** |
| `TMDB_API_KEY` | TMDb → Settings → API → *API Key* (chave **v3**, usada pela busca da tela Adicionar) |

> É o client ID **Web** (não o Android) que vai no app: ele é o `aud` do ID token, e é contra ele
> que o Supabase valida o token.

### 2. Google Cloud Console

1. **OAuth consent screen** configurada (tipo "External"; app de teste basta).
2. Crie um **OAuth client ID** do tipo **Web application** → copie o *Client ID* para
   `WEB_CLIENT_ID`. Guarde o *Client secret* para o passo 3 (ele **não** entra no app).
3. Crie um **OAuth client ID** do tipo **Android** com:
   - *Package name*: `app.moviematch`
   - *SHA-1* do keystore que assina o app.

   O Credential Manager exige esse client Android registrado (com o SHA-1 do keystore) para liberar
   o `serverClientId` Web. Para o keystore de **debug**:

   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

   Em release, repita com o keystore real (mesmo *package*, SHA-1 do keystore de release).

### 3. Supabase Dashboard

Authentication → Providers → **Google**: habilite e informe o *Client ID* e o *Client secret* do
client **Web** criado acima. Sem `WEB_CLIENT_ID` no `.env`, o botão mostra
"Login com Google indisponível: WEB_CLIENT_ID não configurado." e a UI nativa nem é aberta.

### iOS

O login com Google **não** está ligado no iOS: falta integrar o SDK
[GoogleSignIn-iOS](https://github.com/google/GoogleSignIn-iOS) (Swift Package, sem artefato KMP), um
binding C-interop e um Mac para construir. O `actual` de `rememberGoogleSignInProvider()` devolve uma
mensagem clara. O resto (sessão em `NSUserDefaults`, engine HTTP Darwin, `AuthStore`) já é
compartilhado e funciona no iOS.

---

## Dados (Supabase + TMDb)

`domain/MovieRepository` tem duas implementações, ambas em `data/`:

- `data/supabase/SupabaseMovieRepository.kt` — backend real (Postgrest + RPCs, busca no TMDb);
  escolhida quando o `.env` traz `SUPABASE_URL` + `SUPABASE_ANON_KEY`;
- `data/mock/MockMovieRepository.kt` — semente em memória, usada quando não há credenciais
  (clone limpo, CI sem `.env`).

A escolha acontece **só** no `dataModule` (Koin); nenhuma tela ou `ViewModel` sabe qual está ativa.

### Schema

`supabase/0001_init.sql` é uma cópia verbatim do schema já aplicado no projeto Supabase — tabelas
`movies`/`series`, as sessões `sessions`/`series_sessions`, `ratings`/`series_ratings`, `profiles`,
`groups`/`group_members`, com RLS por grupo e os RPCs `upsert_movie`/`upsert_series`. Nada foi
alterado no arquivo: o que está versionado aqui é exatamente o que roda no servidor.

Três pontos do schema que explicam decisões do repositório:

- o catálogo global (`movies`/`series`) **não** tem policy de escrita: a única porta é o RPC
  `upsert_movie`/`upsert_series` (`SECURITY DEFINER`), que resolve a linha pelo `tmdb_id`;
- apagar um título remove apenas a sessão do grupo (`ratings` caem por cascade); a linha do catálogo
  compartilhado permanece — outros grupos podem estar usando o mesmo filme;
- `ratings`/`series_ratings` guardam o `group_id` desnormalizado, lido da própria sessão na hora de
  salvar a nota.

### Busca no TMDb

A tela **Adicionar** busca em `https://api.themoviedb.org/3` (`language=pt-BR`, timeout de 12s) pelo
`data/supabase/TmdbClient.kt`; a chave vem de `TMDB_API_KEY`. Sem chave, a busca falha com uma
mensagem clara ("Chave do TMDb não configurada...") em vez de chamar a rede.

Adicionar um resultado chama `addToWatchList(result)`, que busca os detalhes no TMDb, grava o título
no catálogo pelo RPC (`upsert_*`) e cria a sessão no grupo como `PENDING` — o `tmdb_id` é a amarração
entre os dois.

Os pôsteres são gravados como `poster_path` cru (padrão do TMDb/banco) e só viram URL completa
(`https://image.tmdb.org/t/p/<size>`) nos modelos que a UI consome: `ContentItem.posterUrl`
(w500) e `SearchResult.posterUrl` (w185).

---

## Versões fixadas

| Componente | Versão |
| :--- | :--- |
| Gradle | 9.6.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 (material3 `1.11.0-alpha07`) |
| AndroidX Activity | 1.11.0 |
| AndroidX Lifecycle (multiplatform) | 2.11.0 |
| Navigation Compose (multiplatform) | 2.9.2 |
| Ktor | 3.5.1 |
| Kotlinx coroutines / serialization / datetime | 1.11.0 / 1.11.0 / 0.8.0 |
| supabase-kt | 3.8.0 |
| Koin | 4.2.2 |
| Coil3 | 3.5.0 |
| Material 3 (`org.jetbrains.compose.material3`) | 1.11.0-alpha07 |
| AndroidX Credential Manager (`credentials` + `credentials-play-services-auth`) | 1.6.0 |
| `com.google.android.libraries.identity.googleid` | 1.2.1 |
| multiplatform-settings | 1.3.0 |
| Turbine | 1.2.1 |

### Por que `compileSdk = 37`

As libs AndroidX declaram um `minCompileSdk` nos metadados do AAR
(`aar-metadata.properties`). O Gradle falha o build quando esse valor é maior que
`android-compileSdk`, então o maior requisito do grafo manda. Verificado baixando os `.aar` e
lendo a propriedade:

| Artefato (variante Android) | Versão | `minCompileSdk` |
| :--- | :--- | :--- |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | **37** ← define o piso |
| `androidx.activity:activity` / `activity-compose` | 1.11.0 | 36 |
| `io.coil-kt.coil3:coil-compose` | 3.5.0 | 36 |
| `androidx.core:core` | 1.18.0 | 36 |
| `androidx.compose.ui:ui` | 1.11.2 | 35 |
| `androidx.compose.material3:material3` | 1.5.0-alpha17 | 35 |
| `androidx.navigation:navigation-compose` | 2.9.7 | 35 |
| `androidx.compose.runtime:runtime` | 1.11.2 | 34 |
| `androidx.compose.material:material-icons-core` | 1.7.3 | 34 |
| `org.jetbrains.androidx.lifecycle:*` (KMP) | 2.11.0 | 34 |
| `koin`, `supabase-kt`, `multiplatform-settings` | 4.2.2 / 3.8.0 / 1.3.0 | 1 |

Como o piso é 37, `android-compileSdk`/`android-targetSdk` ficam em **37** e as libs que haviam
sido rebaixadas por causa do compileSdk 35 subiram para as versões estáveis mais novas:
`androidx.activity` **1.11.0**, `androidx.lifecycle` **2.11.0** (artefatos multiplataforma
`org.jetbrains.androidx.lifecycle`, que por sua vez puxam `androidx.lifecycle` 2.11.0) e
`coil3` **3.5.0**. O `minSdk` continua 24.

---

## Testes

- `shared/src/commonTest` contém `domain/MovieMathTest.kt` (cálculos de média/veredito portados do
  app de referência), um teste de fumaça do `expect/actual` (`PlatformTest.kt`) e os testes da camada
  de dados (`data/supabase/*`: mappers de linha e `search`/`addToWatchList` contra um `MockEngine` do
  Ktor, sem rede).
- Dependências de teste já preparadas: `kotlin-test`, `kotlinx-coroutines-test`, `turbine`,
  `ktor-client-mock`.
- O bloco `androidLibrary { }` habilita `withHostTest { }`, então `commonTest` roda **no host JVM**
  (Linux/CI), sem precisar de macOS:

```bash
./gradlew :shared:testAndroidHostTest    # relatório em shared/build/reports/tests/
```

- No macOS, os mesmos testes também rodam no alvo iOS: `./gradlew :shared:iosSimulatorArm64Test`.

## Próximos passos planejados

- [x] Cliente Supabase + Ktor `HttpClient` no `dataModule` (Koin).
- [x] Login com **Google Sign-In → ID token → `supabase.auth.signInWithIdToken`** (Android; iOS é
  stub documentado, ver "Login com Google (Supabase)").
- [x] Repositório real (`SupabaseMovieRepository`) + busca no TMDb na tela Adicionar.
- [ ] Login com Google no iOS (SDK GoogleSignIn-iOS + binding C-interop).
- [ ] Telas de grupos/convites (2 pessoas) conforme `docs/native-app/`.
- [ ] Realtime na publicação `supabase_realtime` para convites e sessões.

Strings de UI na tela inicial estão em inglês e são **placeholders** — a camada de
internacionalização será definida junto com as telas reais.
