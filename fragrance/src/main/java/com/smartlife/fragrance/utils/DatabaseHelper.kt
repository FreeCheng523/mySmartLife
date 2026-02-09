package com.smartlife.fragrance.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据库帮助类，用于确保所有数据库操作都在IO线程上执行
 */
object DatabaseHelper {
    /**
     * 在IO线程上执行数据库操作
     * @param block 要执行的操作块
     * @return 操作结果
     */
    suspend fun <T> executeOnIOThread(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }
}

