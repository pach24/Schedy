# 🎲 S&R — Schedule and Role

<p align="center">
  <img src= "https://github.com/user-attachments/assets/3b2e68e8-c741-4dfd-9485-27186a50398c" alt="S&R Hero Banner" width="80%" />
</p>

<p align="center">
  <strong>Effortless scheduling for tabletop RPG groups.</strong><br/>
  Coordinate sessions, compare availability, and find the best dates — fast.
</p>

---

## ✨ Overview

**S&R — Schedule and Role** is a modern Android application designed to solve the "scheduling boss fight" for tabletop RPG groups. It allows players to sync their availability in real-time, providing group leaders with data-driven insights to pick the perfect session date.

Instead of endless group chats and polls, participants submit their availability, and S&R automatically highlights the best dates based on group attendance.

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

This project was built following **Clean Architecture** principles and **SOLID** design patterns to ensure scalability and testability.

* **UI Layer:** 100% Jetpack Compose with Material 3. Implements advanced UI components like Custom Calendars and Availability Grids.
* **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern.
* **Dependency Injection:** Hilt (Dagger) for managing scoped dependencies and improving modularity.
* **Asynchronous Programming:** Kotlin Coroutines and Flow for handling reactive data streams from Firestore.
* **Backend Services:**
    * **Cloud Firestore:** Real-time NoSQL database.
    * **Firebase Auth:** For seamless anonymous user sessions.
    * **Cloud Messaging (FCM):** Topic-based push notifications for event updates.
* **Dependency Management:** Gradle Version Catalogs for a unified and clean build configuration.

---

## 📁 Project Highlights

* **Domain Logic:** Centrally managed Use Cases (e.g., `ComputeDateSummariesUseCase`) that handle complex availability calculations independently of the UI.
* **Modern Navigation:** Uses the Compose Navigation component with a centralized and type-safe approach.
* **Custom Theming:** A fully implemented Material 3 theme with support for dynamic colors and specialized RPG-style typography.
* **Efficient Data Access:** Robust Repository pattern implementation that abstracts Firebase complexity from the business logic.

---

## ⚙️ Requirements & Setup

* **Min SDK:** 29
* **Target SDK:** 35
* **Java Version:** 11+

1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Build the project using the included Gradle wrapper: `./gradlew assembleDebug`.
---

