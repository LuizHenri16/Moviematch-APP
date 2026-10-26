# MovieMatch — Material Design 3

Este documento descreve a implementação de Material Design 3 no MovieMatch. Ele é a referência
para qualquer tela nova e para a revisão das telas existentes.

## Princípios

1. **Material 3 puro.** A UI usa os componentes, papéis de cor, escala tipográfica e escala de forma
   do Material 3. Não há camadas de vidro, gradientes decorativos, bordas de "sheen" nem sombras
   difusas em cards.
2. **Tema claro e escuro.** `MovieMatchTheme` segue `isSystemInDarkTheme()`.
3. **Cor dinâmica.** No Android 12+ o esquema é derivado do wallpaper (Material You). Fora disso
   (iOS, Android < 12) valem os esquemas estáticos gerados a partir da seed da marca.
4. **Semântica antes de estética.** Todo texto usa o papel `on*` da superfície em que está.

## Tokens

### Cor (`theme/Color.kt`)

`BrandSeed = #7DD3FC`. Os dois esquemas (`MovieMatchLightColorScheme`, `MovieMatchDarkColorScheme`)
foram gerados com `@material/material-color-utilities` (`SchemeTonalSpot`) e contêm **todos** os
papéis MD3, incluindo `surfaceDim`, `surfaceBright` e a família `surfaceContainer*`.

Escolha do papel:

| Necessidade | Papel |
| :--- | :--- |
| Ação principal / destaque | `primary` / `onPrimary` |
| Ação secundária | `secondary` / `secondaryContainer` / `onSecondaryContainer` |
| Família de dados alternativa | `tertiary` / `tertiaryContainer` |
| Canvas | `background` / `onBackground` |
| Card de conteúdo | `surfaceContainerLow` |
| Card sobre card / tile | `surfaceContainerHighest` |
| Chrome (navigation bar) | `surfaceContainer` |
| Texto principal | `onSurface` |
| Texto de apoio | `onSurfaceVariant` |
| Divisor / borda neutra | `outlineVariant` |
| Borda enfatizada / campo focado | `outline` |
| Erro / destrutivo | `error` / `errorContainer` / `onErrorContainer` |
| Overlay modal | `scrim` |

### Tipografia (`theme/Type.kt`)

`MovieMatchTypography = Typography()` — escala oficial MD3. Aliases semânticos (todos `@Composable`):

| Alias | Papel MD3 |
| :--- | :--- |
| `ScreenTitleStyle` | `headlineSmall` |
| `ScreenSubtitleStyle` | `bodyMedium` |
| `HeroTitleStyle` | `headlineLarge` |
| `CardTitleStyle` | `titleLarge` |
| `CardMetaStyle` | `bodySmall` |
| `StatNumberStyle` | `displaySmall` |
| `DialogTitleStyle` | `headlineSmall` |
| `DialogBodyStyle` | `bodyMedium` |
| `MicroLabelStyle` | `labelSmall` |
| `NumericHeroStyle` | `displaySmall` |

### Forma (`theme/Theme.kt`)

`MaterialTheme.shapes.extraSmall/small/medium/large/extraLarge` = 4 / 8 / 12 / 16 / 28 dp.
Botões usam a forma *full* (`Dimens.RadiusPill`), chips usam `small` e a barra de busca usa o
`extraLarge` (pill de 28dp). Os demais campos de texto usam `extraSmall` (4dp), como no MD3 — a
pill é reservada ao componente de busca.

### Espaçamento e tamanhos (`theme/Dimens.kt`)

Grid de 4dp (4/8/12/16/24). Botões 40dp, chips 32dp, campos 56dp, icon buttons 40dp (alvo 48dp),
navigation bar 80dp, ícones 16/20/24dp (24dp na navigation bar).

## Componentes

| Componente | Base MD3 |
| :--- | :--- |
| `BentoCard` | `Card` (`surfaceContainerLow`, shape large, elevação 0) |
| `BentoTile` | `Surface` (`surfaceContainerHighest`, shape medium) |
| `AppButton` | `Button` / `OutlinedButton` (error role para destrutivo) |
| `AppSmallButton` | `FilledTonalButton` |
| `Chip` | `Surface` com shape small e papéis tonais (selected = `secondaryContainer`) |
| `Pill` | `Surface` + `labelMedium` |
| `IconBadge` | `Surface` (`secondaryContainer`) |
| `HeaderIconButton` | `IconButton` / `Surface(onClick)` |
| `ScreenTopBar` | `TopAppBar` |
| `BottomBar` | `NavigationBar` + `NavigationBarItem` |
| `SearchInput` | `OutlinedTextField` (search bar, pill de 28dp) |
| `AvatarUrlField` | `OutlinedTextField` (shape extraSmall) |
| `ProgressBar` | `LinearProgressIndicator` |
| `ConfirmDialog` | `AlertDialog` |
| `StateMessage` | `Surface` semântica + `FilledTonalButton` |
| `Skeleton` | bloco tonal com pulse (`surfaceContainerHighest`) |

## Migração de tokens antigos

Os nomes abaixo **não existem mais** em `theme/`. Substitua por:

| Antes | Depois |
| :--- | :--- |
| `Accent` | `MaterialTheme.colorScheme.primary` |
| `TextOnAccent` | `MaterialTheme.colorScheme.onPrimary` |
| `Accent2`, `Lavender` | `MaterialTheme.colorScheme.tertiary` |
| `AccentGlow`, `LavenderSurface` | `MaterialTheme.colorScheme.tertiaryContainer` |
| `LavenderBorder` | `MaterialTheme.colorScheme.outlineVariant` |
| `Background` | `MaterialTheme.colorScheme.background` |
| `Surface`, `SurfaceCard` | `MaterialTheme.colorScheme.surfaceContainerLow` |
| `SurfaceDeep`, `SurfaceHero` | `MaterialTheme.colorScheme.surfaceContainer` |
| `SurfaceCardElevated`, `GlassPanel` | `MaterialTheme.colorScheme.surfaceContainerHigh` |
| `GlassTileFill`, `PillSurface`, `ChipSurface` | `MaterialTheme.colorScheme.surfaceContainerHighest` |
| `TextPrimary` | `MaterialTheme.colorScheme.onSurface` |
| `TextSecondary`, `TextMuted`, `TextSoft` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `Border`, `GlassBorder`, `Divider` | `MaterialTheme.colorScheme.outlineVariant` |
| `BorderStrong`, `Outline` | `MaterialTheme.colorScheme.outline` |
| `Danger`, `DangerStrong` | `MaterialTheme.colorScheme.error` |
| `DangerSurface` | `MaterialTheme.colorScheme.errorContainer` |
| `DangerBorder` | `MaterialTheme.colorScheme.error` |
| `TextOnDanger` | `MaterialTheme.colorScheme.onError` |
| `Amber` | `MaterialTheme.colorScheme.tertiary` |
| `Scrim` | `MaterialTheme.colorScheme.scrim` |
| `White` (texto sobre imagem) | `Color.White` + scrim |
| `ScoreGreat*` | `primaryContainer` / `onPrimaryContainer` |
| `ScoreBad*` | `errorContainer` / `onErrorContainer` |
| `ScoreGood*` | `surfaceContainerHigh` / `onSurface` |
| `LockBadgeSurface/Content` | `secondaryContainer` / `onSecondaryContainer` |
| `DisabledContent` | `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)` |
| `GlassSurface` | `Card` ou `Surface` |
| `GlassChrome`, `LocalGlassHazeState` | removidos (Haze não é mais usado) |
| `glassLayers`, `glassShadow` | removidos |
| `ShadowBleed`, `FloatingHeaderInset` | removidos (não há sombra difusa nem header flutuante) |
| `RadiusBottomBar`, `RadiusHero` | removidos (usa-se `shapes.large` ou `shapes.extraLarge`) |
| `roundedShape` | mantido (apenas para raios parametrizados) |

## Acessibilidade

* Todo alvo interativo tem no mínimo 48dp.
* Ícones decorativos usam `contentDescription = null`; ações usam descrição obrigatória.
* Contraste: sempre um papel `on*` sobre o seu container.
* Texto dinâmico nunca é forçado a uppercase (exceto micro-rótulos estáticos).

## Validação

```bash
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest
./gradlew :androidApp:lintDebug
# alterna o tema do sistema no emulador/dispositivo
adb shell cmd uimode night no   # claro
adb shell cmd uimode night yes  # escuro
```

No Android 12+, a cor dinâmica pode ser conferida trocando o wallpaper (Material You). Fora do
Android 12+ (e no iOS) valem os esquemas estáticos de `Color.kt`.
