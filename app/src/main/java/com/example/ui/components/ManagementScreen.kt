package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedReport
import com.example.data.Tariff
import com.example.ui.AlertSeverity
import com.example.ui.SystemAlert
import com.example.ui.UtilityViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManagementScreen(
    viewModel: UtilityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tariffs by viewModel.tariffs.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val alerts by viewModel.activeAlerts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val meters by viewModel.meters.collectAsState()
    val bills by viewModel.bills.collectAsState()

    var activeSubTab by remember { mutableStateOf("TARIFFS") }

    // Settings fields
    var companyName by remember { mutableStateOf("Aardvark Power Grid Ltd") }
    var companyAddress by remember { mutableStateOf("702 Spark Towers, Seattle WA") }
    var billingContactEmail by remember { mutableStateOf("billing@aardvarkenergy.com") }
    var standardVatRate by remember { mutableStateOf("5") }

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
            // Header
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "UTILITY GRID CONFIGURATOR",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ElectricTeal,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "System Administration",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sub tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("TARIFFS", "REPORTS & ALERTS", "SETTINGS").forEach { tab ->
                    val isSel = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) ElectricTeal else Color.Transparent)
                            .clickable { activeSubTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) DarkBackground else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Switcher
            when (activeSubTab) {
                "TARIFFS" -> TariffsSubTab(
                    tariffs = tariffs,
                    onAddTariff = { name, desc, rate, peak, offPeak, standing, vat, disc ->
                        viewModel.addTariff(name, desc, rate, peak, offPeak, standing, vat, disc)
                    },
                    onDeleteTariff = { viewModel.deleteTariff(it) }
                )

                "REPORTS & ALERTS" -> ReportsAlertsSubTab(
                    reports = reports,
                    alerts = alerts,
                    onCompileReport = { title, type, desc ->
                        viewModel.generateReport(title, type, desc)
                    }
                )

                "SETTINGS" -> SettingsSubTab(
                    companyName = companyName,
                    companyAddress = companyAddress,
                    contactEmail = billingContactEmail,
                    onCompanyUpdate = { name, addr, email ->
                        companyName = name
                        companyAddress = addr
                        billingContactEmail = email
                        Toast.makeText(context, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    vatRate = standardVatRate,
                    onVatUpdate = { standardVatRate = it },
                    customersCount = customers.size,
                    metersCount = meters.size,
                    billsCount = bills.size,
                    totalLoad = meters.sumOf { it.currentReading }
                )
            }
        }
    }
}

// ---------------------- TARIFFS SUB TAB ----------------------
@Composable
fun TariffsSubTab(
    tariffs: List<Tariff>,
    onAddTariff: (String, String, Double, Double, Double, Double, Double, Double) -> Unit,
    onDeleteTariff: (Tariff) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UTILITY TARIFF PLANS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = DarkBackground)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Tariff", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(tariffs) { tariff ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tariff.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = tariff.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                                )
                            }
                            IconButton(onClick = {
                                onDeleteTariff(tariff)
                                Toast.makeText(context, "Tariff removed", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Tariff", tint = MeterOffline)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = DarkSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Rates Details Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "STANDARD RATE", color = Color.Gray, fontSize = 10.sp)
                                Text(text = "$${tariff.ratePerKwh}/kWh", color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text(text = "PEAK RATE", color = Color.Gray, fontSize = 10.sp)
                                Text(text = "$${tariff.peakRatePerKwh}/kWh", color = ElectricAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text(text = "OFF-PEAK", color = Color.Gray, fontSize = 10.sp)
                                Text(text = "$${tariff.offPeakRatePerKwh}/kWh", color = MeterOnline, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "DAILY STANDING CHARGE", color = Color.Gray, fontSize = 10.sp)
                                Text(text = "$${tariff.standingCharge}/day", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "VAT RATE | TAX", color = Color.Gray, fontSize = 10.sp)
                                Text(text = "${(tariff.vatRate * 100).toInt()}% VAT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var stdRate by remember { mutableStateOf("0.15") }
        var peakRate by remember { mutableStateOf("0.22") }
        var offPeakRate by remember { mutableStateOf("0.08") }
        var standingCharge by remember { mutableStateOf("0.45") }
        var vatPercent by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Configure Custom Tariff Plan", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tariff Name", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Short Description", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stdRate,
                            onValueChange = { stdRate = it },
                            label = { Text("Std Rate ($)", color = DarkTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.0f)
                        )
                        OutlinedTextField(
                            value = peakRate,
                            onValueChange = { peakRate = it },
                            label = { Text("Peak Rate ($)", color = DarkTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = offPeakRate,
                            onValueChange = { offPeakRate = it },
                            label = { Text("Off-Peak ($)", color = DarkTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.0f)
                        )
                        OutlinedTextField(
                            value = standingCharge,
                            onValueChange = { standingCharge = it },
                            label = { Text("Standing ($)", color = DarkTextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                    OutlinedTextField(
                        value = vatPercent,
                        onValueChange = { vatPercent = it },
                        label = { Text("VAT Percent (e.g. 5 for 5%)", color = DarkTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            onAddTariff(
                                name,
                                desc,
                                stdRate.toDoubleOrNull() ?: 0.15,
                                peakRate.toDoubleOrNull() ?: 0.22,
                                offPeakRate.toDoubleOrNull() ?: 0.08,
                                standingCharge.toDoubleOrNull() ?: 0.45,
                                (vatPercent.toDoubleOrNull() ?: 5.0) / 100.0,
                                0.0
                            )
                            Toast.makeText(context, "Custom Tariff added to system directory", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Register Tariff", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}


// ---------------------- REPORTS & ALERTS SUB TAB ----------------------
@Composable
fun ReportsAlertsSubTab(
    reports: List<SavedReport>,
    alerts: List<SystemAlert>,
    onCompileReport: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var isCompiling by remember { mutableStateOf(false) }

    var selectedReportTitle by remember { mutableStateOf("Annual Energy Grid Usage Analytics") }
    var selectedReportType by remember { mutableStateOf("ENERGY_USAGE") }
    var selectedFormat by remember { mutableStateOf("PDF Statement") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core System Alerts
        Text(
            text = "LIVE TELEMETRY FAULT & ALERTS INDEX",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        if (alerts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MeterOnline)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Grid operational logs clear. Zero hardware faults.", color = DarkTextSecondary)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.forEach { alert ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (alert.severity == AlertSeverity.CRITICAL) MeterOffline.copy(alpha = 0.08f) else MeterMaintenance.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (alert.severity == AlertSeverity.CRITICAL) MeterOffline.copy(alpha = 0.4f) else MeterMaintenance.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alert.severity == AlertSeverity.CRITICAL) Icons.Default.Error else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (alert.severity == AlertSeverity.CRITICAL) MeterOffline else MeterMaintenance
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert.severity == AlertSeverity.CRITICAL) MeterOffline else MeterMaintenance
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = alert.message, style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        }
                    }
                }
            }
        }

        Divider(color = DarkSurfaceVariant)

        // Compile Reports Hub
        Text(
            text = "EXPORT COMPLIANCE UTILITY REPORTS",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Configure Audit Export", color = ElectricTeal, fontWeight = FontWeight.Bold)

                // Report type selector buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ENERGY_USAGE", "REVENUE", "CUSTOMER_SUMMARY").forEach { t ->
                        val isSel = selectedReportType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) ElectricTeal else DarkSurfaceVariant)
                                .clickable {
                                    selectedReportType = t
                                    selectedReportTitle = when (t) {
                                        "ENERGY_USAGE" -> "Grid Load Load & Usage Profile Audit"
                                        "REVENUE" -> "Corporate Billing & Ledger Statements"
                                        else -> "Active Customer Account Directory"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.replace("_", " "),
                                color = if (isSel) DarkBackground else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // File format radio button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Export Format: ", color = DarkTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("PDF Statement", "CSV Spreadsheet").forEach { fmt ->
                        val isSel = selectedFormat == fmt
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedFormat = fmt }
                                .padding(horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedFormat = fmt },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricTeal)
                            )
                            Text(text = fmt, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        val parameterSummary = "Generated Audit: $selectedReportTitle | format: $selectedFormat | timestamp: ${System.currentTimeMillis()}"
                        onCompileReport(selectedReportTitle, selectedReportType, parameterSummary)
                        Toast.makeText(context, "Report compiled in database successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compile & Export Audit Report", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Saved Reports index
        Text(text = "COMPILED REPORT METADATA INDEX", style = MaterialTheme.typography.labelMedium.copy(color = DarkTextSecondary))

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No compiled reports saved yet.", color = DarkTextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { r ->
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
                            Text(text = r.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = r.parameters, color = DarkTextSecondary, fontSize = 10.sp)
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, "Downloaded simulated audit successfully!\nSaved as /downloads/${r.fileUrl}", Toast.LENGTH_LONG).show()
                        }) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Download Report", tint = ElectricTeal)
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- SETTINGS SUB TAB ----------------------
@Composable
fun SettingsSubTab(
    companyName: String,
    companyAddress: String,
    contactEmail: String,
    vatRate: String,
    customersCount: Int,
    metersCount: Int,
    billsCount: Int,
    totalLoad: Double,
    onCompanyUpdate: (String, String, String) -> Unit,
    onVatUpdate: (String) -> Unit
) {
    var editName by remember { mutableStateOf(companyName) }
    var editAddress by remember { mutableStateOf(companyAddress) }
    var editEmail by remember { mutableStateOf(contactEmail) }
    var editVat by remember { mutableStateOf(vatRate) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Company Details Config
        Text(
            text = "UTILITY REGULATORY PROFILE",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Company Legal Title", color = DarkTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = { Text("Corporate HQ Address", color = DarkTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Operations / Support Email", color = DarkTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editVat,
                    onValueChange = {
                        editVat = it
                        onVatUpdate(it)
                    },
                    label = { Text("Default Grid Surcharge Tax (%)", color = DarkTextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onCompanyUpdate(editName, editAddress, editEmail) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Corporate Settings", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Database Statistics Summaries
        Text(
            text = "DATABASE AND GRID METRIC SUMMARY",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Operation Diagnostics:", color = ElectricTeal, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Active Registered Customers:", color = DarkTextSecondary)
                    Text(text = "$customersCount accounts", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Total Grid Smart Meters:", color = DarkTextSecondary)
                    Text(text = "$metersCount nodes", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Statements & Invoices Compiled:", color = DarkTextSecondary)
                    Text(text = "$billsCount archives", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Total Energy Synced Load:", color = DarkTextSecondary)
                    Text(text = String.format(Locale.US, "%,.1f kWh", totalLoad), color = ElectricAmber, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Grid Operating Kernels Status:", color = DarkTextSecondary)
                    Text(text = "NOMINAL (100% ONLINE)", color = MeterOnline, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
