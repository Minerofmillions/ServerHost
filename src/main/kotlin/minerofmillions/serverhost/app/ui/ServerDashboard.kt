package minerofmillions.serverhost.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import minerofmillions.serverhost.Server
import minerofmillions.serverhost.Server.ServerState.*
import minerofmillions.serverhost.app.ui.utils.ScrollableColumn
import minerofmillions.utils.truncate

@Composable
fun ColumnScope.ServerDashboard(server: Server) {
    val serverState by server.serverState.subscribeAsState()
    val isErrored by server.isErrored.subscribeAsState()

    if (isErrored) ErrorDashboard((serverState as ERRORED).reason)
    else ValidDashboard(server)
}

@Composable
private fun ErrorDashboard(reason: String) = Column(Modifier.border(2.dp, MaterialTheme.colors.error)) {
    Text("Error: $reason")
}

@Composable
private fun ColumnScope.ValidDashboard(server: Server) {
    val serverState by server.serverState.subscribeAsState()
    val logShowing by server.logShowing.subscribeAsState()
    val autoOff by server.autoOff.subscribeAsState()
    val autoOffDelay by server.autoOffDuration.subscribeAsState()

    val modifier = Modifier.border(2.dp, MaterialTheme.colors.secondary).run {
        if (logShowing) weight(1f) else height(IntrinsicSize.Min)
    }

    Box(modifier) {
        val logState = rememberLazyListState()

        Column(Modifier.padding(4.dp)) {
            Text(server.serverName, style = MaterialTheme.typography.h1)
            Text(server.baseDirectory.truncate(50))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto Off:", Modifier.weight(1f))
                Checkbox(autoOff, server::setAutoOff)
                TextField(autoOffDelay.toString(),
                    server::setAutoOffDelay,
                    label = { Text("Auto Off Delay") },
                    trailingIcon = { Text("ms") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Is Active:", Modifier.weight(1f))
                if (serverState != STOPPING) Checkbox(serverState == STARTED, server::setServerActive)
                else CircularProgressIndicator()
            }
            if (logShowing) {
                val log by server.serverLog.subscribeAsState()
                val revLog by remember { derivedStateOf { log.asReversed() } }
                val command by server.command.subscribeAsState()

                Text("Log:")
                ScrollableColumn(
                    Modifier.weight(1f).fillMaxWidth().border(1.dp, MaterialTheme.colors.secondary),
                    logState,
                    PaddingValues(3.dp),
                    reverseLayout = true
                ) {
                    items(revLog) {
                        Text(it, fontFamily = FontFamily.Monospace)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(server::gatherFavicon) {
                        Text("Gather Favicon")
                    }
                    TextField(command,
                        server::setCommand,
                        Modifier.weight(1f),
                        singleLine = true,
                        keyboardActions = KeyboardActions { server.sendCommand() }
                    )
                    IconButton(onClick = server::sendCommand) {
                        Icon(Icons.Default.Send, "Send Command")
                    }
                }
            }
        }
    }
}
