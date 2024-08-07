package minerofmillions.serverhost.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import minerofmillions.serverhost.Server
import minerofmillions.serverhost.Server.ServerState.*
import minerofmillions.serverhost.app.ui.utils.ScrollableColumn
import minerofmillions.serverhost.app.ui.utils.Selector
import minerofmillions.utils.truncate

@Composable
fun ServerDashboard(server: Server) {
    val serverState by server.serverState.subscribeAsState()
    val isErrored by server.isErrored.subscribeAsState()

    if (isErrored) ErrorDashboard(server.serverName, (serverState as ERRORED).reason)
    else ValidDashboard(server)
}

@Composable
private fun ErrorDashboard(name: String, reason: String) = Box(Modifier.border(2.dp, MaterialTheme.colors.error)) {
    Column(Modifier.padding(2.dp)) {
        Text(name, style = MaterialTheme.typography.h1)
        Text("Error: $reason")
    }
}

@Composable
private fun ValidDashboard(server: Server) {
    val serverState by server.serverState.subscribeAsState()
    val logShowing by server.logShowing.subscribeAsState()
    val isUnfolded by server.isUnfolded.subscribeAsState()
    val timeoutEnabled by server.timeoutEnabled.subscribeAsState()
    val timeoutDuration by server.timeoutDuration.subscribeAsState()

    val modifier = Modifier.border(2.dp, MaterialTheme.colors.secondary).height(IntrinsicSize.Min)

    Box(modifier) {
        val logState = rememberLazyListState()

        Column(Modifier.padding(4.dp).fillMaxWidth()) {
            // Server Name
            Row {
                Text(server.serverName, style = MaterialTheme.typography.h1, modifier = Modifier.weight(1f))
                IconButton(server::toggleFolded) {
                    if (isUnfolded) Icon(Icons.Default.ArrowDropUp, "Fold")
                    else Icon(Icons.Default.ArrowDropDown, "Unfold")
                }
            }
            if (isUnfolded || logShowing) {
                // Server Location
                Text(server.baseDirectory.truncate(50))
                // Auto-off timeout
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto Off:", Modifier.weight(1f))
                        Checkbox(timeoutEnabled, server::setAutoOff)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(timeoutDuration.toString(),
                            server::setAutoOffDelay,
                            modifier = Modifier.weight(1f),
                            label = { Text("Auto Off Delay") })
                        val selectedTimeoutUnit by server.timeoutUnit.subscribeAsState()
                        Selector(
                            Server.TimeUnit.entries,
                            selectedTimeoutUnit,
                            server::selectTimeoutUnit,
                            label = Server.TimeUnit::abbreviation
                        )
                    }
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
                        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colors.secondary).height(100.dp),
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
}
