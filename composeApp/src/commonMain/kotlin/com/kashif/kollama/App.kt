package com.kashif.kollama

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import com.kashif.kollama.di.getAppModule
import com.kashif.kollama.presentation.ChatScreen
import com.kashif.kollama.presentation.state.ChatViewModel
import com.kashif.kollama.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject


@Composable
internal fun App() = AppTheme {
    val context = LocalPlatformContext.current
    KoinApplication(application = {
        modules(getAppModule(context))
    }) {
        val viewModel = koinInject<ChatViewModel>()
        ChatScreen(viewModel)
    }
}


