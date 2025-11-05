package aarambh.apps.resqheat.ui.auth

import aarambh.apps.resqheat.model.UserRole
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RolePickerDialog(
    onSubmit: (UserRole, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var role by remember { mutableStateOf(UserRole.VICTIM) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val nameLabel = if (role == UserRole.VICTIM) "Victim name" else "NGO/Org name"
    val phoneLabel = if (role == UserRole.VICTIM) "Victim phone" else "NGO/Org phone"
    val canSubmit = name.isNotBlank() && phone.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Choose your role") },
        text = {
            Column {
                Text("Select role and provide details")
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    if (role == UserRole.VICTIM) {
                        Button(onClick = { role = UserRole.VICTIM }) { Text("Victim") }
                        Spacer(modifier = Modifier.height(0.dp))
                        OutlinedButton(onClick = { role = UserRole.NGO_ORG }) { Text("NGO / Org") }
                    } else {
                        OutlinedButton(onClick = { role = UserRole.VICTIM }) { Text("Victim") }
                        Spacer(modifier = Modifier.height(0.dp))
                        Button(onClick = { role = UserRole.NGO_ORG }) { Text("NGO / Org") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(phoneLabel) })
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(role, name.trim(), phone.trim()) }, enabled = canSubmit) {
                Text("Continue")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


