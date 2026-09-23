package com.example.data

import kotlinx.coroutines.flow.Flow

class InsulinRepository(private val insulinDao: InsulinDao) {
    val allRecords: Flow<List<InsulinRecord>> = insulinDao.getAllRecords()

    fun getRecordByDate(date: String): Flow<InsulinRecord?> = insulinDao.getRecordByDate(date)

    suspend fun insertRecord(record: InsulinRecord) = insulinDao.insertRecord(record)

    suspend fun insertAll(records: List<InsulinRecord>) = insulinDao.insertAll(records)

    suspend fun ensureTodayRecord(today: String) {
        if (insulinDao.getRecordByDateDirect(today) == null) {
            insulinDao.insertRecord(InsulinRecord(date = today))
        }
    }

    suspend fun getRecordCount(): Int = insulinDao.getCount()

    suspend fun deleteRecord(record: InsulinRecord) = insulinDao.deleteRecord(record)

    suspend fun deleteByDate(date: String) = insulinDao.deleteByDate(date)

    suspend fun resetToInitialData() {
        insulinDao.clearAll()
        insulinDao.insertAll(AppDatabase.INITIAL_MOCK_DATA)
    }

    suspend fun clearAll() {
        insulinDao.clearAll()
    }

    suspend fun migrateLegacyDefaultMedNames() {
        insulinDao.updateLegacyBfMed()
        insulinDao.updateLegacyLunchMed()
        insulinDao.updateLegacyDinnerMed()
        insulinDao.updateLegacyNightMed()
        insulinDao.migratePreBfToFasting()
        insulinDao.clearPreBfBG()
    }
}
