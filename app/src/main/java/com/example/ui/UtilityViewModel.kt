package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UtilityViewModel(private val repository: UtilityRepository) : ViewModel() {

    // Main Flows
    val tariffs: StateFlow<List<Tariff>> = repository.allTariffs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meters: StateFlow<List<SmartMeter>> = repository.allMeters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<SavedReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<AppUser>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentReadings: StateFlow<List<MeterReading>> = repository.getRecentReadings(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Flows
    val customerSearchQuery = MutableStateFlow("")
    val meterSearchQuery = MutableStateFlow("")
    val billSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedCustomers: StateFlow<List<Customer>> = customerSearchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allCustomers
            else repository.searchCustomers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedMeters: StateFlow<List<SmartMeter>> = meterSearchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allMeters
            else repository.searchMeters(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedBills: StateFlow<List<Bill>> = billSearchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allBills
            else repository.searchBills(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // System Alert Calculations
    val activeAlerts: StateFlow<List<SystemAlert>> = combine(meters, bills) { meterList, billList ->
        val alerts = mutableListOf<SystemAlert>()

        // Check offline meters
        meterList.filter { it.status == "OFFLINE" }.forEach {
            alerts.add(
                SystemAlert(
                    id = "OFFLINE_${it.id}",
                    title = "Meter Connection Interrupted",
                    message = "Smart Meter ${it.meterId} in ${it.address} has gone offline. Dispatch diagnostics suggested.",
                    severity = AlertSeverity.CRITICAL,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Check high monthly bill estimations
        val highThreshold = 350.0
        billList.filter { it.status == "UNPAID" && it.totalAmount > highThreshold }.forEach { bill ->
            val custName = repository.getCustomerById(bill.customerId)?.name ?: "Customer"
            alerts.add(
                SystemAlert(
                    id = "BUDGET_EXCEEDED_${bill.id}",
                    title = "High Invoice Detected",
                    message = "Invoice ${bill.invoiceNumber} for $custName exceeds standard budget threshold: $${String.format(Locale.US, "%.2f", bill.totalAmount)}",
                    severity = AlertSeverity.WARNING,
                    timestamp = bill.generatedDate
                )
            )
        }

        // Generative warning for peak demand limits
        alerts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer CRUD
    fun addCustomer(name: String, address: String, email: String, phone: String, tariffId: Long?) {
        viewModelScope.launch {
            repository.insertCustomer(
                Customer(
                    name = name,
                    address = address,
                    email = email,
                    phone = phone,
                    tariffId = tariffId
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // Smart Meter CRUD
    fun addMeter(meterId: String, customerId: Long?, address: String, initialReading: Double, status: String) {
        viewModelScope.launch {
            repository.insertMeter(
                SmartMeter(
                    meterId = meterId,
                    customerId = customerId,
                    address = address,
                    currentReading = initialReading,
                    previousReading = initialReading,
                    status = status
                )
            )
        }
    }

    fun updateMeter(meter: SmartMeter) {
        viewModelScope.launch {
            repository.updateMeter(meter)
        }
    }

    fun deleteMeter(meter: SmartMeter) {
        viewModelScope.launch {
            repository.deleteMeter(meter)
        }
    }

    // Meter Readings
    fun addMeterReading(meterId: Long, readingValue: Double, demandKw: Double) {
        viewModelScope.launch {
            repository.addMeterReading(meterId, readingValue, demandKw)
        }
    }

    fun getReadingsForMeter(meterId: Long): Flow<List<MeterReading>> {
        return repository.getReadingsForMeter(meterId)
    }

    // Bill Generation
    fun generateBillForCustomer(customerId: Long, meterId: Long, durationDays: Int) {
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            val start = end - (durationDays.toLong() * 24 * 60 * 60 * 1000)
            repository.generateBill(customerId, meterId, start, end)
        }
    }

    // Record Payment
    fun payBill(billId: Long, customerId: Long, amount: Double, method: String) {
        viewModelScope.launch {
            repository.recordPayment(
                Payment(
                    billId = billId,
                    customerId = customerId,
                    amount = amount,
                    paymentMethod = method
                )
            )
        }
    }

    // Tariff Actions
    fun addTariff(name: String, desc: String, rate: Double, peak: Double, offPeak: Double, standing: Double, vat: Double, discount: Double) {
        viewModelScope.launch {
            repository.insertTariff(
                Tariff(
                    name = name,
                    description = desc,
                    ratePerKwh = rate,
                    peakRatePerKwh = peak,
                    offPeakRatePerKwh = offPeak,
                    standingCharge = standing,
                    vatRate = vat,
                    discountRate = discount
                )
            )
        }
    }

    fun updateTariff(tariff: Tariff) {
        viewModelScope.launch {
            repository.updateTariff(tariff)
        }
    }

    fun deleteTariff(tariff: Tariff) {
        viewModelScope.launch {
            repository.deleteTariff(tariff)
        }
    }

    // Generate Reports (Simulated export to CSV/PDF)
    fun generateReport(title: String, type: String, description: String) {
        viewModelScope.launch {
            val randomId = UUID.randomUUID().toString().take(6).uppercase()
            val filename = "Aardvark_${type.lowercase()}_$randomId"
            repository.insertReport(
                SavedReport(
                    title = title,
                    reportType = type,
                    parameters = description,
                    generatedBy = "Aardvark Operator Console",
                    fileUrl = "$filename.pdf"
                )
            )
        }
    }
}

// Alert Data Representation
data class SystemAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long
)

enum class AlertSeverity {
    CRITICAL, WARNING, INFO
}

class UtilityViewModelFactory(private val repository: UtilityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UtilityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UtilityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
