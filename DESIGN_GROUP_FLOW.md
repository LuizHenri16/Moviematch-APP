# MovieMatch — Redesign do Fluxo Login/Grupo

**Data:** 18/10/2026  
**Autor:** Arquiteto de Software  
**Status:** Para implementação pelo @desenvolvedor

---

## 1. Visão Geral

O fluxo atual já removeu o `GroupGate` obrigatório (login → vai direto para `AppShell`). O que falta é **tornar a tela de grupo acessível** via navegação, com ponto de entrada claro ("Minha roda") e comportamento adequado para dois modos:

| Modo | Quando | O que mostra |
|------|--------|--------------|
| **Onboarding** (sem grupo) | Usuário logado, sem grupo ativo | Criar grupo, Entrar por código, Convites pendentes |
| **Gerenciar Grupo** (com grupo) | Usuário tem grupo ativo | Código do convite, parceiro, convidar nova pessoa (se vaga) |

---

## 2. Diagrama de Navegação (Textual)

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP ENTRY POINT                          │
│  App() → SessionGate() → SignedIn? → AppShell()                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      APP SHELL (Bottom Bar)                     │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  HOME   │  │ ADICIONAR│ │ FILMES  │  │ PERFIL  │  ← pushed  │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
│       │            │            │            │                  │
│       │            │            │            ▼                  │
│       │            │            │  ┌─────────────────────┐      │
│       │            │            │  │  SETTINGS SCREEN    │      │
│       │            │            │  │  • Perfil           │      │
│       │            │            │  │  • Conta Google     │      │
│       │            │            │  │  • Avatar           │      │
│       │            │            │  │  ┌───────────────┐  │      │
│       │            │            │  │  │ MINHA RODA    │──┼──┐   │
│       │            │            │  │  │ (card clicável)│  │  │   │
│       │            │            │  │  └───────────────┘  │  │   │
│       │            │            │  └─────────────────────┘  │   │
│       │            │            │                            │   │
│       ▼            ▼            ▼                            ▼   ▼
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                  GROUP SCREEN (pushed)                      ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │            MODO ONBOARDING (sem grupo)              │   ││
│  │  │  • Criar grupo        • Entrar com código           │   ││
│  │  │  • Convites pendentes (aceitar/recusar)             │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │            MODO GERENCIAR (com grupo)               │   ││
│  │  │  • Parceria (avatars)   • Código do convite         │   ││
│  │  │  • Convidar por busca   • Convidar por @handle      │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Ponto de Entrada para Tela de Grupo

### Decisão: **Card "Minha roda" em SettingsScreen** (já existe visualmente)

**Por que Settings?**
- Já carrega `groupState` via `SettingsViewModel.loadGroup()`
- Já mostra o código do convite e parceiro quando há grupo
- Usuário espera configurar "minha roda" em Configurações
- Não polui Home/Header/BottomBar com mais um item

### Comportamento do Card

| Estado do Grupo | Texto do Card | Ação ao Clicar |
|-----------------|---------------|----------------|
| **Sem grupo** | "Minha roda — Criar ou entrar" | Navega para `GroupScreen` (modo onboarding) |
| **Com grupo** | "Minha roda — [CÓDIGO]" + parceiro | Navega para `GroupScreen` (modo gerenciar) |

> **Nota:** O card já existe visualmente (linhas 206-286 de `SettingsScreen.kt`). Só falta torná-lo clicável e passar o callback de navegação.

---

## 4. Mudanças Necessárias por Arquivo

### 4.1 `Routes.kt` — Nova Rota

```kotlin
object Routes {
    const val HOME = "home"
    const val MOVIES = "movies"
    const val ADD = "add"
    const val DETAILS = "details"
    const val SETTINGS = "settings"
    const val GROUP = "group"          // ← NOVA
}
```

**Grupo NÃO entra em `BottomBarRoutes`** — é tela *pushed* (como Settings/Details), não tab.

---

### 4.2 `AppShell.kt` — Adicionar no Grafo de Navegação

```kotlin
// Imports
import app.moviematch.presentation.group.GroupScreen
import app.moviematch.presentation.group.GroupViewModel

// Dentro do NavHost:
shellComposable(Routes.GROUP, currentRoute, onVisibleRoute) {
    GroupScreen(
        onBack = { navController.popBackStack() },
        // ViewModel injetado via Koin (já configurado)
    )
}
```

> **Importante:** `GroupScreen` hoje espera `viewModel` injetado. Como agora estará no grafo de navegação, o `koinViewModel()` padrão funciona (escopo `activity` ou `navigation` conforme módulo Koin). Não precisa passar manualmente.

---

### 4.3 `SettingsScreen.kt` — Tornar Card Clicável

**Mudança na assinatura:**
```kotlin
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGroup: () -> Unit,        // ← NOVO callback
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
)
```

**No `SettingsContent` (linha ~206):**
```kotlin
groupState.dataOrNull()?.let { group ->
    BentoCard(
        padding = Dimens.Spacing16,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGroup)    // ← NOVO
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Dimens.RadiusCard)),
    ) {
        // ... conteúdo existente ...
    }
} ?: run {
    // ← NOVO: Card para quando NÃO há grupo
    BentoCard(
        padding = Dimens.Spacing16,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGroup)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Dimens.RadiusCard)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Minha roda",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = "Criar ou entrar",
                fontSize = 13.sp,
                color = Accent,
            )
        }
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Monte sua dupla para escolher, assistir e dar nota junto.",
            fontSize = 12.sp,
            color = TextSecondary,
        )
    }
}
```

**Em `AppShell.kt` ao chamar `SettingsScreen`:**
```kotlin
shellComposable(Routes.SETTINGS, currentRoute, onVisibleRoute) {
    SettingsScreen(
        onBack = { navController.popBackStack() },
        onOpenGroup = { navController.navigate(Routes.GROUP) },   // ← NOVO
    )
}
```

---

### 4.4 `GroupScreen.kt` — Ajustes para Navegação Pushed

#### 4.4.1 Assinatura Atualizada

```kotlin
@Composable
fun GroupScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,                    // ← NOVO (obrigatório para pushed)
    viewModel: GroupViewModel = koinViewModel(),
)
```

#### 4.4.2 Header com Botão Voltar

Substituir `ScreenHeader` atual (linha 102-105) por:

```kotlin
ScreenHeader(
    title = "Sua roda",
    subtitle = if (overview?.group != null) "Dupla de filmes e séries" else "Monte a sua dupla",
    action = {
        HeaderIconButton(onClick = onBack) {
            IconChevronLeft(modifier = Modifier.iconSize(18.dp), color = TextPrimary)
        }
    },
)
```

#### 4.4.3 Remover `SignOutRow` (linha 154 + 680-694)

**Motivo:** O logout agora vive no header do `AppShell` (Home/Settings). `GroupScreen` é tela pushed, não gate. O botão "Sair da conta" duplicaria e confundiria.

#### 4.4.4 Padding Inferior

Adicionar padding para não colar no bottom bar (embora bottom bar fique oculto em rotas pushed, o `NavHost` ainda reserva espaço):

```kotlin
Column(
    modifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(bottom = rememberBottomBarBottomPadding()),   // ← NOVO
    // ...
)
```

---

### 4.5 `GroupViewModel.kt` — Sem Mudanças de Lógica

O `GroupViewModel` **já suporta ambos os modos** via `overview.group == null` vs `!= null` (veja linhas 128-151). O `load()` recarrega ao aceitar convite/criar grupo/entrar por código, e o `state` reflete automaticamente.

**Único ponto de atenção:** O `init` observa `currentProfile.profile` e recarrega. Como a tela agora é *pushed* (não gate), o ciclo de vida do VM continua correto (scoped to activity/navigation). **Nenhuma mudança necessária.**

---

## 5. Contrato de Navegação

| Rota | Tipo | Parâmetros | Bottom Bar | Back Stack |
|------|------|------------|------------|------------|
| `home` | Tab (start) | — | ✅ Visível | Raiz |
| `movies` | Tab | — | ✅ Visível | `popUpTo(HOME)` |
| `add` | Tab | — | ✅ Visível | `popUpTo(HOME)` |
| `details` | Pushed | `ContentSelectionStore` | ❌ Oculto | Sobre tab atual |
| `settings` | Pushed | — | ❌ Oculto | Sobre tab atual |
| `group` | Pushed | — | ❌ Oculto | Sobre tab atual |

**Transição Settings → Group:**
```kotlin
navController.navigate(Routes.GROUP) { launchSingleTop = true }
```
Não precisa `popUpTo` — Settings fica no stack (usuário volta com back).

**Volta Group → Settings → Home:** Back nativo do Android / swipe iOS / botão header.

---

## 6. Como GroupScreen Sabe o Modo (Onboarding vs Gerenciar)

**Já implementado no `GroupScreen.kt` (linhas 120-151):**

```kotlin
overview = state.dataOrNull()

when {
    overview == null -> StateError(...)
    overview.group == null -> NoGroupContent(...)     // MODO ONBOARDING
    else -> WithGroupContent(...)                      // MODO GERENCIAR
}
```

O `GroupOverview` (VM linha 36-41) carrega:
- `group: Group?` — null = sem grupo ativo
- `partner: Profile?` — null = vaga aberta

**Zero mudança necessária.** A tela é *state-driven*.

---

## 7. Fluxo Completo (User Journey)

```
1. Usuário abre app → LoginScreen
2. Login bem-sucedido → AppShell (HOME tab ativa)
3. Usuário vê Home vazio (sem dados de grupo) — OK, não bloqueia
4. Usuário toca "Perfil" (bottom bar) → SettingsScreen
5. Em Settings:
   a) SEM grupo: card "Minha roda — Criar ou entrar" → toca → GroupScreen (onboarding)
   b) COM grupo: card "Minha roda — 3F9A20C7 + Parceiro" → toca → GroupScreen (gerenciar)
6. Em GroupScreen (onboarding):
   - "Criar grupo" → RPC create_group → reload → volta modo gerenciar
   - "Entrar com código" → RPC join_group_by_code → reload → volta modo gerenciar
   - "Aceitar convite" → RPC accept_invite → reload → volta modo gerenciar
7. Em GroupScreen (gerenciar):
   - Vê código, parceiro
   - Se vaga: "Convidar por busca" ou "Convidar por @handle"
8. Usuário toca voltar (header) → Settings → Home (bottom bar)
9. Home agora mostra cards de grupo (Harmonia, Duelo, Consenso) com dados reais
```

---

## 8. Checklist de Implementação

| # | Arquivo | Mudança | Status |
|---|---------|---------|--------|
| 1 | `Routes.kt` | Adicionar `const val GROUP = "group"` | ☐ |
| 2 | `AppShell.kt` | Importar `GroupScreen`, `GroupViewModel`; adicionar `shellComposable(Routes.GROUP...)` | ☐ |
| 3 | `AppShell.kt` | Passar `onOpenGroup` para `SettingsScreen` | ☐ |
| 4 | `SettingsScreen.kt` | Adicionar param `onOpenGroup: () -> Unit` | ☐ |
| 5 | `SettingsScreen.kt` | Tornar card "Minha roda" clicável (com/sem grupo) | ☐ |
| 6 | `GroupScreen.kt` | Adicionar param `onBack: () -> Unit` | ☐ |
| 7 | `GroupScreen.kt` | Header com botão voltar (IconChevronLeft) | ☐ |
| 8 | `GroupScreen.kt` | Remover `SignOutRow` (linha 154 + componente) | ☐ |
| 9 | `GroupScreen.kt` | Adicionar `padding(bottom = rememberBottomBarBottomPadding())` | ☐ |

---

## 9. Considerações Técnicas

### 9.1 Ciclo de Vida do GroupViewModel
- Atualmente: `koinViewModel()` no `GroupGate` (escopo activity)
- Após mudança: `koinViewModel()` no composable `GroupScreen` dentro do `NavHost`
- **Verificar** em `Koin.kt` / `Koin.android.kt` se o escopo é `viewModel { GroupViewModel(...) }` (activity) ou `navigationViewModel`. Recomendado: **activity** para sobreviver a navegações e recarregar ao trocar de conta.

### 9.2 Bottom Bar em GroupScreen
- `visibleRoute` em `AppShell` só mostra barra para `BottomBarRoutes` (HOME, MOVIES, ADD)
- `GROUP` **não** está na lista → barra **oculta** automaticamente ✅
- `rememberBottomBarBottomPadding()` retorna 0 quando barra oculta, mas mantém padding seguro.

### 9.3 Recarregamento ao Voltar
- `GroupViewModel` observa `currentProfile.profile.id` (linha 93-99) → recarrega se conta mudou
- `SettingsViewModel.loadGroup()` chamado no `init` → recarrega ao voltar para Settings
- Funciona sem lógica extra.

---

## 10. Fora de Escopo (Não Fazer Agora)

- ❌ Botão "Minha roda" no Header do Home
- ❌ Item extra na Bottom Bar
- ❌ Deep link direto para grupo (ex: `app://group/INVITE_CODE`)
- ❌ Notificação push de convite → abrir GroupScreen
- ❌ Tela de "Sair do grupo" (já existe no backend, mas não no fluxo mobile)

---

## 11. Referências de Código

| Arquivo | Linhas Relevantes |
|---------|-------------------|
| `App.kt` | 83-89 (GroupGate removido) |
| `Routes.kt` | 12-21 (rotas atuais) |
| `AppShell.kt` | 103-133 (grafo), 136-143 (bottom bar logic) |
| `SettingsScreen.kt` | 206-286 (card "Minha roda"), 76 (groupState) |
| `SettingsViewModel.kt` | 86-93 (loadGroup) |
| `GroupScreen.kt` | 120-151 (switch modo), 102-105 (header), 154 (SignOutRow) |
| `GroupViewModel.kt` | 36-41 (GroupOverview), 256-266 (overview()) |

---

**Fim do Documento** — Pronto para implementação.