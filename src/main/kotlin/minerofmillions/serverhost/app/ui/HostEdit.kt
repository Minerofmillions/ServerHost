package minerofmillions.serverhost.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import minerofmillions.serverhost.ServerConfig
import minerofmillions.serverhost.app.components.HostEditComponent

@Composable
fun HostEdit(component: HostEditComponent) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Editing Host Config") })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            val initialPort by component.startPortValue.subscribeAsState()
            val servers by component.serversValue.subscribeAsState()
            TextField(initialPort.toString(),
                component::setInitialPort,
                singleLine = true,
                label = { Text("Start Port") })
            LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(minSize = 500.dp), Modifier.weight(1f)) {
                items(servers) { server ->
                    ServerEdit(server, component)
                }
            }
            Button(component::addServer, Modifier.fillMaxWidth()) {
                Text("Add Server")
            }
            Row {
                Button(
                    component::cancel, colors = ButtonDefaults.buttonColors(MaterialTheme.colors.error)
                ) { Text("Cancel changes") }
                Button(component::save) { Text("Save changes") }
            }
        }
    }
}

@Composable
fun ServerEdit(server: ServerConfig, component: HostEditComponent) =
    Box(Modifier.border(2.dp, MaterialTheme.colors.secondary)) {
        Column(Modifier.padding(4.dp)) {
            var expanded by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.serverName, Modifier.weight(1f))
                IconButton({ expanded = !expanded }) {
                    if (expanded) Icon(Icons.Default.ArrowDropUp, "Hide Server Configuration")
                    else Icon(Icons.Default.ArrowDropDown, "Show Server Configuration")
                }
            }

            if (expanded) {
                TextField(server.serverName,
                    { component.setServerName(server, it) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Server Name") })
                TextField(server.baseDirectory,
                    { component.setServerBase(server, it) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Base Directory") })
                TextField(server.startCommand,
                    { component.setServerStart(server, it) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Start Command") })
            }
        }
    }