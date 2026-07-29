package com.schednd.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schednd.model.NoteTag
import com.schednd.ui.theme.TagLoot
import com.schednd.ui.theme.TagNpc
import com.schednd.ui.theme.TagOtros
import com.schednd.ui.theme.TagPersonaje
import com.schednd.ui.theme.TagTrama
import com.schednd.ui.theme.pressScale
import androidx.compose.ui.res.stringResource

@Composable
fun NoteTag.color(): Color = when (this) {
    NoteTag.TRAMA -> TagTrama
    NoteTag.LOOT -> TagLoot
    NoteTag.NPC -> TagNpc
    NoteTag.PERSONAJE -> TagPersonaje
    NoteTag.OTROS -> TagOtros
}

@Composable
fun TagChip(
    tag: NoteTag,
    modifier: Modifier = Modifier
) {
    val color = tag.color()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(tag.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
fun FilterChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val containerColor = if (selected) MaterialTheme.colorScheme.onSurface
                         else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val textColor = if (selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurface
    val countColor = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                     else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50)
            )
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
        if (count != null) {
            Text(
                text = "  $count",
                style = MaterialTheme.typography.labelMedium,
                color = countColor
            )
        }
    }
}
