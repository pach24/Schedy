package com.schednd.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.schednd.R

/**
 * Un mes, el carril de meses a los lados y el estirado que lo amplía. Lo que cambia de una
 * pantalla a otra es lo que se pinta dentro de cada día, y eso lo pone [day].
 *
 * Vive aparte de los calendarios que lo usan porque el armazón es delicado —el alto de la
 * celda se resuelve en la capa de medida, el ancla del carril se clava en la primera
 * composición, el mes asentado manda sobre el alto— y con dos copias esas cuentas se
 * separarían a la primera corrección que solo se hiciera en una.
 *
 * @param initialMonth el mes que se enseña al abrir. Solo se lee una vez: es el ancla del
 *   carril, y moverlo después correría todos los meses de sitio.
 * @param expansion 0 deja el mes en su tamaño de reposo, con la celda cuadrada; 1 lo estira
 *   hasta llenar [expandedSpace]. Admite pasarse de 1: el muelle que lo trae se pasa de
 *   rosca a propósito y la rejilla lo acompaña.
 * @param expandedSpace hasta dónde puede crecer el mes ampliado, cabecera incluida. Es una
 *   medida y no un factor porque el mes tiene que acabar justo donde empieza la barra, y
 *   eso depende de la pantalla y de si el mes ocupa cinco semanas o seis.
 * @param swipeMonths si el deslizar horizontal es de aquí. Con el mes en reposo es del
 *   pager de pestañas, y este no lo toca.
 * @param day cómo se pinta un día. Recibe el modificador con el tamaño ya resuelto —ancho
 *   de la semana y alto de la ampliación—, que tiene que ser lo primero de su cadena.
 */
@Composable
fun MonthCalendar(
    initialMonth: YearMonth,
    modifier: Modifier = Modifier,
    expansion: () -> Float = { 0f },
    expandedSpace: Dp = Dp.Unspecified,
    swipeMonths: Boolean = false,
    day: @Composable (date: LocalDate, modifier: Modifier) -> Unit
) {
    // Los meses van en un pager y no en un `AnimatedContent` que cambia de mes al soltar:
    // así el mes de al lado ya se ve entrando mientras el dedo arrastra, en vez de aparecer
    // de golpe cuando el gesto termina. Las páginas son un carril de meses alrededor del
    // primero que se enseña, tantas como para no llegar nunca al final —un siglo por lado—
    // y compuestas solo las que se ven.
    val pagerState = rememberPagerState(initialPage = MonthPagerAnchor) { MonthPagerPages }
    val scope = rememberCoroutineScope()
    // El mes de partida se clava en la primera composición: es el ancla del carril, y
    // recalcularlo al llegar datos nuevos correría todos los meses de sitio sin que la
    // página se entere, dejando la cabecera diciendo un mes y la rejilla enseñando otro.
    val anchorMonth = remember { initialMonth }
    fun monthAt(page: Int): YearMonth = anchorMonth.plusMonths((page - MonthPagerAnchor).toLong())
    // `currentPage` cambia al cruzar la mitad, no en cada frame: la cabecera se entera
    // cuando el mes nuevo ya manda en pantalla, y no recompone mientras el dedo arrastra.
    val shownMonth = monthAt(pagerState.currentPage)

    val weekDays = remember { weekDayInitials() }

    // Semanas del mes que se está viendo, animadas. Un mes que se desborda a una sexta
    // semana es más alto que el anterior, y sin esto la diferencia aparecía de golpe: lo
    // de debajo daba un salto en vez de que lo empujaran hacia abajo. Se anima el número
    // de semanas y no una altura en dp para no pelearse con la ampliación, que manda sobre
    // el alto de la celda al mismo tiempo.
    val weeks = animateFloatAsState(
        targetValue = weeksIn(shownMonth).toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "calendarWeeks"
    )

    // Lo que ocupan la cabecera del mes y la fila de iniciales. Se mide en vez de estimarse
    // porque de ahí sale el hueco que le queda a la rejilla, y una estimación de más o de
    // menos es exactamente lo que dejaría el mes sin encajar con la barra.
    var chromeHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gridSpacePx = with(density) {
        if (expandedSpace.isSpecified) expandedSpace.toPx() - chromeHeightPx else 0f
    }

    /**
     * Alto de celda para un ancho dado. En reposo es cuadrada; ampliada, lo que toque a
     * repartir el hueco entre las semanas del mes, que es lo que hace que cinco semanas o
     * seis acaben igual de abajo. Sin hueco medido todavía —primer frame, vistas previas—
     * se cae a un factor fijo.
     */
    fun cellHeightPx(cellWidthPx: Float): Float {
        val expanded = if (gridSpacePx > 0f) {
            (gridSpacePx / weeks.value).coerceAtLeast(cellWidthPx)
        } else {
            cellWidthPx * FallbackStretch
        }
        return lerp(cellWidthPx, expanded, expansion().coerceAtLeast(0f))
    }

    // El hueco vacío y la celda con día tienen que medir igual, así que la cuenta del alto
    // se escribe una vez y la usan los dos.
    val cellSize = Modifier.calendarCellHeight { width -> cellHeightPx(width) }

    val separatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val separatorWidthPx = with(density) { MonthSeparatorWidth.toPx() }
    val pageSpacingPx = with(density) { MonthPageSpacing.toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.onSizeChanged { chromeHeightPx = it.height }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month), tint = MaterialTheme.colorScheme.onSurface)
            }
            MonthTitles(
                pagerState = pagerState,
                monthAt = { page -> monthAt(page) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // La rejilla arranca algo más abajo: la cabecera de dos líneas necesita aire por
        // debajo o el año se lee pegado a los días de la semana.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)) {
            weekDays.forEach { weekDay ->
                Text(
                    text = weekDay,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }

        // El alto lo manda el mes asentado, no el más alto de los que se ven pasar: es lo
        // que deja que la sexta semana empuje hacia abajo en vez de aparecer de golpe. Va
        // en la capa de medida, leyendo las dos animaciones —semanas y ampliación— sin
        // recomponer la rejilla.
        Box(
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val height = (cellHeightPx(constraints.maxWidth / 7f) * weeks.value)
                        .roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    )
                    layout(placeable.width, height) { placeable.place(0, 0) }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = swipeMonths,
                // Los meses se alinean por arriba. Centrados —lo que hace el pager por
                // defecto—, uno de cinco semanas flotaría a media altura junto a uno de seis.
                verticalAlignment = Alignment.Top,
                pageSpacing = MonthPageSpacing,
                key = { it }
            ) { page ->
                MonthGrid(
                    month = monthAt(page),
                    cellSize = cellSize,
                    day = day,
                    // La raya que separa un mes del siguiente, en mitad del hueco que los
                    // aparta. En reposo cae fuera de la pantalla: solo se ve mientras el
                    // dedo arrastra, que es cuando hay dos meses que separar.
                    modifier = Modifier.drawBehind {
                        // Se queda algo por dentro del alto del mes, arriba y abajo. La
                        // sisa va en proporción y no en dp fijos: la rejilla casi dobla su
                        // alto al ampliarse, y una sisa fija se comería la raya en reposo
                        // o no se notaría con el mes abierto.
                        val inset = size.height * MonthSeparatorInset
                        drawRoundRect(
                            color = separatorColor,
                            topLeft = Offset(
                                x = size.width + (pageSpacingPx - separatorWidthPx) / 2f,
                                y = inset
                            ),
                            size = Size(separatorWidthPx, size.height - inset * 2f),
                            cornerRadius = CornerRadius(separatorWidthPx / 2f)
                        )
                    }
                )
            }
        }
    }
}

/**
 * El mes y su año. El mes viaja con el dedo igual que la rejilla: el que se va se aparta y
 * se apaga mientras el que llega entra por el otro lado. El año, en cambio, solo se mueve
 * cuando cambia —de diciembre a enero—; el resto del año se queda quieto, que es lo que
 * hace, y moverlo doce veces para acabar diciendo lo mismo es ruido.
 *
 * Ambos se colocan leyendo la posición continua del pager dentro de `graphicsLayer`, o sea
 * en la capa de dibujado: el texto se mueve cada frame sin que se recomponga nada.
 */
@Composable
private fun MonthTitles(
    pagerState: PagerState,
    monthAt: (Int) -> YearMonth,
    modifier: Modifier = Modifier
) {
    val settledPage = pagerState.currentPage
    val travelPx = with(LocalDensity.current) { TitleTravel.toPx() }
    /** Posición del pager en páginas, con decimales: 3.4 es entre la tercera y la cuarta. */
    fun position(): Float = pagerState.currentPage + pagerState.currentPageOffsetFraction

    // Página de la izquierda del par que se está cruzando. Se saca por `derivedStateOf`
    // para que avise al cambiar de página y no en cada frame del deslizamiento.
    val leftPage by remember(pagerState) {
        derivedStateOf { floor(position()).toInt() }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            for (step in -1..1) {
                val page = settledPage + step
                Text(
                    text = monthAt(page).month
                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        // Cuánto le falta a este mes para ser el de en medio, en páginas.
                        val distance = page - position()
                        translationX = distance * travelPx
                        alpha = (1f - abs(distance)).coerceIn(0f, 1f)
                    }
                )
            }
        }

        // Las dos líneas se tocaban: puestas al ras, el año parece cortado del mes en vez
        // de un dato aparte.
        Spacer(modifier = Modifier.height(4.dp))

        val leftYear = monthAt(leftPage).year
        val rightYear = monthAt(leftPage + 1).year
        Box(
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (leftYear == rightYear) {
                YearLabel(year = leftYear)
            } else {
                // Se cruza un fin de año: los dos se relevan al ritmo del dedo.
                YearLabel(
                    year = leftYear,
                    modifier = Modifier.graphicsLayer {
                        val crossed = (position() - leftPage).coerceIn(0f, 1f)
                        translationX = -crossed * travelPx
                        alpha = 1f - crossed
                    }
                )
                YearLabel(
                    year = rightYear,
                    modifier = Modifier.graphicsLayer {
                        val crossed = (position() - leftPage).coerceIn(0f, 1f)
                        translationX = (1f - crossed) * travelPx
                        alpha = crossed
                    }
                )
            }
        }
    }
}

@Composable
private fun YearLabel(year: Int, modifier: Modifier = Modifier) {
    Text(
        text = "$year",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
    )
}

/** Semanas que ocupa un mes en la rejilla, empezando en lunes: cinco o seis. */
private fun weeksIn(month: YearMonth): Int {
    val startOffset = month.atDay(1).dayOfWeek.value - 1
    return (startOffset + month.lengthOfMonth() + 6) / 7
}

/**
 * La rejilla de un mes, con las semanas que necesite: cinco o seis. No se reserva la sexta
 * "por si acaso" —un mes que empieza en domingo y tiene 31 días—, porque ese hueco de
 * reserva se paga en los otros once meses del año como espacio muerto.
 *
 * Lo que sí implica: al cambiar a un mes de seis semanas, lo que va debajo baja una fila.
 * Mientras el dedo arrastra manda el más alto de los dos meses a la vista, que es lo que
 * hace el pager con páginas de distinto tamaño.
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    cellSize: Modifier,
    day: @Composable (date: LocalDate, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val cells = remember(month) {
        val startOffset = month.atDay(1).dayOfWeek.value - 1
        buildList<LocalDate?> {
            repeat(startOffset) { add(null) }
            for (dayOfMonth in 1..month.lengthOfMonth()) add(month.atDay(dayOfMonth))
            // Solo se completa la última semana, no el mes entero hasta seis.
            while (size % 7 != 0) add(null)
        }
    }

    Column(modifier = modifier) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    // El reparto de la fila va aquí y no en la celda: los huecos de los
                    // días que no son de este mes tienen que ocupar su séptimo igual que
                    // los demás, y quien pinta el día no tiene por qué saberlo.
                    val cell = Modifier.weight(1f).then(cellSize)
                    if (date == null) Box(modifier = cell) else day(date, cell)
                }
            }
        }
    }
}

/**
 * Iniciales de los días, de lunes a domingo, en el idioma del móvil.
 *
 * Estaban escritas a mano —L M X J V S D—, que fuera del castellano es una fila de letras
 * que no dicen nada. La semana sigue empezando en lunes en todos los idiomas: cambiarla
 * por locale movería también la rejilla, y las sesiones de rol de esta app caen en fin de
 * semana, que partido en dos se lee peor.
 */
internal fun weekDayInitials(locale: Locale = Locale.getDefault()): List<String> =
    DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale).uppercase(locale) }

/**
 * Alto de una celda del calendario: cuadrada en reposo y estirada a lo alto al ampliarse.
 * El ancho no se toca, que lo manda la semana.
 *
 * Va en la capa de medida y no en un `height()` calculado al componer porque la ampliación
 * cambia en cada frame: así se remide la rejilla, que es inevitable, pero no se recompone
 * ni una de las cuarenta y dos celdas.
 */
private fun Modifier.calendarCellHeight(heightPx: (cellWidthPx: Float) -> Float): Modifier =
    layout { measurable, constraints ->
        // El ancho viene fijado por el reparto de la fila, así que el lado se sabe aquí.
        val height = heightPx(constraints.maxWidth.toFloat()).roundToInt()
        val placeable = measurable.measure(
            constraints.copy(minHeight = height, maxHeight = height)
        )
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

/**
 * Estirado de la celda cuando no se sabe cuánto sitio hay: primer frame, vistas previas o
 * quien use el calendario sin decirle hasta dónde puede crecer. Con la medida en la mano
 * manda ella, que es lo que hace que el mes acabe justo en la barra.
 */
private const val FallbackStretch = 2f

/** Hueco entre un mes y el siguiente. Solo se ve mientras el dedo arrastra. */
private val MonthPageSpacing = 20.dp

/** Grosor de la raya que separa los meses; las puntas van redondeadas a su medida. */
private val MonthSeparatorWidth = 1.5.dp

/** Cuánto se queda corta la raya por arriba y por abajo, en tanto por uno del alto. */
private const val MonthSeparatorInset = 0.08f

/**
 * Cuánto se aparta el título de un mes al pasar al de al lado. Es bastante menos que el
 * ancho de la pantalla: el título acompaña al gesto, no lo recorre entero como la rejilla.
 */
private val TitleTravel = 72.dp

/** Meses que se pueden recorrer a cada lado del primero que se enseña. */
private const val MonthPagerAnchor = 1200

/** Cien años por lado: nadie llega al final, y las páginas se componen según se ven. */
private const val MonthPagerPages = MonthPagerAnchor * 2
