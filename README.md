# 🎬 MovieMatch

App **Android + iOS** que organiza a "roda" de um casal: duas pessoas criam um grupo, buscam filmes e séries no TMDb, montam uma lista compartilhada, marcam o que já assistiram e dão notas de 0 a 10 — com média e veredito do par.

UI e regras de negócio são escritas **uma única vez em Kotlin** (Kotlin Multiplatform + Compose Multiplatform) e rodam nos dois sistemas. O backend é **Supabase** (Auth + Postgres com RLS e RPCs) e o catálogo de títulos vem do **TMDb**.

---

## ✨ Funcionalidades

| Área | O que está implementado |
| :--- | :--- |
| 🔐 Login | "Entrar com Google" via Supabase Auth (Credential Manager no Android); sessão persistida; logout |
| 👥 Roda (grupo) | criar grupo, entrar por código, convidar por perfil ou `@handle`, aceitar/recusar convite, sair, remover parceiro e excluir/dissolver — ações destrutivas sempre com confirmação |
| 🎥 Catálogo | busca no TMDb (pt-BR) e adição à lista do grupo; filmes e séries |
| 📋 Lista | status `PENDING`/`WATCHED`, comentário e exclusão de título da lista |
| ⭐ Notas | nota individual de 0 a 10 por pessoa, média do par, veredito e "duelo de críticos" na Home |
| 🎨 UI | Material Design 3, tema claro/escuro e cor dinâmica (Material You no Android 12+) |
| 📴 Offline | sem `.env`, o app abre com dados de demonstração em memória (modo demo) |

---

## 🛠 Tecnologias

| Camada | Tecnologia | Versão |
| :--- | :--- | :--- |
| Linguagem | Kotlin (Multiplatform) | 2.4.10 |
| UI | Compose Multiplatform + Material 3 | 1.11.1 / 1.11.0-alpha07 |
| Build | Gradle / Android Gradle Plugin | 9.6.1 / 9.1.1 |
| HTTP | Ktor Client | 3.5.1 |
| Backend | supabase-kt (Auth + Postgrest) | 3.8.0 |
| Injeção de dependência | Koin | 4.2.2 |
| Imagens | Coil3 | 3.5.0 |
| Android | `minSdk` 24 · `compileSdk`/`targetSdk` 37 | — |
| iOS | `iosArm64` + `iosSimulatorArm64` (framework estático `Shared`) | — |

Todas as versões são centralizadas em [`gradle/libs.versions.toml`](gradle/libs.versions.toml). A tabela completa e o racional do `compileSdk 37` estão em [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 📁 Estrutura do projeto

```text
Moviematch-APP/
├── androidApp/          # host Android (Activity + Application + recursos)
├── iosApp/              # host SwiftUI + projeto Xcode
├── shared/              # todo o código compartilhado (UI + domínio + dados)
│   └── src/
│       ├── commonMain/  # App.kt, auth/, domain/, data/ (supabase/ e mock/), presentation/, di/
│       ├── androidMain/ # actuals Android (Google Sign-In, Settings, engine HTTP, Koin)
│       ├── iosMain/     # actuals iOS + MainViewController
│       └── commonTest/  # 118 testes (rodam no host JVM, sem macOS)
├── supabase/            # migrações SQL versionadas (schema, RLS, RPCs) — 0001…0004
├── gradle/
│   └── libs.versions.toml   # fonte única de versões
├── .env.example             # template de configuração (sem segredos)
├── README.md                # este documento
└── ARCHITECTURE.md          # documento canônico da arquitetura
```

Detalhamento pacote a pacote em [`ARCHITECTURE.md` (§3)](ARCHITECTURE.md).

---

## ✅ Pré-requisitos

| Ferramenta | Versão | Observação |
| :--- | :--- | :--- |
| JDK | **17** | obrigatório |
| Android Studio | com suporte a AGP 9.1.1 (2025.3.3+) | para Android |
| Android SDK | Platform **37** + Build-Tools 36.0.0 | mínimo exigido pelo AGP 9 |
| Xcode | 15+ | apenas para iOS (requer macOS) |
| Projeto Supabase | gratuito basta | backend (Auth + Postgres) |
| Chave de API TMDb | v3 | busca de títulos |

---

## ⚙️ Configuração

### 1. Clone e variáveis de ambiente

```bash
git clone https://github.com/LuizHenri16/Moviematch-APP.git
cd Moviematch-APP
cp .env.example .env
```

O `.env` é lido **em tempo de build** pela task `generateSecrets`, que gera a classe `app.moviematch.config.Secrets`. Ele está no `.gitignore` — nunca commite segredos reais.

| Variável | Onde pegar | O que habilita |
| :--- | :--- | :--- |
| `SUPABASE_URL` | Supabase Dashboard → Project Settings → API | backend real |
| `SUPABASE_ANON_KEY` | idem (chave pública `anon` / `sb_publishable_*`) | backend real |
| `TMDB_API_KEY` | [TMDb → Settings → API](https://www.themoviedb.org/settings/api) (chave **v3**) | busca na tela Adicionar |
| `WEB_CLIENT_ID` | Google Cloud Console → Credentials → OAuth client do tipo **Web application** | botão "Entrar com Google" |

> **Sem nada preenchido o app roda offline** com dados de demonstração. Com `SUPABASE_URL` + `SUPABASE_ANON_KEY` ele usa o backend real; sem `WEB_CLIENT_ID`, o botão de login exibe uma mensagem clara em vez de abrir a UI nativa do Google.

> É o client ID **Web** (não o do Android) que vai no app: ele é o `aud` do ID token, e é contra ele que o Supabase valida o token.

### 2. Google Cloud Console (login)

1. Configure o **OAuth consent screen** (tipo "External"; app de teste basta).
2. Crie um **OAuth client ID — Web application** → copie o *Client ID* para `WEB_CLIENT_ID`. Guarde o *Client secret* para o passo 3 (ele **não** entra no app).
3. Crie um **OAuth client ID — Android** com *Package name* `app.moviematch` e o SHA-1 do keystore que assina o app. Para o keystore de **debug**:

   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

   Em release, repita com o keystore de release (mesmo *package*).

### 3. Supabase (banco e provider Google)

1. Aplique as migrações de [`supabase/`](supabase/) na ordem `0001` → `0004` (SQL Editor ou Supabase CLI). **A `0004` é obrigatória**: sem ela, sair/remover membros deixa dados antigos travando o fluxo de grupo.
2. Authentication → Providers → **Google**: habilite e informe o *Client ID* e o *Client secret* do client **Web** criado no passo 2.

### 4. iOS (assinatura)

1. Abra `iosApp/iosApp.xcodeproj` no Xcode (macOS).
2. Em *Signing & Capabilities*, selecione o seu **Team** (ou preencha `TEAM_ID` em `iosApp/Configuration/Config.xcconfig`). O bundle id já é `app.moviematch`.

---

## ▶️ Como rodar

### Android

**Android Studio:** abra a pasta raiz do projeto (`File > Open`), aguarde o **Gradle Sync**, selecione a run configuration **androidApp** e clique em ▶️ Run.

**Linha de comando:**

```bash
./gradlew :androidApp:installDebug    # instala em emulador/dispositivo conectado
./gradlew :androidApp:assembleDebug   # apenas gera o APK de debug
```

### iOS (macOS)

Rode o app pelo Xcode. O build chama automaticamente a task Gradle que compila o framework `Shared` (`embedAndSignAppleFrameworkForXcode`).

> **O login com Google ainda não está disponível no iOS** — falta integrar o SDK [GoogleSignIn-iOS](https://github.com/google/GoogleSignIn-iOS). Todo o resto (sessão, dados, telas) já é compartilhado. Detalhes em [`ARCHITECTURE.md` (§11.3)](ARCHITECTURE.md).

### Modo offline

Sem `.env` (ou sem `SUPABASE_*`), o build funciona normalmente e o app abre com catálogo, grupo e perfil de demonstração — útil para mexer na UI, CI ou clone limpo, sem configurar nada.

---

## 🧪 Testes

```bash
./gradlew :shared:testAndroidHostTest   # 118 testes no host JVM (Linux/CI, sem macOS)
./gradlew :shared:allTests             # todos os alvos (os testes iOS exigem macOS)
./gradlew :androidApp:lintDebug        # lint do app Android
```

- São **118 testes** em `shared/src/commonTest`: regras puras (`MovieMath`), fluxo de grupo (repositório, store e ViewModels), catálogo, TMDb e mappers de linha — a maioria com `MockEngine` do Ktor, sem rede.
- Relatório HTML após a execução: `shared/build/reports/tests/testAndroidHostTest/`.

---

## 🚀 Como usar (fluxo principal)

1. Entre com **Google** na tela de login.
2. Em **Perfil → Minha roda**, crie uma roda ou entre com o código de convite.
3. Convide a outra pessoa por busca ou `@handle` — ela aceita pelo próprio convite.
4. Na aba **Adicionar**, busque um título no TMDb e coloque-o na lista do par.
5. Em **Detalhes**, dê sua nota de 0 a 10 (marque "já assistiram juntos" se quiser) — a média e o veredito aparecem na Home.

---

## 🗺 Roadmap

- [x] App KMP com UI Compose compartilhada (Android + iOS)
- [x] Login com Google → Supabase Auth (Android)
- [x] Catálogo TMDb + lista, status e notas do grupo
- [x] Telas de roda/convites (2 pessoas) com permissões no backend (RLS + RPCs)
- [ ] Consumo de **Realtime** (a publicação já existe no SQL; o app ainda não assina eventos)
- [ ] Login com Google no iOS (SDK GoogleSignIn-iOS + binding C-interop)
- [ ] Cancelar convite enviado (exige RPC nova)
- [ ] Corrigir identidade da nota por `profileId` em vez de nome (P0-B — ver [`ARCHITECTURE.md` (§10.4)](ARCHITECTURE.md))
- [ ] i18n formal, acessibilidade validada em device e testes de UI

---

## 📚 Documentação

| Documento | Conteúdo |
| :--- | :--- |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | **Fonte única da arquitetura**: camadas, navegação, estado, backend (RLS/RPCs), decisões (ADRs), dívidas e riscos |
| [`gradle/libs.versions.toml`](gradle/libs.versions.toml) | versões de todas as dependências e plugins |
| [`supabase/`](supabase/) | schema, RLS e RPCs versionados |

> Em caso de divergência entre documentação e código, **o código prevalece**.
