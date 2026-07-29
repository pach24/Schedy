package com.schednd.model

import androidx.annotation.StringRes
import com.schednd.R

/**
 * Los textos son referencias a recursos, no cadenas: la plantilla se define aqui pero se
 * lee en el idioma del sistema, en el momento de pintarla.
 */
data class NoteTemplate(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val emoji: String,
    val tag: NoteTag,
    @StringRes val titleSeedRes: Int,
    @StringRes val bodySeedRes: Int
) {
    companion object {
        val DEFAULTS: List<NoteTemplate> = listOf(
            NoteTemplate(
                id = "resumen",
                titleRes = R.string.template_recap_title,
                subtitleRes = R.string.template_recap_subtitle,
                emoji = "\uD83D\uDCDC",
                tag = NoteTag.TRAMA,
                titleSeedRes = R.string.template_recap_seed_title,
                bodySeedRes = R.string.template_recap_seed_body
            ),
            NoteTemplate(
                id = "loot",
                titleRes = R.string.template_loot_title,
                subtitleRes = R.string.template_loot_subtitle,
                emoji = "\uD83D\uDCB0",
                tag = NoteTag.LOOT,
                titleSeedRes = R.string.template_loot_seed_title,
                bodySeedRes = R.string.template_loot_seed_body
            )
        )
    }
}
