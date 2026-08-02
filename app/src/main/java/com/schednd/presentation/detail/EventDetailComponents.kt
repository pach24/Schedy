package com.schednd.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.pressScale
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.schednd.R

/** Duración por defecto del evento de calendario cuando la sesión tiene hora. */
internal const val SESSION_DEFAULT_DURATION_MS = 3L * 60 * 60 * 1000

/** Lo que dura una sesión "en curso": la misma ventana que se lleva al calendario. */
private val LIVE_WINDOW: Duration = Duration.ofMillis(SESSION_DEFAULT_DURATION_MS)

/** La app maneja horas en 24 h en todas partes; la cuenta atrás no es la excepción. */
private val HERO_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Abreviatura de la zona horaria ("CET", "CEST") del instante de la sesión. */
private val ZONE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("zzz")

@Composable
internal fun GenActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.72f) else Color(0xFFFFFFFF)
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(FullRoundShape)
            .background(fillColor)
            .border(1.dp, borderBrush, FullRoundShape)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = { content() }
        )
    }
}

/**
 * En qué momento de su vida está la sesión confirmada. Sin una hora fijada no hay forma de
 * saber si ya ha empezado, así que esas sesiones se saltan [LIVE] y son [UPCOMING] el día
 * entero.
 */
internal enum class SessionStage { UPCOMING, LIVE, PAST }

/** Cuándo arranca la sesión; sin hora fijada, el comienzo de su día. */
private fun sessionStart(date: LocalDate, time: LocalTime?): LocalDateTime =
    LocalDateTime.of(date, time ?: LocalTime.MIDNIGHT)

/** Cuándo deja de estar en curso: la ventana de duración o, sin hora, el final del día. */
private fun sessionEnd(date: LocalDate, time: LocalTime?): LocalDateTime =
    if (time == null) date.plusDays(1).atStartOfDay() else sessionStart(date, time) + LIVE_WINDOW

internal fun stageAt(now: LocalDateTime, date: LocalDate, time: LocalTime?): SessionStage = when {
    !now.isBefore(sessionEnd(date, time)) -> SessionStage.PAST
    time == null || now.isBefore(sessionStart(date, time)) -> SessionStage.UPCOMING
    else -> SessionStage.LIVE
}

/**
 * Sigue la sesión mientras la pantalla está abierta: en vez de mirar el reloj una vez al
 * componer, duerme hasta el siguiente salto de fase y despierta justo ahí. Así el hero pasa
 * solo de próxima a en curso y de en curso a pasada, sin quedarse clavado.
 */
@Composable
internal fun rememberSessionStage(confirmedDate: LocalDate, startTime: LocalTime?): SessionStage {
    val stage = remember(confirmedDate, startTime) {
        mutableStateOf(stageAt(LocalDateTime.now(), confirmedDate, startTime))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(confirmedDate, startTime, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = LocalDateTime.now()
                val current = stageAt(now, confirmedDate, startTime)
                stage.value = current
                // Pasada es el final del camino: no hay más saltos que esperar.
                if (current == SessionStage.PAST) break
                val next = if (current == SessionStage.UPCOMING && startTime != null) {
                    sessionStart(confirmedDate, startTime)
                } else {
                    sessionEnd(confirmedDate, startTime)
                }
                // El margen evita despertar un pelo antes del salto y tener que reintentarlo.
                delay(Duration.between(now, next).toMillis().coerceAtLeast(0L) + 250L)
            }
        }
    }
    return stage.value
}

@Composable
internal fun SessionCountdown(
    confirmedDate: LocalDate,
    modifier: Modifier = Modifier,
    startTime: LocalTime? = null,
    createdAt: LocalDateTime? = null,
    stage: SessionStage = rememberSessionStage(confirmedDate, startTime)
) {
    Column(modifier = modifier) {
        when (stage) {
            SessionStage.LIVE -> LiveSessionLabel()
            else -> Text(
                text = stringResource(
                    if (stage == SessionStage.PAST) R.string.detail_past_session
                    else R.string.detail_next_session
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (stage == SessionStage.PAST) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        if (stage == SessionStage.PAST) {
            PastSessionSummary(confirmedDate = confirmedDate, startTime = startTime)
        } else {
            UpcomingSessionHero(
                confirmedDate = confirmedDate,
                startTime = startTime,
                createdAt = createdAt,
                live = stage == SessionStage.LIVE
            )
        }
    }
}

/** Cabecera de la sesión en marcha: el punto late para que se note que corre el tiempo. */
@Composable
private fun LiveSessionLabel() {
    val transition = rememberInfiniteTransition(label = "live")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.detail_live_session),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** La sesión ya pasó: la fecha en grande y cuánto hace que terminó. */
@Composable
private fun PastSessionSummary(confirmedDate: LocalDate, startTime: LocalTime?) {
    val pastLabel = confirmedDate.format(
        DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_weekday_day_month), Locale.getDefault())
    ).replaceFirstChar { it.uppercaseChar() }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = pastLabel,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = elapsedSinceSessionText(confirmedDate, startTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

/**
 * "terminó hace 2 h" mientras la cosa está reciente y "hace 3 días" a partir de ahí. El
 * corte va por tiempo transcurrido y no por día natural: una sesión que acaba a la 01:00 no
 * puede decir "hace 1 día" a la 01:30. Un tic de minuto lo mantiene fresco con la pantalla
 * abierta.
 */
@Composable
private fun elapsedSinceSessionText(date: LocalDate, startTime: LocalTime?): String {
    val now by rememberMinuteTick()
    val minutes = ChronoUnit.MINUTES.between(sessionEnd(date, startTime), now)
    return when {
        minutes < 1L -> stringResource(R.string.session_ended_just_now)
        minutes < 60L -> pluralStringResource(
            R.plurals.session_ended_minutes_ago, minutes.toInt(), minutes.toInt()
        )
        minutes < 24L * 60L -> {
            val hours = (minutes / 60L).toInt()
            pluralStringResource(R.plurals.session_ended_hours_ago, hours, hours)
        }
        // Ya en días, lo natural es contarlos por el calendario, como los cuenta quien mira.
        else -> {
            val days = ChronoUnit.DAYS.between(date, now.toLocalDate()).toInt().coerceAtLeast(1)
            pluralStringResource(R.plurals.days_ago, days, days)
        }
    }
}

/**
 * Hero de la próxima sesión: días grandes, la hora al fondo como marca de agua, la barra
 * de lo que llevamos esperando y el desglose que corre segundo a segundo. Con [live] el
 * mismo hero sirve la sesión en marcha: el desglose pasa a contar lo que lleva.
 *
 * El tic solo lo leen las piezas que cambian: la cuenta vive en un [State] que se pasa sin
 * abrir, así que el número gordo, la fecha y la hora fantasma se componen una vez y no
 * vuelven a recomponerse cada segundo.
 */
@Composable
private fun UpcomingSessionHero(
    confirmedDate: LocalDate,
    startTime: LocalTime?,
    createdAt: LocalDateTime?,
    live: Boolean
) {
    val target = remember(confirmedDate, startTime) { sessionStart(confirmedDate, startTime) }
    val remaining = rememberCountdown(target)
    // Cada unidad se deriva por separado: los segundos invalidan su hueco cada segundo,
    // pero minutos, horas y días solo cuando de verdad cambian.
    // Sin hora fijada se apunta a la medianoche de ese día, que ya es parte de la sesión:
    // los días se redondean hacia arriba para que el número gordo diga los que faltan.
    val days = remember(remaining, startTime) {
        derivedStateOf {
            if (startTime == null) ceilDays(remaining.value) else remaining.value.toDays().toInt()
        }
    }
    val hours = remember(remaining) { derivedStateOf { (remaining.value.toHours() % 24).toInt() } }
    val minutes = remember(remaining) { derivedStateOf { (remaining.value.toMinutes() % 60).toInt() } }
    val seconds = remember(remaining) { derivedStateOf { (remaining.value.seconds % 60).toInt() } }

    // Leer los días aquí recompone el hero una vez al día, no una vez por segundo.
    val daysLeft by days
    val isToday = daysLeft <= 0

    val isDark = isSystemInDarkTheme()
    val locale = Locale.getDefault()
    val dayLabel = confirmedDate
        .format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_weekday_day_padded), locale))
        .replaceFirstChar { it.uppercaseChar() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = if (isToday) stringResource(R.string.detail_countdown_today) else twoDigits(daysLeft),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (isToday) 56.sp else 72.sp,
                letterSpacing = (-3).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        // En curso el día sobra: "HOY" ya lo dice, y su hueco lo necesita la hora para que
        // la fila quepa entera en pantallas estrechas.
        if (!live) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                if (!isToday) {
                    Text(
                        text = stringResource(R.string.detail_countdown_days),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // La hora en grande solo cabe mientras el número sea de dos cifras; el día de la
        // sesión el hueco se lo queda "HOY", y en curso entra encogida para que quepan las dos.
        if (startTime != null && (!isToday || live)) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = HERO_TIME_FORMAT.format(startTime),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (isToday) 48.sp else 72.sp,
                    letterSpacing = (-3).sp
                ),
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = if (isDark) 0.16f else 0.20f)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    WaitProgressBar(target = target, createdAt = createdAt, remaining = remaining)
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (live) startedAtLine(target, locale)
            else fullDateLine(confirmedDate, startTime, target, locale),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp, bottom = 2.dp)
        )
        when {
            // En curso el desglose cambia de sentido: ya no falta, lleva.
            live -> LiveElapsedBreakdown(start = target)
            // Sin hora no hay instante al que contar; los ceros solo serían ruido.
            startTime == null -> Unit
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountdownUnit(value = days, labelRes = R.string.countdown_unit_days)
                CountdownUnit(value = hours, labelRes = R.string.countdown_unit_hours)
                CountdownUnit(value = minutes, labelRes = R.string.countdown_unit_minutes)
                CountdownUnit(value = seconds, labelRes = R.string.countdown_unit_seconds)
            }
        }
    }
}

/** Lo que lleva corriendo la sesión, en horas y minutos. */
@Composable
private fun LiveElapsedBreakdown(start: LocalDateTime) {
    val elapsed = rememberElapsed(start)
    val hours = remember(elapsed) { derivedStateOf { elapsed.value.toHours().toInt() } }
    val minutes = remember(elapsed) { derivedStateOf { (elapsed.value.toMinutes() % 60).toInt() } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountdownUnit(value = hours, labelRes = R.string.countdown_unit_hours)
        CountdownUnit(value = minutes, labelRes = R.string.countdown_unit_minutes)
    }
}

/** "EMPEZÓ A LAS 18:00 CEST": mientras corre, la hora de inicio es la referencia útil. */
@Composable
private fun startedAtLine(target: LocalDateTime, locale: Locale): String =
    stringResource(R.string.detail_live_started_at, zonedTimeText(target, locale))

/** "SÁB 01 DE AGOSTO · 18:00 CEST": la línea completa, para no dejarla a la interpretación. */
@Composable
private fun fullDateLine(
    date: LocalDate,
    startTime: LocalTime?,
    target: LocalDateTime,
    locale: Locale
): String {
    val datePattern = stringResource(R.string.date_pattern_weekday_day_month)
    val dateText = remember(date, datePattern, locale) {
        date.format(DateTimeFormatter.ofPattern(datePattern, locale)).uppercase(locale)
    }
    // Sin hora se dice, en vez de callarlo: es lo que le falta a la sesión para estar cerrada.
    val timeText = if (startTime == null) stringResource(R.string.detail_no_time_yet)
    else zonedTimeText(target, locale)
    return stringResource(R.string.detail_date_with_time, dateText, timeText)
}

/**
 * "18:00 CEST". La zona sale del instante concreto de la sesión, no de hoy: en verano es
 * CEST y en invierno CET, y quien lee el hero en marzo no tiene por qué hacer la conversión.
 */
@Composable
private fun zonedTimeText(target: LocalDateTime, locale: Locale): String =
    remember(target, locale) {
        val zoned = target.atZone(ZoneId.systemDefault())
        "${HERO_TIME_FORMAT.format(zoned)} ${ZONE_FORMAT.withLocale(locale).format(zoned)}"
    }

/** Un bloque del desglose: cifra arriba, unidad abajo. */
@Composable
private fun CountdownUnit(value: State<Int>, labelRes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // La cifra en su propio composable: al cambiar solo se recompone ella, no la etiqueta.
        CountdownValue(value)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountdownValue(value: State<Int>) {
    Text(
        text = twoDigits(value.value),
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * Barra de la espera: se llena desde que se creó la sesión hasta que empieza. El progreso
 * se lee dentro del dibujo, así que el tic de cada segundo repinta la barra sin recomponer
 * nada. Sin fecha de creación cae a una ventana de dos semanas, que es lo que suele durar
 * cuadrar una sesión.
 */
@Composable
private fun WaitProgressBar(
    target: LocalDateTime,
    createdAt: LocalDateTime?,
    remaining: State<Duration>
) {
    val totalSeconds = remember(target, createdAt) {
        val start = createdAt?.takeIf { it.isBefore(target) } ?: target.minusWeeks(2)
        Duration.between(start, target).seconds.coerceAtLeast(1L)
    }
    val progress = remember(totalSeconds, remaining) {
        derivedStateOf {
            val left = remaining.value.seconds.coerceAtLeast(0L)
            ((totalSeconds - left).toFloat() / totalSeconds).coerceIn(0f, 1f)
        }
    }
    // Barrido de entrada: la barra crece hasta su valor una sola vez, al aparecer el hero.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        intro.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val fillColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val filled = (size.width * progress.value * intro.value).coerceIn(0f, size.width)
        if (filled > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size(filled.coerceAtLeast(size.height), size.height),
                cornerRadius = radius
            )
        }
    }
}

/**
 * Cuenta atrás alineada con el reloj del sistema: duerme hasta el siguiente segundo en
 * vez de cada 1000 ms, para no ir acumulando desfase. Se para con la pantalla en segundo
 * plano y al llegar a la hora, que un contador a cero no necesita seguir despertando.
 */
@Composable
private fun rememberCountdown(target: LocalDateTime): State<Duration> {
    val remaining = remember(target) {
        mutableStateOf(Duration.between(LocalDateTime.now(), target).coerceAtLeast(Duration.ZERO))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(target, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val left = Duration.between(LocalDateTime.now(), target)
                if (left.isNegative || left.isZero) {
                    remaining.value = Duration.ZERO
                    break
                }
                remaining.value = left
                delay(1_000L - System.currentTimeMillis() % 1_000L)
            }
        }
    }
    return remaining
}

/**
 * Tiempo transcurrido desde que empezó la sesión. Solo se pintan horas y minutos, así que
 * despierta en cada cambio de minuto en vez de cada segundo.
 */
@Composable
private fun rememberElapsed(start: LocalDateTime): State<Duration> {
    val elapsed = remember(start) {
        mutableStateOf(Duration.between(start, LocalDateTime.now()).coerceAtLeast(Duration.ZERO))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(start, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                elapsed.value = Duration.between(start, LocalDateTime.now())
                    .coerceAtLeast(Duration.ZERO)
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    return elapsed
}

/** Reloj de grano grueso para los textos en minutos y días, que no necesitan más. */
@Composable
private fun rememberMinuteTick(): State<LocalDateTime> {
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now.value = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    return now
}

/** Días hacia arriba: 4 h que quedan del día siguen siendo "1 día". */
internal fun ceilDays(remaining: Duration): Int {
    val seconds = remaining.seconds.coerceAtLeast(0L)
    return ((seconds + 86_399L) / 86_400L).toInt()
}

/** Cifras a dos dígitos sin pasar por el formateador: son las mismas 100 cadenas siempre. */
private val TWO_DIGITS: Array<String> = Array(100) { if (it < 10) "0$it" else it.toString() }

private fun twoDigits(value: Int): String =
    if (value in 0..99) TWO_DIGITS[value] else value.toString()

/**
 * Envoltorio común de los diálogos de la pantalla: scrim que cierra al tocar fuera y
 * entrada/salida con rebote. Los tres diálogos del detalle compartían este mismo bloque.
 */
@Composable
internal fun ScrimDialogOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(200))
    ) {
        val animScope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            with(animScope) {
                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.86f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(180)),
                        exit = scaleOut(targetScale = 0.92f, animationSpec = tween(160)) +
                            fadeOut(tween(160))
                    )
                ) {
                    content()
                }
            }
        }
    }
}
