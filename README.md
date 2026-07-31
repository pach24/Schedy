# Schedy — Schedule and Role

<p align="center">
  <img src="https://github.com/user-attachments/assets/87742d9e-67c5-483a-9d7e-1a7cd0eb3bdd" alt="Schedy Hero Banner" width="80%" />
</p>

<p align="center">
  <strong>Getting five adults in the same room on the same evening, without the group chat.</strong>
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.1" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=android&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://firebase.google.com/"><img src="https://img.shields.io/badge/Firebase-Firestore_|_Auth_|_FCM-FFCA28?logo=firebase&logoColor=black" alt="Firebase" /></a>
  <img src="https://img.shields.io/badge/Architecture-Clean_+_MVVM-2DC653" alt="Clean Architecture + MVVM" />
  <img src="https://img.shields.io/badge/minSdk-29-3DDC84?logo=android&logoColor=white" alt="minSdk 29" />
</p>

---

## Live Demo

<p align="center">
  <img src="https://github.com/user-attachments/assets/a70c71ea-0505-45e0-9cb3-4bcc2872b059" width="30%" alt="Schedy screenshot 1" />
  &nbsp;
  <img src="https://github.com/user-attachments/assets/e173d831-9950-44ea-93d6-3b92a38f089f" width="30%" alt="Schedy screenshot 2" />
  &nbsp;
  <img src="https://github.com/user-attachments/assets/88137bbb-4a8f-4a58-9515-89124a2e2710" width="30%" alt="Schedy screenshot 3" />
</p>

<!--
  Nota: ajustá los alt="" de arriba con lo que muestra cada pantalla
  (ej. "Countdown", "Grilla de disponibilidad", "Notas de sesión")
  para que quede accesible y quede mejor documentado.
-->

## The problem

Scheduling a tabletop session is the real boss fight. Somebody asks "who can on Friday?",
four people answer at different times, two of them change their mind, and a week later the
group still has no date.

Schedy replaces that thread. Everyone marks the days they can make it, the app ranks the
dates by how much of the group shows up, and the DM picks one. Everybody gets a push, and a
reminder the evening before.

## How it works

1. **Create a session.** You get a six-character code and a shareable link.
2. **Share it.** The link is a verified Android App Link: tapping it opens Schedy straight
   on the join screen with the code already filled in. No app? A fallback page shows the
   code to type by hand.
3. **Everyone marks their days.** The availability grid updates live, for everyone, as
   people tap.
4. **The DM sets date and time.** Recommended dates come sorted by attendance; the time is
   picked on a digital clock. The group gets notified, and each phone schedules its own
   reminder for the night before.

No sign-up anywhere in that flow: players come in through anonymous auth, and the only
identity is the name they type once.

## Features

- **Live availability grid** — Firestore snapshots, not polling. Another player's tap moves
  your screen.
- **Attendance tiers** — every date is scored against the group: *full* (≥86%), *viable*
  (≥71%), *limited* (≥57%) or *insufficient*, with the names of who is missing.
- **Session codes and App Links** — six characters, or an `https://getschedy.web.app/join`
  link verified against the app's signing certificate.
- **Shared notes** — plot, loot, NPCs, characters. Tagged, pinnable, with templates, and the
  group gets a push when someone writes one.
- **Countdown hero** — days left, start time, the wait as a progress bar, and a live
  breakdown down to seconds.
- **Reminders** — a local WorkManager notification the evening before, rescheduled whenever
  the date changes, plus one-tap *add to calendar*.
- **Spanish and English**, following the system language, with per-app language support.
- **Light and dark**, with a custom design system: Golos Text, superellipse ("squircle")
  corners, and glass surfaces built on a refraction shader with a blur fallback.

## Architecture

Clean architecture in three layers with MVVM on top. Dependencies point inwards:
`presentation` and `data` both know `domain`; `domain` knows nobody.

```
presentation/   Compose screens + ViewModels — one immutable UiState per screen
      │ use cases
domain/         models · repository interfaces · 32 use cases      ← pure Kotlin
      ▲ implements
data/           Firestore · Auth · FCM · SharedPreferences · WorkManager
```

The practical upshot: **no ViewModel knows a repository**, and Firebase is confined to
`data/`. `RepositoryModule` is the single file where a domain interface meets its Firebase
implementation — the seam you would cut to swap the backend.

| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, custom shape and glass system |
| Presentation | MVVM, one `StateFlow<XxxUiState>` per screen |
| Domain | Plain Kotlin: models, repository interfaces, one use case per operation |
| Data | Cloud Firestore, anonymous Firebase Auth, FCM, SharedPreferences, WorkManager |
| DI | Hilt — `AppModule` for third-party SDKs, `RepositoryModule` for `@Binds` |
| Async | Coroutines and Flow, `callbackFlow` over Firestore listeners |
| Backend | Firestore security rules + one Cloud Function that publishes pushes |
| Build | Gradle version catalogs; release signing read from `~/.gradle/gradle.properties` |

```
app/src/main/java/com/schednd/
├── data/          repository impls · FCM service · WorkManager
├── domain/        model · repository (interfaces) · usecase · util
├── di/            AppModule · RepositoryModule
├── presentation/  onboarding · home · create · join · detail · notes · session · navigation
└── ui/            components · theme
```

## Documentation

The deep dives live in [`docs/`](docs/):

| | |
|---|---|
| [architecture.md](docs/architecture.md) | The layers, MVVM as applied here, DI, navigation, conventions |
| [structure.md](docs/structure.md) | Package map and the files you will open most |
| [data.md](docs/data.md) | Firestore model, security rules, how a notification travels |
| [environment.md](docs/environment.md) | Setup, everyday commands, release signing, emulator gotchas |
| [links.md](docs/links.md) | App Links: the signing-fingerprint rule and how to verify it |
| [ui.md](docs/ui.md) | Design system: type, squircles, glass, animation patterns |

## Getting started

**Requirements:** Android Studio (recent), JDK 17 to build, `minSdk 29` / `targetSdk 35`.
Node 18+ only if you touch the Cloud Functions or the hosting site.

```bash
git clone https://github.com/pach24/Schedule-and-Role.git
# drop your google-services.json into app/  (Firebase console → Android app com.schednd)
./gradlew :app:assembleDebug
```

The release build is signed from properties in `~/.gradle/gradle.properties`; without them
it still builds, just unsigned. Signing, deploys and App Link verification are covered in
[docs/environment.md](docs/environment.md) and [docs/links.md](docs/links.md).

## Backend

Everything runs on one Firebase project:

- **Firestore** — sessions keyed by their own code, with participants, notes and an
  ephemeral notification queue as subcollections. Rules in [`firestore.rules`](firestore.rules).
- **Cloud Function** (`functions/`) — watches the queue and publishes each item to the
  session's FCM topic as a data-only message, so the client can drop the notification it
  triggered itself.
- **Hosting** (`public/`) — serves the shared link's fallback page and the
  `assetlinks.json` that makes App Link verification pass.

## Status

Working end to end: create, join, availability, dates, times, notes, notifications and
reminders. The profile tab is still a placeholder, and file storage is wired but unused —
both are waiting on the profile screen. [`PLAN_MEJORAS.md`](PLAN_MEJORAS.md) tracks where the
app is heading (in Spanish).

Code comments and commit messages are in Spanish; documentation is in English.
