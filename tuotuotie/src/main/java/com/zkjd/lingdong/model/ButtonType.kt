package com.zkjd.lingdong.model

enum class ButtonType {
    SHORT_PRESS,
    LONG_PRESS,
    DOUBLE_CLICK,
    LEFT_ROTATE,
    RIGHT_ROTATE,
    FONE_PESS,
    FTWO_PESS;

    companion object {
        val ROTATE_TYPES = listOf(LEFT_ROTATE, RIGHT_ROTATE)
    }
} 