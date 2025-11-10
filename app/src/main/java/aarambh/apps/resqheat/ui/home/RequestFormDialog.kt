package aarambh.apps.resqheat.ui.home

import aarambh.apps.resqheat.model.Priority
import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestType
import aarambh.apps.resqheat.utils.ValidationUtils
import aarambh.apps.resqheat.utils.AppConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var isResourceMode by remember { mutableStateOf(false) }
    
    // Validation states
    var titleError by remember { mutableStateOf<String?>(null) }
    var notesError by remember { mutableStateOf<String?>(null) }
    var estimatedDaysError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Create Request",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Request type selection (Rescue / Resource)
                Text(
                    text = "Request Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = !isResourceMode,
                        onClick = { isResourceMode = false },
                        label = { 
                            Text(
                                text = "Rescue",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.FilterChip(
                        selected = isResourceMode,
                        onClick = { isResourceMode = true },
                        label = { 
                            Text(
                                text = "Resource",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        // Clear error when user starts typing
                        if (titleError != null) {
                            val validation = ValidationUtils.validateTitle(it)
                            titleError = if (!validation.isValid) validation.errorMessage else null
                        }
                    },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { 
                        notes = it
                        // Clear error when user starts typing
                        if (notesError != null) {
                            val validation = ValidationUtils.validateNotes(it, required = false)
                            notesError = if (!validation.isValid) validation.errorMessage else null
                        }
                    },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    isError = notesError != null,
                    supportingText = notesError?.let { { Text(it) } }
                )

                if (isResourceMode) {
                    OutlinedTextField(
                        value = resourceType,
                        onValueChange = { resourceType = it },
                        label = { Text("Resource type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
//                    OutlinedTextField(
//                        value = estimatedDaysCoveredText,
//                        onValueChange = {
//                            estimatedDaysCoveredText = it
//                            // Clear error when user starts typing
//                            if (estimatedDaysError != null) {
//                                val days = it.toIntOrNull()
//                                val validation = ValidationUtils.validateEstimatedDays(days)
//                                estimatedDaysError = if (!validation.isValid) validation.errorMessage else null
//                            }
//                        },
//                        label = { Text("Estimated days covered") },
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(top = 8.dp),
//                        isError = estimatedDaysError != null,
//                        supportingText = estimatedDaysError?.let { { Text(it) } }
//                    )
                }

                // Priority selection
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.entries.forEach { p ->
                            androidx.compose.material3.FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { 
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Location info
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📍 Location will be captured automatically on submit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    // Validate before submitting
                    val titleValidation = ValidationUtils.validateTitle(title)
                    if (!titleValidation.isValid) {
                        titleError = titleValidation.errorMessage
                        return@Button
                    }
                    
                    val notesValidation = ValidationUtils.validateNotes(notes, required = false)
                    if (!notesValidation.isValid) {
                        notesError = notesValidation.errorMessage
                        return@Button
                    }
                    
                    val days = estimatedDaysCoveredText.toIntOrNull()
                    if (isResourceMode && estimatedDaysCoveredText.isNotBlank()) {
                        val daysValidation = ValidationUtils.validateEstimatedDays(days)
                        if (!daysValidation.isValid) {
                            estimatedDaysError = daysValidation.errorMessage
                            return@Button
                        }
                    }
                    
                    // Clear errors if validation passes
                    titleError = null
                    notesError = null
                    estimatedDaysError = null
                    
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}


