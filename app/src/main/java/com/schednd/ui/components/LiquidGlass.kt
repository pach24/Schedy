package com.schednd.ui.components

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.min
import org.intellij.lang.annotations.Language

/**
 * Efecto "liquid glass": las piezas registradas refractan el fondo que tienen detrás, lo
 * difuminan y llevan un brillo especular en el filo.
 *
 * Toma la distorsión y el rim highlight del shader AGSL de Mortd3kay/liquid-glass-compose
 * (Apache 2.0), pero no su blur: aquel recorría un kernel cuadrado de 121 muestras por
 * píxel a pantalla completa. El difuminado de aquí es propio, un disco de 24 muestras en
 * espiral, y solo corre dentro de las piezas de cristal.
 *
 * Se usa en dos pasos:
 * 1. [liquidGlassBackdrop] sobre el contenido que hace de fondo.
 * 2. [liquidGlassShape] sobre cada pieza de cristal, que debe dibujarse *fuera* de ese
 *    fondo — si no, se refractaría a sí misma. Para piezas animadas por `graphicsLayer`
 *    sin recomposición, como la bolita de la barra, está [LiquidGlassState.updateShape].
 *
 * Requiere AGSL (API 33+). Por debajo, [LiquidGlassState.isSupported] es false, los
 * modifiers son no-op y cada pieza debe pintarse opaca.
 */
@Stable
class LiquidGlassState internal constructor(val isSupported: Boolean) {

    private val ids = LongArray(MaxShapes)
    private val profileSamples = FloatArray(MaxProfileSamples)
    internal val centers = FloatArray(MaxShapes * 2)
    internal val halfSizes = FloatArray(MaxShapes * 2)
    internal val radii = FloatArray(MaxShapes)

    /**
     * La barra inferior no es un rectángulo: su borde superior lleva la muesca por la que
     * asoma la bolita. Va aparte del array de piezas porque solo puede haberla una y
     * porque se describe con un perfil de alturas, no con centro y tamaño.
     */
    internal val profileTop = FloatArray(MaxProfileSamples)
    internal var profileLeft = 0f
        private set
    internal var profileRight = 0f
        private set
    internal var profileTopY = 0f
        private set
    internal var profileBottom = 0f
        private set
    internal var profileSpanStart = 0f
        private set
    internal var profileSpanEnd = 0f
        private set
    internal var hasProfile = false
        private set

    /** Cambia en cada movimiento: el backdrop lo lee para saber que debe redibujarse. */
    internal var revision by mutableIntStateOf(0)
        private set
    internal var size = 0
        private set

    internal val isMeasured: Boolean
        get() = size > 0 || hasProfile

    /**
     * Registra o mueve una pieza. Todas son rectángulos redondeados; un círculo es el
     * caso en que el radio llega a la mitad del lado corto.
     *
     * El hueco de la barra no se pasa por aquí: lo lleva [updateHole], porque se mueve en
     * cada frame mientras el rectángulo de la pieza solo cambia al medirse.
     */
    fun updateShape(
        id: Long,
        centerX: Float,
        centerY: Float,
        halfWidth: Float,
        halfHeight: Float,
        cornerRadius: Float,
    ) {
        if (!isSupported) return
        val index = indexOf(id) ?: allocate(id) ?: return

        val radius = cornerRadius.coerceIn(0f, min(halfWidth, halfHeight))
        if (
            centers[index * 2] approx centerX &&
            centers[index * 2 + 1] approx centerY &&
            halfSizes[index * 2] approx halfWidth &&
            halfSizes[index * 2 + 1] approx halfHeight &&
            radii[index] approx radius
        ) {
            return
        }

        centers[index * 2] = centerX
        centers[index * 2 + 1] = centerY
        halfSizes[index * 2] = halfWidth
        halfSizes[index * 2 + 1] = halfHeight
        radii[index] = radius
        revision++
    }

    /**
     * Coloca la muesca de la barra inferior, con la misma curva que [TravelingHoleShape]
     * traza: se muestrea su borde superior y el shader interpola entre esas alturas. No es
     * una aproximación de la curva, es la curva.
     *
     * Va aparte de [updateShape] porque se mueve en cada frame, mientras que el rectángulo
     * de la barra solo cambia cuando se mide.
     *
     * @param topY borde superior de la barra, en coordenadas de la raíz
     * @param holeSize diámetro de la muesca, el de la bolita
     * @param depthProgress 0 = borde recto, 1 = muesca completa
     */
    fun updateHole(centerX: Float, topY: Float, holeSize: Float, depthProgress: Float) {
        if (!isSupported || !hasProfile) return

        val half = holeSize * TravelingHoleSpanFactor
        val spanStart: Float
        val spanEnd: Float
        if (depthProgress <= 0.001f) {
            // Sin fondo no hay muesca: el borde vuelve a ser recto de lado a lado.
            spanStart = 0f
            spanEnd = 0f
        } else {
            spanStart = centerX - half
            spanEnd = centerX + half
            sampleTravelingHoleTop(profileSamples, holeSize, depthProgress)
        }

        var changed = profileSpanStart != spanStart || profileSpanEnd != spanEnd || profileTopY != topY
        if (spanEnd > spanStart) {
            for (index in profileTop.indices) {
                val value = topY + profileSamples[index]
                if (!(profileTop[index] approx value)) {
                    profileTop[index] = value
                    changed = true
                }
            }
        }
        if (!changed) return

        profileSpanStart = spanStart
        profileSpanEnd = spanEnd
        profileTopY = topY
        revision++
    }

    /** Rectángulo de la pieza perfilada. Su borde superior lo pone [updateHole]. */
    internal fun setProfileBounds(left: Float, top: Float, right: Float, bottom: Float) {
        if (!isSupported) return
        if (
            hasProfile &&
            profileLeft approx left && profileTopY approx top &&
            profileRight approx right && profileBottom approx bottom
        ) {
            return
        }
        profileLeft = left
        profileRight = right
        profileBottom = bottom
        if (!hasProfile) {
            // Hasta que llegue la primera muesca, borde recto.
            profileTop.fill(top)
            profileTopY = top
            hasProfile = true
        }
        revision++
    }

    internal fun clearProfile() {
        if (!hasProfile) return
        hasProfile = false
        profileSpanStart = 0f
        profileSpanEnd = 0f
        revision++
    }

    fun removeShape(id: Long) {
        val index = indexOf(id) ?: return
        val last = size - 1
        // Se compacta moviendo la última al hueco: el array no tiene agujeros y el
        // shader puede recorrerlo hasta `size` sin comprobar nada.
        if (index != last) {
            ids[index] = ids[last]
            centers[index * 2] = centers[last * 2]
            centers[index * 2 + 1] = centers[last * 2 + 1]
            halfSizes[index * 2] = halfSizes[last * 2]
            halfSizes[index * 2 + 1] = halfSizes[last * 2 + 1]
            radii[index] = radii[last]
        }
        size = last
        revision++
    }

    private fun indexOf(id: Long): Int? {
        for (i in 0 until size) {
            if (ids[i] == id) return i
        }
        return null
    }

    private fun allocate(id: Long): Int? {
        if (size == MaxShapes) return null
        ids[size] = id
        return size++
    }

    private infix fun Float.approx(other: Float): Boolean = abs(this - other) < 0.5f

    companion object {
        /** Tope del array de uniforms del shader. */
        internal const val MaxShapes = 8
        private val nextId = AtomicLong(1L)

        /** Id estable para piezas que no pueden usar [liquidGlassShape]. */
        fun newShapeId(): Long = nextId.getAndIncrement()

        /** Alturas con las que se describe el borde perfilado de la barra. */
        internal const val MaxProfileSamples = 24
    }
}

@Composable
fun rememberLiquidGlassState(
    enabled: Boolean = true
): LiquidGlassState = remember(enabled) {
    LiquidGlassState(isSupported = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
}

/**
 * Marca este composable como pieza de cristal: se mide sola y se registra en [state].
 * Debe dibujarse fuera del contenido que lleva [liquidGlassBackdrop].
 *
 * Solo vale para piezas quietas: mide el layout, que no recoge lo que haga un
 * `graphicsLayer` por encima. Una pieza que se escale o se mueva por ahí debe registrarse
 * a mano con [LiquidGlassState.updateShape], como hacen la bolita de la barra y el
 * diálogo de día.
 *
 * @param cornerRadius radio de la pieza; para un círculo, la mitad del lado
 * @param carveHole si esta es la pieza a la que [LiquidGlassState.updateHole] le resta el
 *   hueco de la bolita. Solo puede serlo una.
 */
@Composable
fun Modifier.liquidGlassShape(
    state: LiquidGlassState?,
    cornerRadius: Dp,
    carveHole: Boolean = false,
): Modifier {
    if (state?.isSupported != true) return this

    val id = remember { LiquidGlassState.newShapeId() }
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    DisposableEffect(state, id, carveHole) {
        onDispose {
            if (carveHole) state.clearProfile() else state.removeShape(id)
        }
    }

    return this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInRoot()
        if (carveHole) {
            state.setProfileBounds(
                left = position.x,
                top = position.y,
                right = position.x + coordinates.size.width,
                bottom = position.y + coordinates.size.height,
            )
            return@onGloballyPositioned
        }
        val halfWidth = coordinates.size.width / 2f
        val halfHeight = coordinates.size.height / 2f
        state.updateShape(
            id = id,
            centerX = position.x + halfWidth,
            centerY = position.y + halfHeight,
            halfWidth = halfWidth,
            halfHeight = halfHeight,
            cornerRadius = cornerRadiusPx,
        )
    }
}

/**
 * Aplica el shader al contenido que queda *detrás* del cristal.
 *
 * @param warpStrength 0..1, cuánto refracta
 * @param warpBand grosor máximo de la franja que refracta, medido desde el borde hacia
 *   dentro. Las piezas más finas que esto refractan en todo su cuerpo
 * @param blurRadius radio del difuminado dentro de la pieza (0 = nítido)
 * @param rimWidth grosor del brillo del filo
 * @param rimStrength 0..1, intensidad del brillo
 */
@SuppressLint("NewApi") // Comprobado vía LiquidGlassState.isSupported
@Composable
fun Modifier.liquidGlassBackdrop(
    state: LiquidGlassState,
    warpStrength: Float = 0.55f,
    warpBand: Dp = 28.dp,
    blurRadius: Dp = 7.dp,
    rimWidth: Dp = 2.5.dp,
    rimStrength: Float = 0.65f,
    reflectionOffset: Dp = 18.dp,
): Modifier {
    if (!state.isSupported) return this

    val shader = remember { RuntimeShader(LIQUID_GLASS_SHADER) }
    val density = LocalDensity.current
    val warpBandPx = with(density) { warpBand.toPx() }
    val blurRadiusPx = with(density) { blurRadius.toPx() }
    val rimWidthPx = with(density) { rimWidth.toPx() }
    val reflectionPx = with(density) { reflectionOffset.toPx() }

    return this.graphicsLayer {
        // Leer `revision` es lo que suscribe esta capa a los movimientos de las piezas.
        val shapeCount = if (state.revision >= 0) state.size else 0
        if (!state.isMeasured) {
            renderEffect = null
            return@graphicsLayer
        }

        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setIntUniform("shapeCount", shapeCount)
        shader.setFloatUniform("shapeCenters", state.centers)
        shader.setFloatUniform("shapeHalfSizes", state.halfSizes)
        shader.setFloatUniform("shapeRadii", state.radii)
        shader.setIntUniform("profileActive", if (state.hasProfile) 1 else 0)
        shader.setFloatUniform(
            "profileRect",
            state.profileLeft, state.profileTopY, state.profileRight, state.profileBottom
        )
        shader.setFloatUniform("profileSpan", state.profileSpanStart, state.profileSpanEnd)
        shader.setFloatUniform("profileTop", state.profileTop)
        shader.setFloatUniform("warpStrength", warpStrength)
        shader.setFloatUniform("maxWarpBand", warpBandPx)
        shader.setFloatUniform("blurRadius", blurRadiusPx)
        shader.setFloatUniform("rimWidth", rimWidthPx)
        shader.setFloatUniform("rimStrength", rimStrength)
        shader.setFloatUniform("reflectionOffset", reflectionPx)

        renderEffect = RenderEffect
            .createRuntimeShaderEffect(shader, "contents")
            .asComposeRenderEffect()
    }
}

@Language("AGSL")
private val LIQUID_GLASS_SHADER = """
    uniform shader contents;
    uniform float2 resolution;
    uniform int shapeCount;
    uniform float2 shapeCenters[${LiquidGlassState.MaxShapes}];
    uniform float2 shapeHalfSizes[${LiquidGlassState.MaxShapes}];
    uniform float shapeRadii[${LiquidGlassState.MaxShapes}];
    uniform int profileActive;
    uniform float4 profileRect;
    uniform float2 profileSpan;
    uniform float profileTop[${LiquidGlassState.MaxProfileSamples}];
    uniform float warpStrength;
    uniform float maxWarpBand;
    uniform float blurRadius;
    uniform float rimWidth;
    uniform float rimStrength;
    uniform float reflectionOffset;

    // Muestra el fondo sin salirse de la capa: evita el halo transparente en los bordes.
    float4 sampleContents(float2 p) {
        return contents.eval(clamp(p, float2(0.0), resolution));
    }

    float smoothMin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    float smoothMax(float a, float b, float k) {
        return -smoothMin(-a, -b, k);
    }

    /**
     * Distancia con signo a un rectángulo redondeado.
     *
     * El `max(q.x, q.y)` del interior forma una cresta en las diagonales que salen de
     * cada esquina: justo ahí el gradiente salta de horizontal a vertical de golpe, y la
     * refracción se parte en una raya diagonal. Se suaviza el máximo para que el
     * gradiente gire de forma continua. Fuera de la pieza el término no interviene, así
     * que la silueta no cambia.
     */
    float sdfRoundedRect(float2 p, float2 center, float2 halfSize, float radius) {
        float2 q = abs(p - center) - halfSize + radius;
        float crease = max(min(halfSize.x, halfSize.y) * 0.6, 1.0);
        return length(max(q, 0.0)) + min(smoothMax(q.x, q.y, crease), 0.0) - radius;
    }

    // Los arrays de uniforms solo se pueden indexar con la variable de un bucle que el
    // compilador desenrolle, nunca dentro de una función: de ahí que aquí se pasen los
    // valores de la pieza y no su índice.
    float2 gradientOfRect(float2 p, float2 center, float2 halfSize, float radius) {
        float e = 1.0;
        return float2(
            sdfRoundedRect(p + float2(e, 0.0), center, halfSize, radius)
                - sdfRoundedRect(p - float2(e, 0.0), center, halfSize, radius),
            sdfRoundedRect(p + float2(0.0, e), center, halfSize, radius)
                - sdfRoundedRect(p - float2(0.0, e), center, halfSize, radius)
        );
    }

    float2 safeNormalize(float2 v) {
        return length(v) > 0.0001 ? normalize(v) : float2(0.0, -1.0);
    }

    // Ruido de gradiente entrelazado: varía suavemente entre píxeles vecinos, así que
    // rompe el patrón sin el grano sucio que deja un hash puro.
    float interleavedGradientNoise(float2 p) {
        return fract(52.9829189 * fract(0.06711056 * p.x + 0.00583715 * p.y));
    }

    /**
     * Difuminado en disco de 48 muestras repartidas por espiral de ángulo áureo, con
     * peso gaussiano según la distancia. Sigue siendo bastante más barato que el kernel
     * cuadrado de 121 del original, y sobre todo solo corre dentro del cristal.
     *
     * Dos detalles son los que hacen que parezca un desenfoque de verdad y no un puñado
     * de copias desplazadas: el radio va como sqrt(i/n), que reparte las muestras por
     * área en vez de amontonarlas en el centro, y la espiral se gira un poco en cada
     * píxel, que disuelve lo que si no se ve como una trama sobre el texto de detrás.
     */
    float4 hazeSample(float2 p) {
        if (blurRadius < 0.5) {
            return sampleContents(p);
        }

        const int SAMPLES = 48;
        const float GOLDEN_ANGLE = 2.39996323;

        float4 sum = float4(0.0);
        float total = 0.0;
        float jitter = interleavedGradientNoise(p) * 6.2831853;

        for (int i = 0; i < SAMPLES; i++) {
            float index = float(i) + 0.5;
            float distance = sqrt(index / float(SAMPLES));
            float angle = jitter + index * GOLDEN_ANGLE;

            float weight = exp(-1.6 * distance * distance);
            float2 offset = float2(cos(angle), sin(angle)) * distance * blurRadius;

            sum += sampleContents(p + offset) * weight;
            total += weight;
        }

        return sum / total;
    }

    float4 main(float2 fragCoord) {
        // Se busca la pieza que contiene este píxel. El descarte por caja evita calcular
        // el SDF completo en la inmensa mayoría de la pantalla.
        bool hit = false;
        float sdf = 0.0;
        // Distancia al borde que hace de silueta. Coincide con el SDF salvo en la barra,
        // cuyos otros tres bordes son el filo de la pantalla y no de la pieza.
        float edgeSdf = 0.0;
        float2 hitCenter = float2(0.0);
        float2 hitHalfSize = float2(0.0);
        float hitRadius = 0.0;
        // La pieza perfilada trae su propia normal: su contorno no es un rectángulo y el
        // gradiente por diferencias finitas no la sacaría bien en las esquinas en pico.
        bool hasOwnNormal = false;
        float2 ownNormal = float2(0.0, -1.0);

        for (int i = 0; i < ${LiquidGlassState.MaxShapes}; i++) {
            if (i >= shapeCount) break;

            float2 center = shapeCenters[i];
            float2 halfSize = shapeHalfSizes[i];

            float2 q = abs(fragCoord - center) - halfSize;
            if (max(q.x, q.y) > 0.0) continue;

            float radius = shapeRadii[i];
            float d = sdfRoundedRect(fragCoord, center, halfSize, radius);
            if (d < 0.0 && (!hit || d < sdf)) {
                hit = true;
                sdf = d;
                edgeSdf = d;
                hitCenter = center;
                hitHalfSize = halfSize;
                hitRadius = radius;
            }
        }

        // La barra inferior: rectángulo cuyo borde superior sigue el perfil muestreado de
        // la muesca. Va fuera del bucle de piezas porque no cabe en centro-y-tamaño, y
        // porque interpolar el perfil pide su propio bucle desenrollado.
        if (profileActive == 1
            && fragCoord.x >= profileRect.x && fragCoord.x <= profileRect.z
            && fragCoord.y >= profileRect.y && fragCoord.y <= profileRect.w) {

            float edgeY = profileRect.y;
            float slope = 0.0;
            float span = profileSpan.y - profileSpan.x;

            if (span > 0.0 && fragCoord.x > profileSpan.x && fragCoord.x < profileSpan.y) {
                float step = span / float(${LiquidGlassState.MaxProfileSamples} - 1);
                for (int j = 0; j < ${LiquidGlassState.MaxProfileSamples} - 1; j++) {
                    float x0 = profileSpan.x + step * float(j);
                    if (fragCoord.x >= x0 && fragCoord.x < x0 + step) {
                        float y0 = profileTop[j];
                        float y1 = profileTop[j + 1];
                        edgeY = mix(y0, y1, (fragCoord.x - x0) / step);
                        slope = (y1 - y0) / step;
                    }
                }
            }

            // Distancia a cada borde; manda el mayor. Con `max` pelado —sin el suavizado
            // que llevan los rectángulos redondeados— las esquinas quedan en pico y el
            // filo no se curva al llegar a los extremos.
            //
            // La del borde superior se mide en vertical, así que en las cuestas de la
            // muesca sale más larga que la real; se corrige por la pendiente para que el
            // filo mantenga su grosor también ahí.
            float dTop = (edgeY - fragCoord.y) * inversesqrt(1.0 + slope * slope);
            float dBottom = fragCoord.y - profileRect.w;
            float dLeft = profileRect.x - fragCoord.x;
            float dRight = fragCoord.x - profileRect.z;
            float d = max(max(dTop, dBottom), max(dLeft, dRight));

            if (d < 0.0 && (!hit || d < sdf)) {
                hit = true;
                sdf = d;
                // Solo el borde de arriba es silueta: los laterales y el de abajo son el
                // borde de la pantalla, y ahí no hay canto que refracte ni que brille.
                // Midiendo desde él, el warp y el filo se apagan solos en los otros tres.
                edgeSdf = dTop;
                hitCenter = float2(
                    (profileRect.x + profileRect.z) * 0.5,
                    (profileRect.y + profileRect.w) * 0.5
                );
                hitHalfSize = float2(
                    (profileRect.z - profileRect.x) * 0.5,
                    (profileRect.w - profileRect.y) * 0.5
                );
                hitRadius = 0.0;
                hasOwnNormal = true;
                // Sigue la pendiente de la curva, así que la refracción y el filo se
                // hunden con la muesca en vez de cruzarla rectos.
                ownNormal = normalize(float2(slope, -1.0));
            }
        }

        if (!hit) {
            return sampleContents(fragCoord);
        }

        // El lado corto manda: en una píldora ancha la refracción se concentra en los
        // bordes largos y el centro queda limpio, igual que en un círculo. Y se topa con
        // `maxWarpBand`, porque el grosor del cristal es propiedad del material, no del
        // tamaño de la pieza: sin tope, un diálogo grande refractaría en una franja
        // enorme y arrastraría el fondo medio cuerpo hacia dentro.
        float reference = max(min(min(hitHalfSize.x, hitHalfSize.y), maxWarpBand), 1.0);
        float2 n = hasOwnNormal
            ? ownNormal
            : safeNormalize(gradientOfRect(fragCoord, hitCenter, hitHalfSize, hitRadius));

        // Refracta en todo el cuerpo: la muestra se aleja del centro de forma cúbica,
        // así que el centro queda casi limpio y el borde comprime lo que hay alrededor,
        // como una gota de cristal.
        float d = clamp(1.0 + edgeSdf / reference, 0.0, 1.0);
        float amount = d * d * d * warpStrength * reference;
        float4 color = hazeSample(fragCoord + n * amount);

        // Brillo del filo, por dentro del borde. La luz viene de arriba.
        if (edgeSdf > -rimWidth && rimStrength > 0.0) {
            float rim = (rimWidth + edgeSdf) / rimWidth;
            rim = rim * rim;
            float lighting = mix(0.6, 1.25, clamp((1.0 - n.y) * 0.5, 0.0, 1.0));

            float4 reflected = sampleContents(fragCoord + n * reflectionOffset);
            reflected.rgb = max(reflected.rgb * 1.8 + 0.25, float3(0.12)) * lighting;
            color = mix(color, reflected, clamp(rim * rimStrength, 0.0, 1.0));
        }

        return color;
    }
""".trimIndent()
