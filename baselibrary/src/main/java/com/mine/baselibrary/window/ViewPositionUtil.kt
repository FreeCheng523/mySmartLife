package com.mine.baselibrary.window

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/**
 * View 位置计算工具类
 * 用于计算 View 在 Window 中的位置坐标
 */
object ViewPositionUtil {

    /**
     * 获取 View 在 Window 中的位置
     * @param view 目标 View
     * @return Pair<Int, Int> 返回 (x, y) 坐标，单位为像素
     */
    @JvmStatic
    fun getViewPositionInWindow(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return Pair(location[0], location[1])
    }

    /**
     * 获取 View 在屏幕中的位置
     * @param view 目标 View
     * @return Pair<Int, Int> 返回 (x, y) 坐标，单位为像素
     */
    @JvmStatic
    fun getViewPositionOnScreen(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Pair(location[0], location[1])
    }

    /**
     * 获取 View 在 Window 中的位置和大小
     * @param view 目标 View
     * @return Rect 包含 left, top, right, bottom 坐标
     */
    @JvmStatic
    fun getViewRectInWindow(view: View): Rect {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
    }

    /**
     * 获取 View 在屏幕中的位置和大小
     * @param view 目标 View
     * @return Rect 包含 left, top, right, bottom 坐标
     */
    @JvmStatic
    fun getViewRectOnScreen(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
    }

    /**
     * 获取 View 的中心点在 Window 中的坐标
     * @param view 目标 View
     * @return Pair<Int, Int> 返回中心点 (x, y) 坐标
     */
    @JvmStatic
    fun getViewCenterInWindow(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return Pair(
            location[0] + view.width / 2,
            location[1] + view.height / 2
        )
    }

    /**
     * 获取 View 的中心点在屏幕中的坐标
     * @param view 目标 View
     * @return Pair<Int, Int> 返回中心点 (x, y) 坐标
     */
    @JvmStatic
    fun getViewCenterOnScreen(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Pair(
            location[0] + view.width / 2,
            location[1] + view.height / 2
        )
    }

    /**
     * 判断指定的坐标点是否在 View 的范围内（相对于 Window）
     * @param view 目标 View
     * @param x 坐标点 x
     * @param y 坐标点 y
     * @return Boolean 如果坐标点在 View 范围内返回 true
     */
    @JvmStatic
    fun isPointInView(view: View, x: Int, y: Int): Boolean {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return x >= location[0] &&
                x <= location[0] + view.width &&
                y >= location[1] &&
                y <= location[1] + view.height
    }

    /**
     * 获取 View 相对于父容器的位置（不包括 Window 偏移）
     * @param view 目标 View
     * @return Pair<Int, Int> 返回相对于父容器的 (x, y) 坐标
     */
    @JvmStatic
    fun getViewPositionInParent(view: View): Pair<Int, Int> {
        return Pair(view.left, view.top)
    }
}

/**
 * View 扩展函数：获取在 Window 中的位置
 * @return Pair<Int, Int> 返回 (x, y) 坐标
 */
fun View.getPositionInWindow(): Pair<Int, Int> {
    return ViewPositionUtil.getViewPositionInWindow(this)
}

/**
 * View 扩展函数：获取在屏幕中的位置
 * @return Pair<Int, Int> 返回 (x, y) 坐标
 */
fun View.getPositionOnScreen(): Pair<Int, Int> {
    return ViewPositionUtil.getViewPositionOnScreen(this)
}

/**
 * View 扩展函数：获取在 Window 中的位置和大小
 * @return Rect 包含 left, top, right, bottom 坐标
 */
fun View.getRectInWindow(): Rect {
    return ViewPositionUtil.getViewRectInWindow(this)
}

/**
 * View 扩展函数：获取在屏幕中的位置和大小
 * @return Rect 包含 left, top, right, bottom 坐标
 */
fun View.getRectOnScreen(): Rect {
    return ViewPositionUtil.getViewRectOnScreen(this)
}

/**
 * View 扩展函数：获取中心点在 Window 中的坐标
 * @return Pair<Int, Int> 返回中心点 (x, y) 坐标
 */
fun View.getCenterInWindow(): Pair<Int, Int> {
    return ViewPositionUtil.getViewCenterInWindow(this)
}

/**
 * View 扩展函数：获取中心点在屏幕中的坐标
 * @return Pair<Int, Int> 返回中心点 (x, y) 坐标
 */
fun View.getCenterOnScreen(): Pair<Int, Int> {
    return ViewPositionUtil.getViewCenterOnScreen(this)
}

