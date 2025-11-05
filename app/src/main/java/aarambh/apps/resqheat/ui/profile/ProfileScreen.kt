package aarambh.apps.resqheat.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import aarambh.apps.resqheat.model.UserProfile
import aarambh.apps.resqheat.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    onBackClick: () -> Unit,
    onSave: (UserProfile) -> Unit,
    isLoading: Boolean = false
) {
    val name = if (userProfile?.role == UserRole.VICTIM) {
        userProfile.victimName ?: ""
    } else {
        userProfile?.ngoOrgName ?: ""
    }
    
    val phone = if (userProfile?.role == UserRole.VICTIM) {
        userProfile.victimPhone ?: ""
    } else {
        userProfile?.ngoOrgPhone ?: ""
    }
    
    var editedName by remember { mutableStateOf(name) }
    var editedPhone by remember { mutableStateOf(phone) }
    var editedAddress by remember { mutableStateOf(userProfile?.address ?: "") }
    
    // Update when profile changes
    androidx.compose.runtime.LaunchedEffect(userProfile) {
        editedName = if (userProfile?.role == UserRole.VICTIM) {
            userProfile.victimName ?: ""
        } else {
            userProfile?.ngoOrgName ?: ""
        }
        editedPhone = if (userProfile?.role == UserRole.VICTIM) {
            userProfile.victimPhone ?: ""
        } else {
            userProfile?.ngoOrgPhone ?: ""
        }
        editedAddress = userProfile?.address ?: ""
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile / Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Your Information",
                style = MaterialTheme.typography.titleLarge
            )
            
            val nameLabel = if (userProfile?.role == UserRole.VICTIM) {
                "Victim Name"
            } else {
                "NGO/Org Name"
            }
            
            val phoneLabel = if (userProfile?.role == UserRole.VICTIM) {
                "Victim Phone"
            } else {
                "NGO/Org Phone"
            }
            
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text(nameLabel) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = editedPhone,
                onValueChange = { editedPhone = it },
                label = { Text(phoneLabel) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = editedAddress,
                onValueChange = { editedAddress = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val canSave = editedName.isNotBlank() && editedPhone.isNotBlank()
            
            Button(
                onClick = {
                    if (userProfile != null && canSave) {
                        val updatedProfile = if (userProfile.role == UserRole.VICTIM) {
                            userProfile.copy(
                                victimName = editedName.trim(),
                                victimPhone = editedPhone.trim(),
                                address = editedAddress.trim().takeIf { it.isNotBlank() },
                                displayName = editedName.trim()
                            )
                        } else {
                            userProfile.copy(
                                ngoOrgName = editedName.trim(),
                                ngoOrgPhone = editedPhone.trim(),
                                address = editedAddress.trim().takeIf { it.isNotBlank() },
                                displayName = editedName.trim()
                            )
                        }
                        onSave(updatedProfile)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave && !isLoading
            ) {
                Text(if (isLoading) "Saving..." else "Save Changes")
            }
            
            if (userProfile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Role: ${userProfile.role.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "User ID: ${userProfile.uid.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

