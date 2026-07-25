import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";
import { initializeApp } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";

initializeApp();

/**
 * Los clientes encolan avisos en `events/{code}/pendingNotifications/{id}`.
 * Esta función los publica al topic `event_{code}` y borra el doc de la cola.
 *
 * Se envían mensajes SOLO de datos (sin bloque `notification`) a propósito:
 * así `onMessageReceived` se ejecuta siempre en el cliente, también con la
 * app en segundo plano, y este puede descartar el aviso que él mismo disparó
 * comparando `senderId` con su propio uid.
 */
export const publishPendingNotification = onDocumentCreated(
  {
    document: "events/{code}/pendingNotifications/{id}",
    region: "europe-west1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const code = event.params.code;
    const data = snap.data() ?? {};

    const { title, body } = buildMessage(code, data);

    try {
      await getMessaging().send({
        topic: `event_${code}`,
        data: {
          type: String(data.type ?? "GENERIC"),
          code,
          senderId: String(data.senderId ?? ""),
          title,
          body,
        },
        android: { priority: "high" },
      });
      logger.info(`Aviso publicado en event_${code}`, { type: data.type });
    } catch (err) {
      logger.error(`Fallo publicando en event_${code}`, err);
    } finally {
      // La cola no debe crecer aunque el envío falle: el aviso es efímero.
      await snap.ref.delete();
    }
  }
);

type Payload = Record<string, unknown>;

function buildMessage(code: string, data: Payload): { title: string; body: string } {
  const who = String(data.authorName ?? data.senderName ?? "Alguien");

  switch (data.type) {
    case "NEW_NOTE":
      return {
        title: "📝 Nueva nota",
        body: `${who}: ${String(data.title ?? "").trim() || "sin título"}`,
      };
    case "AVAILABILITY_UPDATED":
      return {
        title: "🗓️ Disponibilidad actualizada",
        body: `${who} actualizó su disponibilidad`,
      };
    case "DATE_CONFIRMED":
      return {
        title: "📅 ¡Ya hay fecha!",
        body: String(data.body ?? "La sesión tiene fecha confirmada"),
      };
    case "DATE_CLEARED":
      return {
        title: "⚠️ Fecha cancelada",
        body: "La sesión vuelve a estar sin fecha",
      };
    default:
      return { title: "S&R", body: `Novedades en la sesión ${code}` };
  }
}
