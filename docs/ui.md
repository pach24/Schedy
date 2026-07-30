# Design system

Everything under `ui/` is reusable and knows nothing about features. If a component needs an
`EventDetailUiState`, it doesn't belong here — it belongs in `presentation/detail/`.

## Typography

**Golos Text**, six weights in `res/font/`, loaded in `GolosFamily` (`ui/theme/Type.kt`).
The scale uses **negative letter spacing** at the large sizes (−0.2 to −0.5 sp) so headlines
tighten up, and heavy weights: `titleLarge` is already Bold, and the giant hero numbers are
Black.

Big numbers (countdown, clock, codes) are treated as graphics rather than text: 56–88 sp,
Black, very negative tracking.

## Color

`ui/theme/Color.kt`, two flat palettes — no dynamic color, the identity wins:

|  | Light | Dark |
|---|---|---|
| Background | `#F2F2F7` | `#0D0D0D` |
| Surface | `#FFFFFF` | `#1A1A1A` |
| Text | `#111111` | `#F5F5F5` |
| Secondary | `#6E6E73` | `#8A8A8E` |

Plus the colors that carry meaning, which **don't change** between themes because they are
data: `TierFull` green, `TierViable` amber, `TierLimited` orange, `TierInsufficient` red, and
the five note tag colors. The action blue is `#0082F3`.

The theme follows the system. Text always uses roles (`onSurface`, `onSurfaceVariant`), never
a hardcoded color, except for those meaningful cases.

## Shapes: the squircle

`ui/theme/Shape.kt`. Almost nothing uses `RoundedCornerShape`: corners are **superellipses**
drawn by hand with Bézier curves (`k = 0.5519`), the iOS-style continuous corner. It shows
most on calendar cells and large cards.

- `SquircleShape(radius)` — the standard one.
- `VerticalSquircleShape(radius, extraTop, extraBottom)` — allows overflow above or below,
  for the availability grid's headers and footers.
- Ready-made instances: `SquircleCellShape` (12 dp), `CalendarCellShape` (14 dp),
  `GridHeaderShape`, `GridFooterShape`, `SquircleMiniShape`.

Pills and buttons do use `FullRoundShape` (`RoundedCornerShape(50)`).

## Glass

Two paths, in `LiquidGlass.kt` and `FrostedSurface.kt`, on top of the
[haze](https://github.com/chrisbanes/haze) library:

- **Custom glass** (`liquidGlassBackdrop` + `liquidGlassShape`): a shader with refraction, a
  rim and blur. Only where the device supports it (`LiquidGlassState.isSupported`).
- **Haze fallback** (`hazeEffect`): blur and tint, no refraction.

`Modifier.frostedSurface(...)` picks one or the other and is what you actually use: top bar,
bottom bar and dialogs.

A requirement you have to respect: **the glass piece must be drawn outside the content that
carries `liquidGlassBackdrop`**, and that content is what appears refracted. That is why
dialogs and the top bar hang off the screen's outer `Box` rather than the `Scaffold`. Put a
glass piece inside the content and it samples itself, which looks wrong.

And no glass on glass: a list inside a frosted dialog uses a flat fill, because it would
sample the same background as the dialog while ignoring that the dialog sits in front of it.

## Animations

`ui/theme/Animations.kt` holds the shared transitions:

- **Navigation** — `NavEnterTransition` and friends: the incoming screen springs up from the
  bottom while the outgoing one scales down and dims; reversed on the way back.
- **`Modifier.pressScale(interactionSource)`** — the 0.965 press scale, spring-driven. It
  goes on nearly everything tappable.
- **`FadeIn(delayMs)`** — staggered reveal of a screen's content. Uses `rememberSaveable` so
  it doesn't replay when you come back to a tab.
- **`CrossfadeLoadingContent`** — loading-state change with a subtle scale.

Everything else lives next to its component. Two worth reading before writing another:

- **`DigitalTimePicker`** — the set-the-time clock. Each digit leaves and enters in the
  direction of the change; the tick lives in a `State` that is passed without being read,
  each unit is derived with `derivedStateOf`, and the bar reads progress **inside the
  `Canvas`**, so it repaints without recomposing. If you need something that ticks by the
  second, copy this pattern.
- **`SessionCountdown`** (in `presentation/detail/`) — the hero with days, ghost time,
  waiting bar and a second-by-second breakdown, under the same recomposition discipline.

## Components that already exist

Before writing a new one, look in `ui/components/`:

| | |
|---|---|
| `AvailabilityGrid` | The who-can-make-which-day grid |
| `CalendarGrid`, `ScheduleCalendar`, `MiniWeekCalendar` | Calendars in their three formats |
| `SplitSquircleCell` | Cell split into morning/afternoon |
| `GenCard`, `GenTextField`, `GenTopBar` | Standard card, field and top bar |
| `FrostedSurface`, `LiquidGlass`, `HoleRectShape` | Glass, and the bottom bar's notch |
| `DigitalTimePicker` | Start-time clock |
| `DayTimeSlotDialog`, `ComingSoonDayDialog` | Day dialogs |
| `NoteCard`, `TagChip` | Notes |
| `UserAvatar`, `AvatarPickerButton`, `LoadingDots` | Odds and ends |

## Rules when writing UI

1. **No literal text in a composable.** Always `stringResource`, and the string in both
   `strings.xml` files.
2. **Minimal state in the ViewModel.** Ephemeral things (open dialog, scroll) stay in the
   composable with `remember`.
3. **Watch what recomposes.** If a value changes many times per second, read it inside
   `graphicsLayer` or the `Canvas` (draw phase) instead of during composition.
4. **Previews.** Every screen has an `XxxPreviews.kt` with light and dark; it is the quick
   way to see a change without deploying.
