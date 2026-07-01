package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "tariffs")
data class Tariff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val ratePerKwh: Double, // standard rate in $ or £
    val peakRatePerKwh: Double,
    val offPeakRatePerKwh: Double,
    val standingCharge: Double, // daily standing charge
    val vatRate: Double, // e.g., 0.05 for 5% VAT
    val discountRate: Double // e.g., 0.02 for 2% discount
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = Tariff::class,
            parentColumns = ["id"],
            childColumns = ["tariffId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["tariffId"])]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val email: String,
    val phone: String,
    val tariffId: Long?,
    val registrationDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "smart_meters",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class SmartMeter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: String, // Unique alpha-numeric serial
    val customerId: Long?,
    val address: String,
    val currentReading: Double, // in kWh
    val previousReading: Double, // in kWh
    val status: String, // "ONLINE", "OFFLINE", "MAINTENANCE"
    val installationDate: Long = System.currentTimeMillis(),
    val meterType: String = "Electricity"
)

@Entity(
    tableName = "meter_readings",
    foreignKeys = [
        ForeignKey(
            entity = SmartMeter::class,
            parentColumns = ["id"],
            childColumns = ["meterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["meterId"])]
)
data class MeterReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val reading: Double, // cumulative reading in kWh
    val demandKw: Double, // demand in kW (instantaneous rate)
    val cost: Double, // calculated cost for this reading step
    val isPeak: Boolean = false
)

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SmartMeter::class,
            parentColumns = ["id"],
            childColumns = ["meterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["meterId"])]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val meterId: Long?,
    val invoiceNumber: String,
    val billingPeriodStart: Long,
    val billingPeriodEnd: Long,
    val usageKwh: Double,
    val baseCost: Double,
    val standingChargeCost: Double,
    val vatAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val status: String, // "PAID", "UNPAID", "OVERDUE"
    val dueDate: Long,
    val generatedDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"]), Index(value = ["customerId"])]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val customerId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String // "CARD", "BANK_TRANSFER", "DIRECT_DEBIT", "CASH"
)

@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val role: String, // "ADMIN", "OPERATOR", "VIEWER"
    val lastLogin: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_reports")
data class SavedReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val reportType: String, // "ENERGY_USAGE", "REVENUE", "CUSTOMER_SUMMARY"
    val parameters: String, // simple query details description
    val generatedBy: String,
    val fileUrl: String, // simulated URL or internal storage URI
    val generatedDate: Long = System.currentTimeMillis()
)
