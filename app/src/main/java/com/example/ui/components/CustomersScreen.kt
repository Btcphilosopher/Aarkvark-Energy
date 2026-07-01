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
import com.example.data.Tariff
import com.example.ui.UtilityViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomersScreen(
    viewModel: UtilityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.customerSearchQuery.collectAsState()
    val customers by viewModel.searchedCustomers.collectAsState()
    val tariffs by viewModel.tariffs.collectAsState()
    val meters by viewModel.meters.collectAsState()
    val bills by viewModel.bills.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<Customer?>(null) }
    var showEditDialog by remember { mutableStateOf<Customer?>(null) }

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
                        text = "UTILITY ACCOUNT MANAGEMENT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Customer Ledger",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                    modifier = Modifier.testTag("add_customer_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer", tint = DarkBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Account", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.customerSearchQuery.value = it },
                placeholder = { Text("Search by name, address, email...", color = DarkTextSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ElectricTeal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.customerSearchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_customers_field"),
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

            // Customer ledger list
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = "Empty Customers",
                            tint = DarkSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Customer Accounts Registered",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add a new customer profile using the registration button.",
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
                    items(customers) { customer ->
                        val activeTariffName = tariffs.firstOrNull { it.id == customer.tariffId }?.name ?: "No Tariff Applied"
                        val customerMeters = meters.filter { it.customerId == customer.id }

                        CustomerItemCard(
                            customer = customer,
                            tariffName = activeTariffName,
                            metersCount = customerMeters.size,
                            onClick = { selectedCustomerForDetails = customer },
                            onEdit = { showEditDialog = customer },
                            onDelete = {
                                viewModel.deleteCustomer(customer)
                                Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Add Customer FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ElectricAmber,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_customer_fab")
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = DarkBackground)
        }
    }

    // ADD NEW CUSTOMER DIALOG
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var selectedTariff by remember { mutableStateOf<Tariff?>(null) }
        var isTariffDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register Customer Account", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Billing / Service Address", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Contact", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Contact", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tariff selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTariff?.name ?: "Select Electricity Tariff Plan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Energy Tariff", color = DarkTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { isTariffDropdownExpanded = !isTariffDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isTariffDropdownExpanded,
                            onDismissRequest = { isTariffDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                        ) {
                            tariffs.forEach { tariff ->
                                DropdownMenuItem(
                                    text = { Text(tariff.name, color = Color.White) },
                                    onClick = {
                                        selectedTariff = tariff
                                        isTariffDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty() && address.isNotEmpty() && email.isNotEmpty()) {
                            viewModel.addCustomer(
                                name = name,
                                address = address,
                                email = email,
                                phone = phone,
                                tariffId = selectedTariff?.id
                            )
                            Toast.makeText(context, "Account profile registered successfully", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Please fill in all mandatory details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Register Profile", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // EDIT CUSTOMER DIALOG
    showEditDialog?.let { customer ->
        var name by remember { mutableStateOf(customer.name) }
        var address by remember { mutableStateOf(customer.address) }
        var email by remember { mutableStateOf(customer.email) }
        var phone by remember { mutableStateOf(customer.phone) }
        var selectedTariff by remember { mutableStateOf(tariffs.firstOrNull { it.id == customer.tariffId }) }
        var isTariffDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Update Customer Profile", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Billing / Service Address", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Contact", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Contact", color = DarkTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tariff Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTariff?.name ?: "Assign Tariff Plan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Energy Tariff", color = DarkTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { isTariffDropdownExpanded = !isTariffDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricTeal)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isTariffDropdownExpanded,
                            onDismissRequest = { isTariffDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                        ) {
                            tariffs.forEach { tariff ->
                                DropdownMenuItem(
                                    text = { Text(tariff.name, color = Color.White) },
                                    onClick = {
                                        selectedTariff = tariff
                                        isTariffDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty() && address.isNotEmpty() && email.isNotEmpty()) {
                            viewModel.updateCustomer(
                                customer.copy(
                                    name = name,
                                    address = address,
                                    email = email,
                                    phone = phone,
                                    tariffId = selectedTariff?.id
                                )
                            )
                            Toast.makeText(context, "Account configurations synchronized", Toast.LENGTH_SHORT).show()
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

    // CUSTOMER DOSSIER & GENERATIVE BILL DIALOG
    selectedCustomerForDetails?.let { customer ->
        val customerMeters = meters.filter { it.customerId == customer.id }
        val customerBills = bills.filter { it.customerId == customer.id }
        val activeTariff = tariffs.firstOrNull { it.id == customer.tariffId }

        var showBillingGenerator by remember { mutableStateOf(false) }
        var selectedMeterIndex by remember { mutableStateOf(0) }
        var billingDurationDays by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { selectedCustomerForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account: ${customer.name}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = { selectedCustomerForDetails = null }) {
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
                    // Contact Details
                    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "CONTACT DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricTeal))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Email: ${customer.email}", color = Color.White, fontSize = 13.sp)
                            Text(text = "Phone: ${customer.phone}", color = Color.White, fontSize = 13.sp)
                            Text(text = "Address: ${customer.address}", color = DarkTextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Active Tariff Detail Card
                    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "ENERGY TARIFF PLAN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricAmber))
                                Text(text = activeTariff?.name ?: "No Tariff Plan Linked", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (activeTariff != null) {
                                    Text(text = "Std Rate: $${activeTariff.ratePerKwh}/kWh | VAT: ${(activeTariff.vatRate*100).toInt()}%", color = DarkTextSecondary, fontSize = 11.sp)
                                }
                            }
                            Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(28.dp))
                        }
                    }

                    // On-Demand Billing Generator Actions
                    if (customerMeters.isNotEmpty()) {
                        Button(
                            onClick = { showBillingGenerator = !showBillingGenerator },
                            colors = ButtonDefaults.buttonColors(containerColor = if (showBillingGenerator) MeterOffline else ElectricTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = if (showBillingGenerator) Icons.Default.Cancel else Icons.Default.Calculate, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (showBillingGenerator) "Close Generator" else "Generate Smart Bill Now", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(visible = showBillingGenerator) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "BILLING CALCULATOR ENGINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricTeal))

                                    // Meter selector dropdown
                                    Text(text = "Select smart meter to bill:", color = DarkTextSecondary, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        customerMeters.forEachIndexed { index, m ->
                                            val isSel = selectedMeterIndex == index
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) ElectricTeal else DarkSurface)
                                                    .clickable { selectedMeterIndex = index }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(text = m.meterId, color = if (isSel) DarkBackground else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = billingDurationDays,
                                        onValueChange = { billingDurationDays = it },
                                        label = { Text("Billing Cycle Duration (Days)", color = DarkTextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricTeal, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val activeMeter = customerMeters[selectedMeterIndex]
                                            val cycleDays = billingDurationDays.toIntOrNull() ?: 30
                                            viewModel.generateBillForCustomer(customer.id, activeMeter.id, cycleDays)
                                            Toast.makeText(context, "Calculated & Invoiced successfully!", Toast.LENGTH_SHORT).show()
                                            showBillingGenerator = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Generate Custom Utility Invoice", color = DarkBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Historical Invoices Section
                    Text(text = "BILLING & INVOICE HISTORIC INDEX", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DarkTextSecondary))

                    if (customerBills.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No bills registered for this customer yet.", color = DarkTextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customerBills) { bill ->
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
                                        Text(text = bill.invoiceNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Usage: ${bill.usageKwh} kWh | Due: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(bill.dueDate))}", color = DarkTextSecondary, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = String.format(Locale.US, "$%,.2f", bill.totalAmount), color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = bill.status,
                                            color = if (bill.status == "PAID") MeterOnline else MeterOffline,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
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
                    onClick = { selectedCustomerForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Close", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    tariffName: String,
    metersCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ElectricTeal)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = customer.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = DarkTextSecondary)
                        )
                    }
                }

                // Edit/Delete options
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
                            text = { Text("Edit Profile", color = Color.White) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = ElectricTeal) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = MeterOffline) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MeterOffline) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
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
                    Text(text = "ACTIVE PLAN", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(text = tariffName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CONNECTED METERS", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(text = "$metersCount Smart Unit(s)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ElectricAmber))
                }
            }
        }
    }
}
