package com.schednd.presentation.notes

import android.content.Intent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.NoteTag
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.Avatar
import com.schednd.ui.components.TagChip
import com.schednd.ui.components.color
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import androidx.compose.ui.res.stringResource
import com.schednd.R

@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isDone, uiState.isDeleted) {
        if (uiState.isDone || uiState.isDeleted) onClose()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            EditorTopBar(
                isEditMode = uiState.isEditMode,
                canSave = uiState.canSave,
                onCancel = onClose,
                onSave = viewModel::save
            )

            Spacer(modifier = Modifier.height(8.dp))

            AuthorRow(
                authorName = uiState.authorName,
                sessionName = uiState.sessionName,
                tag = uiState.tag,
                isEditMode = uiState.isEditMode,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenCard(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TitleField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    BodyField(
                        value = uiState.body,
                        onValueChange = viewModel::onBodyChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.note_editor_counter, uiState.body.length, 1000),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }

            if (uiState.isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                EditActionsRow(
                    isPinned = uiState.pinned,
                    onTogglePin = viewModel::onTogglePinned,
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${uiState.title}\n\n${uiState.body}".trim()
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir nota"))
                    },
                    onDelete = { showDeleteSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.note_editor_tag_header),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteTag.entries.forEach { tag ->
                    TagSelectChip(
                        tag = tag,
                        selected = uiState.tag == tag,
                        onClick = { viewModel.onTagChange(tag) }
                    )
                }
            }

            if (!uiState.isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                GenCard(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)) {
                    // Aquí había un interruptor de "avisar al grupo". Lo único que hacía
                    // era encolar un aviso para una Cloud Function que no está
                    // desplegada, así que prometía un push que no salía nunca.
                    ToggleRow(
                        icon = Icons.Filled.PushPin,
                        label = stringResource(R.string.note_editor_pin),
                        checked = uiState.pinned,
                        onCheckedChange = { viewModel.onTogglePinned() }
                    )
                }
            }

            uiState.error?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteSheet) {
        DeleteNoteSheet(
            noteTitle = uiState.title,
            onDelete = {
                showDeleteSheet = false
                viewModel.delete()
            },
            onDismiss = { showDeleteSheet = false }
        )
    }
}

@Composable
private fun EditorTopBar(
    isEditMode: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onCancel)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.notes_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = if (isEditMode) "Editar" else "Nueva nota",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = canSave, onClick = onSave)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = if (isEditMode) "Guardar" else "Listo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (canSave) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AuthorRow(
    authorName: String,
    sessionName: String,
    tag: NoteTag,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = authorName.ifBlank { "?" }, size = 36)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = authorName.ifBlank { stringResource(R.string.anonymous_player) },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isEditMode) stringResource(R.string.note_editor_editing_now)
                       else stringResource(
                           R.string.note_editor_publishing_in,
                           sessionName.ifBlank { stringResource(R.string.note_editor_session_fallback) }
                       ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TagChip(tag = tag)
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.note_editor_title_placeholder),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BodyField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(modifier = Modifier.heightIn(min = 120.dp)) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.note_editor_body_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TagSelectChip(
    tag: NoteTag,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = tag.color()
    val interaction = remember { MutableInteractionSource() }
    val bg = if (selected) MaterialTheme.colorScheme.onSurface
             else color.copy(alpha = 0.14f)
    val textColor = if (selected) MaterialTheme.colorScheme.surface else color
    Row(
        modifier = Modifier
            .pressScale(interaction)
            .clip(FullRoundShape)
            .background(bg)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Text(
                text = "✓ ",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
        Text(
            text = stringResource(tag.labelRes),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun EditActionsRow(
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditActionButton(
            label = if (isPinned) "Fijado" else "Fijar",
            icon = Icons.Filled.PushPin,
            tint = MaterialTheme.colorScheme.onSurface,
            highlighted = isPinned,
            onClick = onTogglePin,
            modifier = Modifier.weight(1f)
        )
        EditActionButton(
            label = "Compartir",
            icon = Icons.Filled.Share,
            tint = MaterialTheme.colorScheme.onSurface,
            highlighted = false,
            onClick = onShare,
            modifier = Modifier.weight(1f)
        )
        EditActionButton(
            label = "Borrar",
            icon = Icons.Filled.Delete,
            tint = Color(0xFFFD3744),
            highlighted = false,
            onClick = onDelete,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = SquircleShape(16.dp)
    Box(
        modifier = modifier
            .pressScale(interaction)
            .clip(shape)
            .background(
                if (highlighted) tint.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape
            )
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint
            )
        }
    }
}

