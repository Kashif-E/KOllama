import androidx.compose.ui.window.ComposeUIViewController
import com.kashif.kollama.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
