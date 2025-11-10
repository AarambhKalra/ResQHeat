package aarambh.apps.resqheat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import aarambh.apps.resqheat.ui.theme.ResQHeatTheme
import aarambh.apps.resqheat.ui.home.HomeScreen
import aarambh.apps.resqheat.ui.profile.ProfileScreen
import aarambh.apps.resqheat.data.FirestoreRepository
import aarambh.apps.resqheat.data.UserRepository
import aarambh.apps.resqheat.data.SafeShelterDataUploader
import aarambh.apps.resqheat.model.UserProfile
import aarambh.apps.resqheat.model.UserRole
import aarambh.apps.resqheat.ui.auth.RolePickerDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.withContext
import aarambh.apps.resqheat.utils.NotificationHelper
import aarambh.apps.resqheat.utils.ValidationUtils
import aarambh.apps.resqheat.utils.AppConstants
import android.os.Build

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    private fun fetchLastLocation() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lastLat = loc.latitude
                    lastLng = loc.longitude
                }
            }
        }
    }

    private fun ensureLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            fetchLastLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            fetchLastLocation()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ensureLocationPermission()
        // Kick off anonymous auth (non-blocking). We will also enforce auth before writes.
        Firebase.auth.signInAnonymously()
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Anonymous sign-in failed", e)
                Toast.makeText(
                    this,
                    "Anonymous sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        
        // TODO: Remove this after uploading shelters once - it uploads 3 example safe shelters
        // COMMENTED OUT - Shelters already uploaded. Uncomment to re-upload if needed.
        /*
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Wait for Firebase to be ready and auth to complete
                Log.d("MainActivity", "Waiting for Firebase initialization...")
                Firebase.auth.signInAnonymously().await()
                Log.d("MainActivity", "Firebase auth ready, starting shelter upload...")
                
                val uploader = SafeShelterDataUploader()
                uploader.uploadExampleShelters()
                Log.d("MainActivity", "Successfully uploaded example safe shelters")
                
                // Show toast on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Safe shelters uploaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to upload safe shelters", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to upload shelters: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        */
        
        setContent {
            ResQHeatTheme {
                val repo = FirestoreRepository()
                val userRepo = aarambh.apps.resqheat.data.UserRepository()
                val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
                val userProfileState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<UserProfile?>(null) }
                val showRoleDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                val showProfileScreen = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    // Ensure auth and load profile; if anything fails, show role dialog
                    val uid = try {
                        if (Firebase.auth.currentUser == null) {
                            Firebase.auth.signInAnonymously().await()
                        }
                        Firebase.auth.currentUser?.uid
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Auth init failed", e)
                        null
                    }
                    if (uid == null) {
                        showRoleDialog.value = true
                    } else {
                        try {
                            val existing = userRepo.getUserProfile(uid)
                            userProfileState.value = existing
                            if (existing == null) {
                                showRoleDialog.value = true
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to load user profile", e)
                            showRoleDialog.value = true
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    if (showProfileScreen.value) {
                        ProfileScreen(
                            userProfile = userProfileState.value,
                            onBackClick = { 
                                showProfileScreen.value = false
                                // Refresh profile state after update
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val uid = Firebase.auth.currentUser?.uid
                                    if (uid != null) {
                                        val profile = userRepo.getUserProfile(uid)
                                        withContext(Dispatchers.Main) {
                                            userProfileState.value = profile
                                        }
                                    }
                                }
                            },
                            onSave = { /* Profile saving is now handled by ProfileViewModel */ },
                            isLoading = false
                        )
                    } else {
                        HomeScreen(
                            onAddRequest = { /* handled in dialog submit */ },
                            onProfileClick = { showProfileScreen.value = true },
                            onSubmitRequest = { _, _, _ -> 
                                // Request submission is now handled by HomeViewModel
                                // This parameter is kept for backward compatibility but not used
                            },
                            role = userProfileState.value?.role
                        )
                }

                if (showRoleDialog.value) {
                    RolePickerDialog(
                        onSubmit = { role, name, phone ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val uid = Firebase.auth.currentUser?.uid
                                if (uid == null) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            message = "Not signed in. Try again."
                                        )
                                    }
                                    return@launch
                                }
                                // Validate input
                                val nameValidation = ValidationUtils.validateName(name)
                                if (!nameValidation.isValid) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            message = nameValidation.errorMessage ?: "Invalid name"
                                        )
                                    }
                                    return@launch
                                }
                                
                                val phoneValidation = ValidationUtils.validatePhone(phone)
                                if (!phoneValidation.isValid) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            message = phoneValidation.errorMessage ?: "Invalid phone number"
                                        )
                                    }
                                    return@launch
                                }
                                
                                try {
                                    val profile = if (role == UserRole.VICTIM) {
                                        UserProfile(uid = uid, role = role, victimName = name.trim(), victimPhone = phone.trim(), displayName = name.trim())
                                    } else {
                                        UserProfile(uid = uid, role = role, ngoOrgName = name.trim(), ngoOrgPhone = phone.trim(), displayName = name.trim())
                                    }
                                    userRepo.setUserProfile(profile)
                                    withContext(Dispatchers.Main) {
                                        userProfileState.value = profile
                                        showRoleDialog.value = false
                                        snackbarHostState.showSnackbar(
                                            message = "Profile saved"
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to save user role", e)
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to save profile: ${e.message}"
                                        )
                                    }
                                }
                            }
                        },
                        onDismiss = { /* force selection on first run */ }
                    )
                }
            }
        }
    }

}}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun HomePreview() {
//    ResQHeatTheme {
//        HomeScreen(onAddRequest = {})
//    }
//}