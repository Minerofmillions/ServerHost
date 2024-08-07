package minerofmillions.serverhost.app

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.*
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import minerofmillions.serverhost.app.components.AppComponent
import minerofmillions.serverhost.app.ui.App
import javax.swing.SwingUtilities

fun main() {
    val lifecycle = LifecycleRegistry()

    val root = runOnUiThread { AppComponent(DefaultComponentContext(lifecycle)) }

    fun ApplicationScope.close() {
        runBlocking {
            launch(Dispatchers.IO) {
                root.closeServers()
            }.invokeOnCompletion {
                exitApplication()
            }
        }
    }

    application {
        val state = rememberWindowState(placement = WindowPlacement.Maximized)
        val darkMode by root.darkMode.subscribeAsState()
        Window(onCloseRequest = ::close, title = "Server Host", state = state) {
            MaterialTheme(
                typography = MaterialTheme.typography.copy(h1 = MaterialTheme.typography.h1.copy(fontSize = 2.em)),
                colors = if (darkMode) darkColors() else lightColors()
            ) {
                App(root)
            }
        }
    }
}

fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var error: Throwable? = null
    var result: T? = null

    SwingUtilities.invokeAndWait {
        try {
            result = block()
        } catch (e: Throwable) {
            error = e
        }
    }
    error?.let { throw it }

    return result!!
}
