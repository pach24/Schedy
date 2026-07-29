# Development environment

## Getting it running

You need a recent Android Studio, JDK 17 (the one bundled with Studio works), and Node 18+
only if you are going to touch the Cloud Functions or the hosting site.

```bash
git clone https://github.com/pach24/Schedule-and-Role.git
# app/google-services.json must exist: download it from the Firebase console,
# project schednd, Android app com.schednd
./gradlew :app:assembleDebug
```

`minSdk 29`, `targetSdk 35`, `compileSdk 35`, Java 11 as the bytecode target.
Dependencies are declared in `gradle/libs.versions.toml` (version catalog) — don't hardcode
versions in `build.gradle.kts`.

## Everyday commands

```bash
./gradlew :app:assembleDebug                # build
./gradlew :app:installDebug                 # install on the connected device
./gradlew :app:assembleRelease              # signed APK (if you have the keystore)
./gradlew :app:bundleRelease                # AAB for Play
./gradlew :app:signingReport                # SHA-1 / SHA-256 per variant

adb devices -l                              # with more than one, everything needs -s <id>
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm get-app-links com.schednd      # App Link verification status
```

With **several devices connected** (phone + emulator) `adb` fails with *more than one
device*: every command then needs `-s <id>`. And mind you don't install on the real phone by
accident.

### Reinstalling without losing the session

A plain `adb install -r` won't do when the signature changes, and `pm clear` wipes the data
(you lose the stored name and the session list). To keep the data:

```bash
adb -s emulator-5554 shell pm uninstall -k --user 0 com.schednd   # -k keeps data
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Seeing the onboarding again

Onboarding is decided by a preference, not by the account. Deleting that one file is enough;
the Firebase session lives in another and survives:

```bash
adb shell "run-as com.schednd rm shared_prefs/player_prefs.xml"
adb shell am force-stop com.schednd
```

## Release signing

Credentials **never** go in the repo. `build.gradle.kts` reads four properties from
`~/.gradle/gradle.properties`:

```properties
SCHEDY_RELEASE_STORE_FILE=C:/path/to/schedy-release.jks
SCHEDY_RELEASE_STORE_PASSWORD=…
SCHEDY_RELEASE_KEY_ALIAS=schedy
SCHEDY_RELEASE_KEY_PASSWORD=…
```

Use forward slashes even on Windows. If any of the four is missing, or the file is not
there, the release **still builds, unsigned** — that way nobody who clones the repo without
the key gets a broken build.

Creating the keystore (once in the project's life):

```bash
keytool -genkeypair -v -keystore schedy-release.jks -alias schedy \
  -keyalg RSA -keysize 2048 -validity 10000
```

> Keep the `.jks` **and** its password in a password manager. Lose them and you can never
> update the app you distribute through GitHub; for Play you would have to request an upload
> key reset. `*.jks` and `*.keystore` are in `.gitignore`.

And remember: every new signing key means adding its SHA-256 to `assetlinks.json`, or links
stop opening the app. See [links.md](links.md).

## Firebase

Project `schednd` (the id is immutable; the hosting site is called `getschedy`, which is
what shows up in the links).

```bash
npm i -g firebase-tools
firebase login
firebase use                       # should say schednd

firebase deploy --only hosting     # uploads public/ (fallback page + assetlinks)
firebase deploy --only firestore:rules
firebase deploy --only functions   # TypeScript is compiled first, via predeploy
```

`firebase.json` has a deliberate trap: the hosting `ignore` list does **not** include the
`"**/.*"` entry Firebase adds by default. With it, `.well-known/` would never be uploaded
and App Link verification would never pass.

## Emulator gotchas

- **Disk space.** `/data` fills up and `installDebug` fails with
  `INSTALL_FAILED_INSUFFICIENT_STORAGE`. The `pm uninstall -k` trick above frees the old APK
  without losing data.
- **Animations.** To inspect an animation frame by frame:
  `adb shell settings put global animator_duration_scale 10` (set it back to `1` afterwards).
- **Tapping blind.** Take a screenshot before sending `input tap`. The state is not always
  what you assume: Android restores the previous screen on relaunch, and if somebody else is
  using the emulator your taps fight theirs.
