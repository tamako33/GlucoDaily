package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 血糖胰岛素数据访问对象 (InsulinDao)：
 *
 * 架构契约与数据流向：
 * 1. 响应式 Flow 数据流：[getAllRecords] 向上层暴露实时响应式流，数据库有任何写入即自动通知订阅方更新；
 * 2. 协程原子写入：全部增删改操作均为挂起函数 (suspend)，严格要求在 IO 调度器运行；
 * 3. 冲突解决策略：全部插入采用 [OnConflictStrategy.REPLACE]，保障同日期记录幂等覆盖与原子合并。
 */
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
