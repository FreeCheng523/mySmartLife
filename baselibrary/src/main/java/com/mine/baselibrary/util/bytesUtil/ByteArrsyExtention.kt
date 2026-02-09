package com.mine.baselibrary.util.bytesUtil

// 字节数组转换为十六进制字符串,如“80 81 CC A0 F6”
fun ByteArray.bytesToHexString(): String {
    return this.joinToString(" ") { "%02X".format(it) }
}

/**
 * 将字节数组转换为二进制字符串格式
 * @param bytes 字节数组
 * @return 二进制字符串，如 "00000000 00000001 00000010"
 */
fun ByteArray.bytesToBinary(): String {
    val sb = StringBuilder()
    for (i in this.indices) {
        val b = this[i]
        // 将字节转换为8位二进制字符串，确保前导零
        sb.append(
            String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF))
                .replace(' ', '0')
        )

        // 如果不是最后一个字节，添加空格分隔
        if (i < this.size - 1) {
            sb.append(" ")
        }
    }
    return sb.toString()
}



/**
 * 从字节数组中获取指定位置字节的指定位范围的值
 * @param byteIndex 字节在数组中的索引
 * @param startBit 起始位索引（从0开始）
 * @param endBit 结束位索引（从0开始，包含此位）
 * @return 指定位范围的值
 * @throws IllegalArgumentException 当字节索引或位索引超出范围时
 */
fun ByteArray.getBitsRange(byteIndex: Int, startBit: Int, endBit: Int): Int {
    if (byteIndex < 0 || byteIndex >= this.size) {
        throw IllegalArgumentException("字节索引超出范围: $byteIndex，数组大小为 ${this.size}")
    }
    return this[byteIndex].getBitsRange(startBit, endBit)
}


