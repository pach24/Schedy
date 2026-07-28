package com.schednd.ui.session.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.schednd.R

@Composable
fun ProfileTabScreen(
    bottomPadding: PaddingValues,
    onBack: () -> Unit
) {
    ComingSoonScreen(
        title = "Perfil",
        subtitle = "Aquí podrás gestionar tu nombre, tu avatar y las sesiones a las que perteneces.",
        icon = painterResource(R.drawable.ic_user),
        bottomPadding = bottomPadding
    )
}
