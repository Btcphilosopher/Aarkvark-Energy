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
import com.example.data.Bill
import com.example.data.Customer
import com.example.data.SmartMeter
import com.example.ui.UtilityViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BillingScreen(
    viewModel: UtilityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.billSearchQuery.collectAsState()
    val bills by viewModel.searchedBills.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val meters by viewModel.meters.collectAsState()

    var selectedBillForInvoiceSheet by remember { mutableStateOf<Bill?>(null) }
    var showGenerateBillDialog by remember { mutableStateOf(false) }

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
            // Screen Header
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FINANCIAL AUDITING",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Billing & Invoices",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Button(
                    onClick = { showGenerateBillDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                    modifier = Modifier.testTag("open_generate_bill_dialog")
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calculate Bill", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Invoices
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.billSearchQuery.value = it },
                placeholder = { Text("Search by Invoice Number...", color = DarkTextSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ElectricTeal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.billSearchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bills_field"),
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

            // Invoice Index Ledger
            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Empty Invoices",
                            tint = DarkSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Invoices Archived",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Generate a standard billing cycle statement above.",
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
                    items(bills) { bill ->
                        val associatedCustName = customers.firstOrNull { it.id == bill.customerId }?.name ?: "Customer Profile deleted"

                        BillItemRow(
                            bill = bill,
                            customerName = associatedCustName,
                            onClick = { selectedBillForInvoiceSheet = bill }
                        )
                    }
                }
            }
        }
    }

    // ON-DEMAND CYCLE BILLING CALCULATION DIALOG
    if (showGenerateBillDialog) {
        var isCustDropdownExpanded by remember { mutableStateOf(false) }
        var selectedCustomerForBilling by remember { mutableStateOf<Customer?>(null) }
        var isMeterDropdownExpanded by remember { mutableStateOf(false) }
        var selectedMeterForBilling by remember { mutableStateOf<SmartMeter?>(null) }
        var billingDays by remember { mutableStateOf("30") }

        val customerMeters = meters.filter { it.customerId == selectedCustomerForBilling?.id }

        AlertDialog(
            onDismissRequest = { showGenerateBillDialog = false },
            title = { Text("Calculate Statement", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Customer Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomerForBilling?.name ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer Profile", color = DarkTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { isCustDropdownExpanded = !isCustDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isCustDropdownExpanded,
                            onDismissRequest = { isCustDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                        ) {
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name, color = Color.White) },
                                    onClick = {
                                        selectedCustomerForBilling = customer
                                        isCustDropdownExpanded = false
                                        // Auto-reset meter choice
                                        selectedMeterForBilling = null
                                    }
                                )
                            }
                        }
                    }

                    // Meter Selector (filtered by customer)
                    if (selectedCustomerForBilling != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedMeterForBilling?.meterId ?: "Select Smart Meter",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Operational Smart Meter", color = DarkTextSecondary) },
                                trailingIcon = {
                                    IconButton(onClick = { isMeterDropdownExpanded = !isMeterDropdownExpanded }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = isMeterDropdownExpanded,
                                onDismissRequest = { isMeterDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceVariant)
                            ) {
                                if (customerMeters.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No active meters for this account", color = Color.Gray) },
                                        onClick = {}
                                    )
                                } else {
                                    customerMeters.forEach { meter ->
                                        DropdownMenuItem(
                                            text = { Text("${meter.meterId} (${meter.status})", color = Color.White) },
                                            onClick = {
                                                selectedMeterForBilling = meter
                                                isMeterDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = billingDays,
                        onValueChange = { billingDays = it },
                        label = { Text("Billing Cycle Duration (Days)", color = DarkTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cust = selectedCustomerForBilling
                        val mtr = selectedMeterForBilling
                        val days = billingDays.toIntOrNull() ?: 30

                        if (cust != null && mtr != null) {
                            viewModel.generateBillForCustomer(cust.id, mtr.id, days)
                            Toast.makeText(context, "Calculated Statement successfully!", Toast.LENGTH_SHORT).show()
                            showGenerateBillDialog = false
                        } else {
                            Toast.makeText(context, "Please select customer & associated smart meter", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Calculate Statement", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateBillDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // VISUAL INVOICE STATEMENT (BILL SHEET / SIMULATED PDF DOWNLOADS)
    selectedBillForInvoiceSheet?.let { bill ->
        val billCustomer = customers.firstOrNull { it.id == bill.customerId }
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startStr = dateFmt.format(Date(bill.billingPeriodStart))
        val endStr = dateFmt.format(Date(bill.billingPeriodEnd))
        val generatedStr = dateFmt.format(Date(bill.generatedDate))
        val dueStr = dateFmt.format(Date(bill.dueDate))

        var simulatedPaymentMethodExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedBillForInvoiceSheet = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AARDVARK STATEMENT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricTeal, letterSpacing = 1.sp)
                    )
                    IconButton(onClick = { selectedBillForInvoiceSheet = null }) {
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
                    // Invoice Header Details
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bill.invoiceNumber,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp
                                )

                                Text(
                                    text = bill.status,
                                    color = if (bill.status == "PAID") MeterOnline else MeterOffline,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Customer Profile:", color = DarkTextSecondary, fontSize = 11.sp)
                            Text(text = billCustomer?.name ?: "Profile Deleted", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = billCustomer?.address ?: "", color = DarkTextSecondary, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = DarkSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "STATEMENT DATE", color = DarkTextSecondary, fontSize = 10.sp)
                                    Text(text = generatedStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "PAYMENT DUE DATE", color = DarkTextSecondary, fontSize = 10.sp)
                                    Text(text = dueStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Billing Period: $startStr to $endStr", color = DarkTextSecondary, fontSize = 11.sp)
                        }
                    }

                    // Structured Line-Items Table
                    Text(
                        text = "STATEMENT COST BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricTeal)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Item 1: Energy Usage Charge
                            InvoiceBreakdownLine(
                                label = "Energy Consumption (${String.format(Locale.US, "%.1f", bill.usageKwh)} kWh)",
                                amount = bill.baseCost
                            )
                            // Item 2: Standing Daily Charge
                            InvoiceBreakdownLine(
                                label = "Standing Daily Utility Charges",
                                amount = bill.standingChargeCost
                            )
                            // Item 3: Discount if applied
                            if (bill.discountAmount > 0.0) {
                                InvoiceBreakdownLine(
                                    label = "Grid Subscription Discount",
                                    amount = -bill.discountAmount,
                                    color = MeterOnline
                                )
                            }
                            // Item 4: Subtotal
                            Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                            InvoiceBreakdownLine(
                                label = "Subtotal (Excl. VAT)",
                                amount = bill.baseCost + bill.standingChargeCost - bill.discountAmount,
                                isBold = true
                            )
                            // Item 5: VAT
                            InvoiceBreakdownLine(
                                label = "Value Added Tax (VAT)",
                                amount = bill.vatAmount
                            )
                            // Item 6: Total Invoice Amount
                            Divider(color = ElectricTeal.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                            InvoiceBreakdownLine(
                                label = "TOTAL AMOUNT DUE",
                                amount = bill.totalAmount,
                                color = ElectricAmber,
                                isBold = true,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Simulated Exports & Payment Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export simulated PDF
                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Simulated Export Successful!\nSaved to /downloads/${bill.invoiceNumber}.pdf",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export PDF", color = Color.White)
                        }

                        // Pay bill simulation if UNPAID
                        if (bill.status == "UNPAID") {
                            Box(modifier = Modifier.weight(1.2f)) {
                                Button(
                                    onClick = { simulatedPaymentMethodExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = DarkBackground)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay Invoice", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = simulatedPaymentMethodExpanded,
                                    onDismissRequest = { simulatedPaymentMethodExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceVariant)
                                ) {
                                    listOf("CARD", "BANK_TRANSFER", "DIRECT_DEBIT", "CASH").forEach { method ->
                                        DropdownMenuItem(
                                            text = { Text("Pay via $method", color = Color.White) },
                                            onClick = {
                                                simulatedPaymentMethodExpanded = false
                                                viewModel.payBill(bill.id, bill.customerId, bill.totalAmount, method)
                                                Toast.makeText(context, "Payment Processed!\nStatus: PAID", Toast.LENGTH_SHORT).show()
                                                // Refresh reference state
                                                selectedBillForInvoiceSheet = bill.copy(status = "PAID")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBillForInvoiceSheet = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Done", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun InvoiceBreakdownLine(
    label: String,
    amount: Double,
    color: Color = Color.White,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (isBold) Color.White else DarkTextSecondary,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = String.format(Locale.US, "$%,.2f", amount),
            color = color,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BillItemRow(
    bill: Bill,
    customerName: String,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date(bill.generatedDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (bill.status == "PAID") MeterOnline.copy(alpha = 0.12f)
                            else MeterOffline.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (bill.status == "PAID") Icons.Default.CheckCircle else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = if (bill.status == "PAID") MeterOnline else MeterOffline,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = bill.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "$%,.2f", bill.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricAmber)
                )
                Text(
                    text = "Billed $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
        }
    }
}
