package com.schednd.domain.model

import androidx.annotation.StringRes
import com.schednd.R

enum class NoteTag(@StringRes val labelRes: Int) {
    TRAMA(R.string.tag_plot),
    LOOT(R.string.tag_loot),
    NPC(R.string.tag_npc),
    PERSONAJE(R.string.tag_character),
    OTROS(R.string.tag_other);

    companion object {
        fun fromString(value: String?): NoteTag =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTROS
    }
}
