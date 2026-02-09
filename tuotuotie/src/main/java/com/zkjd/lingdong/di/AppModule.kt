package com.zkjd.lingdong.di

import android.content.Context
import com.zkjd.lingdong.bluetooth.TuoTuoTieAbsBleManager
import com.zkjd.lingdong.bluetooth.TuoTuoTieBleManagerImp
import com.zkjd.lingdong.data.AppDatabase
import com.zkjd.lingdong.data.dao.ButtonFunctionMappingDao
import com.zkjd.lingdong.data.dao.DeviceDao
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.repository.DeviceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用程序级别的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * 提供应用程序Context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    // 提供数据库实例
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    // 提供设备DAO
    @Provides
    @Singleton
    fun provideDeviceDao(database: AppDatabase): DeviceDao {
        return database.deviceDao()
    }
    
    // 提供按键功能映射DAO
    @Provides
    @Singleton
    fun provideButtonFunctionMappingDao(database: AppDatabase): ButtonFunctionMappingDao {
        return database.buttonFunctionMappingDao()
    }
}

/**
 * 提供接口与实现类绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    
    @Binds
    abstract fun bindBleManager(
        bleManagerImpl: TuoTuoTieBleManagerImp
    ): TuoTuoTieAbsBleManager
    
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository
} 