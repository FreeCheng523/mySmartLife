package com.smartlife.fragrance.bluetooth

import android.content.Context
import com.mine.baselibrary.bluetooth.TAG
import com.smartlife.fragrance.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class FragranceBleManager(override val appContext: Context) : FragranceAbsBleManager(appContext) {
   val TAG = "FragranceBleManager"
    /**
     * 发送命令到设备的通用方法
     * @param macAddress 设备MAC地址
     * @param command 命令数据包
     * @return 发送成功返回true，否则返回false
     */
    private suspend fun sendCommand(macAddress: String, command: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val gatt = deviceGattMap[macAddress] ?: run {
            Timber.tag(TAG).e("发送命令失败: 设备未连接 $macAddress")
            return@withContext false
        }

        val service = gatt.getService(mainServiceUUID) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到主服务 $macAddress")
            return@withContext false
        }

        val characteristic = service.getCharacteristic(characteristicWriteUuid) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到写入特征 $macAddress")
            return@withContext false
        }

        Timber.tag(TAG).w("发送命令: ${bytesToHex(command)} 到设备 $macAddress")

        // 使用带超时的写入
        return@withContext writeCharacteristicWithTimeout(gatt, characteristic, command)
    }

    override suspend fun setPowerState(macAddress: String, powerState: PowerState): Boolean {
        val command = FragranceCommandBuilder.buildPowerCommand(powerState)
        return sendCommand(macAddress, command)
    }

    override suspend fun setMode(macAddress: String, mode: Mode): Boolean {
        val command = FragranceCommandBuilder.buildModeCommand(mode)
        return sendCommand(macAddress, command)
    }

    override suspend fun setGear(macAddress: String, gear: Gear): Boolean {
        val command = FragranceCommandBuilder.buildGearCommand(gear)
        return sendCommand(macAddress, command)
    }

    override suspend fun restoreFactorySettings(macAddress: String): Boolean {
        val command = FragranceCommandBuilder.buildRestoreFactoryCommand()
        return sendCommand(macAddress, command)
    }

    override suspend fun setCarStartStopEnabled(macAddress: String, enabled: Boolean): Boolean {
        val command = FragranceCommandBuilder.buildCarStartStopEnabledCommand(enabled)
        return sendCommand(macAddress, command)
    }

    override suspend fun setCarStartStopCycle(macAddress: String, cycle: CarStartStopCycle): Boolean {
        val command = FragranceCommandBuilder.buildCarStartStopCycleCommand(cycle)
        return sendCommand(macAddress, command)
    }

    override suspend fun setLightMode(macAddress: String, lightMode: LightMode): Boolean {
        val command = FragranceCommandBuilder.buildLightModeCommand(lightMode)
        return sendCommand(macAddress, command)
    }

    override suspend fun setLightColor(macAddress: String, r: Int, g: Int, b: Int): Boolean {
        val command = FragranceCommandBuilder.buildLightColorCommand(r, g, b)
        return sendCommand(macAddress, command)
    }

    override suspend fun setLightColor(macAddress: String, color: Int): Boolean {
        // 将 Int 颜色值转换为 RGB 分量
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return setLightColor(macAddress, r, g, b)
    }

    override suspend fun setLightColor(macAddress: String, color: String): Boolean {
        val command = FragranceCommandBuilder.buildLightColorCommand(color)
        return sendCommand(macAddress, command)
    }

    override suspend fun setLightBrightness(macAddress: String, brightness: Int): Boolean {
        val command = FragranceCommandBuilder.buildLightBrightnessCommand(brightness)
        return sendCommand(macAddress, command)
    }

    override suspend fun setTimingDuration(macAddress: String, minutes: Int): Boolean {
        val command = FragranceCommandBuilder.buildTimingDurationCommand(minutes)
        return sendCommand(macAddress, command)
    }

    override suspend fun getProgramVersion(macAddress: String): Boolean {
        val command = FragranceCommandBuilder.buildGetProgramVersionCommand()
        return sendCommand(macAddress, command)
    }

    override suspend fun getDeviceStatus(macAddress: String): Boolean {
        val command = FragranceCommandBuilder.buildGetDeviceStatusCommand()
        return sendCommand(macAddress, command)
    }
}