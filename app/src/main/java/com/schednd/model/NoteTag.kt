package com.schednd.model

enum class NoteTag(val label: String) {
    TRAMA("Trama"),
    LOOT("Loot"),
    NPC("NPC"),
    PERSONAJE("Personaje"),
    OTROS("Otros");

    companion object {
        fun fromString(value: String?): NoteTag =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTROS
    }
}
