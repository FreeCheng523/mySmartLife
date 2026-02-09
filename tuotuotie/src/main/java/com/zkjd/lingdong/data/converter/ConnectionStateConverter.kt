package com.zkjd.lingdong.data.converter

import androidx.room.TypeConverter
import com.zkjd.lingdong.model.ConnectionState

class ConnectionStateConverter {
    @TypeConverter
    fun fromConnectionState(state: ConnectionState): String {
        return state.name
    }

    @TypeConverter
    fun toConnectionState(value: String): ConnectionState {
        return ConnectionState.valueOf(value)
    }
} 