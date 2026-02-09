package com.smartlife.fragrance.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartlife.fragrance.data.dao.FragranceDeviceDao
import com.smartlife.fragrance.data.model.FragranceDevice

/**
 * 香氛设备数据库
 * 
 * 配置了AutoMigration支持自动升级
 */
@TypeConverters(Converters::class, com.smartlife.fragrance.data.converter.ConnectionStateConverter::class)
@Database(
    entities = [FragranceDevice::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
        // 当升级到版本2时，取消注释下面这行：
        // AutoMigration(from = 1, to = 2)
    ]
)
abstract class FragranceDatabase : RoomDatabase() {
    
    abstract fun fragranceDeviceDao(): FragranceDeviceDao
    
    companion object {
        @Volatile
        private var INSTANCE: FragranceDatabase? = null
        
        private const val DATABASE_NAME = "fragrance_database"
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): FragranceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FragranceDatabase::class.java,
                    DATABASE_NAME
                )
                // 使用AutoMigration，而不是fallbackToDestructiveMigration
                // 如果需要复杂迁移，可以在这里添加：.addMigrations(...)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

