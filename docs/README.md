# Schedy documentation

Notes for whoever touches this code next — including you, six months from now.
The root README says what the app is; this says how it is built, and why.

| Document | What it covers |
|---|---|
| [architecture.md](architecture.md) | The layers, who may call whom, and the reasoning |
| [structure.md](structure.md) | Package map: where everything lives |
| [data.md](data.md) | Firestore: collections, rules, the notification queue and the Cloud Function |
| [environment.md](environment.md) | Getting the project running, signing, everyday commands |
| [links.md](links.md) | App Links: why the shared link opens the app, and what breaks it |
| [ui.md](ui.md) | Design system: type, squircles, glass and animations |

> Code comments and commit messages are in Spanish; the documentation is in English.

## The short version

Schedy solves exactly one problem: **agreeing on a date for a tabletop RPG session**.
Someone creates a session, shares a six-character code, every player marks the days they
can make it, and the app ranks the dates by attendance. The DM picks one and everybody
gets a push.

Three decisions explain most of the code:

1. **There is no sign-up.** Players come in through Firebase anonymous auth. "Who you are"
   is an opaque uid plus a name stored on the device. That is why there is no login screen,
   and why the list of your sessions lives in `SharedPreferences` rather than on the server.
2. **The session code is the key.** Whoever has it, gets in. Firestore rules let any
   authenticated user read any event, because that is what joining by code requires. What
   is fenced off is writing.
3. **Real-time for real.** Screens listen to Firestore `Flow`s, they never poll. When
   another player marks a day, the grid moves on its own.
