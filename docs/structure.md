# Project structure

Where everything lives, so you don't have to open six folders looking for one file.

```
Schednd/
├── app/                      the Android app
├── functions/                Cloud Functions (TypeScript)
├── public/                   the Firebase Hosting site
│   ├── index.html            fallback page for the shared link
│   └── .well-known/
│       └── assetlinks.json   signing fingerprints for App Links
├── docs/                     this
├── firebase.json             hosting + firestore + functions
├── firestore.rules           who can read and write
└── .firebaserc               project: schednd
```

## app/src/main/java/com/schednd

```
├── MainActivity.kt           picks onboarding vs home and hosts the NavHost
├── SchedyApp.kt              @HiltAndroidApp
│
├── data/
│   ├── repository/           9 *Impl files: Firestore, Auth, FCM, SharedPreferences
│   ├── service/              SchedyMessagingService — receives pushes
│   └── work/                 SessionReminderSchedulerImpl + SessionReminderWorker
│
├── domain/
│   ├── model/                Event, Participant, Note, NoteTag, NoteTemplate,
│   │                         DateSummary, AttendanceTier, DayTimeSlot, AvailabilitySlot
│   ├── repository/           9 interfaces (the domain's ports)
│   ├── usecase/
│   │   ├── auth/             EnsureSignedIn, GetCurrentUserId
│   │   ├── player/           GetPlayerName, SavePlayerName, IsOnboardingComplete
│   │   ├── session/          18 — create, observe, availability, date, reminders,
│   │   │                     saved codes, notification opt-in, ComputeDateSummaries
│   │   ├── note/             Observe, Create, Update, Delete, TogglePin
│   │   └── notification/     NotifyNewNote, NotifyAvailabilityUpdated,
│   │                         NotifyDateConfirmed, NotifyDateCleared
│   └── util/                 EventCodeGenerator
│
├── di/
│   ├── AppModule.kt          Firebase SDKs
│   └── RepositoryModule.kt   @Binds interface → Impl
│
├── presentation/
│   ├── onboarding/           ask for a name
│   ├── home/                 home: next-session hero, the week, listing
│   ├── create/               create a session
│   ├── join/                 join by code
│   ├── detail/               the session: grid, recommended dates, set the date
│   ├── notes/                note list and editor
│   ├── session/              tab shell for one session (+ tabs/)
│   └── navigation/           SchedyNavGraph
│
└── ui/
    ├── components/           19 reusable pieces
    └── theme/                Color, Type, Shape, Theme, Animations
```

## The files you will open most

| File | When you'll need it |
|---|---|
| `presentation/navigation/SchedyNavGraph.kt` | Adding a screen or a deep link |
| `di/RepositoryModule.kt` | Adding a repository |
| `res/values/strings.xml` + `values-en/` | Any visible copy. Both files, always |
| `ui/theme/Color.kt`, `Type.kt` | Touching the palette or the type scale |
| `ui/components/` | Before writing a component: check whether it exists |
| `firestore.rules` | Changing who may write what |

## Resources

```
app/src/main/res/
├── values/strings.xml        Spanish (default locale)
├── values-en/strings.xml     English
├── values-night/             dark theme colors
├── font/                     Golos Text, six weights
├── drawable/                 custom icons for the bottom bar and the app
└── xml/locales_config.xml    supported languages (per-app language)
```

Adding copy means touching **both** `strings.xml` files. If the English one is missing, the
app silently falls back to Spanish, and that shows up later in production as a stray
Spanish sentence.
