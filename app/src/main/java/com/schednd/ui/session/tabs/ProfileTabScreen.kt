package com.schednd.ui.session.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable

@Composable
fun ProfileTabScreen(
    bottomPadding: PaddingValues,
    onBack: () -> Unit
) {
    ComingSoonScreen(
        title = "Perfil",
        subtitle = "Aquí podrás gestionar tu nombre, tu avatar y las sesiones a las que perteneces.",
        icon = Icons.Outlined.Person,
        bottomPadding = bottomPadding
    )
}
