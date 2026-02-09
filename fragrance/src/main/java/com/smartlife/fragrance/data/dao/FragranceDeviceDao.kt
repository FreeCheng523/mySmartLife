package com.smartlife.fragrance.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.ConnectionState
import kotlinx.coroutines.flow.Flow

/**
 * 香氛设备数据访问对象接口
 */
@Dao
interface FragranceDeviceDao {
    
    /**
     * 插入设备
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: FragranceDevice): Long
    
    /**
     * 更新设备
     */
    @Update
    suspend fun updateDevice(device: FragranceDevice): Int
    
    /**
     * 删除设备
     */
    @Delete
    suspend fun deleteDevice(device: FragranceDevice): Int
    
    /**
     * 根据MAC地址删除设备
     */
    @Query("DELETE FROM fragrance_device WHERE macAddress = :macAddress")
    suspend fun deleteDeviceByMacAddress(macAddress: String): Int
    
    /**
     * 根据MAC地址查询设备（Flow方式）
     */
    @Query("SELECT * FROM fragrance_device WHERE macAddress = :macAddress")
    fun getDevice(macAddress: String): Flow<FragranceDevice?>
    
    /**
     * 根据MAC地址查询设备（非Flow方式）
     */
    @Query("SELECT * FROM fragrance_device WHERE macAddress = :macAddress")
    suspend fun getDeviceByMac(macAddress: String): FragranceDevice?
    
    /**
     * 获取所有设备列表（Flow方式）
     */
    @Query("SELECT * FROM fragrance_device ORDER BY createdAt ASC")
    fun getAllDevices(): Flow<List<FragranceDevice>>
    
    /**
     * 获取所有设备列表（非Flow方式）
     */
    @Query("SELECT * FROM fragrance_device ORDER BY createdAt ASC")
    fun getAllDevicesAsList(): List<FragranceDevice>
    
    /**
     * 获取设备数量
     */
    @Query("SELECT COUNT(*) FROM fragrance_device")
    suspend fun getDeviceCount(): Int
    
    /**
     * 更新设备连接状态
     */
    @Query("UPDATE fragrance_device SET connectionState = :state WHERE macAddress = :macAddress")
    suspend fun updateConnectionState(macAddress: String, state: ConnectionState): Int
    
    /**
     * 更新设备自动连接状态
     */
    @Query("UPDATE fragrance_device SET needAutoConnect = :needAutoConnect WHERE macAddress = :macAddress")
    suspend fun updateDeviceAutoConnect(macAddress: String, needAutoConnect: Boolean): Int
}

