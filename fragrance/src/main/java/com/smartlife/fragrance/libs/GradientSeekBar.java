package com.smartlife.fragrance.libs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

public class GradientSeekBar extends AppCompatSeekBar {
    private Paint trackPaint;
    private Paint thumbOuterPaint;
    private Paint thumbInnerPaint;
    private Paint thumbCenterPaint;
    private RectF trackRect;
    private int[] colors;
    private boolean colorsInitialized = false;
    private int thumbRadius = 28; // 滑块半径
    private int trackHeight = 26; // 轨道高度

    public GradientSeekBar(Context context) {
        super(context);
        init();
    }

    public GradientSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GradientSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化轨道画笔
        trackPaint = new Paint();
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setAntiAlias(true);

        // 初始化滑块外圈画笔（白色）
        thumbOuterPaint = new Paint();
        thumbOuterPaint.setStyle(Paint.Style.FILL);
        thumbOuterPaint.setAntiAlias(true);
        thumbOuterPaint.setColor(0xFFFFFFFF); // 白色外圈

        // 初始化滑块内圈画笔
        thumbInnerPaint = new Paint();
        thumbInnerPaint.setStyle(Paint.Style.FILL);
        thumbInnerPaint.setAntiAlias(true);

        // 初始化滑块中心圆点画笔（白色）
        thumbCenterPaint = new Paint();
        thumbCenterPaint.setStyle(Paint.Style.FILL);
        thumbCenterPaint.setAntiAlias(true);
        thumbCenterPaint.setColor(0xFFFFFFFF); // 白色中心

        // 初始化矩形区域
        trackRect = new RectF();

        // 移除默认进度条
        setProgressDrawable(null);
        setSplitTrack(false);

        // 设置最小高度以确保正确显示
        setMinimumHeight(40);

        // 设置透明thumb，我们将自己绘制
        setThumb(null);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        // 获取视图尺寸
        int width = getWidth();
        int height = getHeight();

        // 计算可绘制区域（考虑padding）
        int paddingLeft = getPaddingLeft() + 7;
        int paddingRight = getPaddingRight() + 7;
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int drawWidth = width - paddingLeft - paddingRight;
        int drawHeight = height - paddingTop - paddingBottom;

        // 计算轨道位置和大小
        int top = paddingTop + (drawHeight - trackHeight) / 2;
        int bottom = top + trackHeight;
        int left = paddingLeft;
        int right = left + drawWidth;

        float radius = trackHeight / 2f; // 圆角半径

        // 初始化颜色数组（如果需要）
        if (!colorsInitialized || colors == null || colors.length != drawWidth) {
            initializeColors(drawWidth);
        }

        // 设置渐变着色器 - 整个轨道都显示完整渐变
        LinearGradient gradient = new LinearGradient(
                left, 0, right, 0,
                colors, null, Shader.TileMode.CLAMP
        );
        trackPaint.setShader(gradient);

        // 绘制完整的渐变轨道（无边框）
        trackRect.set(left, top, right, bottom);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        // 计算滑块位置
        int progress = getProgress();
        int max = getMax();
        float ratio = max > 0 ? (float) progress / max : 0;
        int thumbX = (int) (left + ratio * drawWidth);
        int thumbY = paddingTop + drawHeight / 2;

        // 根据当前位置获取颜色
        int colorIndex = (int) (ratio * (colors.length - 1));
        colorIndex = Math.max(0, Math.min(colors.length - 1, colorIndex));
        int thumbColor = colors[colorIndex];

        // 设置内圈颜色
        thumbInnerPaint.setColor(thumbColor);

        // 绘制滑块外圈（白色）
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbOuterPaint);

        // 绘制滑块内圈（当前进度颜色）
        canvas.drawCircle(thumbX, thumbY, thumbRadius - 3, thumbInnerPaint);

        // 绘制滑块中心白色圆点（与轨道高度一致，但确保可见）
        int centerDotRadius = trackHeight / 2;

        // 确保中心圆点不会太大而超出内圈
        int maxCenterRadius = thumbRadius - 5; // 内圈半径减去2像素边距
        centerDotRadius = Math.min(centerDotRadius, maxCenterRadius);

        // 确保中心圆点不会太小
        centerDotRadius = Math.max(centerDotRadius, 3);

        canvas.drawCircle(thumbX, thumbY, centerDotRadius, thumbCenterPaint);

        // 不需要调用super.onDraw，因为我们自己绘制了所有内容
    }

    private void initializeColors(int width) {
        if (width <= 0) return;

        // 创建颜色数组
        colors = new int[width];
        for (int i = 0; i < width; i++) {
            float hue = (float) i / (width - 1) * 360;
            float[] hsv = {hue, 1.0f, 1.0f};
            colors[i] = android.graphics.Color.HSVToColor(hsv);
        }
        colorsInitialized = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 重置颜色初始化状态，以便在onDraw中重新初始化
        colorsInitialized = false;
    }

    // 设置滑块半径的方法
    public void setThumbRadius(int radius) {
        this.thumbRadius = radius;
        invalidate();
    }

    // 获取滑块半径的方法
    public int getThumbRadius() {
        return thumbRadius;
    }

    // 设置轨道高度的方法
    public void setTrackHeight(int height) {
        this.trackHeight = height;
        invalidate();
    }

    // 获取轨道高度的方法
    public int getTrackHeight() {
        return trackHeight;
    }
}