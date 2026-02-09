package com.zkjd.lingdong.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import com.zkjd.lingdong.data.converter.ButtonTypeConverter
import com.zkjd.lingdong.model.ButtonFunctionMapping
import com.zkjd.lingdong.model.ButtonType
import kotlinx.coroutines.flow.Flow

@Dao
interface ButtonFunctionMappingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMapping(mapping: ButtonFunctionMapping): Long
    
    @Query("SELECT * FROM button_function_mappings WHERE deviceMacAddress = :deviceMacAddress")
    fun getMappingsForDevice(deviceMacAddress: String): Flow<List<ButtonFunctionMapping>>
    
    @Query("SELECT * FROM button_function_mappings WHERE deviceMacAddress = :deviceMacAddress AND buttonType = :buttonType")
    fun getMappingForDeviceAndButton(
        deviceMacAddress: String, 
        @TypeConverters(ButtonTypeConverter::class) buttonType: ButtonType
    ): ButtonFunctionMapping?
    
    @Query("UPDATE button_function_mappings SET functionId = :functionId, customParams = :customParams WHERE deviceMacAddress = :deviceMacAddress AND buttonType = :buttonType")
    fun updateMapping(
        deviceMacAddress: String, 
        @TypeConverters(ButtonTypeConverter::class) buttonType: ButtonType, 
        functionId: String?, 
        customParams: String?
    ): Int
    
    @Transaction
    suspend fun setMapping(deviceMacAddress: String, buttonType: ButtonType, functionId: String?, customParams: String? = null) {
        val existing = getMappingForDeviceAndButton(deviceMacAddress, buttonType)
        if (existing == null) {
            insertMapping(
                ButtonFunctionMapping(
                    deviceMacAddress = deviceMacAddress,
                    buttonType = buttonType,
                    functionId = functionId,
                    customParams = customParams
                )
            )
        } else {
            updateMapping(deviceMacAddress, buttonType, functionId, customParams)
        }
    }
    
    @Query("DELETE FROM button_function_mappings WHERE deviceMacAddress = :deviceMacAddress")
    fun deleteAllMappingsForDevice(deviceMacAddress: String): Int
    
    @Query("DELETE FROM button_function_mappings WHERE deviceMacAddress = :deviceMacAddress AND buttonType = :buttonType")
    fun deleteMappingForDeviceAndButton(
        deviceMacAddress: String,
        @TypeConverters(ButtonTypeConverter::class) buttonType: ButtonType
    ): Int
} 