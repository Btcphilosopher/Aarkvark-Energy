package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Customer
import com.example.data.SmartMeter
import com.example.ui.UtilityViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MetersScreen(
    viewModel: UtilityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.meterSearchQuery.collectAsState()
    val meters by viewModel.searchedMeters.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMeterForDetails by remember { mutableStateOf<SmartMeter?>(null) }
    var showEditDialog by remember { mutableStateOf<SmartMeter?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMART GRID HARDWARE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Meter Directory",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                    modifier = Modifier.testTag("add_meter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meter",
                        tint = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Register Meter", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.meterSearchQuery.value = it },
                placeholder = { Text("Search by Meter Serial ID, location...", color = DarkTextSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ElectricTeal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.meterSearchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_meters_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricTeal,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of Meters
            if (meters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ElectricalServices,
                            contentDescription = "Empty",
                            tint = DarkSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Smart Meters Found",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Register a new smart meter above to start grid monitoring.",
                            style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(meters) { meter ->
                        val associatedCustName = customers.firstOrNull { it.id == meter.customerId }?.name ?: "Unassigned Customer"

                        MeterItemCard(
                            meter = meter,
                            customerName = associatedCustName,
                            onClick = { selectedMeterForDetails = meter },
                            onEdit = { showEditDialog = meter },
                            onDelete = {
                                viewModel.deleteMeter(meter)
                                Toast.makeText(context, "Meter deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Floating Register Button on Mobile
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ElectricAmber,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_meter_fab")
        ) {
            Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Add Meter", tint = DarkBackground)
        }
    }

    // REGISTER NEW METER DIALOG
    if (showAddDialog) {
        var meterSerial by remember { mutableStateOf("") }
        var meterAddress by remember { mutableStateOf("") }
        var initialReading by remember { mutableStateOf("0.0") }
        var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
        var status by remember { mutableStateOf("ONLINE") }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register Smart Meter", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = meterSerial,
                        onValueChange = { meterSerial = it },
                        label = { Text("Meter ID (e.g. MTR-2244-X)", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = meterAddress,
                        onValueChange = { meterAddress = it },
                        label = { Text("Installation Address", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialReading,
                        onValueChange = { initialReading = it },
                        label = { Text("Initial Cumulative Reading (kWh)", color = DarkTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Customer Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Assign Customer (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Customer", color = DarkTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricTeal,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned", color = Color.White) },
                                onClick = {
                                    selectedCustomer = null
                                    isDropdownExpanded = false
                                }
                            )
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name, color = Color.White) },
                                    onClick = {
                                        selectedCustomer = customer
                                        isDropdownExpanded = false
                                        meterAddress = customer.address // auto-fill address!
                                    }
                                )
                            }
                        }
                    }

                    // Status selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status: ", color = DarkTextSecondary)
                        listOf("ONLINE", "OFFLINE", "MAINTENANCE").forEach { s ->
                            val isSelected = status == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricTeal else DarkSurfaceVariant)
                                    .clickable { status = s }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DarkBackground else Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (meterSerial.isNotEmpty() && meterAddress.isNotEmpty()) {
                            viewModel.addMeter(
                                meterId = meterSerial.uppercase(),
                                customerId = selectedCustomer?.id,
                                address = meterAddress,
                                initialReading = initialReading.toDoubleOrNull() ?: 0.0,
                                status = status
                            )
                            Toast.makeText(context, "Meter registered successfully", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Please populate required fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Register", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // EDIT METER DIALOG
    showEditDialog?.let { meterToEdit ->
        var meterSerial by remember { mutableStateOf(meterToEdit.meterId) }
        var meterAddress by remember { mutableStateOf(meterToEdit.address) }
        var status by remember { mutableStateOf(meterToEdit.status) }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        var selectedCustomer by remember {
            mutableStateOf(customers.firstOrNull { it.id == meterToEdit.customerId })
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Configure Smart Meter", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = meterSerial,
                        onValueChange = { meterSerial = it },
                        label = { Text("Meter ID", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = meterAddress,
                        onValueChange = { meterAddress = it },
                        label = { Text("Installation Address", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Customer Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Unassigned Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer Association", color = DarkTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricTeal,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned", color = Color.White) },
                                onClick = {
                                    selectedCustomer = null
                                    isDropdownExpanded = false
                                }
                            )
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name, color = Color.White) },
                                    onClick = {
                                        selectedCustomer = customer
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status: ", color = DarkTextSecondary)
                        listOf("ONLINE", "OFFLINE", "MAINTENANCE").forEach { s ->
                            val isSelected = status == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricTeal else DarkSurfaceVariant)
                                    .clickable { status = s }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DarkBackground else Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (meterSerial.isNotEmpty() && meterAddress.isNotEmpty()) {
                            viewModel.updateMeter(
                                meterToEdit.copy(
                                    meterId = meterSerial.uppercase(),
                                    customerId = selectedCustomer?.id,
                                    address = meterAddress,
                                    status = status
                                )
                            )
                            Toast.makeText(context, "Meter settings synchronized", Toast.LENGTH_SHORT).show()
                            showEditDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // METER INSPECTION & TELEMETRY INJECTION DETAILS DIALOG
    selectedMeterForDetails?.let { meter ->
        val associatedCustomer = customers.firstOrNull { it.id == meter.customerId }
        val meterReadingsFlow = remember(meter.id) { viewModel.getReadingsForMeter(meter.id) }
        val readingsHistory by meterReadingsFlow.collectAsState(initial = emptyList())

        var showTelemetryForm by remember { mutableStateOf(false) }
        var inputReadingVal by remember { mutableStateOf((meter.currentReading + 15.0).toString()) }
        var inputDemandVal by remember { mutableStateOf("1.8") }

        AlertDialog(
            onDismissRequest = { selectedMeterForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagnostics: ${meter.meterId}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = { selectedMeterForDetails = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            },
            containerColor = DarkSurface,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Stats Block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "DETAILS & ASSOCIATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricTeal))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Customer: ${associatedCustomer?.name ?: "UNASSIGNED"}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = "Address: ${meter.address}", color = DarkTextSecondary, fontSize = 12.sp)
                            Text(text = "Current Sync Reading: ${meter.currentReading} kWh", color = ElectricAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Status: ${meter.status}", color = if (meter.status == "ONLINE") MeterOnline else MeterOffline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Telemetry Injector Toggle
                    Button(
                        onClick = { showTelemetryForm = !showTelemetryForm },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showTelemetryForm) MeterOffline else ElectricAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = if (showTelemetryForm) Icons.Default.Cancel else Icons.Default.ElectricalServices, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (showTelemetryForm) "Cancel Injection" else "Inject Smart Meter Telemetry", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(visible = showTelemetryForm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SIMULATE METER SYNC",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricAmber)
                                )
                                OutlinedTextField(
                                    value = inputReadingVal,
                                    onValueChange = { inputReadingVal = it },
                                    label = { Text("New Cumulative Reading (kWh)", color = DarkTextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = inputDemandVal,
                                    onValueChange = { inputDemandVal = it },
                                    label = { Text("Instant Peak Demand (kW)", color = DarkTextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val newRead = inputReadingVal.toDoubleOrNull() ?: 0.0
                                        val newDemand = inputDemandVal.toDoubleOrNull() ?: 1.0
                                        if (newRead > meter.currentReading) {
                                            viewModel.addMeterReading(meter.id, newRead, newDemand)
                                            Toast.makeText(context, "Telemetry signal broadcast complete!", Toast.LENGTH_SHORT).show()
                                            showTelemetryForm = false
                                            // update parent reference
                                            selectedMeterForDetails = meter.copy(currentReading = newRead, previousReading = meter.currentReading)
                                        } else {
                                            Toast.makeText(context, "New reading must exceed current read ($meter.currentReading)", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Broadcast Telemetry Packet", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Historical readings list
                    Text(
                        text = "SYNCED READING PACKETS HISTORY",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DarkTextSecondary)
                    )

                    if (readingsHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Reading packets received yet.", color = DarkTextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(readingsHistory) { r ->
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(r.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Reading: ${r.reading} kWh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Active Load: ${r.demandKw} kW | Peak: ${if (r.isPeak) "YES" else "NO"}", color = DarkTextSecondary, fontSize = 11.sp)
                                    }
                                    Text(text = dateStr, color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedMeterForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Done", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun MeterItemCard(
    meter: SmartMeter,
    customerName: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsInputAntenna,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = meter.meterId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = customerName,
                            style = MaterialTheme.typography.bodySmall.copy(color = ElectricAmber)
                        )
                    }
                }

                // Status Badge & Action Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (meter.status) {
                                    "ONLINE" -> MeterOnline.copy(alpha = 0.15f)
                                    "OFFLINE" -> MeterOffline.copy(alpha = 0.15f)
                                    else -> MeterMaintenance.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = meter.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (meter.status) {
                                    "ONLINE" -> MeterOnline
                                    "OFFLINE" -> MeterOffline
                                    else -> MeterMaintenance
                                }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Configure Meter", color = Color.White) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = ElectricTeal) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Meter", color = MeterOffline) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MeterOffline) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = DarkSurfaceVariant, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CURRENT READING", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = String.format(Locale.US, "%,.1f kWh", meter.currentReading),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "LOCATION", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = if (meter.address.length > 25) meter.address.take(22) + "..." else meter.address,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }
            }
        }
    }
}
