package aarambh.apps.resqheat.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aarambh.apps.resqheat.data.UserRepository
import aarambh.apps.resqheat.model.UserProfile
import aarambh.apps.resqheat.ui.common.UiState
import aarambh.apps.resqheat.utils.ValidationUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // User profile state
    private val _userProfile = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val userProfile: StateFlow<UiState<UserProfile>> = _userProfile.asStateFlow()

    // Profile update state
    private val _updateProfileState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val updateProfileState: StateFlow<UiState<Unit>> = _updateProfileState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Load user profile
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfile.value = UiState.Loading
            try {
                val uid = Firebase.auth.currentUser?.uid
                if (uid == null) {
                    _userProfile.value = UiState.Error("User not authenticated")
                    return@launch
                }

                val profile = userRepository.getUserProfile(uid)
                if (profile == null) {
                    _userProfile.value = UiState.Empty
                } else {
                    _userProfile.value = UiState.Success(profile)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load user profile", e)
                _userProfile.value = UiState.Error(
                    "Failed to load profile: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _updateProfileState.value = UiState.Loading

            try {
                // Validate profile data
                val name = if (updatedProfile.role == aarambh.apps.resqheat.model.UserRole.VICTIM) {
                    updatedProfile.victimName ?: ""
                } else {
                    updatedProfile.ngoOrgName ?: ""
                }

                val phone = if (updatedProfile.role == aarambh.apps.resqheat.model.UserRole.VICTIM) {
                    updatedProfile.victimPhone ?: ""
                } else {
                    updatedProfile.ngoOrgPhone ?: ""
                }

                // Validate name
                val nameValidation = ValidationUtils.validateName(name)
                if (!nameValidation.isValid) {
                    _updateProfileState.value = UiState.Error(
                        nameValidation.errorMessage ?: "Invalid name"
                    )
                    return@launch
                }

                // Validate phone
                val phoneValidation = ValidationUtils.validatePhone(phone)
                if (!phoneValidation.isValid) {
                    _updateProfileState.value = UiState.Error(
                        phoneValidation.errorMessage ?: "Invalid phone number"
                    )
                    return@launch
                }

                // Validate address if provided
                if (updatedProfile.address != null && updatedProfile.address.isNotBlank()) {
                    val addressValidation = ValidationUtils.validateAddress(
                        updatedProfile.address,
                        required = false
                    )
                    if (!addressValidation.isValid) {
                        _updateProfileState.value = UiState.Error(
                            addressValidation.errorMessage ?: "Invalid address"
                        )
                        return@launch
                    }
                }

                // Ensure authenticated
                if (Firebase.auth.currentUser == null) {
                    _updateProfileState.value = UiState.Error("User not authenticated")
                    return@launch
                }

                // Update profile
                userRepository.setUserProfile(updatedProfile)

                // Update local state
                _userProfile.value = UiState.Success(updatedProfile)
                _updateProfileState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile", e)
                _updateProfileState.value = UiState.Error(
                    "Failed to update profile: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Clear update profile state
     */
    fun clearUpdateProfileState() {
        _updateProfileState.value = UiState.Empty
    }
}

