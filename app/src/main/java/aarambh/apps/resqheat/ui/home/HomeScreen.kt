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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import aarambh.apps.resqheat.utils.NotificationHelper
import aarambh.apps.resqheat.utils.AppConstants
import aarambh.apps.resqheat.model.RequestStatus
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import aarambh.apps.resqheat.ui.components.RequestCard
import aarambh.apps.resqheat.ui.components.PriorityBadge
import aarambh.apps.resqheat.ui.components.StatusChip
import aarambh.apps.resqheat.ui.components.TypeChip
import aarambh.apps.resqheat.ui.components.EmptyState
import aarambh.apps.resqheat.ui.components.LoadingState
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
    private val snippetText: String?,
    val status: RequestStatus
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = titleText
    override fun getSnippet(): String? = snippetText
    override fun getZIndex(): Float? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddRequest: () -> Unit,
    onSubmitRequest: (Request, Double?, Double?) -> Unit,
    role: UserRole? = null,
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Get current user ID
    val currentUid = Firebase.auth.currentUser?.uid
    
    // Collect state from ViewModel
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val shelters by viewModel.shelters.collectAsStateWithLifecycle()
    val selectedRequestType by viewModel.selectedRequestType.collectAsStateWithLifecycle()
    val mineOnly by viewModel.mineOnly.collectAsStateWithLifecycle()
    val createRequestState by viewModel.createRequestState.collectAsStateWithLifecycle()
    val requestActionState by viewModel.requestActionState.collectAsStateWithLifecycle()
    
    // NGO filters - collect these so they trigger recomposition
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    
    // Local UI state
    var showDialog by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var activeRequest by remember { mutableStateOf<Request?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var activeShelter by remember { mutableStateOf<SafeShelter?>(null) }
    var showShelterDialog by remember { mutableStateOf(false) }
    var clickedRequestId by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Store last location for request creation
    val lastLat = remember { mutableStateOf<Double?>(null) }
    val lastLng = remember { mutableStateOf<Double?>(null) }
    
    // Start listening when role is available
    LaunchedEffect(role) {
        if (role != null) {
            viewModel.startListening(context, role)
        }
    }
    
    // Snackbar host for error messages
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Handle create request state
    LaunchedEffect(createRequestState) {
        when (val state = createRequestState) {
            is aarambh.apps.resqheat.ui.common.UiState.Success -> {
                showDialog = false
                snackbarHostState.showSnackbar("Request submitted successfully")
                viewModel.clearCreateRequestState()
            }
            is aarambh.apps.resqheat.ui.common.UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearCreateRequestState()
            }
            else -> {}
        }
    }
    
    // Handle request action state
    LaunchedEffect(requestActionState) {
        when (val state = requestActionState) {
            is aarambh.apps.resqheat.ui.common.UiState.Success -> {
                showRequestDialog = false
                snackbarHostState.showSnackbar("Action completed successfully")
                viewModel.clearRequestActionState()
            }
            is aarambh.apps.resqheat.ui.common.UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearRequestActionState()
            }
            else -> {}
        }
    }

    // Get filtered requests from ViewModel
    // Include NGO filters (selectedPriority, selectedStatus) in dependencies so it recomputes when filters change
    val visibleRequests = remember(requests, mineOnly, selectedRequestType, selectedPriority, selectedStatus, role) {
        viewModel.getFilteredRequests()
    }
    
    // Get sorted requests for NGO (with user location for distance sorting)
    val sortedRequests = remember(visibleRequests, role, viewModel.sortOrder.value, viewModel.userLocation.value) {
        if (role == UserRole.NGO_ORG) {
            viewModel.getSortedRequests(visibleRequests, viewModel.userLocation.value)
        } else {
            visibleRequests
        }
    }
    
    // Calculate 1/4 of screen height for bottom sheet peek height
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val quarterScreenHeight = screenHeight / 4
    
    // Bottom sheet state - set to PartiallyExpanded initially and prevent hiding completely
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true  // Prevent hiding completely
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    
    // Coroutine scope for bottom sheet operations
    val scope = rememberCoroutineScope()
    
    // Callback to handle cluster item clicks - captured for use in MapEffect
    val onClusterItemClick: (String) -> Unit = remember {
        { requestId: String ->
            clickedRequestId = requestId
        }
    }
    
    // Remember previous ClusterManager to clean it up properly
    val previousClusterManagerRef = remember { 
        androidx.compose.runtime.mutableStateOf<ClusterManager<RequestClusterItem>?>(null) 
    }
    
    // Handle clicked request from map marker
    LaunchedEffect(clickedRequestId, visibleRequests) {
        clickedRequestId?.let { requestId ->
            val req = visibleRequests.firstOrNull { request -> request.id == requestId }
            if (req != null) {
                activeRequest = req
                showRequestDialog = true
                // Expand bottom sheet
                scope.launch {
                    bottomSheetState.expand()
                }
                clickedRequestId = null // Reset
            }
        }
    }
    
    // Persisted camera state
    var savedLat by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_MAP_LAT) }
    var savedLng by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_MAP_LNG) }
    var savedZoom by rememberSaveable { mutableStateOf(AppConstants.INITIAL_MAP_ZOOM) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(savedLat, savedLng), savedZoom)
            }

            // Try to center on current location only on first run when no manual position saved
            LaunchedEffect(context) {
                try {
            if (savedZoom <= AppConstants.INITIAL_MAP_ZOOM + 0.01f) {
                        val fused = LocationServices.getFusedLocationProviderClient(context)
                        val token = CancellationTokenSource()
                        val current = fused.getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY, token.token).await()
                        if (current != null) {
                            val latLng = LatLng(current.latitude, current.longitude)
                    lastLat.value = current.latitude
                    lastLng.value = current.longitude
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, AppConstants.DEFAULT_MAP_ZOOM)
                } else {
                    // Try last known location
                    val last = fused.lastLocation.await()
                    if (last != null) {
                        lastLat.value = last.latitude
                        lastLng.value = last.longitude
                    }
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

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            RequestListBottomSheet(
                visibleRequests = if (role == UserRole.NGO_ORG) sortedRequests else visibleRequests,
                role = role,
                currentUid = currentUid,
                mineOnly = mineOnly,
                viewModel = viewModel,
                onRequestClick = { request ->
                    activeRequest = request
                    showRequestDialog = true
                },
                onFilterClick = { showFilterDialog = true }
            )
        },
        sheetPeekHeight = quarterScreenHeight,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full screen map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = canShowMyLocation),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false, // Disable default button, using custom one
                    zoomGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    rotationGesturesEnabled = true
                ),
                onMapLongClick = if (role != UserRole.NGO_ORG) {
                    { latLng ->
                    selectedLatLng = latLng
                    }
                } else {
                    { }
                }
            ) {
                // Selected pin (only for victims)
                if (role != UserRole.NGO_ORG) {
                selectedLatLng?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = "Selected location"
                    )
                    }
                }

                // Clustered markers for visible requests
                // Key based on visible request IDs - when filters change, this changes and recreates ClusterManager
                val requestsKey = remember(visibleRequests) {
                    visibleRequests.map { it.id }.sorted().joinToString(",")
                }
                
                MapEffect(requestsKey, role, onClusterItemClick) { map ->
                    // Clear listeners first to detach previous ClusterManager
                    map.setOnCameraIdleListener(null)
                    map.setOnMarkerClickListener(null)
                    
                    // IMPORTANT: Clear all markers to prevent duplication
                    // This removes markers from the previous ClusterManager
                    // Compose markers (shelters, selected location) will be automatically re-rendered
                    map.clear()
                    
                    // Create new ClusterManager
                    val clusterManager = ClusterManager<RequestClusterItem>(context, map)
                    previousClusterManagerRef.value = clusterManager
                    
                    // Custom renderer to set marker colors and translucency based on status
                    val renderer = object : com.google.maps.android.clustering.view.DefaultClusterRenderer<RequestClusterItem>(context, map, clusterManager) {
                        override fun onBeforeClusterItemRendered(item: RequestClusterItem, markerOptions: com.google.android.gms.maps.model.MarkerOptions) {
                            super.onBeforeClusterItemRendered(item, markerOptions)
                            if (item.status == RequestStatus.SERVED) {
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                markerOptions.alpha(0.5f)
                            } else {
                                markerOptions.alpha(1.0f)
                            }
                        }
                    }
                    clusterManager.renderer = renderer
                    
                    clusterManager.setOnClusterItemClickListener { item ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onClusterItemClick(item.requestId)
                        }
                        true
                    }
                    
                    // Add items from visibleRequests
                    clusterManager.clearItems()
                    val items = visibleRequests.map { r ->
                        RequestClusterItem(r.id, LatLng(r.lat, r.lng), r.title, r.notes, r.status)
                    }
                    clusterManager.addItems(items)
                    clusterManager.cluster()
                    
                    // Set listeners after clustering
                    map.setOnCameraIdleListener(clusterManager)
                    map.setOnMarkerClickListener(clusterManager)
                }

                // Safe shelter markers (blue icons)
                android.util.Log.d("HomeScreen", "Rendering ${shelters.size} shelters on map")
                shelters.forEach { shelter ->
                    // Validate coordinates using threshold to avoid invalid (0.0, 0.0) locations
                    val hasValidCoordinates = kotlin.math.abs(shelter.lat) >= AppConstants.INVALID_COORDINATE_THRESHOLD &&
                                             kotlin.math.abs(shelter.lng) >= AppConstants.INVALID_COORDINATE_THRESHOLD
                    if (hasValidCoordinates && shelter.isActive) {
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
                        // Validate coordinates using threshold
                        val hasValidCoordinates = kotlin.math.abs(r.lat) >= AppConstants.INVALID_COORDINATE_THRESHOLD &&
                                                 kotlin.math.abs(r.lng) >= AppConstants.INVALID_COORDINATE_THRESHOLD
                        if (!hasValidCoordinates) return@mapNotNull null
                        val latLng = LatLng(r.lat, r.lng)
                        val weight = when (r.priority) {
                            aarambh.apps.resqheat.model.Priority.HIGH -> AppConstants.HIGH_PRIORITY_WEIGHT
                            aarambh.apps.resqheat.model.Priority.MEDIUM -> AppConstants.MEDIUM_PRIORITY_WEIGHT
                            aarambh.apps.resqheat.model.Priority.LOW -> AppConstants.LOW_PRIORITY_WEIGHT
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

            // Top bar overlay
            TopBarOverlay(
                role = role,
                mineOnly = mineOnly,
                selectedRequestType = selectedRequestType,
                requestCount = visibleRequests.size,
                onProfileClick = onProfileClick,
                onMineOnlyChange = { viewModel.setMineOnlyFilter(it) },
                onRequestTypeFilterChange = { viewModel.setRequestTypeFilter(it) },
                onBottomSheetToggle = {
                    scope.launch {
                        // Toggle between expanded and partially expanded (1/4 screen)
                        if (bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                            bottomSheetState.expand()
                        } else {
                            bottomSheetState.partialExpand()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
            
                    // Clear pin button overlay (only for victims)
                    if (role != UserRole.NGO_ORG && selectedLatLng != null) {
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 180.dp, start = 16.dp, end = 16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { selectedLatLng = null },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.padding(start = 8.dp))
                                Text("Clear selected location")
                            }
                        }
                    }
            
            // Location button (GPS) - Left side, above bottom sheet
            if (canShowMyLocation) {
                FloatingActionButton(
                    onClick = {
                        // Center map on current location
                        scope.launch {
                            try {
                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                val token = CancellationTokenSource()
                                val current = fused.getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY, token.token).await()
                                if (current != null) {
                                    val latLng = LatLng(current.latitude, current.longitude)
                                    lastLat.value = current.latitude
                                    lastLng.value = current.longitude
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, AppConstants.DEFAULT_MAP_ZOOM)
                                } else {
                                    // Try last known location
                                    val last = fused.lastLocation.await()
                                    if (last != null) {
                                        val latLng = LatLng(last.latitude, last.longitude)
                                        lastLat.value = last.latitude
                                        lastLng.value = last.longitude
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, AppConstants.DEFAULT_MAP_ZOOM)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Failed to get location", e)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = quarterScreenHeight + 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "My Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Floating Action Button (Add Request) - Right side, above bottom sheet
            val canCreate = role != UserRole.NGO_ORG
            if (canCreate) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = quarterScreenHeight + 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Request",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    // Dialogs (outside scaffold but inside composable)
    if (showDialog) {
        RequestFormDialog(
            onDismiss = { showDialog = false },
            onSubmit = { request ->
                val lat = selectedLatLng?.latitude
                val lng = selectedLatLng?.longitude
                viewModel.createRequest(request, lat, lng, lastLat.value, lastLng.value)
            }
        )
    }

    // Request details dialog (for all users)
    val req = activeRequest
    if (req != null && showRequestDialog) {
        RequestDetailsDialog(
            request = req,
            role = role,
            onDismiss = { showRequestDialog = false },
            onClaim = { requestId, eta ->
                viewModel.claimRequest(requestId, eta)
            },
            onComplete = { requestId, estimatedDaysCovered ->
                viewModel.completeRequest(requestId, estimatedDaysCovered)
            }
        )
    }
    
    // Filter dialog for NGO (outside bottom sheet)
    if (showFilterDialog && role == UserRole.NGO_ORG) {
        val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
        val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
        val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
        
        NgoFilterDialog(
            selectedPriority = selectedPriority,
            selectedStatus = selectedStatus,
            sortOrder = sortOrder,
            onPriorityChange = { viewModel.setPriorityFilter(it) },
            onStatusChange = { viewModel.setStatusFilter(it) },
            onSortOrderChange = { viewModel.setSortOrder(it) },
            onClearFilters = { viewModel.clearNgoFilters() },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Safe Shelter Details Dialog
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
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        // Fallback to web maps if app not installed
                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${shelter.lat},${shelter.lng}")
                        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                        context.startActivity(webIntent)
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
    onClaim: (String, String) -> Unit,
    onComplete: (String, Int?) -> Unit
) {
    val userRepo = remember { UserRepository() }
    val currentUid = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    
    // Fetch victim profile
    var victimProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(request.createdByUid) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        victimProfile = userRepo.getUserProfile(request.createdByUid)
        }
    }
    
    // For NGO role: Calculate distance from device location to request
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var isLoadingDistance by remember { mutableStateOf(false) }
    
    LaunchedEffect(request, role) {
        if (role == UserRole.NGO_ORG) {
            isLoadingDistance = true
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val token = CancellationTokenSource()
                val current = fused.getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY, token.token).await()
                if (current != null) {
                    // Calculate distance using haversine formula
                    val lat1 = Math.toRadians(current.latitude)
                    val lat2 = Math.toRadians(request.lat)
                    val deltaLat = Math.toRadians(request.lat - current.latitude)
                    val deltaLng = Math.toRadians(request.lng - current.longitude)
                    
                    val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                            Math.cos(lat1) * Math.cos(lat2) *
                            Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
                    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                    val distance = AppConstants.EARTH_RADIUS_KM * c
                    
                    distanceKm = distance
                } else {
                    // Try last known location
                    val last = fused.lastLocation.await()
                    if (last != null) {
                        val lat1 = Math.toRadians(last.latitude)
                        val lat2 = Math.toRadians(request.lat)
                        val deltaLat = Math.toRadians(request.lat - last.latitude)
                        val deltaLng = Math.toRadians(request.lng - last.longitude)
                        
                        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                                Math.cos(lat1) * Math.cos(lat2) *
                                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
                        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                        val distance = AppConstants.EARTH_RADIUS_KM * c
                        
                        distanceKm = distance
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RequestDetailsDialog", "Failed to get location for distance", e)
            } finally {
                isLoadingDistance = false
            }
        }
    }
    
    // For NGOs: check if they can claim or complete
    val canClaim = role == UserRole.NGO_ORG && request.status == RequestStatus.NOT_SERVED
    val canComplete = role == UserRole.NGO_ORG && 
                     request.status != RequestStatus.SERVED && 
                     request.claimedBy == currentUid
    
    // ETA input (for claiming - both RESCUE and RESOURCE)
    var etaHours by remember { mutableStateOf("") }
    var etaError by remember { mutableStateOf<String?>(null) }
    
    // estimatedDaysCovered input (only for completing RESOURCE requests)
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
                
                // Distance (for NGO role only)
                if (role == UserRole.NGO_ORG) {
                    when {
                        isLoadingDistance -> {
                            Text(
                                text = "Distance: Calculating...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        distanceKm != null -> {
                            val distanceText = if (distanceKm!! < 1.0) {
                                String.format("%.0f m", distanceKm!! * 1000)
                            } else {
                                String.format("%.1f km", distanceKm!!)
                            }
                            Text(
                                text = "Distance: $distanceText",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> {
                            Text(
                                text = "Distance: Unable to calculate",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // ETA input (for claiming - both RESCUE and RESOURCE)
                if (canClaim) {
                    OutlinedTextField(
                        value = etaHours,
                        onValueChange = {
                            etaHours = it
                            // Clear error when user starts typing
                            if (etaError != null) {
                                val hours = it.toIntOrNull()
                                if (it.isNotBlank() && (hours == null || hours <= 0)) {
                                    etaError = "Please enter a valid number of hours (greater than 0)"
                                } else {
                                    etaError = null
                                }
                            }
                        },
                        label = { Text("ETA (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = etaError != null,
                        supportingText = etaError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                
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
                
                // Estimated Days Covered input (only when completing RESOURCE requests)
                if (canComplete && request.type == RequestType.RESOURCE) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            // Validate ETA
                            val hours = etaHours.toIntOrNull()
                            if (etaHours.isBlank()) {
                                etaError = "Please enter ETA in hours"
                            } else if (hours == null || hours <= 0) {
                                etaError = "Please enter a valid number of hours (greater than 0)"
                            } else {
                                etaError = null
                                val etaText = "${hours} hour${if (hours != 1) "s" else ""}"
                                onClaim(request.id, etaText)
                            }
                        },
                        enabled = etaError == null && etaHours.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Text("Accept Request")
                    }
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


