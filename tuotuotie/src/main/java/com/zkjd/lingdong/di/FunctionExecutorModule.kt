package com.zkjd.lingdong.di

import android.content.Context
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.service.FunctionExecutor
import com.zkjd.lingdong.service.FunctionExecutorImpl
import com.zkjd.lingdong.service.executor.AppFunctionExecutor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 功能执行器依赖注入模块
 * 提供各种功能执行器的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FunctionExecutorModule {

    /**
     * 绑定FunctionExecutor接口到其实现类
     */
    @Binds
    @Singleton
    abstract fun bindFunctionExecutor(impl: FunctionExecutorImpl): FunctionExecutor

    /**
     * 提供各种功能执行器的依赖对象
     */
    companion object {
        /**
         * 提供应用功能执行器
         */
        @Provides
        @Singleton
        fun provideAppFunctionExecutor(@ApplicationContext context: Context,deviceRepository: DeviceRepository): AppFunctionExecutor {
            return AppFunctionExecutor(context,deviceRepository)
        }
    }
} 