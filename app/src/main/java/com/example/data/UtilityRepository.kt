package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import kotlin.random.Random

class UtilityRepository(private val utilityDao: UtilityDao) {

    val allTariffs: Flow<List<Tariff>> = utilityDao.getAllTariffs()
    val allCustomers: Flow<List<Customer>> = utilityDao.getAllCustomers()
    val allMeters: Flow<List<SmartMeter>> = utilityDao.getAllMeters()
    val allBills: Flow<List<Bill>> = utilityDao.getAllBills()
    val allPayments: Flow<List<Payment>> = utilityDao.getAllPayments()
    val allReports: Flow<List<SavedReport>> = utilityDao.getAllReports()
    val allUsers: Flow<List<AppUser>> = utilityDao.getAllUsers()

    fun searchCustomers(query: String): Flow<List<Customer>> = utilityDao.searchCustomers(query)
    fun searchMeters(query: String): Flow<List<SmartMeter>> = utilityDao.searchMeters(query)
    fun searchBills(query: String): Flow<List<Bill>> = utilityDao.searchBills(query)
    fun getMetersForCustomer(customerId: Long): Flow<List<SmartMeter>> = utilityDao.getMetersByCustomer(customerId)
    fun getBillsForCustomer(customerId: Long): Flow<List<Bill>> = utilityDao.getBillsByCustomer(customerId)
    fun getReadingsForMeter(meterId: Long): Flow<List<MeterReading>> = utilityDao.getReadingsForMeter(meterId)
    fun getRecentReadings(limit: Int): Flow<List<MeterReading>> = utilityDao.getRecentReadings(limit)

    suspend fun getCustomerById(id: Long): Customer? = utilityDao.getCustomerById(id)
    suspend fun getMeterById(id: Long): SmartMeter? = utilityDao.getMeterById(id)
    suspend fun getTariffById(id: Long): Tariff? = utilityDao.getTariffById(id)
    suspend fun getBillById(id: Long): Bill? = utilityDao.getBillById(id)

    // Tariff Actions
    suspend fun insertTariff(tariff: Tariff): Long = utilityDao.insertTariff(tariff)
    suspend fun updateTariff(tariff: Tariff) = utilityDao.updateTariff(tariff)
    suspend fun deleteTariff(tariff: Tariff) = utilityDao.deleteTariff(tariff)

    // Customer Actions
    suspend fun insertCustomer(customer: Customer): Long = utilityDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = utilityDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = utilityDao.deleteCustomer(customer)

    // Smart Meter Actions
    suspend fun insertMeter(meter: SmartMeter): Long = utilityDao.insertMeter(meter)
    suspend fun updateMeter(meter: SmartMeter) = utilityDao.updateMeter(meter)
    suspend fun deleteMeter(meter: SmartMeter) = utilityDao.deleteMeter(meter)

    // Payment Actions
    suspend fun recordPayment(payment: Payment): Long {
        val paymentId = utilityDao.insertPayment(payment)
        // Mark the bill as PAID if the payment amount meets or exceeds total amount
        val bill = utilityDao.getBillById(payment.billId)
        if (bill != null) {
            val payments = utilityDao.getPaymentsForBill(bill.id).first()
            val totalPaid = payments.sumOf { it.amount }
            if (totalPaid >= bill.totalAmount) {
                utilityDao.updateBill(bill.copy(status = "PAID"))
            }
        }
        return paymentId
    }

    // Insert Report
    suspend fun insertReport(report: SavedReport): Long = utilityDao.insertReport(report)

    // Custom flow for meter reading generation with auto tariff calculation
    suspend fun addMeterReading(meterId: Long, readingValue: Double, demandKw: Double): Long {
        val meter = utilityDao.getMeterById(meterId) ?: return -1
        val customer = meter.customerId?.let { utilityDao.getCustomerById(it) }
        val tariff = customer?.tariffId?.let { utilityDao.getTariffById(it) }

        val diff = (readingValue - meter.currentReading).coerceAtLeast(0.0)

        // Calculate cost
        val rate = tariff?.ratePerKwh ?: 0.15
        val calculatedCost = diff * rate

        // Insert new reading
        val readingId = utilityDao.insertReading(
            MeterReading(
                meterId = meterId,
                reading = readingValue,
                demandKw = demandKw,
                cost = calculatedCost,
                isPeak = Random.nextBoolean() // dynamic peak allocation for interest
            )
        )

        // Update Smart Meter with new readings
        utilityDao.updateMeter(
            meter.copy(
                previousReading = meter.currentReading,
                currentReading = readingValue
            )
        )

        return readingId
    }

    // Bill Generation
    suspend fun generateBill(customerId: Long, meterId: Long, startMillis: Long, endMillis: Long): Bill? {
        val customer = utilityDao.getCustomerById(customerId) ?: return null
        val meter = utilityDao.getMeterById(meterId) ?: return null
        val tariff = customer.tariffId?.let { utilityDao.getTariffById(it) } ?: Tariff(
            name = "Standard Tarif",
            description = "Default rate",
            ratePerKwh = 0.15,
            peakRatePerKwh = 0.22,
            offPeakRatePerKwh = 0.09,
            standingCharge = 0.40,
            vatRate = 0.05,
            discountRate = 0.0
        )

        // Calculate consumption
        val usage = (meter.currentReading - meter.previousReading).coerceAtLeast(10.0) // default some usage if empty
        val baseCost = usage * tariff.ratePerKwh

        // Daily standing charges
        val days = ((endMillis - startMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toDouble()
        val standingChargeTotal = days * tariff.standingCharge

        // Subtotal
        val subtotal = baseCost + standingChargeTotal
        val discountAmount = subtotal * tariff.discountRate
        val afterDiscount = subtotal - discountAmount
        val vatAmount = afterDiscount * tariff.vatRate
        val totalAmount = afterDiscount + vatAmount

        val randomId = UUID.randomUUID().toString().take(8).uppercase()
        val invoiceNumber = "INV-2026-$randomId"

        val bill = Bill(
            customerId = customerId,
            meterId = meterId,
            invoiceNumber = invoiceNumber,
            billingPeriodStart = startMillis,
            billingPeriodEnd = endMillis,
            usageKwh = usage,
            baseCost = baseCost,
            standingChargeCost = standingChargeTotal,
            vatAmount = vatAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            status = "UNPAID",
            dueDate = endMillis + (14L * 24 * 60 * 60 * 1000) // 14 days later
        )

        val billId = utilityDao.insertBill(bill)
        return bill.copy(id = billId)
    }

    // Seeding sample data
    suspend fun seedDatabaseIfEmpty() {
        val currentTariffs = utilityDao.getAllTariffs().first()
        if (currentTariffs.isNotEmpty()) {
            Log.d("UtilityRepository", "Database already seeded.")
            return
        }

        Log.d("UtilityRepository", "Database empty. Seeding initial data...")

        // 1. Seed Tariffs
        val t1 = Tariff(
            name = "Standard Saver",
            description = "Standard rate suitable for small households with uniform usage.",
            ratePerKwh = 0.14,
            peakRatePerKwh = 0.21,
            offPeakRatePerKwh = 0.08,
            standingCharge = 0.35,
            vatRate = 0.05,
            discountRate = 0.0
        )
        val t2 = Tariff(
            name = "Green EcoFlex",
            description = "100% renewable electricity with low overnight rates for EVs and smart devices.",
            ratePerKwh = 0.17,
            peakRatePerKwh = 0.26,
            offPeakRatePerKwh = 0.07,
            standingCharge = 0.45,
            vatRate = 0.05,
            discountRate = 0.04
        )
        val t3 = Tariff(
            name = "Commercial Power Pro",
            description = "High volume commercial energy plan with structured discounts.",
            ratePerKwh = 0.11,
            peakRatePerKwh = 0.16,
            offPeakRatePerKwh = 0.05,
            standingCharge = 1.10,
            vatRate = 0.20,
            discountRate = 0.08
        )

        val t1Id = utilityDao.insertTariff(t1)
        val t2Id = utilityDao.insertTariff(t2)
        val t3Id = utilityDao.insertTariff(t3)

        // 2. Seed Customers
        val customers = listOf(
            Customer(name = "Olivia Vance", address = "104 Redwood Ave, Seattle WA", email = "olivia@vance.net", phone = "+1-555-0142", tariffId = t1Id),
            Customer(name = "Marcus Finch", address = "287 Cascades Highway, Portland OR", email = "marcus@finchtech.com", phone = "+1-555-0178", tariffId = t2Id),
            Customer(name = "Aurora Manufacturing Ltd", address = "890 Industrial Way, Tacoma WA", email = "operations@aurora-mfg.com", phone = "+1-555-0199", tariffId = t3Id),
            Customer(name = "Dr. Raymond Holt", address = "1003 Brooklyn Heights, New York NY", email = "rholt@precinct99.gov", phone = "+1-555-0999", tariffId = t1Id)
        )

        val customerIds = customers.map { utilityDao.insertCustomer(it) }

        // 3. Seed Meters
        val meters = listOf(
            SmartMeter(meterId = "MTR-8821-X", customerId = customerIds[0], address = "104 Redwood Ave, Seattle WA", currentReading = 4280.5, previousReading = 4120.0, status = "ONLINE", installationDate = System.currentTimeMillis() - 120L*24*60*60*1000),
            SmartMeter(meterId = "MTR-4490-A", customerId = customerIds[1], address = "287 Cascades Highway, Portland OR", currentReading = 8950.2, previousReading = 8410.5, status = "ONLINE", installationDate = System.currentTimeMillis() - 200L*24*60*60*1000),
            SmartMeter(meterId = "MTR-0099-IND", customerId = customerIds[2], address = "890 Industrial Way, Tacoma WA", currentReading = 142300.0, previousReading = 129400.0, status = "ONLINE", installationDate = System.currentTimeMillis() - 365L*24*60*60*1000),
            SmartMeter(meterId = "MTR-1102-M", customerId = customerIds[3], address = "1003 Brooklyn Heights, New York NY", currentReading = 1205.8, previousReading = 1205.8, status = "OFFLINE", installationDate = System.currentTimeMillis() - 15L*24*60*60*1000)
        )

        val meterIds = meters.map { utilityDao.insertMeter(it) }

        // 4. Seed Historical Readings (some recent hourly/daily records)
        val now = System.currentTimeMillis()
        val hourMillis = 60 * 60 * 1000L
        val dayMillis = 24 * hourMillis

        for (i in 0 until 3) {
            val mId = meterIds[i]
            val startingReading = meters[i].previousReading
            val finalReading = meters[i].currentReading
            val steps = 12
            val readingStep = (finalReading - startingReading) / steps

            for (step in 0 until steps) {
                val timestamp = now - (steps - step) * 2L * dayMillis
                val currentStepReading = startingReading + step * readingStep
                val demand = if (i == 2) Random.nextDouble(45.0, 85.0) else Random.nextDouble(1.2, 4.5)
                utilityDao.insertReading(
                    MeterReading(
                        meterId = mId,
                        timestamp = timestamp,
                        reading = currentStepReading,
                        demandKw = demand,
                        cost = readingStep * (if (i == 0) t1.ratePerKwh else if (i == 1) t2.ratePerKwh else t3.ratePerKwh),
                        isPeak = step % 4 == 0
                    )
                )
            }
        }

        // 5. Seed historical bills
        // Alice Vance invoice
        val bill1 = Bill(
            customerId = customerIds[0],
            meterId = meterIds[0],
            invoiceNumber = "INV-2026-AE01",
            billingPeriodStart = now - 60 * dayMillis,
            billingPeriodEnd = now - 30 * dayMillis,
            usageKwh = 160.5,
            baseCost = 160.5 * t1.ratePerKwh,
            standingChargeCost = 30 * t1.standingCharge,
            vatAmount = (160.5 * t1.ratePerKwh + 30 * t1.standingCharge) * t1.vatRate,
            discountAmount = 0.0,
            totalAmount = (160.5 * t1.ratePerKwh + 30 * t1.standingCharge) * (1 + t1.vatRate),
            status = "PAID",
            dueDate = now - 16 * dayMillis,
            generatedDate = now - 30 * dayMillis
        )
        val b1Id = utilityDao.insertBill(bill1)

        // Aurora Manufacturing invoice
        val bill2 = Bill(
            customerId = customerIds[2],
            meterId = meterIds[2],
            invoiceNumber = "INV-2026-AE02",
            billingPeriodStart = now - 30 * dayMillis,
            billingPeriodEnd = now,
            usageKwh = 12900.0,
            baseCost = 12900.0 * t3.ratePerKwh,
            standingChargeCost = 30 * t3.standingCharge,
            vatAmount = (12900.0 * t3.ratePerKwh + 30 * t3.standingCharge) * (1 - t3.discountRate) * t3.vatRate,
            discountAmount = (12900.0 * t3.ratePerKwh + 30 * t3.standingCharge) * t3.discountRate,
            totalAmount = ((12900.0 * t3.ratePerKwh + 30 * t3.standingCharge) * (1 - t3.discountRate)) * (1 + t3.vatRate),
            status = "UNPAID",
            dueDate = now + 14 * dayMillis,
            generatedDate = now
        )
        utilityDao.insertBill(bill2)

        // 6. Seed payments
        val pay1 = Payment(
            billId = b1Id,
            customerId = customerIds[0],
            amount = bill1.totalAmount,
            paymentDate = now - 20 * dayMillis,
            paymentMethod = "DIRECT_DEBIT"
        )
        utilityDao.insertPayment(pay1)

        // 7. Seed Admin User
        val admin = AppUser(
            username = "Admin Staff",
            role = "ADMIN",
            lastLogin = now
        )
        utilityDao.insertUser(admin)

        // 8. Seed Saved Report
        val report = SavedReport(
            title = "Annual Grid Peak Demand & Load Analysis",
            reportType = "ENERGY_USAGE",
            parameters = "Date Range: Jan-Jun 2026 | All Meter Classes",
            generatedBy = "System Core Daemon",
            fileUrl = "simulated_report_energy_load_2026.pdf"
        )
        utilityDao.insertReport(report)

        Log.d("UtilityRepository", "Seeding complete.")
    }
}
