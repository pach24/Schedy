package com.schednd.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.R
import com.schednd.ui.theme.GolosFamily
import com.schednd.ui.theme.SchedyTheme
import com.schednd.ui.theme.TierFull
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    val isValid = name.trim().isNotBlank()

    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(160)
        subtitleVisible = true
        delay(180)
        formVisible = true
        delay(350)
        focusRequester.requestFocus()
    }

    val confirm = {
        if (isValid) {
            keyboard?.hide()
            viewModel.savePlayerName(name.trim())
            onComplete()
        }
    }

    val breathTransition = rememberInfiniteTransition(label = "d20Breath")
    val breathScale by breathTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── D20 decorativo de fondo ───────────────────────────────────────────
        Icon(
            painter = painterResource(R.drawable.ic_dice),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .offset(y = (-48).dp)
                .scale(breathScale)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ── Cabecera ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // "Hola."
                AnimatedVisibility(
                    visible = titleVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Hola")
                            withStyle(SpanStyle(color = TierFull)) { append(".") }
                        },
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GolosFamily,
                        letterSpacing = (-2.5).sp,
                        lineHeight = 62.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Subtítulo
                AnimatedVisibility(
                    visible = subtitleVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 6 }
                    )
                ) {
                    Text(
                        text = "Bienvenido a Schedy.\nEmpecemos por conocerte.",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Formulario ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(300)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "¿Cómo te llamas?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )

                    // Campo limpio, sin decoración
                    val fieldInteraction = remember { MutableInteractionSource() }

                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        interactionSource = fieldInteraction,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { confirm() }),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 17.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (name.isEmpty()) {
                                    Text(
                                        text = "Tu nombre de jugador",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.45f)
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Botón Continuar
                    val btnInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(btnInteraction)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isValid) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                            )
                            .clickable(
                                indication = LocalIndication.current,
                                interactionSource = btnInteraction,
                                enabled = isValid,
                                onClick = confirm
                            )
                            .padding(vertical = 17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continuar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isValid) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=393dp,height=852dp")
@Composable
private fun OnboardingPreview() {
    SchedyTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dice),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.Center)
                    .offset(y = (-48).dp)
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Hola")
                            withStyle(SpanStyle(color = TierFull)) { append(".") }
                        },
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GolosFamily,
                        letterSpacing = (-2.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Bienvenido a Schedy.\nEmpecemos por conocerte.",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        text = "¿Cómo te llamas?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 17.dp)
                    ) {
                        Text(
                            text = "Tu nombre de jugador",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(vertical = 17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continuar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=393dp,height=852dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPreviewDark() {
    SchedyTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dice),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f),
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.Center)
                    .offset(y = (-48).dp)
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Hola")
                            withStyle(SpanStyle(color = TierFull)) { append(".") }
                        },
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GolosFamily,
                        letterSpacing = (-2.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Bienvenido a Schedy.\nEmpecemos por conocerte.",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        text = "¿Cómo te llamas?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 17.dp)
                    ) {
                        Text(
                            text = "Tu nombre de jugador",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                            .padding(vertical = 17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continuar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}
