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
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import aarambh.apps.resqheat.model.UserProfile
import aarambh.apps.resqheat.model.UserRole
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    onBackClick: () -> Unit,
    onSave: (UserProfile) -> Unit,
    isLoading: Boolean = false,
    viewModel: ProfileViewModel = viewModel()
) {
    // Collect state from ViewModel
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val updateState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    
    // Snackbar for error messages
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Handle update state
    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is aarambh.apps.resqheat.ui.common.UiState.Success -> {
                snackbarHostState.showSnackbar("Profile updated successfully")
                onBackClick()
                viewModel.clearUpdateProfileState()
            }
            is aarambh.apps.resqheat.ui.common.UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearUpdateProfileState()
            }
            else -> {}
        }
    }
    
    // Use ViewModel profile if available, otherwise use passed profile
    val currentProfile = when (val state = profileState) {
        is aarambh.apps.resqheat.ui.common.UiState.Success -> state.data
        else -> userProfile
    }
    
    val name = if (currentProfile?.role == UserRole.VICTIM) {
        currentProfile.victimName ?: ""
    } else {
        currentProfile?.ngoOrgName ?: ""
    }
    
    val phone = if (currentProfile?.role == UserRole.VICTIM) {
        currentProfile.victimPhone ?: ""
    } else {
        currentProfile?.ngoOrgPhone ?: ""
    }
    
    var editedName by remember { mutableStateOf(name) }
    var editedPhone by remember { mutableStateOf(phone) }
    var editedAddress by remember { mutableStateOf(currentProfile?.address ?: "") }
    
    // Update when profile changes
    LaunchedEffect(currentProfile) {
        editedName = if (currentProfile?.role == UserRole.VICTIM) {
            currentProfile.victimName ?: ""
        } else {
            currentProfile?.ngoOrgName ?: ""
        }
        editedPhone = if (currentProfile?.role == UserRole.VICTIM) {
            currentProfile.victimPhone ?: ""
        } else {
            currentProfile?.ngoOrgPhone ?: ""
        }
        editedAddress = currentProfile?.address ?: ""
    }
    
    val isSaving = updateState.isLoading || isLoading
    
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
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
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
            
            val nameLabel = if (currentProfile?.role == UserRole.VICTIM) {
                "Victim Name"
            } else {
                "NGO/Org Name"
            }
            
            val phoneLabel = if (currentProfile?.role == UserRole.VICTIM) {
                "Victim Phone"
            } else {
                "NGO/Org Phone"
            }
            
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text(nameLabel) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            
            OutlinedTextField(
                value = editedPhone,
                onValueChange = { editedPhone = it },
                label = { Text(phoneLabel) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            
            OutlinedTextField(
                value = editedAddress,
                onValueChange = { editedAddress = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val canSave = editedName.isNotBlank() && editedPhone.isNotBlank()
            
            Button(
                onClick = {
                    if (currentProfile != null && canSave) {
                        val updatedProfile = if (currentProfile.role == UserRole.VICTIM) {
                            currentProfile.copy(
                                victimName = editedName.trim(),
                                victimPhone = editedPhone.trim(),
                                address = editedAddress.trim().takeIf { it.isNotBlank() },
                                displayName = editedName.trim()
                            )
                        } else {
                            currentProfile.copy(
                                ngoOrgName = editedName.trim(),
                                ngoOrgPhone = editedPhone.trim(),
                                address = editedAddress.trim().takeIf { it.isNotBlank() },
                                displayName = editedName.trim()
                            )
                        }
                        viewModel.updateProfile(updatedProfile)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave && !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save Changes")
            }
            
            if (currentProfile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Role: ${currentProfile.role.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "User ID: ${currentProfile.uid.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

