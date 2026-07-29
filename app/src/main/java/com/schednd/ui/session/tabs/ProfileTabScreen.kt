package com.schednd.ui.session.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.schednd.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileTabScreen(
    bottomPadding: PaddingValues,
    onBack: () -> Unit
) {
    ComingSoonScreen(
        title = stringResource(R.string.profile_title),
        subtitle = stringResource(R.string.profile_soon),
        icon = painterResource(R.drawable.ic_user),
        bottomPadding = bottomPadding
    )
}
