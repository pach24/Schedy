# Data: Firestore, rules and notifications

## The model

Everything hangs off the session, and **the document id is the six-character code itself**
(`EventCodeGenerator`). There is no users collection: a player is an anonymous uid plus a
name repeated in every session they belong to.

```
events/{code}
├── name           String     "Weekly D&D session"
├── creatorId      String     the DM's uid; the only one who can set the date or delete
├── createdAt      Timestamp  origin of the waiting bar in the home hero
├── confirmedDate  Timestamp? chosen day (stored at noon UTC, read as a bare date)
├── startTime      String?    "HH:mm"; null = day only
│
├── participants/{uid}
│   ├── name            String
│   ├── availableDates  Timestamp[]
│   └── notes           String[]
│
├── notes/{noteId}
│   ├── authorId, authorName
│   ├── title, body
│   ├── tag        String   TRAMA · LOOT · NPC · PERSONAJE · OTROS
│   ├── pinned     Boolean
│   └── createdAt, updatedAt
│
└── pendingNotifications/{id}    ephemeral queue, see below
```

**Why `confirmedDate` is stored at noon UTC:** it is a date, not an instant. Stored at
12:00 UTC, no reasonable time zone shifts it to the previous or next day when read back.
The actual time lives separately in `startTime`, as text.

**Why the list of your sessions is not in Firestore:** there is nowhere to hang it without
inventing a users collection. It lives in `SharedPreferences` (`recent_events.xml`), managed
by `RecentEventsRepository`. Practical consequence: **uninstalling the app loses the list**,
though the sessions themselves are still there and come back with the code.

## The rules

`firestore.rules`. The gist in a few lines:

- **Reading:** any authenticated user can read any event. That is intentional — it is what
  joining with a code you don't own yet requires. The code is the only key.
- **Writing the event:** create only in your own name (`creatorId == request.auth.uid`).
  Setting the date, changing the time or deleting the table: creator only, and `creatorId`
  is immutable.
- **Participants:** each user writes only their own document (`uid == auth.uid`). The
  creator can additionally delete participants, so the table can be cleaned up on delete.
- **Notes:** created by whoever signs as the author; edited and deleted by the author only.
- **Notification queue:** clients may only create. Read, update and delete are shut — that
  is the function's job, and it runs with the Admin SDK, which bypasses rules.
- Anything not covered above: denied.

## How a notification travels

Pushes are not sent from the phone (that would need the server key on the client, which is
exactly what we avoid). The path is:

```
App ──creates doc──▶ events/{code}/pendingNotifications/{id}
                            │
                            ▼  onDocumentCreated (europe-west1)
                  publishPendingNotification
                            │
                            ├──▶ FCM topic  event_{code}
                            └──▶ deletes the queue doc
                                       │
                                       ▼
                      SchedyMessagingService on every phone
```

Two design details worth not breaking:

1. **Messages are data-only**, with no `notification` block. That way `onMessageReceived`
   always runs on the client, background included, so it can **drop the notification it
   triggered itself** by comparing `senderId` with its own uid. With a notification
   message, Android would render it on its own and the author would notify themselves.
2. **The queue doc is deleted no matter what** (`finally`), even if sending fails: a
   notification is ephemeral and the queue must not grow.

Notification types and who enqueues them: `NEW_NOTE` (note editor),
`AVAILABILITY_UPDATED`, `DATE_CONFIRMED` and `DATE_CLEARED` (session detail).

Topic opt-in (`event_{code}`) is done by `SubscribeToSessionUseCase` when creating or
joining a session.

## The night-before reminder

This one never reaches the server: it is local. `ScheduleSessionReminderUseCase` →
`SessionReminderSchedulerImpl` schedules a `OneTimeWorkRequest` for the previous evening (at
the session's time, or at a civilised hour when only the day is known). It is rescheduled
with `REPLACE` every time a date change arrives from Firestore, so reminders never pile up.
If the date has already passed, it cancels instead.

## Changing the model without breaking anyone

Models are deserialized straight through `toObject()`, so **every field has a default
value**. A new field on an old document arrives as the default rather than as a failure —
that is why `startTime` is a `String?` with a comment saying so. If you add a field, give it
a default and assume existing documents don't have it.
