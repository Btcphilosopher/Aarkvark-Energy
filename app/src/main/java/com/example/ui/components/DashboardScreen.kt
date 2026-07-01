package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeterReading
import com.example.data.SmartMeter
import com.example.ui.AlertSeverity
import com.example.ui.SystemAlert
import com.example.ui.UtilityViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: UtilityViewModel,
    modifier: Modifier = Modifier,
    onNavigateToMeters: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val meters by viewModel.meters.collectAsState()
    val readings by viewModel.recentReadings.collectAsState()
    val alerts by viewModel.activeAlerts.collectAsState()
    val bills by viewModel.bills.collectAsState()

    // Aggregate statistics
    val totalMeters = meters.size
    val onlineMeters = meters.count { it.status == "ONLINE" }
    val offlineMeters = meters.count { it.status == "OFFLINE" }

    // Simulated total consumption & costs
    val totalConsumption = readings.sumOf { it.reading }.coerceAtLeast(14800.0)
    val totalCost = bills.sumOf { it.totalAmount }
    val estimatedMonthlyBill = totalCost * 1.25

    var selectedChartTab by remember { mutableStateOf("Weekly Usage") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        // Welcome and Header Block
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AARDVARK ENERGY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "Operations Command Center",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // Grid Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (offlineMeters == 0) MeterOnline else MeterMaintenance)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (offlineMeters == 0) "GRID STATUS: NOMINAL" else "GRID STATUS: DEGRADED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Active Alerts Alert Banner (Collapsible / High Visibility)
        if (alerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAlerts() }
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    MeterOffline.copy(alpha = 0.8f),
                                    ElectricAmber.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MeterOffline.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Active System Warning",
                            tint = MeterOffline,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "CRITICAL SYSTEM WARNING",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MeterOffline
                                )
                            )
                            Text(
                                text = "${alerts.size} active issues require attention (click to inspect grid anomalies).",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Inspect Alerts",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Live KPI Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        title = "GRID DEMAND",
                        value = "1.45 kW",
                        subtitle = "Avg Instant Rate",
                        icon = Icons.Default.Bolt,
                        color = ElectricTeal,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "TOTAL CAPACITY",
                        value = "21,650 kWh",
                        subtitle = "Cumulative Grid Load",
                        icon = Icons.Default.ElectricalServices,
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        title = "BILLED COST",
                        value = String.format(Locale.US, "$%,.2f", totalCost),
                        subtitle = "Standing & Usage",
                        icon = Icons.Default.ReceiptLong,
                        color = MeterOnline,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        title = "EST. MONTHLY",
                        value = String.format(Locale.US, "$%,.2f", estimatedMonthlyBill),
                        subtitle = "Projected Outlay",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Smart Meters Connection Status Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMeters() },
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SMART METERS NETWORK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalMeters Registered Meters",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Operational diagnostics and telemetry sync",
                                style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MeterStatusIndicator(count = onlineMeters, label = "ONLINE", color = MeterOnline)
                            MeterStatusIndicator(count = offlineMeters, label = "OFFLINE", color = MeterOffline)
                        }
                    }
                }
            }
        }

        // Live Usage Charts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRID CONSUMPTION TREND",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextSecondary
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(4.dp)
                        ) {
                            listOf("Weekly Usage", "Peak Demand").forEach { tab ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedChartTab == tab) ElectricTeal else Color.Transparent)
                                        .clickable { selectedChartTab = tab }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tab,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedChartTab == tab) DarkBackground else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedChartTab == "Weekly Usage") {
                        MetricLineChart(
                            data = listOf(420.0, 510.0, 480.0, 620.0, 580.0, 710.0, 650.0),
                            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        MetricBarChart(
                            data = listOf(3.2, 4.8, 2.9, 5.1, 4.0, 6.5, 4.2),
                            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }

        // Recent Activity Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT GRID READINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "LIVE FEED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ElectricTeal
                    )
                )
            }
        }

        if (readings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Recent Readings Logged", color = DarkTextSecondary)
                }
            }
        } else {
            items(readings.take(5)) { reading ->
                RecentReadingRow(reading = reading)
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = DarkTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = DarkTextSecondary
                )
            )
        }
    }
}

@Composable
fun MeterStatusIndicator(count: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        )
    }
}

@Composable
fun RecentReadingRow(reading: MeterReading) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val formattedTime = timeFormat.format(Date(reading.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Meter Sync Reading",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Instant Demand: ${String.format(Locale.US, "%.2f", reading.demandKw)} kW",
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.1f kWh", reading.reading),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ElectricAmber
                    )
                )
                Text(
                    text = "at $formattedTime",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
        }
    }
}
