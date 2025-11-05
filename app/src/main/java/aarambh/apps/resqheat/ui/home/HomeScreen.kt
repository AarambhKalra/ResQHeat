package aarambh.apps.resqheat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.SafeShelter
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.runtime.LaunchedEffect
import aarambh.apps.resqheat.data.FirestoreRepository
import aarambh.apps.resqheat.data.UserRepository
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.compose.TileOverlay
import androidx.compose.runtime.DisposableEffect
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority as LocationPriority
import kotlinx.coroutines.tasks.await
import aarambh.apps.resqheat.model.UserRole
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import aarambh.apps.resqheat.utils.NotificationHelper
import aarambh.apps.resqheat.model.RequestStatus
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import aarambh.apps.resqheat.model.RequestType
import aarambh.apps.resqheat.model.Priority
import aarambh.apps.resqheat.model.UserProfile
import kotlinx.coroutines.launch

private class RequestClusterItem(
    val requestId: String,
    private val latLng: LatLng,
    private val titleText: String,
    private val snippetText: String?
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = titleText
    override fun getSnippet(): String? = snippetText
    override fun getZIndex(): Float? = null
}

@Composable
fun HomeScreen(
    onAddRequest: () -> Unit,
    onSubmitRequest: (Request, Double?, Double?) -> Unit,
    role: UserRole? = null,
    onProfileClick: () -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    // NGO: marker click dialog state (hoisted)
    var activeRequest by remember { mutableStateOf<Request?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    // Safe shelter dialog state
    var activeShelter by remember { mutableStateOf<SafeShelter?>(null) }
    var showShelterDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            val canCreate = role != UserRole.NGO_ORG
            if (canCreate) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Request")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ResQHeat",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Profile / Settings"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val repo = remember { FirestoreRepository() }
            val userRepo = remember { UserRepository() }
            var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
            var shelters by remember { mutableStateOf<List<SafeShelter>>(emptyList()) }
            var previousRequests by remember { mutableStateOf<Map<String, Request>>(emptyMap()) }
            var listenerReg by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
            var sheltersListenerReg by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
            
            // Monitor request changes for notifications (victims and NGOs)
            val context = LocalContext.current
            val currentUid = Firebase.auth.currentUser?.uid
            var isFirstLoad by remember { mutableStateOf(true) }
            var userLocation by remember { mutableStateOf<LatLng?>(null) }
            
            // Helper function to calculate distance in kilometers
            fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
                val earthRadius = 6371.0 // km
                val dLat = Math.toRadians(lat2 - lat1)
                val dLng = Math.toRadians(lng2 - lng1)
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                return earthRadius * c
            }
            
            // Fetch user location for NGOs
            if (role == UserRole.NGO_ORG) {
                LaunchedEffect(Unit) {
                    try {
                        val fused = LocationServices.getFusedLocationProviderClient(context)
                        val token = CancellationTokenSource()
                        val current = fused.getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY, token.token).await()
                        if (current != null) {
                            userLocation = LatLng(current.latitude, current.longitude)
                        } else {
                            // Try last known location
                            val last = fused.lastLocation.await()
                            if (last != null) {
                                userLocation = LatLng(last.latitude, last.longitude)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeScreen", "Failed to get user location for NGO notifications", e)
                    }
                }
            }
            
            DisposableEffect(role, currentUid) {
                listenerReg = repo.listenToAllRequests(callback = { items -> 
                    // Check for status changes and show notifications (Victims)
                    if (role == UserRole.VICTIM && currentUid != null) {
                        // On first load, just initialize previousRequests without checking for changes
                        if (isFirstLoad) {
                            previousRequests = items.associateBy { it.id }
                            isFirstLoad = false
                        } else {
                            items.forEach { newRequest ->
                                if (newRequest.createdByUid == currentUid) {
                                    val oldRequest = previousRequests[newRequest.id]
                                    
                                    // Debug logging for all status changes
                                    if (oldRequest != null) {
                                        if (oldRequest.status != newRequest.status) {
                                            android.util.Log.d("HomeScreen", "Status change detected for ${newRequest.title}: ${oldRequest.status} -> ${newRequest.status}")
                                            android.util.Log.d("HomeScreen", "Old NGO: ${oldRequest.claimedByNgoName}, New NGO: ${newRequest.claimedByNgoName}")
                                        }
                                    }
                                    
                                    // Check if request was just accepted (status changed to BEING_SERVED)
                                    if (oldRequest != null) {
                                        val statusChanged = oldRequest.status != newRequest.status
                                        val nowBeingServed = newRequest.status == RequestStatus.BEING_SERVED
                                        val wasNotBeingServed = oldRequest.status != RequestStatus.BEING_SERVED
                                        val hasNgoName = newRequest.claimedByNgoName != null
                                        
                                        android.util.Log.d("HomeScreen", "Checking BEING_SERVED for ${newRequest.title}:")
                                        android.util.Log.d("HomeScreen", "  statusChanged=$statusChanged, nowBeingServed=$nowBeingServed, wasNotBeingServed=$wasNotBeingServed, hasNgoName=$hasNgoName")
                                        
                                        // Show notification even if NGO name is null (use generic message)
                                        if (wasNotBeingServed && nowBeingServed) {
                                            val ngoName = newRequest.claimedByNgoName ?: "an NGO"
                                            android.util.Log.d("HomeScreen", "✓✓✓ TRIGGERING BEING_SERVED NOTIFICATION for: ${newRequest.title} by $ngoName")
                                            NotificationHelper.showRequestAcceptedNotification(
                                                context = context,
                                                requestTitle = newRequest.title,
                                                ngoName = ngoName
                                            )
                                        }
                                    }
                                    
                                    // Check if request was just fulfilled (status changed to SERVED)
                                    if (oldRequest != null && 
                                        oldRequest.status != RequestStatus.SERVED && 
                                        newRequest.status == RequestStatus.SERVED) {
                                        android.util.Log.d("HomeScreen", "✓ Triggering SERVED notification for: ${newRequest.title}")
                                        NotificationHelper.showRequestFulfilledNotification(
                                            context = context,
                                            requestTitle = newRequest.title
                                        )
                                    }
                                }
                            }
                            previousRequests = items.associateBy { it.id }
                        }
                    } else if (role == UserRole.NGO_ORG) {
                        // NGO notifications: New requests and high-priority requests nearby
                        if (isFirstLoad) {
                            previousRequests = items.associateBy { it.id }
                            isFirstLoad = false
                        } else {
                            items.forEach { newRequest ->
                                val oldRequest = previousRequests[newRequest.id]
                                
                                // Check if this is a new request (not in previousRequests)
                                if (oldRequest == null && newRequest.status == RequestStatus.NOT_SERVED) {
                                    // Check if request is in user's area (within 50km radius)
                                    val isNearby = userLocation?.let { userLoc ->
                                        val distance = calculateDistance(
                                            userLoc.latitude, userLoc.longitude,
                                            newRequest.lat, newRequest.lng
                                        )
                                        distance <= 50.0 // 50km radius
                                    } ?: true // If location not available, show notification anyway
                                    
                                    if (isNearby) {
                                        val requestType = newRequest.type.name.lowercase().replaceFirstChar { it.uppercase() }
                                        android.util.Log.d("HomeScreen", "✓ New request notification for NGO: ${newRequest.title}")
                                        NotificationHelper.showNewRequestNotification(
                                            context = context,
                                            requestTitle = newRequest.title,
                                            requestType = requestType
                                        )
                                    }
                                }
                                
                                // Check for high-priority requests nearby
                                if (newRequest.status == RequestStatus.NOT_SERVED && 
                                    newRequest.priority == Priority.HIGH) {
                                    val isNearby = userLocation?.let { userLoc ->
                                        val distance = calculateDistance(
                                            userLoc.latitude, userLoc.longitude,
                                            newRequest.lat, newRequest.lng
                                        )
                                        distance <= 50.0 // 50km radius
                                    } ?: true // If location not available, show notification anyway
                                    
                                    if (isNearby) {
                                        // Check if we haven't already notified for this request
                                        val wasAlreadyHighPriority = oldRequest?.priority == Priority.HIGH
                                        if (!wasAlreadyHighPriority || oldRequest == null) {
                                            val requestType = newRequest.type.name.lowercase().replaceFirstChar { it.uppercase() }
                                            android.util.Log.d("HomeScreen", "✓ High priority request notification for NGO: ${newRequest.title}")
                                            NotificationHelper.showHighPriorityRequestNotification(
                                                context = context,
                                                requestTitle = newRequest.title,
                                                requestType = requestType
                                            )
                                        }
                                    }
                                }
                            }
                            previousRequests = items.associateBy { it.id }
                        }
                    } else {
                        // For other roles or if no UID, just update requests without notification logic
                        previousRequests = items.associateBy { it.id }
                    }
                    requests = items
                })
                sheltersListenerReg = repo.listenToAllShelters(
                    callback = { items -> 
                        android.util.Log.d("HomeScreen", "Received ${items.size} shelters from Firestore")
                        items.forEach { shelter ->
                            android.util.Log.d("HomeScreen", "Shelter: ${shelter.name} at (${shelter.lat}, ${shelter.lng}), active: ${shelter.isActive}")
                        }
                        shelters = items
                    },
                    onError = { e ->
                        android.util.Log.e("HomeScreen", "Error listening to shelters", e)
                    }
                )
                onDispose {
                    listenerReg?.remove()
                    listenerReg = null
                    sheltersListenerReg?.remove()
                    sheltersListenerReg = null
                }
            }
            

            // Victim-only: Mine only filter
            var mineOnly by remember { mutableStateOf(false) }
            if (role == UserRole.VICTIM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Mine only", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = mineOnly, onCheckedChange = { mineOnly = it })
                }
            }

            // NGO-only: Rescue/Resource toggle filter
            var selectedRequestType by remember { mutableStateOf<RequestType?>(null) }
            if (role == UserRole.NGO_ORG) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedRequestType == RequestType.RESCUE,
                        onClick = { 
                            selectedRequestType = if (selectedRequestType == RequestType.RESCUE) null else RequestType.RESCUE
                        },
                        label = { Text("Rescue") }
                    )
                    FilterChip(
                        selected = selectedRequestType == RequestType.RESOURCE,
                        onClick = { 
                            selectedRequestType = if (selectedRequestType == RequestType.RESOURCE) null else RequestType.RESOURCE
                        },
                        label = { Text("Resource") }
                    )
                }
            }

            val visibleRequests = remember(requests, mineOnly, currentUid, role, selectedRequestType) {
                var filtered = requests
                
                // Filter by request type for NGOs
                if (role == UserRole.NGO_ORG && selectedRequestType != null) {
                    filtered = filtered.filter { it.type == selectedRequestType }
                }
                
                // Filter by "mine only" for victims
                if (role == UserRole.VICTIM && mineOnly && currentUid != null) {
                    filtered = filtered.filter { it.createdByUid == currentUid }
                }
                
                filtered
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Persisted camera state
            var savedLat by rememberSaveable { mutableStateOf(20.5937) }
            var savedLng by rememberSaveable { mutableStateOf(78.9629) }
            var savedZoom by rememberSaveable { mutableStateOf(4f) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(savedLat, savedLng), savedZoom)
            }

            // Try to center on current location only on first run when no manual position saved
            LaunchedEffect(context) {
                try {
                    if (savedZoom <= 4f + 0.01f) {
                        val fused = LocationServices.getFusedLocationProviderClient(context)
                        val token = CancellationTokenSource()
                        val current = fused.getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY, token.token).await()
                        if (current != null) {
                            val latLng = LatLng(current.latitude, current.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            // Persist camera updates
            LaunchedEffect(cameraPositionState) {
                snapshotFlow { cameraPositionState.position }
                    .map { it.target.latitude to it.target.longitude to it.zoom }
                    .distinctUntilChanged()
                    .collect {
                        savedLat = cameraPositionState.position.target.latitude
                        savedLng = cameraPositionState.position.target.longitude
                        savedZoom = cameraPositionState.position.zoom
                    }
            }

            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val canShowMyLocation = hasFine || hasCoarse

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = canShowMyLocation
                ),
                onMapLongClick = { latLng ->
                    selectedLatLng = latLng
                }
            ) {
                // Selected pin
                selectedLatLng?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = "Selected location"
                    )
                }

                // Clustered markers for visible requests
                MapEffect(visibleRequests, role) { map ->
                    val clusterManager = ClusterManager<RequestClusterItem>(context, map)
                    clusterManager.clearItems()
                    val items = visibleRequests.map { r ->
                        RequestClusterItem(
                            r.id,
                            LatLng(r.lat, r.lng),
                            r.title,
                            r.notes
                        )
                    }
                    clusterManager.addItems(items)
                    clusterManager.setOnClusterItemClickListener { item ->
                        // Make markers clickable for all users
                        val req = visibleRequests.firstOrNull { it.id == item.requestId }
                        if (req != null) {
                            showRequestDialog = false
                            activeRequest = req
                            showRequestDialog = true
                        }
                        true
                    }
                    clusterManager.cluster()
                    map.setOnCameraIdleListener(clusterManager)
                    map.setOnMarkerClickListener(clusterManager)
                }

                // Safe shelter markers (blue icons)
                android.util.Log.d("HomeScreen", "Rendering ${shelters.size} shelters on map")
                shelters.forEach { shelter ->
                    if (shelter.lat != 0.0 && shelter.lng != 0.0 && shelter.isActive) {
                        android.util.Log.d("HomeScreen", "Adding marker for: ${shelter.name} at (${shelter.lat}, ${shelter.lng})")
                        Marker(
                            state = MarkerState(position = LatLng(shelter.lat, shelter.lng)),
                            title = shelter.name,
                            snippet = "Available: ${shelter.availabilityText}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = {
                                activeShelter = shelter
                                showShelterDialog = true
                                true
                            }
                        )
                    } else {
                        android.util.Log.w("HomeScreen", "Skipping shelter ${shelter.name}: lat=${shelter.lat}, lng=${shelter.lng}, active=${shelter.isActive}")
                    }
                }

                // Heatmap overlay weighted by priority
                if (visibleRequests.isNotEmpty()) {
                    val weightedPoints = visibleRequests.mapNotNull { r ->
                        if (r.lat == 0.0 && r.lng == 0.0) return@mapNotNull null
                        val latLng = LatLng(r.lat, r.lng)
                        val weight = when (r.priority) {
                            aarambh.apps.resqheat.model.Priority.HIGH -> 3.0
                            aarambh.apps.resqheat.model.Priority.MEDIUM -> 2.0
                            aarambh.apps.resqheat.model.Priority.LOW -> 1.0
                        }
                        WeightedLatLng(latLng, weight)
                    }
                    if (weightedPoints.isNotEmpty()) {
                        val provider = HeatmapTileProvider.Builder()
                            .weightedData(weightedPoints)
                            .build()
                        TileOverlay(tileProvider = provider)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recenter + selection controls
            val scope = rememberCoroutineScope()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedLatLng != null) {
                    androidx.compose.material3.OutlinedButton(onClick = { selectedLatLng = null }) {
                        Text("Clear pin")
                    }
                }
            }

            Text(text = "Requests", style = MaterialTheme.typography.titleMedium)
            Text(text = "${visibleRequests.size} total", modifier = Modifier.padding(bottom = 8.dp))

            // Sorted list by priority (High -> Low) - NGO only
            if (role == UserRole.NGO_ORG) {
                val sortedByPriority = remember(visibleRequests) {
                    visibleRequests.sortedByDescending {
                        when (it.priority) {
                            aarambh.apps.resqheat.model.Priority.HIGH -> 3
                            aarambh.apps.resqheat.model.Priority.MEDIUM -> 2
                            aarambh.apps.resqheat.model.Priority.LOW -> 1
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sortedByPriority) { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    showRequestDialog = false
                                    activeRequest = req
                                    showRequestDialog = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = req.title, style = MaterialTheme.typography.bodyMedium)
                            val p = when (req.priority) {
                                aarambh.apps.resqheat.model.Priority.HIGH -> "High"
                                aarambh.apps.resqheat.model.Priority.MEDIUM -> "Medium"
                                aarambh.apps.resqheat.model.Priority.LOW -> "Low"
                            }
                            Text(text = p, style = MaterialTheme.typography.bodySmall)
                        }
                        Divider()
                    }
                }
            }

            // Victim list view - shows own requests with NGO info for BEING_SERVED
            if (role == UserRole.VICTIM && currentUid != null) {
                val myRequests = remember(visibleRequests, currentUid) {
                    visibleRequests.filter { it.createdByUid == currentUid }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(myRequests) { req ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    activeRequest = req
                                    showRequestDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = req.title, style = MaterialTheme.typography.bodyMedium)
                                val statusText = when (req.status) {
                                    RequestStatus.NOT_SERVED -> "Pending"
                                    RequestStatus.BEING_SERVED -> "In Progress"
                                    RequestStatus.SERVED -> "Completed"
                                }
                                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                            }
                            
                            // Show NGO info for BEING_SERVED requests
                            if (req.status == RequestStatus.BEING_SERVED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 4.dp)
                                ) {
                                    if (req.claimedByNgoName != null) {
                                        Text(
                                            text = "NGO: ${req.claimedByNgoName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (req.claimedByNgoPhone != null) {
                                        Text(
                                            text = "Contact: ${req.claimedByNgoPhone}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (req.eta != null) {
                                        Text(
                                            text = "ETA: ${req.eta}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }

    if (showDialog) {
        RequestFormDialog(
            onDismiss = { showDialog = false },
            onSubmit = { request ->
                val lat = selectedLatLng?.latitude
                val lng = selectedLatLng?.longitude
                onSubmitRequest(request, lat, lng)
                showDialog = false
            }
        )
    }

    // Request details dialog (for all users)
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepository() }
    val userRepo = remember { UserRepository() }
    val currentUid = Firebase.auth.currentUser?.uid ?: ""
    
    val req = activeRequest
    if (req != null && showRequestDialog) {
        RequestDetailsDialog(
            request = req,
            role = role,
            onDismiss = { showRequestDialog = false },
            onClaim = { requestId ->
                scope.launch {
                    runCatching {
                        val profile = userRepo.getUserProfile(currentUid)
                        android.util.Log.d("HomeScreen", "Claiming request - Profile: ${profile?.ngoOrgName}, Phone: ${profile?.ngoOrgPhone}")
                        repo.claimRequest(
                            requestId,
                            currentUid,
                            profile?.ngoOrgName,
                            profile?.ngoOrgPhone,
                            null // ETA can be set later if needed
                        )
                        android.util.Log.d("HomeScreen", "Claim request completed for $requestId")
                    }
                    showRequestDialog = false
                }
            },
            onComplete = { requestId, estimatedDaysCovered ->
                scope.launch {
                    runCatching { 
                        repo.completeRequest(requestId, estimatedDaysCovered)
                    }
                    showRequestDialog = false
                }
            }
        )
    }

    // Safe Shelter Details Dialog
    val shelterContext = LocalContext.current
    val shelter = activeShelter
    if (shelter != null && showShelterDialog) {
        AlertDialog(
            onDismissRequest = { showShelterDialog = false },
            title = { Text(text = shelter.name) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (shelter.address != null) {
                        Text(
                            text = "Address: ${shelter.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = "Capacity: ${shelter.availabilityText}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (shelter.contactPhone != null) {
                        Text(
                            text = "Contact: ${shelter.contactPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (shelter.facilities.isNotEmpty()) {
                        Text(
                            text = "Facilities: ${shelter.facilities.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Open Google Maps with directions
                    val uri = Uri.parse("google.navigation:q=${shelter.lat},${shelter.lng}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    try {
                        shelterContext.startActivity(mapIntent)
                    } catch (e: Exception) {
                        // Fallback to web maps if app not installed
                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${shelter.lat},${shelter.lng}")
                        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                        shelterContext.startActivity(webIntent)
                    }
                }) {
                    Text("Get Directions")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showShelterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun RequestDetailsDialog(
    request: Request,
    role: UserRole?,
    onDismiss: () -> Unit,
    onClaim: (String) -> Unit,
    onComplete: (String, Int?) -> Unit
) {
    val userRepo = remember { UserRepository() }
    val currentUid = Firebase.auth.currentUser?.uid
    val uid = currentUid ?: ""
    
    // Fetch victim profile
    var victimProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(request.createdByUid) {
        victimProfile = userRepo.getUserProfile(request.createdByUid)
    }
    
    // For NGOs: check if they can claim or complete
    val canClaim = role == UserRole.NGO_ORG && request.status == RequestStatus.NOT_SERVED
    val canComplete = role == UserRole.NGO_ORG && 
                     request.status != RequestStatus.SERVED && 
                     request.claimedBy == uid
    
    // estimatedDaysCovered input (only for completing)
    var estimatedDaysCovered by remember { mutableStateOf("") }
    var estimatedDaysCoveredError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = request.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Request Type
                Text(
                    text = "Type: ${request.type.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (request.type == RequestType.RESOURCE && request.resourceType != null) {
                    Text(
                        text = "Resource: ${request.resourceType}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Priority
                Text(
                    text = "Priority: ${request.priority.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Status
                val statusText = when (request.status) {
                    RequestStatus.NOT_SERVED -> "Status: Pending"
                    RequestStatus.BEING_SERVED -> "Status: In Progress"
                    RequestStatus.SERVED -> "Status: Completed"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Notes
                if (request.notes != null && request.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${request.notes}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Location
                Text(
                    text = "Location: ${String.format("%.6f", request.lat)}, ${String.format("%.6f", request.lng)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Victim Info
                Text(
                    text = "Victim Information",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val profile = victimProfile
                if (profile != null) {
                    Text(
                        text = "Name: ${profile.victimName ?: profile.displayName ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (profile.address != null) {
                        Text(
                            text = "Village/Address: ${profile.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (profile.victimPhone != null) {
                        Text(
                            text = "Phone: ${profile.victimPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Loading victim information...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // NGO Info (if BEING_SERVED)
                if (request.status == RequestStatus.BEING_SERVED) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "NGO Information",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (request.claimedByNgoName != null) {
                        Text(
                            text = "NGO: ${request.claimedByNgoName}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (request.claimedByNgoPhone != null) {
                        Text(
                            text = "Contact: ${request.claimedByNgoPhone}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (request.eta != null) {
                        Text(
                            text = "ETA: ${request.eta}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Estimated Days Covered input (only when completing)
                if (canComplete) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = estimatedDaysCovered,
                        onValueChange = {
                            estimatedDaysCovered = it
                            estimatedDaysCoveredError = false
                        },
                        label = { Text("Estimated Days Covered (optional)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        isError = estimatedDaysCoveredError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (estimatedDaysCoveredError) {
                        Text(
                            text = "Please enter a valid number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (canClaim) {
                Button(onClick = { onClaim(request.id) }) {
                    Text("Accept Request")
                }
            } else if (canComplete) {
                Button(onClick = {
                    val days = estimatedDaysCovered.toIntOrNull()
                    if (estimatedDaysCovered.isNotBlank() && days == null) {
                        estimatedDaysCoveredError = true
                    } else {
                        onComplete(request.id, days)
                    }
                }) {
                    Text("Mark Completed")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun FirebaseAuthUid(): String? {
    return try {
        com.google.firebase.ktx.Firebase.auth.currentUser?.uid
    } catch (_: Exception) { null }
}


