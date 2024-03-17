package minerofmillions.serverhost.app.ui.utils

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <E> Selector(
    options: List<E>,
    selected: E,
    onSelect: (E) -> Unit,
    modifier: Modifier = Modifier,
    label: (E) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        TextField(value = label(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(onClick = { onSelect(it); expanded = false }) {
                    Text(label(it))
                }
            }
        }
    }
}

@Composable
fun <E> Selector(
    options: Array<E>,
    selected: E,
    onSelect: (E) -> Unit,
    modifier: Modifier = Modifier,
    label: (E) -> String,
) = Selector(options.toList(), selected, onSelect, modifier, label)