package aarambh.apps.resqheat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextOverflow
import aarambh.apps.resqheat.R
import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestStatus
import aarambh.apps.resqheat.model.RequestType
import aarambh.apps.resqheat.model.UserRole
import aarambh.apps.resqheat.ui.components.EmptyState
import aarambh.apps.resqheat.ui.components.PriorityBadge
import aarambh.apps.resqheat.ui.components.RequestCard
import aarambh.apps.resqheat.ui.components.StatusChip
import aarambh.apps.resqheat.ui.components.TypeChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import aarambh.apps.resqheat.ui.home.HomeViewModel
import aarambh.apps.resqheat.model.Priority
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.google.maps.android.compose.TileOverlay
import aarambh.apps.resqheat.model.SafeShelter
import aarambh.apps.resqheat.utils.AppConstants
import androidx.compose.material.icons.filled.Settings

@Composable
fun TopBarOverlay(
    role: UserRole?,
    mineOnly: Boolean,
    selectedRequestType: RequestType?,
    requestCount: Int,
    onProfileClick: () -> Unit,
    onMineOnlyChange: (Boolean) -> Unit,
    onRequestTypeFilterChange: (RequestType?) -> Unit,
    onBottomSheetToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 48.dp, end = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ResQHeat",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Request count button
                    Card(
                        modifier = Modifier.clickable(onClick = onBottomSheetToggle),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Requests",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$requestCount",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile / Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filters
            if (role == UserRole.VICTIM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show only my requests",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = mineOnly,
                        onCheckedChange = onMineOnlyChange
                    )
                }
            } else if (role == UserRole.NGO_ORG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRequestType == RequestType.RESCUE,
                        onClick = {
                            onRequestTypeFilterChange(
                                if (selectedRequestType == RequestType.RESCUE) null else RequestType.RESCUE
                            )
                        },
                        label = { Text("Rescue") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRequestType == RequestType.RESOURCE,
                        onClick = {
                            onRequestTypeFilterChange(
                                if (selectedRequestType == RequestType.RESOURCE) null else RequestType.RESOURCE
                            )
                        },
                        label = { Text("Resource") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RequestListBottomSheet(
    visibleRequests: List<Request>,
    role: UserRole?,
    currentUid: String?,
    mineOnly: Boolean,
    viewModel: HomeViewModel,
    onRequestClick: (Request) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect filter states
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    
    // Check if any filters are active
    val hasActiveFilters = selectedPriority != null || selectedStatus != null || sortOrder != "priority"
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with filter button for NGO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            // Filter button for NGO
            if (role == UserRole.NGO_ORG) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .then(
                            if (hasActiveFilters) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                            } else Modifier
                        )
                ) {
                    // Custom filter icon (three horizontal lines)
                    FilterIcon(
                        tint = if (hasActiveFilters) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            role == UserRole.NGO_ORG -> {
                if (visibleRequests.isEmpty()) {
                    EmptyState(
                        title = "No requests found",
                        message = "There are no requests matching your filters",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleRequests) { req ->
                            RequestCard(
                                request = req,
                                onClick = { onRequestClick(req) }
                            )
                        }
                    }
                }
            }
            role == UserRole.VICTIM && currentUid != null -> {
                val myRequests = remember(visibleRequests, currentUid) {
                    visibleRequests.filter { it.createdByUid == currentUid }
                }
                
                if (myRequests.isEmpty()) {
                    EmptyState(
                        title = "No requests yet",
                        message = if (mineOnly) {
                            "You haven't created any requests. Tap the + button to create one."
                        } else {
                            "No requests to display"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(myRequests) { req ->
                            VictimRequestCard(
                                request = req,
                                onClick = { onRequestClick(req) }
                            )
                        }
                    }
                }
            }
            else -> {
                EmptyState(
                    title = "No requests",
                    message = "No requests to display",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Filter Icon using drawable
@Composable
private fun FilterIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.baseline_filter_alt_24),
        contentDescription = "Filter",
        modifier = modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint)
    )
}

@Composable
fun NgoFilterDialog(
    selectedPriority: Priority?,
    selectedStatus: RequestStatus?,
    sortOrder: String,
    onPriorityChange: (Priority?) -> Unit,
    onStatusChange: (RequestStatus?) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter & Sort Requests") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                        // Priority Filter
                        Column {
                            Text(
                                text = "Priority",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Priority.values().forEach { priority ->
                                    FilterChip(
                                        selected = selectedPriority == priority,
                                        onClick = {
                                            onPriorityChange(if (selectedPriority == priority) null else priority)
                                        },
                                        label = { 
                                            Text(
                                                text = priority.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider()
                        
                        // Status Filter
                        Column {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Use Row with smaller font to prevent text wrapping
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RequestStatus.values().forEach { status ->
                                    FilterChip(
                                        selected = selectedStatus == status,
                                        onClick = {
                                            onStatusChange(if (selectedStatus == status) null else status)
                                        },
                                        label = {
                                            Text(
                                                text = when (status) {
                                                    RequestStatus.NOT_SERVED -> "Pending"
                                                    RequestStatus.BEING_SERVED -> "In Progress"
                                                    RequestStatus.SERVED -> "Completed"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                
                HorizontalDivider()
                
                // Sort Order
                Column {
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortOptions = listOf(
                            "priority" to "Priority",
                            "distance" to "Distance",
                            "date" to "Date (Newest First)"
                        )
                        sortOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = sortOrder == value,
                                onClick = { onSortOrderChange(value) },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                onClearFilters()
                onDismiss()
            }) {
                Text("Clear All")
            }
        }
    )
}

@Composable
fun VictimRequestCard(
    request: Request,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = request.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityBadge(priority = request.priority)
                TypeChip(type = request.type)
            }
            
            // Show NGO info for BEING_SERVED requests
            if (request.status == RequestStatus.BEING_SERVED) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NGO Information",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (request.claimedByNgoName != null) {
                        Text(
                            text = request.claimedByNgoName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (request.claimedByNgoPhone != null) {
                        Text(
                            text = request.claimedByNgoPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (request.eta != null) {
                        Text(
                            text = "ETA: ${request.eta}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapContent(
    visibleRequests: List<Request>,
    shelters: List<SafeShelter>,
    selectedLatLng: LatLng?,
    role: UserRole?,
    onRequestClick: (Request) -> Unit,
    onShelterClick: (SafeShelter) -> Unit,
    modifier: Modifier = Modifier
) {
    // This will be populated by the parent GoogleMap composable
    // This is just a placeholder for organization
}

