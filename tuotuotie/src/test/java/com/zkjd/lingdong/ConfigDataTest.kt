package com.zkjd.lingdong

import com.mine.baselibrary.util.bytesUtil.getBitsRange
import org.junit.Test
import org.junit.Assert.*

class ConfigDataTest {

    @Test
    fun testGetBitFromByteArray() {
        // 给定的字节数组
        val byteArray = byteArrayOf(
            0x80.toByte(), 0x81.toByte(), 0xCC.toByte(), 0xA0.toByte(), 0xF6.toByte(), 0xF2.toByte(), 0x73.toByte(), 0xD3.toByte(),
            0x94.toByte(), 0x86.toByte(), 0x30.toByte(), 0x70.toByte(), 0x66.toByte(), 0x55.toByte(), 0x10.toByte(), 0x20.toByte(),
            0x16.toByte(), 0xF0.toByte(), 0x8F.toByte(), 0x95.toByte(), 0x17.toByte(), 0x44.toByte(), 0x13.toByte(), 0x09.toByte(),
            0x27.toByte(), 0xB2.toByte(), 0x00.toByte(), 0x8C.toByte(), 0x8E.toByte(), 0x04.toByte(), 0x28.toByte(), 0x48.toByte(),
            0x44.toByte(), 0x15.toByte(), 0x1E.toByte(), 0x23.toByte(), 0xD5.toByte(), 0x0D.toByte(), 0x40.toByte(), 0x2E.toByte(),
            0x50.toByte(), 0x33.toByte(), 0x9C.toByte(), 0x29.toByte(), 0xBE.toByte(), 0x01.toByte(), 0x6C.toByte(), 0x58.toByte(),
            0x94.toByte(), 0x00.toByte(), 0x7D.toByte(), 0xAB.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0x0F.toByte(),
            0xFE.toByte(), 0x83.toByte(), 0x60.toByte(), 0x00.toByte(), 0xAC.toByte(), 0x27.toByte(), 0x61.toByte(), 0x03.toByte(),
            0x69.toByte(), 0x00.toByte()
        )


        val bit0 = byteArray.getBitsRange(8,0,0)

        // 验证结果
        // 0x94 = 10010100 (二进制)
        // 第0个二进制位（最低位）是0
        assertEquals("第8个字节的第0个二进制位应该是1", 0, bit0)

    }

    /**
     * 辅助方法：从字节数组中获取指定字节的指定位
     * @param byteArray 字节数组
     * @param byteIndex 字节索引（从0开始）
     * @param bitIndex 位索引（从0开始，0是最低位）
     * @return 位的值（0或1）
     */
    private fun getBit(byteArray: ByteArray, byteIndex: Int, bitIndex: Int): Int {
        if (byteIndex >= byteArray.size || bitIndex < 0 || bitIndex > 7) {
            throw IllegalArgumentException("索引超出范围")
        }
        val byte = byteArray[byteIndex].toInt() and 0xFF
        return (byte shr bitIndex) and 0x01
    }

    @Test
    fun testGetBitHelperMethod() {
        val byteArray = byteArrayOf(
            0x80.toByte(), 0x81.toByte(), 0xCC.toByte(), 0xA0.toByte(), 0xF6.toByte(), 0xF2.toByte(), 0x73.toByte(), 0xD3.toByte()
        )

        // 测试获取第8个字节（索引7）的第0个二进制位
        val bit0 = getBit(byteArray, 7, 0)
        assertEquals("使用辅助方法获取第8个字节的第0个二进制位应该是1", 1, bit0)

        // 测试获取第8个字节的其他位
        val bit1 = getBit(byteArray, 7, 1)  // 应该是1
        val bit2 = getBit(byteArray, 7, 2)  // 应该是0
        val bit3 = getBit(byteArray, 7, 3)  // 应该是0
        val bit4 = getBit(byteArray, 7, 4)  // 应该是1
        val bit5 = getBit(byteArray, 7, 5)  // 应该是0
        val bit6 = getBit(byteArray, 7, 6)  // 应该是1
        val bit7 = getBit(byteArray, 7, 7)  // 应该是1

        assertEquals("第1位应该是1", 1, bit1)
        assertEquals("第2位应该是0", 0, bit2)
        assertEquals("第3位应该是0", 0, bit3)
        assertEquals("第4位应该是1", 1, bit4)
        assertEquals("第5位应该是0", 0, bit5)
        assertEquals("第6位应该是1", 1, bit6)
        assertEquals("第7位应该是1", 1, bit7)

        println("第8个字节(0xD3)的所有位: $bit7$bit6$bit5$bit4$bit3$bit2$bit1$bit0")
    }
    
    @Test
    fun testGetBitsFromThirdByte() {
        // 给定的字节数组
        val byteArray = byteArrayOf(
            0x80.toByte(), 0x81.toByte(), 0xCC.toByte(), 0xA0.toByte(), 0xF6.toByte(), 0xF2.toByte(), 0x73.toByte(), 0xD3.toByte(),
            0x94.toByte(), 0x86.toByte(), 0x30.toByte(), 0x70.toByte(), 0x66.toByte(), 0x55.toByte(), 0x10.toByte(), 0x20.toByte(),
            0x16.toByte(), 0xF0.toByte(), 0x8F.toByte(), 0x95.toByte(), 0x17.toByte(), 0x44.toByte(), 0x13.toByte(), 0x09.toByte(),
            0x27.toByte(), 0xB2.toByte(), 0x00.toByte(), 0x8C.toByte(), 0x8E.toByte(), 0x04.toByte(), 0x28.toByte(), 0x48.toByte(),
            0x44.toByte(), 0x15.toByte(), 0x1E.toByte(), 0x23.toByte(), 0xD5.toByte(), 0x0D.toByte(), 0x40.toByte(), 0x2E.toByte(),
            0x50.toByte(), 0x33.toByte(), 0x9C.toByte(), 0x29.toByte(), 0xBE.toByte(), 0x01.toByte(), 0x6C.toByte(), 0x58.toByte(),
            0x94.toByte(), 0x00.toByte(), 0x7D.toByte(), 0xAB.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0x0F.toByte(),
            0xFE.toByte(), 0x83.toByte(), 0x60.toByte(), 0x00.toByte(), 0xAC.toByte(), 0x27.toByte(), 0x61.toByte(), 0x03.toByte(),
            0x69.toByte(), 0x00.toByte()
        )
        
        // 使用ByteArray.getBitsRange获取第三个字节（索引为3）的3-5位的值
        val bits3to5 = byteArray.getBitsRange(3, 3, 5)  // 获取第3-5位
        
        // 验证结果
        // 0xA0 =  10 100 000 (二进制)
        // 第3-5位是: 100
        assertEquals("第三个字节的第3-5位应该是4", 4, bits3to5)

    }
}