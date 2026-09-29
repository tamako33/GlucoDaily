package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 血糖胰岛素应用 Room 数据库中心 (AppDatabase)：
 *
 * 架构契约与版本演进：
 * 1. 单例持有：基于双重校验锁机制 (@Volatile + synchronized) 维护全局唯一实例 [INSTANCE]；
 * 2. 数据库版本迁移历程 (Version 1 -> 6)：
 *    - v1: 基础餐前餐后血糖及胰岛素字段；
 *    - v2: 增加用药名称与时机字段；
 *    - v3: 增加饮食与运动记录字段；
 *    - v4: 增加餐后多阶段扩展 JSON 字段 (postBfBGExtra 等)；
 *    - v5/v6: 结构平滑兼容迁移，提供完善的 [Migration] 策略保障旧版本用户数据零丢失；
 * 3. 初始演示数据 [INITIAL_MOCK_DATA]：为首次启动或恢复演示数据提供标准临床样例。
 */
@Database(entities = [InsulinRecord::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun insulinDao(): InsulinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val INITIAL_MOCK_DATA = listOf(
            InsulinRecord(
                date = "2026-09-01",
                fastingBG = 5.8f,
                postBfBG = 7.9f,
                bfMedName = "胰岛素",
                bfInsulin = 8f,
                bfDiet = "燕麦片半碗、水煮蛋1个、无糖纯豆浆",
                preLunchBG = 6.1f,
                postLunchBG = 8.4f,
                lunchMedName = "胰岛素",
                lunchInsulin = 6f,
                lunchDiet = "杂粮饭、清蒸鲈鱼、清炒西兰花",
                preDinnerBG = 5.8f,
                postDinnerBG = 7.6f,
                dinnerMedName = "胰岛素",
                dinnerInsulin = 6f,
                dinnerDiet = "荞麦面、蒜蓉生菜、水煮牛肉片",
                preNightBG = 6.4f,
                postNightBG = 6.0f,
                nightMedName = "胰岛素",
                bedtimeInsulin = 12f,
                nightDiet = "无糖温水一杯",
                notes = "状态良好，晨间慢跑"
            ),
            InsulinRecord(
                date = "2026-09-02",
                fastingBG = 6.2f,
                postBfBG = 8.5f,
                bfMedName = "胰岛素",
                bfInsulin = 8f,
                bfDiet = "全麦面包两片、低脂纯牛奶、煎荷包蛋",
                preLunchBG = 5.9f,
                postLunchBG = 7.9f,
                lunchMedName = "胰岛素",
                lunchInsulin = 6f,
                lunchDiet = "荞麦面半碗、番茄炒蛋、清炖鸡胸肉",
                preDinnerBG = 6.3f,
                postDinnerBG = 8.1f,
                dinnerMedName = "胰岛素",
                dinnerInsulin = 7f,
                dinnerDiet = "紫薯小半个、白灼大虾、炒油麦菜",
                preNightBG = 6.8f,
                postNightBG = 6.5f,
                nightMedName = "胰岛素",
                bedtimeInsulin = 12f,
                nightDiet = "",
                notes = "午餐吃了一碗杂粮饭"
            ),
            InsulinRecord(
                date = "2026-09-03",
                fastingBG = 5.4f,
                postBfBG = 7.2f,
                bfMedName = "胰岛素",
                bfInsulin = 8f,
                bfDiet = "玉米半根、鸡蛋羹一小碗、无糖杏仁奶",
                preLunchBG = 5.8f,
                postLunchBG = 8.0f,
                lunchMedName = "胰岛素",
                lunchInsulin = 6f,
                lunchDiet = "黑米饭小半碗、黑椒牛柳、白灼菜心",
                preDinnerBG = 5.6f,
                postDinnerBG = 7.3f,
                dinnerMedName = "胰岛素",
                dinnerInsulin = 6f,
                dinnerDiet = "清汤蔬菜豆腐锅、去皮鸡腿肉",
                preNightBG = 6.2f,
                postNightBG = 5.9f,
                nightMedName = "胰岛素",
                bedtimeInsulin = 12f,
                nightDiet = "",
                notes = "指标平稳"
            ),
            InsulinRecord(
                date = "2026-09-04",
                fastingBG = 6.7f,
                bfInsulin = 9f,
                postBfBG = 9.3f,
                lunchInsulin = 6f,
                postLunchBG = 8.6f,
                dinnerInsulin = 6f,
                postDinnerBG = 8.0f,
                bedtimeInsulin = 12f,
                notes = "前晚睡眠较浅，略微偏高"
            ),
            InsulinRecord(
                date = "2026-09-05",
                fastingBG = 5.5f,
                bfInsulin = 8f,
                postBfBG = 7.6f,
                lunchInsulin = 6f,
                postLunchBG = 7.8f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.2f,
                bedtimeInsulin = 12f,
                notes = "晚餐后散步40分钟"
            ),
            InsulinRecord(
                date = "2026-09-06",
                fastingBG = 5.9f,
                bfInsulin = 8f,
                postBfBG = 8.1f,
                lunchInsulin = 6f,
                postLunchBG = 8.3f,
                dinnerInsulin = 7f,
                postDinnerBG = 7.5f,
                bedtimeInsulin = 12f,
                notes = "无低血糖"
            ),
            InsulinRecord(
                date = "2026-09-07",
                fastingBG = 5.7f,
                bfInsulin = 8f,
                postBfBG = 7.5f,
                lunchInsulin = 6f,
                postLunchBG = 8.0f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.6f,
                bedtimeInsulin = 12f,
                notes = "指标平稳达标"
            ),
            InsulinRecord(
                date = "2026-09-08",
                fastingBG = 5.6f,
                bfInsulin = 8f,
                postBfBG = 7.7f,
                lunchInsulin = 6f,
                postLunchBG = 8.1f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.4f,
                bedtimeInsulin = 12f,
                notes = "控糖平稳，适量补充水分"
            ),
            InsulinRecord(
                date = "2026-09-09",
                fastingBG = 6.1f,
                bfInsulin = 8f,
                postBfBG = 8.3f,
                lunchInsulin = 6f,
                postLunchBG = 7.8f,
                dinnerInsulin = 7f,
                postDinnerBG = 8.0f,
                bedtimeInsulin = 12f,
                notes = "晚餐外出就餐，多注射了1U"
            ),
            InsulinRecord(
                date = "2026-09-10",
                fastingBG = 5.7f,
                bfInsulin = 8f,
                postBfBG = 7.6f,
                lunchInsulin = 6f,
                postLunchBG = 7.9f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.3f,
                bedtimeInsulin = 12f,
                notes = "精神饱满，餐后轻度散步"
            ),
            InsulinRecord(
                date = "2026-09-11",
                fastingBG = 5.3f,
                bfInsulin = 8f,
                postBfBG = 7.4f,
                lunchInsulin = 6f,
                postLunchBG = 8.2f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.5f,
                bedtimeInsulin = 12f,
                notes = "饮食清淡"
            ),
            InsulinRecord(
                date = "2026-09-12",
                fastingBG = 6.4f,
                bfInsulin = 9f,
                postBfBG = 8.8f,
                lunchInsulin = 6f,
                postLunchBG = 8.5f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.9f,
                bedtimeInsulin = 12f,
                notes = "早餐略晚，加测血糖"
            ),
            InsulinRecord(
                date = "2026-09-13",
                fastingBG = 5.8f,
                bfInsulin = 8f,
                postBfBG = 7.8f,
                lunchInsulin = 6f,
                postLunchBG = 7.7f,
                dinnerInsulin = 6f,
                postDinnerBG = 7.1f,
                bedtimeInsulin = 12f,
                notes = "周末作息规律"
            ),
            InsulinRecord(
                date = "2026-09-14",
                fastingBG = 5.5f,
                postBfBG = 7.6f,
                bfMedName = "胰岛素",
                bfInsulin = 8f,
                bfDiet = "杂粮粥小碗、蒸蛋羹、凉拌黄瓜",
                preLunchBG = 5.8f,
                postLunchBG = 8.0f,
                lunchMedName = "胰岛素",
                lunchInsulin = 6f,
                lunchDiet = "糙米饭半碗、清炒虾仁、西兰花",
                preDinnerBG = 5.7f,
                postDinnerBG = 7.4f,
                dinnerMedName = "胰岛素",
                dinnerInsulin = 6f,
                dinnerDiet = "豆腐蔬菜汤、煎鸡胸肉片",
                preNightBG = 6.1f,
                postNightBG = 5.8f,
                nightMedName = "胰岛素",
                bedtimeInsulin = 12f,
                nightDiet = "",
                notes = "全天状态理想"
            ),
            InsulinRecord(
                date = "2026-09-15",
                fastingBG = 5.6f,
                postBfBG = 7.5f,
                bfMedName = "胰岛素",
                bfInsulin = 8f,
                bfDiet = "燕麦片、纯牛奶、水煮蛋",
                preLunchBG = 6.0f,
                postLunchBG = 8.1f,
                lunchMedName = "胰岛素",
                lunchInsulin = 6f,
                lunchDiet = "杂粮饭、清蒸鱼排、小白菜",
                preDinnerBG = null,
                postDinnerBG = null,
                dinnerMedName = "胰岛素",
                dinnerInsulin = null,
                dinnerDiet = "",
                preNightBG = null,
                postNightBG = null,
                nightMedName = "胰岛素",
                bedtimeInsulin = null,
                nightDiet = "",
                notes = "今日晨间午间监测完成"
            )
        )

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN postBfBGExtra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN postLunchBGExtra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN postDinnerBGExtra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN postNightBGExtra TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN bfExercise TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN lunchExercise TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN dinnerExercise TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN nightExercise TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE insulin_records ADD COLUMN itemTimesJson TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "insulin_tracker_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
