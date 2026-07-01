package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityDao {
    // Tariffs
    @Query("SELECT * FROM tariffs ORDER BY name ASC")
    fun getAllTariffs(): Flow<List<Tariff>>

    @Query("SELECT * FROM tariffs WHERE id = :id")
    suspend fun getTariffById(id: Long): Tariff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: Tariff): Long

    @Update
    suspend fun updateTariff(tariff: Tariff)

    @Delete
    suspend fun deleteTariff(tariff: Tariff)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // Smart Meters
    @Query("SELECT * FROM smart_meters ORDER BY meterId ASC")
    fun getAllMeters(): Flow<List<SmartMeter>>

    @Query("SELECT * FROM smart_meters WHERE id = :id")
    suspend fun getMeterById(id: Long): SmartMeter?

    @Query("SELECT * FROM smart_meters WHERE customerId = :customerId")
    fun getMetersByCustomer(customerId: Long): Flow<List<SmartMeter>>

    @Query("SELECT * FROM smart_meters WHERE meterId LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchMeters(query: String): Flow<List<SmartMeter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeter(meter: SmartMeter): Long

    @Update
    suspend fun updateMeter(meter: SmartMeter)

    @Delete
    suspend fun deleteMeter(meter: SmartMeter)

    // Readings
    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY timestamp DESC")
    fun getReadingsForMeter(meterId: Long): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int): Flow<List<MeterReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading): Long

    // Bills
    @Query("SELECT * FROM bills ORDER BY generatedDate DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY generatedDate DESC")
    fun getBillsByCustomer(customerId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE invoiceNumber LIKE '%' || :query || '%'")
    fun searchBills(query: String): Flow<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    // Payments
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE billId = :billId")
    fun getPaymentsForBill(billId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    // Users
    @Query("SELECT * FROM app_users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<AppUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUser): Long

    // Reports
    @Query("SELECT * FROM saved_reports ORDER BY generatedDate DESC")
    fun getAllReports(): Flow<List<SavedReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SavedReport): Long
}
