package aarambh.apps.resqheat.ui.home

import aarambh.apps.resqheat.model.Priority
import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (Request) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var resourceType by remember { mutableStateOf("") }
    var estimatedDaysCoveredText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var isResourceMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Request") },
        text = {
            Column {
                // Request type selection (Rescue / Resource)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isResourceMode) {
                        Button(onClick = { isResourceMode = false }) { Text("Rescue") }
                        OutlinedButton(onClick = { isResourceMode = true }) { Text("Resource") }
                    } else {
                        OutlinedButton(onClick = { isResourceMode = false }) { Text("Rescue") }
                        Button(onClick = { isResourceMode = true }) { Text("Resource") }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Text(text = "Location will be captured automatically on submit", modifier = Modifier.padding(top = 8.dp))

                if (isResourceMode) {
                    OutlinedTextField(
                        value = resourceType,
                        onValueChange = { resourceType = it },
                        label = { Text("Resource type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = estimatedDaysCoveredText,
                        onValueChange = { estimatedDaysCoveredText = it },
                        label = { Text("Estimated days covered") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "Priority: ${'$'}{priority.name}")
                    Button(onClick = { priorityMenuExpanded = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Choose priority")
                    }
                    DropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                        Priority.values().forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                priority = p
                                priorityMenuExpanded = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val days = estimatedDaysCoveredText.toIntOrNull()
                val request = Request(
                    title = title.trim(),
                    notes = notes.ifBlank { null },
                    lat = 0.0,
                    lng = 0.0,
                    priority = priority,
                    type = if (isResourceMode) RequestType.RESOURCE else RequestType.RESCUE,
                    resourceType = if (isResourceMode) resourceType.ifBlank { null } else null,
                    estimatedDaysCovered = if (isResourceMode) days else null
                )
                onSubmit(request)
            }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


