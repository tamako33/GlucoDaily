package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinDao {
    @Query("SELECT * FROM insulin_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<InsulinRecord>>

    @Query("SELECT * FROM insulin_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: String): Flow<InsulinRecord?>

    @Query("SELECT * FROM insulin_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDateDirect(date: String): InsulinRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: InsulinRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InsulinRecord>)

    @Delete
    suspend fun deleteRecord(record: InsulinRecord)

    @Query("SELECT COUNT(*) FROM insulin_records")
    suspend fun getCount(): Int

    @Query("DELETE FROM insulin_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM insulin_records")
    suspend fun clearAll()

    @Query("UPDATE insulin_records SET bfMedName = '胰岛素' WHERE bfMedName = '门冬胰岛素'")
    suspend fun updateLegacyBfMed()

    @Query("UPDATE insulin_records SET lunchMedName = '胰岛素' WHERE lunchMedName = '门冬胰岛素'")
    suspend fun updateLegacyLunchMed()

    @Query("UPDATE insulin_records SET dinnerMedName = '胰岛素' WHERE dinnerMedName = '门冬胰岛素'")
    suspend fun updateLegacyDinnerMed()

    @Query("UPDATE insulin_records SET nightMedName = '胰岛素' WHERE nightMedName = '甘精胰岛素'")
    suspend fun updateLegacyNightMed()

    @Query("UPDATE insulin_records SET fastingBG = preBfBG WHERE fastingBG IS NULL AND preBfBG IS NOT NULL")
    suspend fun migratePreBfToFasting()

    @Query("UPDATE insulin_records SET preBfBG = NULL WHERE preBfBG IS NOT NULL")
    suspend fun clearPreBfBG()
}
