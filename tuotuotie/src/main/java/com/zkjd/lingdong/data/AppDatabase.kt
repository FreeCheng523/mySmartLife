package com.zkjd.lingdong.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zkjd.lingdong.data.dao.ButtonFunctionMappingDao
import com.zkjd.lingdong.data.dao.DeviceDao
import com.zkjd.lingdong.model.ButtonFunctionMapping
import com.zkjd.lingdong.model.Device

/**
 * 数据类型转换器
 */
@TypeConverters(Converters::class)
@Database(
    entities = [
        Device::class,
        ButtonFunctionMapping::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun deviceDao(): DeviceDao
    abstract fun buttonFunctionMappingDao(): ButtonFunctionMappingDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingdong_tie_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 