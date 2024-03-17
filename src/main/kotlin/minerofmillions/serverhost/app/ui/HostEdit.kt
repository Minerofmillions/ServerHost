package minerofmillions.serverhost.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import minerofmillions.serverhost.app.components.HostEditComponent
import minerofmillions.serverhost.app.ui.utils.ScrollableColumn

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
            ScrollableColumn(Modifier.weight(1f)) {
                items(servers) { server ->
                    Column {
                        TextField(server.serverName, { component.setServerName(server, it) }, Modifier.fillMaxWidth())
                        TextField(server.baseDirectory, { component.setServerBase(server, it) }, Modifier.fillMaxWidth())
                        TextField(server.startCommand, { component.setServerStart(server, it) }, Modifier.fillMaxWidth())
                    }
                }
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