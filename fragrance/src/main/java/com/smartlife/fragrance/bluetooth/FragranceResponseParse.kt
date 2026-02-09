package com.smartlife.fragrance.bluetooth

import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.Gear
import com.smartlife.fragrance.data.model.Mode
import com.smartlife.fragrance.data.model.PowerState
import timber.log.Timber

object FragranceResponseParse {
    const val TAG = "FragranceResponseParse"
    val HEADER_NOTIFICATION: Byte = 0xFF.toByte()

    /**
     * 计算校验和
     * 校验和 = 所有字节的累加和（不包括校验和本身）取低8位
     */
    private fun calculateChecksum(data: ByteArray, endIndex: Int): Byte {
        var sum = 0
        for (i in 0 until endIndex) {
            sum += data[i].toInt() and 0xFF
        }
        return (sum and 0xFF).toByte()
    }

    /**
     * 验证数据包校验和
     */
    private fun verifyChecksum(byteArray: ByteArray): Boolean {
        if (byteArray.size < 17) {
            return false // 数据包长度不足
        }
        val expectedChecksum = calculateChecksum(byteArray, byteArray.size - 1)
        val actualChecksum = byteArray[byteArray.size - 1]
        return expectedChecksum == actualChecksum
    }

    /**
     * 将RGB字节转换为十六进制字符串（格式: "GGRRBB"）
     * 协议要求存储格式为GGRRBB（绿色、红色、蓝色）
     */
    private fun rgbToHexString(r: Byte, g: Byte, b: Byte): String {
        // 数据包中顺序是R, G, B，但存储格式要求是GGRRBB
        return String.format("%02X%02X%02X", r.toInt() and 0xFF,g.toInt() and 0xFF,b.toInt() and 0xFF)
    }

    /**
     * 返回的FragranceDevice只是用来放解析后的数据，不要用来存储和传递FragranceDevice
     * 
     * 数据包格式：
     * [0] header (0xFF)
     * [1] cmd (0x00)
     * [2] length (数据长度)
     * [3] 开关 (0x00=关, 0x01=开)
     * [4] 模式 (0x01=待机, 0x02=香型A, 0x03=香型B)
     * [5] 档位 (0x01=低档, 0x02=中档, 0x03=高档)
     * [6] A槽状态 (0x00=未插, 0x01=已插)
     * [7] A槽香薰名字 (0x01/0x02/0x03)
     * [8] A槽香薰余量 (0-100)
     * [9] B槽状态 (0x00=未插, 0x01=已插)
     * [10] B槽香薰名字 (0x01/0x02/0x03)
     * [11] B槽香薰余量 (0-100)
     * [12] 灯光开关 (0x00=关, 0x01=开)
     * [13] RGB_R
     * [14] RGB_G
     * [15] RGB_B
     * [16] 校验和
     */
    fun parse(macAddress: String, name: String, byteArray: ByteArray): FragranceDevice {
        // 验证数据包最小长度（至少需要16字节，不包括校验和）
        if (byteArray.size < 16) {
            // 数据包长度不足，返回默认值
            Timber.tag(TAG).e("长度错误")
            return FragranceDevice(
                macAddress = macAddress,
                deviceName = name,
                powerState = PowerState.OFF,
                mode = Mode.STANDBY,
                gear = Gear.LOW
            )
        }

        // 验证校验和（可选，根据实际需求决定是否启用）
        // if (byteArray.size >= 17 && !verifyChecksum(byteArray)) {
        //     // 校验和失败，记录日志但不阻止解析
        // }

        // 解析基础字段
        val switchValue = byteArray[3].toInt() and 0xFF
        val powerState = when (switchValue) {
            0x01 -> PowerState.ON
            0x00, 0x02 -> PowerState.OFF
            else -> PowerState.OFF
        }
        val mode = Mode.fromValue(byteArray[4].toInt() and 0xFF)
        val gear = Gear.fromValue(byteArray[5].toInt() and 0xFF)

        // 解析A槽信息
        val slotAInserted = (byteArray[6].toInt() and 0xFF) == 0x01
        val slotAFragranceName = (byteArray[7].toInt() and 0xFF).toString()
        val slotAFragranceLevel = byteArray[8].toInt() and 0xFF

        // 解析B槽信息
        val slotBInserted = (byteArray[9].toInt() and 0xFF) == 0x01
        val slotBFragranceName = (byteArray[10].toInt() and 0xFF).toString()
        val slotBFragranceLevel = byteArray[11].toInt() and 0xFF

        // 解析灯光开关
        val lightSwitch = (byteArray[12].toInt() and 0xFF) == 0x01

        // 解析RGB颜色
        val rgbR = byteArray[13]
        val rgbG = byteArray[14]
        val rgbB = byteArray[15]
        val lightColor = rgbToHexString(rgbR, rgbG, rgbB)


        Timber.tag(TAG).i("powerState $powerState,mode $mode gear $gear,slotACombined \n" +
                "slotAInserted $slotAInserted,slotAFragranceName $slotAFragranceName, slotAFragranceLevel $slotAFragranceLevel, \n" +
                "slotBInserted $slotBInserted,slotBFragranceName $slotBFragranceName, slotBFragranceLevel $slotBFragranceLevel,\n" +
                "lightSwitch $lightSwitch,  lightColor $lightColor ")
        return FragranceDevice(
            macAddress = macAddress,
            deviceName = name,
            powerState = powerState,
            mode = mode,
            gear = gear,
            lightColor = lightColor,
            slotAFragranceName = slotAFragranceName,
            slotBFragranceName = slotBFragranceName,
            lightSwitch = lightSwitch
        )
    }
}