package minerofmillions.serverhost.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import minerofmillions.serverhost.app.components.ServerHostComponent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerHost(component: ServerHostComponent) {
    Scaffold(topBar = {
        TopAppBar(title = {
            Text("Server Host")
        }, actions = {
            IconButton({ component.configure() }) {
            val darkMode by component.darkMode.subscribeAsState()
            if (darkMode) IconButton({ component.setDarkMode(false) }) {
                Icon(Icons.Default.LightMode, "Change to Light Mode")
            } else IconButton({ component.setDarkMode(true) }) {
                Icon(Icons.Default.DarkMode, "Change to Dark Mode")
            }

                TooltipArea(tooltip = {
                    Surface(
                        modifier = Modifier.shadow(4.dp),
                        color = MaterialTheme.colors.error,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Warning: Changing settings will stop all currently running servers.")
                    }
                }) { Icon(Icons.Default.Settings, "Settings") }
            }
        })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            val anyServerActive by component.anyServerActive.subscribeAsState()
            val coroutineScope = rememberCoroutineScope()
            component.servers.forEach {
                ServerDashboard(it)
            }
            if (anyServerActive) Button({ coroutineScope.launch(Dispatchers.IO) { component.closeServers() } }) {
                Text("Shut down all servers")
            }
        }
    }
}