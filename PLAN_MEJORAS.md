# 🎲 Improvement plan — Schednd

> **Goal:** turn Schednd from "pretty demo" into an app that tabletop groups **want to open and keep**.
>
> **Guiding principle:** the real competition isn't D&D Beyond, it's **the WhatsApp group + a When2Meet**. Every improvement has to make coordinating the session cost *less effort than sending a message*. We're not after character sheets, dice or maps — we're after **closing the coordination loop** and **removing friction**.

---

## Where things stand (analysis summary)

**What already works well**
- Clean architecture: MVVM + Hilt + real-time Firestore + Coroutines/Flow.
- Best-date computation by attendance (`ComputeDateSummariesUseCase`, `AttendanceTier`).
- Very polished Compose UI (availability heatmap, countdown, notes with tags/templates, deep links).
- Anonymous auth, no sign-up friction.

**Critical gaps found**
- 🔴 **Push notifications are NOT sent.** The wiring is there (`enqueueNotification`, `subscribeToEvent`) but **there is no Cloud Function** publishing the FCM message. And only *notes* enqueue anything — neither "availability changed" nor "date confirmed" notify.
- 🔴 **No Firestore security rules.** Anyone can read/write/delete somebody else's events.
- 🔴 **Inconsistent branding:** README, `strings.xml` and the code used different names.
- 🟠 **The player's name isn't reused:** onboarding saves it, but Create/Join ask for it again.
- 🟠 **Only the day is agreed, not the time** (even though `DayTimeSlot`/`AvailabilitySlot`/`SlotCounts` already exist, unused).
- 🟡 Empty **Profile** tab, Home search not implemented, no recurring sessions.

---

## Prioritisation summary

| # | Improvement | Phase | Impact | Effort | Backend |
|---|-------------|-------|--------|--------|---------|
| 1 | Real push notifications | 0 | 🔥🔥🔥 | L | Yes (Cloud Function) |
| 2 | Firestore security rules | 0 | 🔥🔥🔥 | M | Yes (rules) |
| 3 | Unify branding/name | 0 | 🔥 | S | No |
| 4 | Reuse the player's name | 1 | 🔥🔥 | S | No |
| 5 | Session time / slot | 1 | 🔥🔥 | M | No* |
| 6 | Local reminder + add to calendar | 1 | 🔥🔥 | M | No |
| 7 | Recurring sessions / suggested dates | 1 | 🔥🔥 | M | No |
| 8 | Working Profile screen | 2 | 🔥 | M | No |
| 9 | Quorum configurable by the DM | 2 | 🔥 | S | No* |
| 10 | "Nudge the missing players" | 2 | 🔥 | S | Depends on #1 |
| 11 | Home search / fix "next session" | 2 | 🔥 | S | No |
| 12 | i18n + domain tests | 3 | ➕ | M | No |

\* *Only changes the document shape in Firestore (backwards compatible).*

Effort: **S** ≈ half a day · **M** ≈ 1–2 days · **L** ≈ 3–5 days (one dev).

---

## PHASE 0 — Fix what looks like it's there but isn't

> This is the highest-impact work: today the headline promise (that the app chases people for you) **isn't kept**, and the foundation isn't safe to publish on.

### 0.1 — End-to-end push notifications 🔴 ◑ (code ready, deploy pending)
**Why:** without this the DM keeps chasing people over chat → no reason to open the app.

**Backend (new)**
- [x] `functions/` in TypeScript (`package.json`, `tsconfig.json`, `src/index.ts`) + `firebase.json`.
- [x] `onDocumentCreated` trigger on `events/{code}/pendingNotifications/{id}` → publishes to the `event_{code}` topic and deletes the queue doc (also when sending fails, so it doesn't grow).
- [x] Messages are sent **data-only** (no `notification` block) so `onMessageReceived` always runs and the client can filter by `senderId`.
- [ ] **On you:** enable the **Blaze plan** and run `firebase deploy --only functions`.

**Client**
- [x] `NotificationRepository` centralises enqueuing (out of `NoteRepository`, whose `enqueueNotification` has been removed) with `notifyNewNote` / `notifyAvailabilityUpdated` / `notifyDateConfirmed` / `notifyDateCleared`.
- [x] Enqueue on saving availability and on confirming/clearing the date, on top of creating a note.
- [x] `SchedndMessagingService`: drops the message if `data.senderId == uid` is your own, respects `POST_NOTIFICATIONS` and opens `schednd://event/{code}`.
- [x] New deep link `schednd://event/{code}` in the nav graph and the manifest.

**DoD:** ◑ the client enqueues and knows how to receive; the real push arrives **once the function is deployed**.

### 0.2 — Firestore security rules 🔴 ✅ (deploy pending)
**Why:** today the model is wide open; a malicious actor can delete anyone's table.

- [x] `firestore.rules` written:
  - `request.auth != null` required everywhere; final `match /{document=**}` closed off.
  - `events/{code}`: read for authenticated users. **Create** if `creatorId == request.auth.uid`. **Update/Delete** for the creator only, and `creatorId` is immutable.
  - `participants/{uid}`: each user only writes **their own** doc; the creator can delete other people's when deleting the table.
  - `notes/{id}`: read for authenticated users; create/update/delete only if `authorId == request.auth.uid`.
  - `pendingNotifications`: create with `senderId == uid`; read/delete only from the function (the Admin SDK bypasses rules).
- [x] **Product decision:** confirming the date = **DM only**. Reflected in the rules, in `EventDetailViewModel` (guards in `confirmDate`/`setStartTime`/`clearConfirmedDate`) and in `MoreOptionsDialog` (everyone else sees "Only the DM can set the date").
- [x] `firestore.indexes.json` with the composite index `pinned desc, updatedAt desc` that `observeNotes` already needed.
- [ ] **On you:** `firebase deploy --only firestore:rules,firestore:indexes`.

**DoD:** ✅ rules written and aligned with the UI; deploying them is what's left.

### 0.3 — Unify branding 🔴 ✅
**Decision:** official name = **Schedy — Schedule and Role** (short form **Schedy**).
- [x] Final name chosen: **Schedy**.
- [x] `strings.xml` (`app_name` → Schedy), `README.md` (title + overview), onboarding ("Welcome to Schedy") and notification fallback.
- [x] Share texts externalised to `strings.xml` (`share_event`) and unified (Create's and Detail's had diverged).

**DoD:** ✅ the name is identical in the launcher, the README and the share messages.
**Note:** internals are kept as they are (they aren't visible branding): package `com.schednd`, scheme `schednd://`, channel ID `schednd_events`, class names `Schednd*`.

---

## PHASE 1 — Take friction out of the main loop

### 1.1 — Reuse the player's name 🟠 ✅
**Why:** onboarding saves the name and then asks for it again. `getPlayerName()` is only used for `isOnboardingComplete()`.

- [x] Inject `PlayerRepository` into `CreateEventViewModel`, `JoinEventViewModel` and `EventDetailViewModel`.
- [x] Pre-fill `creatorName` / `participantName` / `myName` with `getPlayerName()` (in `init`).
- [x] Save the name on create / join / save availability (`savePlayerName`).

**DoD:** ✅ a user who has finished onboarding never types their default name again. (Compiles.)

### 1.2 — Session time / slot 🟠 ✅ (v1)
**Why:** pinning down Saturday but still negotiating "what time?" leaves the loop half closed.

- [x] **v1:** `startTime` field ("HH:mm", nullable) on `Event`, with a derived `startLocalTime`. `confirmDate(code, date, startTime)` and `setStartTime` on `EventRepository`; `clearConfirmedDate` clears both. Backwards compatible: old sessions read `null`.
- [x] After picking the day, the DM gets a `TimePicker`; dismissing it sets **the day only** (a session with no time is still valid).
- [x] The confirmed-date card shows "12 July · 18:00".
- [x] The calendar event becomes a real 3 h event when there is a time, and stays all-day when there isn't.
- [ ] **v2 (optional, already half modelled):** availability per `(date, slot)` reusing `DayTimeSlot` / `AvailabilitySlot` / `SlotCounts`. Means migrating the shape of `availableDates` and adapting `AvailabilityGrid` + `ComputeDateSummariesUseCase`.
- [ ] **Deliberately not done:** the share text is an *invitation to join*, not a session summary, so it doesn't carry the time. The countdown is still in days, where the time changes nothing.

**DoD:** ✅ the confirmed session includes day **and** time, visible to everyone.

### 1.3 — Local reminder + add to calendar 🟠 ✅
**Why:** real stickiness without depending on server push.

- [x] **1.3a** **"Add to calendar"** button with `Intent(ACTION_INSERT, CalendarContract.Events.CONTENT_URI)`. No extra permissions. On the confirmed-date card in `EventDetailScreen` (all-day event; the user adjusts the time in their calendar app). (Compiles.)
- [x] **1.3b** `androidx.work:work-runtime-ktx` dependency (catalog `work = "2.10.0"`).
- [x] **1.3b** `SessionReminderWorker` + `SessionReminderScheduler`: a single `OneTimeWorkRequest` per code (`REPLACE`), notice the day before at the session's time (or at 19:00 if there is none). It is rescheduled/cancelled from `EventDetailViewModel`'s `collect`, so it also follows changes made from another device. Tapping the notice opens `schednd://event/{code}`.

**DoD:** ✅ "add to calendar" and local reminder, both done.

### 1.4 — Recurring sessions / suggested dates 🟠
**Why:** groups play weekly/fortnightly; recreating availability every time is the biggest recurring cost.

- [ ] **Pragmatic v1:** on create, a "does it repeat? (weekly / fortnightly)" selector that **pre-fills candidate dates** in the grid (e.g. the next 4 Saturdays). It's still a single event with several candidate dates.
- [ ] **v2:** a "renew for next week" action that clones the session reusing the participants.

**DoD:** creating the weekly table doesn't force you to mark dates by hand one by one.

---

## PHASE 2 — Stickiness and finishing touches

### 2.1 — Working Profile screen
Today it's `ComingSoonScreen` (a dead tab in the bottom bar).
- [ ] Edit name (`PlayerRepository`).
- [ ] List of your sessions + **leave a session** (`RecentEventsRepository.removeEvent` + `unsubscribeFromEvent` + optionally delete your participant doc).
- [ ] Theme toggle (light/dark/system), persisted.

### 2.2 — Quorum configurable by the DM
- [ ] `minPlayers` field on `Event`. Recompute the label: "✅ quorum reached" instead of the fixed % thresholds (86/71/57 %, arbitrary today in `computeAttendanceTier`).

### 2.3 — "Nudge the missing players"
- [ ] "{N} haven't answered → remind" button. If Phase 0.1 is done, it sends a push; if not, it shares a pre-written message.

### 2.4 — Polish
- [ ] Implement the Home search (today `/* TODO buscar sesiones */`) or drop the icon.
- [ ] Fix "next session": stop using the `cards.firstOrNull()` fallback (`HomeViewModel`), which shows an arbitrary session as "NEXT" with a countdown at zero when there is no future confirmed date.

---

## PHASE 3 — Cross-cutting quality (once there's traction)

- [ ] **i18n:** externalise to `strings.xml` the text currently hardcoded in Spanish; add English if we want a wider audience. **Careful:** `strings.xml` only has 2 entries — practically all the text lives inside the Composables, so this is quite a bit bigger than it looks.
- [x] **Tests:** 15 domain unit tests (`ComputeDateSummariesUseCaseTest`, `AttendanceTierTest`) covering counting, absentees, filtering past dates, ordering by attendance and monotonicity of the thresholds. Green with `./gradlew :app:testDebugUnitTest`.
- [ ] Review time zone handling (dates stored as UTC start-of-day).

---

## Out of scope (deliberately)

To avoid scope creep towards a D&D Beyond:
- ❌ Character sheets, dice roller, rules/SRD, maps/VTT.
- ❌ Our own chat (the app pushes to calendar/notifications; they already have chat).
- ❌ Email/password accounts (anonymous auth is a friction advantage).

---

## Suggested starting point

Start with **all of Phase 0** (unlocks the real value + makes the foundation publishable) and, in parallel, the cheap wins from Phase 1 that **don't touch the backend**: **1.1 (name)**, **1.3 (reminder + calendar)**.

Two possible entry points:
- **A) Maximum impact:** 0.1 End-to-end notifications (Cloud Function + enqueuing on availability/confirmation).
- **B) Tangible today, nothing to deploy:** 1.1 + 1.3 + 0.3 (all client-side).