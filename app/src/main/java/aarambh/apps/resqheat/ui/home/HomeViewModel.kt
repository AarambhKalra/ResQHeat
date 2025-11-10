package aarambh.apps.resqheat.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aarambh.apps.resqheat.data.FirestoreRepository
import aarambh.apps.resqheat.data.UserRepository
import aarambh.apps.resqheat.model.Priority
import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestStatus
import aarambh.apps.resqheat.model.RequestType
import aarambh.apps.resqheat.model.SafeShelter
import aarambh.apps.resqheat.model.UserProfile
import aarambh.apps.resqheat.model.UserRole
import aarambh.apps.resqheat.ui.common.UiState
import aarambh.apps.resqheat.utils.AppConstants
import aarambh.apps.resqheat.utils.NotificationHelper
import aarambh.apps.resqheat.utils.ValidationUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority as LocationPriority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // Requests state
    private val _requests = MutableStateFlow<List<Request>>(emptyList())
    val requests: StateFlow<List<Request>> = _requests.asStateFlow()

    // Shelters state
    private val _shelters = MutableStateFlow<List<SafeShelter>>(emptyList())
    val shelters: StateFlow<List<SafeShelter>> = _shelters.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Filters
    private val _selectedRequestType = MutableStateFlow<RequestType?>(null)
    val selectedRequestType: StateFlow<RequestType?> = _selectedRequestType.asStateFlow()

    private val _mineOnly = MutableStateFlow(false)
    val mineOnly: StateFlow<Boolean> = _mineOnly.asStateFlow()
    
    // NGO filters
    private val _selectedPriority = MutableStateFlow<Priority?>(null)
    val selectedPriority: StateFlow<Priority?> = _selectedPriority.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    val selectedStatus: StateFlow<RequestStatus?> = _selectedStatus.asStateFlow()
    
    // Sort order: "priority", "distance", "date"
    private val _sortOrder = MutableStateFlow<String>("priority")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    // User location for NGO notifications
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    // Request creation state
    private val _createRequestState = MutableStateFlow<UiState<String>>(UiState.Empty)
    val createRequestState: StateFlow<UiState<String>> = _createRequestState.asStateFlow()

    // Request claim/complete state
    private val _requestActionState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val requestActionState: StateFlow<UiState<Unit>> = _requestActionState.asStateFlow()

    // Listener registrations
    private var requestsListener: ListenerRegistration? = null
    private var sheltersListener: ListenerRegistration? = null

    // Track previous requests for notification logic
    private var previousRequests = mutableMapOf<String, Request>()
    private var isFirstLoad = true

    init {
        loadUserProfile()
    }

    /**
     * Load user profile
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    val profile = userRepository.getUserProfile(uid)
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load user profile", e)
            }
        }
    }

    /**
     * Start listening to requests and shelters
     */
    fun startListening(context: Context, role: UserRole?) {
        stopListening() // Clean up existing listeners
        
        // Listen to requests
        requestsListener = firestoreRepository.listenToAllRequests(
            callback = { requests ->
                _requests.value = requests
                handleRequestChanges(context, requests, role)
            },
            onError = { e ->
                Log.e("HomeViewModel", "Error listening to requests", e)
            }
        )

        // Listen to shelters
        sheltersListener = firestoreRepository.listenToAllShelters(
            callback = { shelters ->
                _shelters.value = shelters
            },
            onError = { e ->
                Log.e("HomeViewModel", "Error listening to shelters", e)
            }
        )

        // Fetch user location for NGOs
        if (role == UserRole.NGO_ORG) {
            fetchUserLocation(context)
        }
    }

    /**
     * Stop listening to requests and shelters
     */
    fun stopListening() {
        requestsListener?.remove()
        requestsListener = null
        sheltersListener?.remove()
        sheltersListener = null
    }

    /**
     * Fetch user location for NGO notifications
     */
    private fun fetchUserLocation(context: Context) {
        viewModelScope.launch {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val token = CancellationTokenSource()
                val current = fused.getCurrentLocation(
                    LocationPriority.PRIORITY_HIGH_ACCURACY,
                    token.token
                ).await()
                
                if (current != null) {
                    _userLocation.value = LatLng(current.latitude, current.longitude)
                } else {
                    // Try last known location
                    val last = fused.lastLocation.await()
                    if (last != null) {
                        _userLocation.value = LatLng(last.latitude, last.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to get user location", e)
            }
        }
    }

    /**
     * Handle request changes and trigger notifications
     */
    private fun handleRequestChanges(context: Context, requests: List<Request>, role: UserRole?) {
        val currentUid = Firebase.auth.currentUser?.uid ?: return

        if (isFirstLoad) {
            previousRequests = requests.associateBy { it.id }.toMutableMap()
            isFirstLoad = false
            return
        }

        when (role) {
            UserRole.VICTIM -> handleVictimNotifications(context, requests, currentUid)
            UserRole.NGO_ORG -> handleNgoNotifications(context, requests)
            else -> {
                previousRequests = requests.associateBy { it.id }.toMutableMap()
            }
        }
    }

    /**
     * Handle notifications for victims
     */
    private fun handleVictimNotifications(context: Context, requests: List<Request>, currentUid: String) {
        requests.forEach { newRequest ->
            if (newRequest.createdByUid == currentUid) {
                val oldRequest = previousRequests[newRequest.id]

                // Check if request was accepted
                if (oldRequest != null &&
                    oldRequest.status != RequestStatus.BEING_SERVED &&
                    newRequest.status == RequestStatus.BEING_SERVED
                ) {
                    val ngoName = newRequest.claimedByNgoName ?: "an NGO"
                    Log.d("HomeViewModel", "Triggering BEING_SERVED notification for: ${newRequest.title}")
                    NotificationHelper.showRequestAcceptedNotification(
                        context = context,
                        requestTitle = newRequest.title,
                        ngoName = ngoName
                    )
                }

                // Check if request was fulfilled
                if (oldRequest != null &&
                    oldRequest.status != RequestStatus.SERVED &&
                    newRequest.status == RequestStatus.SERVED
                ) {
                    Log.d("HomeViewModel", "Triggering SERVED notification for: ${newRequest.title}")
                    NotificationHelper.showRequestFulfilledNotification(
                        context = context,
                        requestTitle = newRequest.title
                    )
                }
            }
        }
        previousRequests = requests.associateBy { it.id }.toMutableMap()
    }

    /**
     * Handle notifications for NGOs
     */
    private fun handleNgoNotifications(context: Context, requests: List<Request>) {
        val userLoc = _userLocation.value

        requests.forEach { newRequest ->
            val oldRequest = previousRequests[newRequest.id]

            // Check for new requests
            if (oldRequest == null && newRequest.status == RequestStatus.NOT_SERVED) {
                val isNearby = userLoc?.let { loc ->
                    val distance = calculateDistance(
                        loc.latitude, loc.longitude,
                        newRequest.lat, newRequest.lng
                    )
                    distance <= AppConstants.NOTIFICATION_RADIUS_KM
                } ?: true

                if (isNearby) {
                    val requestType = newRequest.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    Log.d("HomeViewModel", "New request notification for NGO: ${newRequest.title}")
                    NotificationHelper.showNewRequestNotification(
                        context = context,
                        requestTitle = newRequest.title,
                        requestType = requestType
                    )
                }
            }

            // Check for high-priority requests
            if (newRequest.status == RequestStatus.NOT_SERVED &&
                newRequest.priority == Priority.HIGH
            ) {
                val isNearby = userLoc?.let { loc ->
                    val distance = calculateDistance(
                        loc.latitude, loc.longitude,
                        newRequest.lat, newRequest.lng
                    )
                    distance <= AppConstants.NOTIFICATION_RADIUS_KM
                } ?: true

                if (isNearby) {
                    val wasAlreadyHighPriority = oldRequest?.priority == Priority.HIGH
                    if (!wasAlreadyHighPriority || oldRequest == null) {
                        val requestType = newRequest.type.name.lowercase().replaceFirstChar { it.uppercase() }
                        Log.d("HomeViewModel", "High priority request notification for NGO: ${newRequest.title}")
                        NotificationHelper.showHighPriorityRequestNotification(
                            context = context,
                            requestTitle = newRequest.title,
                            requestType = requestType
                        )
                    }
                }
            }
        }
        previousRequests = requests.associateBy { it.id }.toMutableMap()
    }

    /**
     * Calculate distance between two coordinates in kilometers
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return AppConstants.EARTH_RADIUS_KM * c
    }

    /**
     * Set request type filter
     */
    fun setRequestTypeFilter(type: RequestType?) {
        _selectedRequestType.value = type
    }

    /**
     * Set mine only filter
     */
    fun setMineOnlyFilter(enabled: Boolean) {
        _mineOnly.value = enabled
    }

    /**
     * Set priority filter (for NGO)
     */
    fun setPriorityFilter(priority: Priority?) {
        _selectedPriority.value = priority
    }
    
    /**
     * Set status filter (for NGO)
     */
    fun setStatusFilter(status: RequestStatus?) {
        _selectedStatus.value = status
    }
    
    /**
     * Set sort order (for NGO): "priority", "distance", "date"
     */
    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }
    
    /**
     * Clear all NGO filters
     */
    fun clearNgoFilters() {
        _selectedPriority.value = null
        _selectedStatus.value = null
        _sortOrder.value = "priority"
    }

    /**
     * Get filtered requests
     */
    fun getFilteredRequests(): List<Request> {
        var filtered = _requests.value

        // Filter by request type
        _selectedRequestType.value?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        // Filter by mine only
        if (_mineOnly.value) {
            val currentUid = Firebase.auth.currentUser?.uid
            if (currentUid != null) {
                filtered = filtered.filter { it.createdByUid == currentUid }
            }
        }
        
        // Filter by priority (for NGO)
        _selectedPriority.value?.let { priority ->
            filtered = filtered.filter { it.priority == priority }
        }
        
        // Filter by status (for NGO)
        _selectedStatus.value?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        return filtered
    }
    
    /**
     * Get sorted requests (for NGO)
     */
    fun getSortedRequests(requests: List<Request>, userLocation: LatLng?): List<Request> {
        val sortOrderValue = _sortOrder.value
        return when (sortOrderValue) {
            "priority" -> {
                requests.sortedByDescending {
                    when (it.priority) {
                        Priority.HIGH -> 3
                        Priority.MEDIUM -> 2
                        Priority.LOW -> 1
                    }
                }
            }
            "distance" -> {
                val userLoc = userLocation ?: _userLocation.value
                if (userLoc != null) {
                    requests.sortedBy { request ->
                        calculateDistance(
                            userLoc.latitude, userLoc.longitude,
                            request.lat, request.lng
                        )
                    }
                } else {
                    requests // If no location, return as-is
                }
            }
            "date" -> {
                requests.sortedByDescending { it.createdAt }
            }
            else -> requests
        }
    }

    /**
     * Create a new request
     */
    fun createRequest(
        request: Request,
        lat: Double?,
        lng: Double?,
        lastLat: Double?,
        lastLng: Double?
    ) {
        viewModelScope.launch {
            _createRequestState.value = UiState.Loading

            try {
                // Validate coordinates
                val finalLat = lat ?: lastLat
                val finalLng = lng ?: lastLng

                if (finalLat == null || finalLng == null) {
                    _createRequestState.value = UiState.Error(
                        "Location not available. Please try again after granting permission."
                    )
                    return@launch
                }

                // Validate input
                val titleValidation = ValidationUtils.validateTitle(request.title)
                if (!titleValidation.isValid) {
                    _createRequestState.value = UiState.Error(
                        titleValidation.errorMessage ?: "Invalid title"
                    )
                    return@launch
                }

                val coordinateValidation = ValidationUtils.validateCoordinates(finalLat, finalLng)
                if (!coordinateValidation.isValid) {
                    _createRequestState.value = UiState.Error(
                        coordinateValidation.errorMessage ?: "Invalid location coordinates"
                    )
                    return@launch
                }

                val notesValidation = ValidationUtils.validateNotes(request.notes ?: "", required = false)
                if (!notesValidation.isValid) {
                    _createRequestState.value = UiState.Error(
                        notesValidation.errorMessage ?: "Invalid notes"
                    )
                    return@launch
                }

                // Ensure authenticated
                if (Firebase.auth.currentUser == null) {
                    Firebase.auth.signInAnonymously().await()
                }

                val uid = Firebase.auth.currentUser?.uid ?: ""
                val requestId = firestoreRepository.createRequest(
                    request.copy(
                        lat = finalLat,
                        lng = finalLng,
                        createdByUid = uid
                    )
                )

                _createRequestState.value = UiState.Success(requestId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to create request", e)
                _createRequestState.value = UiState.Error(
                    "Failed to submit request: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Claim a request (for NGOs)
     */
    fun claimRequest(requestId: String, eta: String) {
        viewModelScope.launch {
            _requestActionState.value = UiState.Loading

            try {
                val currentUid = Firebase.auth.currentUser?.uid
                if (currentUid == null) {
                    _requestActionState.value = UiState.Error("Not signed in")
                    return@launch
                }

                val profile = userRepository.getUserProfile(currentUid)
                firestoreRepository.claimRequest(
                    requestId,
                    currentUid,
                    profile?.ngoOrgName,
                    profile?.ngoOrgPhone,
                    eta
                )

                _requestActionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to claim request", e)
                _requestActionState.value = UiState.Error(
                    "Failed to claim request: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Complete a request (for NGOs)
     */
    fun completeRequest(requestId: String, estimatedDaysCovered: Int?) {
        viewModelScope.launch {
            _requestActionState.value = UiState.Loading

            try {
                if (estimatedDaysCovered != null) {
                    val daysValidation = ValidationUtils.validateEstimatedDays(estimatedDaysCovered)
                    if (!daysValidation.isValid) {
                        _requestActionState.value = UiState.Error(
                            daysValidation.errorMessage ?: "Invalid estimated days"
                        )
                        return@launch
                    }
                }

                firestoreRepository.completeRequest(requestId, estimatedDaysCovered)
                _requestActionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to complete request", e)
                _requestActionState.value = UiState.Error(
                    "Failed to complete request: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Clear request creation state
     */
    fun clearCreateRequestState() {
        _createRequestState.value = UiState.Empty
    }

    /**
     * Clear request action state
     */
    fun clearRequestActionState() {
        _requestActionState.value = UiState.Empty
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}

