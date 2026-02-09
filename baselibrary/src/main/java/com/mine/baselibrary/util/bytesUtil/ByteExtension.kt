package com.mine.baselibrary.util.bytesUtil

/**
 * 辅助方法：从字节中获取指定位范围的值
 * @param byte 字节值
 * @param startBit 起始位索引（从0开始）
 * @param endBit 结束位索引（从0开始，包含此位）
 * @return 指定位范围的值
 */
fun Byte.getBitsRange(startBit: Int, endBit: Int): Int {
    if (startBit < 0 || endBit > 7 || startBit > endBit) {
        throw IllegalArgumentException("位索引超出范围")
    }
    val byteValue = this.toInt() and 0xFF
    val mask = (1 shl (endBit - startBit + 1)) - 1  // 创建掩码
    return (byteValue shr startBit) and mask
}
