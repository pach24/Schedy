# Architecture

Clean architecture in three layers, with MVVM on top. One rule sums it up:

> **Arrows point inwards.** `presentation` knows `domain`; `data` knows `domain`;
> `domain` knows nobody.

```
┌───────────────────────────────────────────────┐
│ presentation/        Compose + ViewModels     │
│   Screen  ──observes──▶ ViewModel ──calls──▶  │
└───────────────────────────────┬───────────────┘
                                │ use cases
┌───────────────────────────────▼───────────────┐
│ domain/     model · repository (interfaces)   │
│             usecase                           │
└───────────────────────────────▲───────────────┘
                                │ implements
┌───────────────────────────────┴───────────────┐
│ data/       Firestore · Auth · FCM · Prefs    │
│             WorkManager                       │
└───────────────────────────────────────────────┘
```

`di/` stitches the two ends together: it is the only place in the project where a domain
interface meets its Firebase implementation.

## Why this shape

The cost is real: 32 five-line use case classes and nine interfaces that look like
paperwork. What it buys:

- **A ViewModel states what it can do.** `EventDetailViewModel` takes eighteen named
  operations instead of six repositories it used two methods of each. Read the constructor
  and you know what the screen does.
- **Firebase is a detail.** It is confined to `data/`. Swapping it out means rewriting nine
  files and touching `RepositoryModule`, without opening a single screen.
- **It can be tested without Android.** `domain` is plain Kotlin: use cases are tested
  against a fake of the interface, no emulator and no Firestore.

## The layers, one by one

### domain

Plain Kotlin. No Compose, no Firebase, no `Context`.

- **`model/`** — the business types: `Event`, `Participant`, `Note`, `DateSummary`,
  `AttendanceTier`. This is also where the one business rule with a name of its own lives:
  `computeAttendanceTier(count, total)`, which files a date under FULL (≥86% of the group),
  VIABLE (≥71%), LIMITED (≥57%) or INSUFFICIENT.

  > These models still carry `com.google.firebase.Timestamp` in a few fields
  > (`Participant.availableDates`, `Note.createdAt`). It is a deliberate crack in the
  > layer's purity, inherited from deserializing straight into Firestore objects. If it
  > ever gets in the way, the way out is a separate data-layer model plus a mapper.

- **`repository/`** — nine interfaces declaring what the domain needs from the outside
  world: `EventRepository`, `NoteRepository`, `AuthRepository`, `PlayerRepository`,
  `RecentEventsRepository`, `NotificationRepository`, `MessagingRepository`,
  `StorageRepository` and `SessionReminderScheduler`. The domain declares them, not data —
  that is what inverts the dependency.

- **`usecase/`** — one class per operation, grouped by area (`auth`, `player`, `session`,
  `note`, `notification`). All of them follow the same mould:

  ```kotlin
  class ConfirmSessionDateUseCase @Inject constructor(
      private val eventRepository: EventRepository
  ) {
      suspend operator fun invoke(code: String, date: LocalDate, startTime: LocalTime? = null) =
          eventRepository.confirmDate(code, date, startTime)
  }
  ```

  The `operator fun invoke` is what lets a ViewModel write
  `confirmSessionDate(code, date, time)` so it reads as an action rather than a service.

  Most are one-liners, and that is fine: their job is to **name** the operation and to be
  the hole where logic goes the day it shows up. `ComputeDateSummariesUseCase` is the one
  with actual meat — it crosses dates, participants and availability, drops anything in the
  past and sorts by attendance.

### data

Everything that talks to the outside.

- **`repository/*Impl.kt`** — implement the domain interfaces against Firestore, Auth, FCM
  and `SharedPreferences`. Real-time `Flow`s are built with `callbackFlow` over snapshot
  listeners.
- **`work/`** — `SessionReminderSchedulerImpl` schedules the night-before reminder through
  WorkManager; `SessionReminderWorker` is what fires the local notification.
- **`service/`** — `SchedyMessagingService` receives FCM pushes.

### presentation

One folder per feature, holding the screen, its ViewModel and its state:

```
presentation/detail/
  EventDetailScreen.kt        the screen's composables
  EventDetailComponents.kt    pieces that belong to this screen only
  EventDetailDialogs.kt
  EventDetailViewModel.kt     + EventDetailUiState
  EventDetailPreviews.kt      @Preview
```

**MVVM as applied here:**

- Each screen's state is **a single immutable data class** (`XxxUiState`) exposed as a
  `StateFlow`. No `LiveData`, no loose pieces of state.
- The ViewModel **knows nothing about Compose**; the screen **knows nothing about
  repositories**. Only data and lambdas cross the line.
- Screens read with `collectAsState()` and call ViewModel functions. No business logic
  inside a composable.
- Ephemeral UI state (which dialog is open, scroll position) stays in the composable with
  `remember`; the ViewModel only holds what must survive a rotation.

### ui

The design system, with no dependency on any feature: `components/` (19 reusable pieces —
glass, calendars, the availability grid, the digital clock) and `theme/` (color,
typography, shapes, animations). See [ui.md](ui.md).

## Dependency injection

Hilt, with two modules:

- **`AppModule`** (`object`, `@Provides`) — third-party SDKs only: `FirebaseFirestore`,
  `FirebaseAuth`, `FirebaseMessaging`, `FirebaseStorage`.
- **`RepositoryModule`** (`abstract class`, `@Binds`) — ties every domain interface to its
  `Impl`. It is the only file that knows Firebase is back there.

Use cases need no module: they are built with `@Inject constructor` and Hilt assembles them.
ViewModels are annotated `@HiltViewModel`; the ones that depend on a session code pull it
from the `SavedStateHandle` (`savedStateHandle.get<String>("code")!!`), which is how
Navigation hands over the route argument.

## Navigation

A single `NavHost` in `presentation/navigation/SchedyNavGraph.kt`:

| Route | Screen |
|---|---|
| `onboarding` | Asks for a name. Start destination when no name is stored |
| `home` | Home: next session, the week, shortcuts |
| `create` | Create a session (two phases: details → code + your days) |
| `join?code={code}` | Join; the code arrives pre-filled from the link |
| `event/{code}` | Nested graph for one session |
| ├ `event/{code}/home` | Tab shell (detail, notes, calendar, profile) |
| └ `event/{code}/edit` | Edit my availability |

The nested graph matters: `event/{code}` is the scope of `EventDetailViewModel`, obtained
with `hiltViewModel(parentEntry)` so the tabs and the edit screen share one instance and one
Firestore listener instead of opening one each.

Deep links (`https://getschedy.web.app/join?code=…` and `schedy://…`) are declared on the
routes themselves with `navDeepLink`. See [links.md](links.md).

## Conventions

- **Use case naming:** `VerbNounUseCase`. ViewModels inject them without the suffix
  (`private val confirmSessionDate: ConfirmSessionDateUseCase`) **except** when that would
  collide with one of the ViewModel's own functions; then the suffix stays
  (`observeParticipantsUseCase`, `deleteNoteUseCase`). Without that care, a
  `private fun observeParticipants` ends up calling itself in a loop: it happened.
- **Comments:** in Spanish, and only where the *why* cannot be read off the code. Nothing
  that just restates the line below it.
- **UI copy:** always in `strings.xml` (ES) and `values-en/strings.xml` (EN). No literals in
  a composable — the app follows the system language.
- **Commits:** in Spanish, imperative, describing the change in behaviour rather than the
  files touched.
