package io.github.minerofmillions.serverhost.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.minerofmillions.serverhost.app.components.ServerHostComponent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerHost(component: ServerHostComponent) {
    val anyServerActive by component.anyServerActive.subscribeAsState()
    Scaffold(topBar = {
        TopAppBar(title = {
            Text("Server Host")
        }, actions = {
            val darkMode by component.darkMode.subscribeAsState()
            if (darkMode) IconButton({ component.setDarkMode(false) }) {
                Icon(Icons.Default.LightMode, "Change to Light Mode")
            } else IconButton({ component.setDarkMode(true) }) {
                Icon(Icons.Default.DarkMode, "Change to Dark Mode")
            }

            IconButton({ component.configure() }, enabled = !anyServerActive) {
                TooltipArea(tooltip = {
                    Surface(
                        modifier = Modifier.shadow(4.dp),
                        color = MaterialTheme.colors.error,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Changing settings requires no currently running servers.")
                    }
                }) { Icon(Icons.Default.Settings, "Settings") }
            }
        })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            val coroutineScope = rememberCoroutineScope()
            val validServers by component.validServers.subscribeAsState()
            val erroredServers by component.erroredServers.subscribeAsState()
            LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(minSize = 500.dp)) {
                items(validServers) {
                    ServerDashboard(it)
                }
                items(erroredServers) {
                    ServerDashboard(it)
                }
            }
            if (anyServerActive) Button({ coroutineScope.launch(Dispatchers.IO) { component.closeServers() } }) {
                Text("Shut down all servers")
            }
        }
    }
}