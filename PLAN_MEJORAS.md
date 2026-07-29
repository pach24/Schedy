# 🎲 Plan de mejoras — Schednd

> **Objetivo:** convertir Schednd de "demo bonita" en una app que los grupos de rol **quieran abrir y mantener**.
>
> **Principio rector:** la competencia real no es D&D Beyond, es **el grupo de WhatsApp + un When2Meet**. Cada mejora debe hacer que coordinar la sesión cueste *menos esfuerzo que mandar un mensaje*. No buscamos fichas de personaje, dados ni mapas — buscamos **cerrar el bucle de coordinación** y **quitar fricción**.

---

## Estado actual (resumen del análisis)

**Lo que ya funciona bien**
- Arquitectura limpia: MVVM + Hilt + Firestore en tiempo real + Coroutines/Flow.
- Cálculo de mejores fechas por asistencia (`ComputeDateSummariesUseCase`, `AttendanceTier`).
- UI muy cuidada en Compose (heatmap de disponibilidad, cuenta atrás, notas con tags/plantillas, deep links).
- Auth anónima sin fricción de registro.

**Agujeros críticos detectados**
- 🔴 **Las notificaciones push NO se envían.** Hay cableado (`enqueueNotification`, `subscribeToEvent`) pero **no existe Cloud Function** que publique el FCM. Y solo las *notas* encolan algo — ni "cambió disponibilidad" ni "fecha confirmada" notifican.
- 🔴 **No hay reglas de seguridad de Firestore.** Cualquiera puede leer/escribir/borrar eventos ajenos.
- 🔴 **Marca inconsistente:** README, `strings.xml` y el código usaban nombres distintos.
- 🟠 **El nombre del jugador no se reutiliza:** el onboarding lo guarda pero Crear/Unirse lo piden otra vez.
- 🟠 **Solo se cuadra el día, no la hora** (aunque `DayTimeSlot`/`AvailabilitySlot`/`SlotCounts` ya existen sin usar).
- 🟡 Pestaña **Perfil vacía**, buscador del Home sin implementar, sin sesiones recurrentes.

---

## Resumen de priorización

| # | Mejora | Fase | Impacto | Esfuerzo | Backend |
|---|--------|------|---------|----------|---------|
| 1 | Notificaciones push reales | 0 | 🔥🔥🔥 | L | Sí (Cloud Function) |
| 2 | Reglas de seguridad Firestore | 0 | 🔥🔥🔥 | M | Sí (rules) |
| 3 | Unificar marca/nombre | 0 | 🔥 | S | No |
| 4 | Reutilizar nombre del jugador | 1 | 🔥🔥 | S | No |
| 5 | Hora / franja de la sesión | 1 | 🔥🔥 | M | No* |
| 6 | Recordatorio local + añadir a calendario | 1 | 🔥🔥 | M | No |
| 7 | Sesiones recurrentes / fechas sugeridas | 1 | 🔥🔥 | M | No |
| 8 | Pantalla de Perfil funcional | 2 | 🔥 | M | No |
| 9 | Quórum configurable por el DM | 2 | 🔥 | S | No* |
| 10 | "Avisar a los que faltan" | 2 | 🔥 | S | Depende de #1 |
| 11 | Buscador del Home / fix "próxima sesión" | 2 | 🔥 | S | No |
| 12 | i18n + tests de dominio | 3 | ➕ | M | No |

\* *Solo cambia el shape del documento en Firestore (compatible hacia atrás).*

Esfuerzo: **S** ≈ medio día · **M** ≈ 1–2 días · **L** ≈ 3–5 días (un dev).

---

## FASE 0 — Arreglar lo que parece estar y no funciona

> Esto es lo de mayor impacto: hoy la promesa estrella (que la app persiga a la gente por ti) **no se cumple**, y la base no es segura para publicar.

### 0.1 — Notificaciones push de punta a punta 🔴 ◑ (código listo, falta desplegar)
**Por qué:** sin esto el DM sigue persiguiendo gente por chat → no hay razón para abrir la app.

**Backend (nuevo)**
- [x] `functions/` en TypeScript (`package.json`, `tsconfig.json`, `src/index.ts`) + `firebase.json`.
- [x] Trigger `onDocumentCreated` en `events/{code}/pendingNotifications/{id}` → publica al topic `event_{code}` y borra el doc de la cola (también si el envío falla, para que no crezca).
- [x] Se envían mensajes **solo de datos** (sin bloque `notification`) para que `onMessageReceived` corra siempre y el cliente pueda filtrar por `senderId`.
- [ ] **Pendiente de ti:** activar **plan Blaze** y `firebase deploy --only functions`.

**Cliente**
- [x] `NotificationRepository` centraliza el encolado (fuera de `NoteRepository`, cuyo `enqueueNotification` se ha eliminado) con `notifyNewNote` / `notifyAvailabilityUpdated` / `notifyDateConfirmed` / `notifyDateCleared`.
- [x] Encolado al guardar disponibilidad y al confirmar/limpiar fecha, además de al crear nota.
- [x] `SchedndMessagingService`: descarta si `data.senderId == uid` propio, respeta `POST_NOTIFICATIONS` y abre `schednd://event/{code}`.
- [x] Nuevo deep link `schednd://event/{code}` en el nav graph y en el manifest.

**DoD:** ◑ el cliente ya encola y sabe recibir; el push real llega **cuando se despliegue la función**.

### 0.2 — Reglas de seguridad de Firestore 🔴 ✅ (falta desplegar)
**Por qué:** hoy el modelo es abierto; un actor malicioso puede borrar la mesa de cualquiera.

- [x] `firestore.rules` creadas:
  - `request.auth != null` obligatorio en todo; `match /{document=**}` final cerrado.
  - `events/{code}`: lectura para autenticados. **Create** si `creatorId == request.auth.uid`. **Update/Delete** solo el creador, y `creatorId` es inmutable.
  - `participants/{uid}`: cada usuario solo escribe **su** doc; el creador puede borrar los ajenos al eliminar la mesa.
  - `notes/{id}`: lectura autenticados; create/update/delete solo `authorId == request.auth.uid`.
  - `pendingNotifications`: create con `senderId == uid`; lectura/borrado solo desde la función (Admin SDK se salta las reglas).
- [x] **Decisión de producto:** confirmar fecha = **solo el DM**. Reflejado en reglas, en `EventDetailViewModel` (guarda en `confirmDate`/`setStartTime`/`clearConfirmedDate`) y en `MoreOptionsDialog` (los demás ven "Solo el DM puede fijar la fecha").
- [x] `firestore.indexes.json` con el índice compuesto `pinned desc, updatedAt desc` que ya necesitaba `observeNotes`.
- [ ] **Pendiente de ti:** `firebase deploy --only firestore:rules,firestore:indexes`.

**DoD:** ✅ reglas escritas y alineadas con la UI; queda desplegarlas.

### 0.3 — Unificar marca 🔴 ✅
**Decisión:** nombre oficial = **Schedy — Schedule and Role** (forma corta **Schedy**).
- [x] Nombre definitivo elegido: **Schedy**.
- [x] `strings.xml` (`app_name` → Schedy), `README.md` (título + overview), onboarding ("Bienvenido a Schedy") y fallback de notificación.
- [x] Textos de compartir externalizados a `strings.xml` (`share_event`) y unificados (los de Crear y Detalle divergían).

**DoD:** ✅ el nombre es idéntico en launcher, README y mensajes de compartir.
**Nota:** se mantienen internos (no son marca visible): package `com.schednd`, scheme `schednd://`, channel ID `schednd_events`, nombres de clase `Schednd*`.

---

## FASE 1 — Quitar fricción del bucle principal

### 1.1 — Reutilizar el nombre del jugador 🟠 ✅
**Por qué:** el onboarding guarda el nombre y luego te lo vuelve a pedir. `getPlayerName()` solo se usa para `isOnboardingComplete()`.

- [x] Inyectar `PlayerRepository` en `CreateEventViewModel`, `JoinEventViewModel` y `EventDetailViewModel`.
- [x] Pre-rellenar `creatorName` / `participantName` / `myName` con `getPlayerName()` (en `init`).
- [x] Guardar el nombre al crear / unirse / guardar disponibilidad (`savePlayerName`).

**DoD:** ✅ un usuario con onboarding hecho nunca vuelve a teclear su nombre por defecto. (Compila.)

### 1.2 — Hora / franja de la sesión 🟠 ✅ (v1)
**Por qué:** cuadrar el sábado pero seguir negociando "¿a qué hora?" deja el bucle a medias.

- [x] **v1:** campo `startTime` ("HH:mm", nullable) en `Event`, con `startLocalTime` derivado. `confirmDate(code, date, startTime)` y `setStartTime` en `EventRepository`; `clearConfirmedDate` limpia ambos. Compatible hacia atrás: las sesiones viejas leen `null`.
- [x] Tras elegir el día, el DM ve un `TimePicker`; descartarlo fija **solo el día** (la sesión sin hora sigue siendo válida).
- [x] La tarjeta de fecha confirmada muestra "12 de julio · 18:00".
- [x] El evento de calendario pasa a ser de 3 h reales cuando hay hora, y sigue siendo de día completo cuando no la hay.
- [ ] **v2 (opcional, ya medio modelado):** disponibilidad por `(fecha, franja)` reaprovechando `DayTimeSlot` / `AvailabilitySlot` / `SlotCounts`. Implica migrar el shape de `availableDates` y adaptar `AvailabilityGrid` + `ComputeDateSummariesUseCase`.
- [ ] **No hecho a propósito:** el texto de compartir es una *invitación a unirse*, no un resumen de la sesión, así que no lleva la hora. La cuenta atrás sigue en días, donde la hora no cambia nada.

**DoD:** ✅ la sesión confirmada incluye día **y** hora visibles para todos.

### 1.3 — Recordatorio local + añadir al calendario 🟠 ✅
**Por qué:** stickiness real sin depender del push de servidor.

- [x] **1.3a** Botón **"Añadir a calendario"** con `Intent(ACTION_INSERT, CalendarContract.Events.CONTENT_URI)`. Sin permisos extra. En la card de fecha confirmada de `EventDetailScreen` (evento de día completo; el usuario ajusta hora en su app de calendario). (Compila.)
- [x] **1.3b** Dependencia `androidx.work:work-runtime-ktx` (catálogo `work = "2.10.0"`).
- [x] **1.3b** `SessionReminderWorker` + `SessionReminderScheduler`: `OneTimeWorkRequest` único por código (`REPLACE`), aviso el día antes a la hora de la sesión (o a las 19:00 si no hay hora). Se reprograma/cancela desde el `collect` del `EventDetailViewModel`, así que también sigue los cambios hechos desde otro dispositivo. Al tocar el aviso se abre `schednd://event/{code}`.

**DoD:** ✅ "añadir a calendario" y recordatorio local, ambos hechos.

### 1.4 — Sesiones recurrentes / fechas sugeridas 🟠
**Por qué:** los grupos juegan semanal/quincenal; recrear disponibilidad cada vez es el mayor coste recurrente.

- [ ] **v1 pragmático:** al crear, selector "¿se repite? (semanal / quincenal)" que **pre-rellena fechas candidatas** en el grid (p. ej. los próximos 4 sábados). Sigue siendo un único evento con varias fechas candidatas.
- [ ] **v2:** acción "renovar para la próxima semana" que clona la sesión reutilizando participantes.

**DoD:** crear la mesa semanal no obliga a marcar fechas a mano una por una.

---

## FASE 2 — Stickiness y remates

### 2.1 — Pantalla de Perfil funcional
Hoy es `ComingSoonScreen` (pestaña muerta en la barra inferior).
- [ ] Editar nombre (`PlayerRepository`).
- [ ] Lista de tus sesiones + **salir de una sesión** (`RecentEventsRepository.removeEvent` + `unsubscribeFromEvent` + opcional borrar tu doc de participante).
- [ ] Toggle de tema (claro/oscuro/sistema) persistido.

### 2.2 — Quórum configurable por el DM
- [ ] Campo `minPlayers` en `Event`. Recalcular etiqueta: "✅ hay quórum" en vez de los umbrales fijos por % (86/71/57 %, hoy arbitrarios en `computeAttendanceTier`).

### 2.3 — "Avisar a los que faltan"
- [ ] Botón "{N} sin responder → recordar". Si Fase 0.1 está hecha, manda push; si no, comparte mensaje pre-redactado.

### 2.4 — Pulidos
- [ ] Implementar el buscador del Home (hoy `/* TODO buscar sesiones */`) o quitar el icono.
- [ ] Fix "próxima sesión": no usar el fallback `cards.firstOrNull()` (`HomeViewModel`) que muestra una sesión cualquiera como "PRÓXIMA" con cuenta atrás a cero cuando no hay fecha confirmada futura.

---

## FASE 3 — Calidad transversal (cuando haya tracción)

- [ ] **i18n:** externalizar a `strings.xml` los textos hoy hardcodeados en español; añadir inglés si se busca audiencia amplia. **Ojo:** `strings.xml` solo tiene 2 entradas — prácticamente todo el texto vive dentro de los Composables, así que esto es bastante más grande de lo que parece.
- [x] **Tests:** 15 unit tests de dominio (`ComputeDateSummariesUseCaseTest`, `AttendanceTierTest`) cubriendo conteo, ausentes, filtrado de fechas pasadas, orden por asistencia y monotonía de los umbrales. Verde con `./gradlew :app:testDebugUnitTest`.
- [ ] Revisar manejo de zonas horarias (fechas guardadas como UTC start-of-day).

---

## Fuera de alcance (deliberadamente)

Para no caer en *scope creep* hacia un D&D Beyond:
- ❌ Fichas de personaje, tirador de dados, reglas/SRD, mapas/VTT.
- ❌ Chat propio (la app empuja a calendario/notificaciones; el chat ya lo tienen).
- ❌ Cuentas con email/contraseña (la auth anónima es una ventaja de fricción).

---

## Recomendación de arranque

Empezar por **Fase 0 completa** (desbloquea el valor real + hace la base publicable) y, en paralelo, las victorias baratas de Fase 1 que **no tocan backend**: **1.1 (nombre)**, **1.3 (recordatorio + calendario)**.

Dos puntos de entrada posibles:
- **A) Impacto máximo:** 0.1 Notificaciones de punta a punta (Cloud Function + encolado en disponibilidad/confirmación).
- **B) Tangible hoy, sin desplegar nada:** 1.1 + 1.3 + 0.3 (todo en cliente).
