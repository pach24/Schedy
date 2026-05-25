package com.schednd.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.model.DayTimeSlot
import com.schednd.ui.theme.CalendarCellShape
import com.schednd.ui.theme.SquircleShape

@Composable
fun SplitSquircleCell(
    slot: DayTimeSlot?,
    dayNumber: String,
    countLabel: String?,
    textOnEmpty: Color,
    textOnFill: Color,
    countOnEmpty: Color,
    countOnFill: Color,
    activeColor: Color,
    outlineColor: Color,
    divider: Color,
    isBold: Boolean,
    modifier: Modifier = Modifier,
    shape: SquircleShape = CalendarCellShape
) {
    val fullTarget = if (slot == DayTimeSlot.BOTH) activeColor else Color.Transparent
    val morningTarget = if (slot == DayTimeSlot.MORNING) activeColor else Color.Transparent
    val afternoonTarget = if (slot == DayTimeSlot.AFTERNOON) activeColor else Color.Transparent

    val colorSpec = tween<Color>(durationMillis = 220)

    val fullColor by animateColorAsState(fullTarget, colorSpec, label = "fullFill")
    val morningColor by animateColorAsState(morningTarget, colorSpec, label = "morningFill")
    val afternoonColor by animateColorAsState(afternoonTarget, colorSpec, label = "afternoonFill")

    val showSplit = slot == DayTimeSlot.MORNING || slot == DayTimeSlot.AFTERNOON
    val outlineAnim by animateColorAsState(
        if (showSplit) outlineColor else Color.Transparent,
        colorSpec,
        label = "outline"
    )
    val dividerAnim by animateColorAsState(
        if (showSplit) divider else Color.Transparent,
        colorSpec,
        label = "divider"
    )

    val morningFilled = slot?.hasMorning == true
    val afternoonFilled = slot?.hasAfternoon == true
    val morningTextTarget = if (morningFilled) textOnFill else textOnEmpty
    val afternoonTextTarget = if (afternoonFilled) textOnFill else textOnEmpty
    val morningCountTarget = if (morningFilled) countOnFill else countOnEmpty
    val afternoonCountTarget = if (afternoonFilled) countOnFill else countOnEmpty
    val morningText by animateColorAsState(morningTextTarget, colorSpec, label = "morningText")
    val afternoonText by animateColorAsState(afternoonTextTarget, colorSpec, label = "afternoonText")
    val morningCount by animateColorAsState(morningCountTarget, colorSpec, label = "morningCount")
    val afternoonCount by animateColorAsState(afternoonCountTarget, colorSpec, label = "afternoonCount")

    Box(modifier = modifier.clip(shape)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            if (fullColor.alpha > 0f) {
                drawRect(color = fullColor)
            }

            if (morningColor.alpha > 0f) {
                drawPath(trianglePath(TrianglePart.TopLeft, w, h), morningColor)
            }

            if (afternoonColor.alpha > 0f) {
                drawPath(trianglePath(TrianglePart.BottomRight, w, h), afternoonColor)
            }

            if (dividerAnim.alpha > 0f) {
                drawLine(
                    color = dividerAnim,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (outlineAnim.alpha > 0f) {
                val outlinePath = shape.buildPath(size, this, layoutDirection)
                drawPath(
                    path = outlinePath,
                    color = outlineAnim,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        DayLabel(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    clipPath(trianglePath(TrianglePart.TopLeft, size.width, size.height)) {
                        this@drawWithContent.drawContent()
                    }
                },
            dayNumber = dayNumber,
            countLabel = countLabel,
            dayColor = morningText,
            countColor = morningCount,
            isBold = isBold
        )
        DayLabel(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    clipPath(trianglePath(TrianglePart.BottomRight, size.width, size.height)) {
                        this@drawWithContent.drawContent()
                    }
                },
            dayNumber = dayNumber,
            countLabel = countLabel,
            dayColor = afternoonText,
            countColor = afternoonCount,
            isBold = isBold
        )
    }
}

@Composable
private fun DayLabel(
    modifier: Modifier,
    dayNumber: String,
    countLabel: String?,
    dayColor: Color,
    countColor: Color,
    isBold: Boolean
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = dayColor
        )
        if (!countLabel.isNullOrEmpty()) {
            Text(
                text = countLabel,
                style = LocalTextStyle.current.copy(fontSize = 8.sp),
                color = countColor
            )
        }
    }
}

private enum class TrianglePart { TopLeft, BottomRight }

private fun trianglePath(part: TrianglePart, w: Float, h: Float): Path = Path().apply {
    when (part) {
        TrianglePart.TopLeft -> {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(0f, h)
        }
        TrianglePart.BottomRight -> {
            moveTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, h)
        }
    }
    close()
}

@Suppress("unused")
private val ClipOpRef = ClipOp.Intersect

private fun SquircleShape.buildPath(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
): Path {
    val outline = createOutline(size, layoutDirection, density)
    return (outline as androidx.compose.ui.graphics.Outline.Generic).path
}
