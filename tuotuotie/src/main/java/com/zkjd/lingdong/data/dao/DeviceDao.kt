package com.zkjd.lingdong.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.zkjd.lingdong.data.converter.ConnectionStateConverter
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDevice(device: Device): Long
    
    @Update
    fun updateDevice(device: Device): Int
    
    @Delete
    fun deleteDevice(device: Device): Int
    
    @Query("DELETE FROM devices WHERE macAddress = :macAddress")
    fun deleteDeviceByMacAddress(macAddress: String): Int


    @Query("SELECT * FROM devices WHERE macAddress = :macAddress ")
    fun getDevice(macAddress: String): Flow<Device?>

    @Query("SELECT * FROM devices ORDER BY createdAt ASC")
    fun getAllDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices ORDER BY createdAt ASC")
    fun getAllDevicesAsList(): List<Device>
    
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    fun getDeviceByMacAddress(macAddress: String): Device?
    
    @Query("SELECT COUNT(*) FROM devices")
    fun getDeviceCount(): Int
    
    @Query("UPDATE devices SET lastConnectionState = :state WHERE macAddress = :macAddress")
    fun updateConnectionState(macAddress: String, @TypeConverters(ConnectionStateConverter::class) state: ConnectionState): Int
    
    @Query("UPDATE devices SET connectedLedColor = :color WHERE macAddress = :macAddress")
    fun updateConnectedLedColor(macAddress: String, color: Int): Int
    
    @Query("UPDATE devices SET reconnectingLedColor = :color WHERE macAddress = :macAddress")
    fun updateReconnectingLedColor(macAddress: String, color: Int): Int
    
    @Query("UPDATE devices SET isAntiMisoperationEnabled = :enabled WHERE macAddress = :macAddress")
    fun updateAntiMisoperation(macAddress: String, enabled: Boolean): Int

    @Query("UPDATE devices SET musicCan = :enabled WHERE macAddress = :macAddress")
    fun updatemusicCan(macAddress: String, enabled: Boolean): Int

    @Query("UPDATE devices SET isAppOpen1 = :enabled WHERE macAddress = :macAddress")
    fun updateIsAppOpen1(macAddress: String, enabled: Boolean): Int

    @Query("UPDATE devices SET isAppOpen2 = :enabled WHERE macAddress = :macAddress")
    fun updateIsAppOpen2(macAddress: String, enabled: Boolean): Int

    @Query("UPDATE devices SET musicName = :value WHERE macAddress = :macAddress")
    fun updatemusicName(macAddress: String, value: String): Int
    
    @Query("UPDATE devices SET name = :name WHERE macAddress = :macAddress")
    fun updateDeviceName(macAddress: String, name: String): Int
    
    @Query("UPDATE devices SET batteryLevel = :level WHERE macAddress = :macAddress")
    fun updateBatteryLevel(macAddress: String, level: Int): Int

    @Query("UPDATE devices SET returnControl = :value WHERE macAddress = :macAddress")
    fun updateReturnControl(macAddress: String, value: Int): Int

    @Query("UPDATE devices SET preventAccidental = :value WHERE macAddress = :macAddress")
    fun updatePreventAccidental(macAddress: String, value: Int): Int

    @Query("UPDATE devices SET needAutoConnect = :needAutoConnect WHERE macAddress = :macAddress")
    fun updateDeviceAutoConnect(macAddress: String, needAutoConnect:Boolean): Int
} 