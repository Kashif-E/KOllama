import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kashif.deepseek.di.appModule
import com.kashif.deepseek.presentation.ChatScreen
import com.kashif.deepseek.presentation.state.ChatViewModel
import com.kashif.deepseek.presentation.theme.AppTheme
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.awt.Dimension

fun main() = application {
    startKoin { modules(appModule) }
    Window(
        title = "Kollama",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(1365, 1080)
        val viewModel = koinInject<ChatViewModel>()
        AppTheme {
            ChatScreen(
              viewModel = viewModel
            )
        }
    }
}
