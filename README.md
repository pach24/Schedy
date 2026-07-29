# 🎲 Schedy — Schedule and Role

<p align="center">
  <img src= "https://github.com/user-attachments/assets/3b2e68e8-c741-4dfd-9485-27186a50398c" alt="Schedy Hero Banner" width="80%" />
</p>

<p align="center">
  <strong>Effortless scheduling for tabletop RPG groups.</strong><br/>
  Coordinate sessions, compare availability, and find the best dates — fast.
</p>

---

## ✨ Overview

**Schedy — Schedule and Role** is a modern Android application designed to solve the "scheduling boss fight" for tabletop RPG groups. It allows players to sync their availability in real-time, providing group leaders with data-driven insights to pick the perfect session date.

Instead of endless group chats and polls, participants submit their availability, and Schedy automatically highlights the best dates based on group attendance.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_|_Firestore_|_Cloud_Messaging-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)

 

---

## 🚀 Key Features

* **Real-time Synchronization:** Instant updates across all participants using Firebase Cloud Firestore.
* **Smart Attendance Tiers:** Algorithms that automatically categorize dates (Full, Viable, Limited, or Insufficient) based on group participation percentages.
* **Deep Linking:** Join events instantly via shared links or unique 6-character codes.
* **Push Notifications:** Built-in messaging service to notify users when group availability changes via Firebase Cloud Messaging.
* **Privacy-First:** Secure anonymous authentication, allowing users to participate without tedious sign-up flows.

---

## 🛠 Technical Stack & Architecture

Clean Architecture in three layers, with MVVM on top. Dependencies point inwards:
`presentation` and `data` both know `domain`; `domain` knows nobody.

```
presentation/   Compose screens + ViewModels (one immutable UiState per screen)
      │ use cases
domain/         models · repository interfaces · 32 use cases   ← pure Kotlin
      ▲ implements
data/           Firestore · Auth · FCM · SharedPreferences · WorkManager
```

* **UI:** 100% Jetpack Compose with Material 3, a custom squircle shape system and
  glass surfaces (haze + a refraction shader).
* **DI:** Hilt. `RepositoryModule` is the single place where a domain interface meets its
  Firebase implementation.
* **Async:** Coroutines and Flow — screens listen to Firestore snapshots, they never poll.
* **Backend:** Cloud Firestore, anonymous Firebase Auth, FCM push published by a Cloud
  Function, and Firebase Hosting for the verified Android App Links.
* **Builds:** Gradle version catalogs; release signing read from `~/.gradle/gradle.properties`.

---

## 📖 Documentation

In-depth docs live in [`docs/`](docs/):

| | |
|---|---|
| [architecture.md](docs/architecture.md) | Layers, MVVM, use cases and the reasoning behind them |
| [structure.md](docs/structure.md) | Package map: where everything lives |
| [data.md](docs/data.md) | Firestore model, security rules, notification queue |
| [environment.md](docs/environment.md) | Setup, everyday commands, release signing |
| [links.md](docs/links.md) | App Links: why the shared link opens the app |
| [ui.md](docs/ui.md) | Design system: type, squircles, glass, animations |

---

## ⚙️ Requirements & Setup

* **Min SDK:** 29 · **Target SDK:** 35 · **Java:** 11+ (JDK 17 to build)

1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Build the project using the included Gradle wrapper: `./gradlew :app:assembleDebug`.

Release signing, Firebase deploys and the App Links setup are covered in
[docs/environment.md](docs/environment.md) and [docs/links.md](docs/links.md).

---

