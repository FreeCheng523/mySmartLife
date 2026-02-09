package com.smartlife.fragrance.di

import android.content.Context
import com.smartlife.fragrance.bluetooth.FragranceAbsBleManager
import com.smartlife.fragrance.bluetooth.FragranceBleManager
import com.smartlife.fragrance.data.FragranceDatabase
import com.smartlife.fragrance.data.dao.FragranceDeviceDao
import com.smartlife.fragrance.repository.FragranceRepository
import com.smartlife.fragrance.repository.FragranceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 * 提供 FragranceDatabase 和 FragranceDeviceDao 的 Hilt 支持
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FragranceDatabase {
        return FragranceDatabase.Companion.getDatabase(context)
    }
    
    /**
     * 提供设备DAO
     */
    @Provides
    @Singleton
    fun provideFragranceDeviceDao(database: FragranceDatabase): FragranceDeviceDao {
        return database.fragranceDeviceDao()
    }
    
    /**
     * 提供 FragranceBleManager 实例
     */
    @Provides
    @Singleton
    fun provideFragranceBleManager(@ApplicationContext context: Context): FragranceAbsBleManager {
        return FragranceBleManager(context)
    }
}

/**
 * 提供接口与实现类绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    
    @Binds
    @Singleton
    abstract fun bindFragranceRepository(
        fragranceRepositoryImpl: FragranceRepositoryImpl
    ): FragranceRepository
}

