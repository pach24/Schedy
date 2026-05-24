package com.schednd.model

data class NoteTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val tag: NoteTag,
    val titleSeed: String,
    val bodySeed: String
) {
    companion object {
        val DEFAULTS: List<NoteTemplate> = listOf(
            NoteTemplate(
                id = "resumen",
                title = "Resumen de sesión",
                subtitle = "Qué pasó, decisiones, cliffhanger",
                emoji = "📜",
                tag = NoteTag.TRAMA,
                titleSeed = "Resumen de la sesión",
                bodySeed = "Qué pasó:\n- \n\nDecisiones:\n- \n\nCliffhanger:\n- "
            ),
            NoteTemplate(
                id = "loot",
                title = "Loot pendiente",
                subtitle = "Lista de objetos por repartir",
                emoji = "💰",
                tag = NoteTag.LOOT,
                titleSeed = "Loot pendiente",
                bodySeed = "Objetos por repartir:\n- \n- \n\nA quién:\n- "
            )
        )
    }
}
