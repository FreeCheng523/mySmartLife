package com.zkjd.lingdong.libs;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.zkjd.lingdong.R;

/**
 * Description:仿 iOS 风格的开关按钮
 */
public class SwitchButtonView extends View {
    private static final String TAG = "SwitchButtonView";
    /**
     * 控件默认宽度
     */
    private static final int DEFAULT_WIDTH = 200;
    /**
     * 控件默认高度
     */
    private static final int DEFAULT_HEIGHT = DEFAULT_WIDTH / 8 * 5;
    /**
     * 画笔
     */
    private Paint mPaint;
    /**
     * 控件背景的矩形范围
     */
    private RectF mRectF;
    /**
     * 开关指示器按钮圆心 X 坐标的偏移量
     */
    private float mButtonCenterXOffset;

    // 添加其他必要的成员变量
    private boolean isChecked = false;
    private int mBackgroundOnColor = 0xFF4CD964; // iOS绿色
    private int mBackgroundOffColor = 0xFFE9E9E9; // iOS灰色
    private int mButtonColor = 0xFFFFFFFF; // 白色
    private int mBorderColor = 0xFFCCCCCC; // 边框颜色

    private float mButtonRadius;
    private float mBackgroundCornerRadius;
    private float mBorderWidth = 1f;

    private ValueAnimator mAnimator;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(SwitchButtonView buttonView, boolean isChecked);
    }

    public SwitchButtonView(Context context) {
        this(context, null);
    }

    public SwitchButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRectF = new RectF();

        // 获取自定义属性
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchButtonView);
            isChecked = a.getBoolean(R.styleable.SwitchButtonView_checked, false);
            mBackgroundOnColor = a.getColor(R.styleable.SwitchButtonView_backgroundOnColor, mBackgroundOnColor);
            mBackgroundOffColor = a.getColor(R.styleable.SwitchButtonView_backgroundOffColor, mBackgroundOffColor);
            mButtonColor = a.getColor(R.styleable.SwitchButtonView_buttonColor, mButtonColor);
            mBorderColor = a.getColor(R.styleable.SwitchButtonView_borderColor, mBorderColor);
            mBorderWidth = a.getDimension(R.styleable.SwitchButtonView_borderWidth, mBorderWidth);
            a.recycle();
        }

        // 初始化按钮位置
        mButtonCenterXOffset = isChecked ? 1 : 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = DEFAULT_WIDTH;
            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, widthSize);
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = DEFAULT_HEIGHT;
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, heightSize);
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 计算背景圆角矩形
        float padding = h * 0.1f; // 10% 内边距
        mRectF.set(padding, padding, w - padding, h - padding);

        // 计算按钮半径
        mButtonRadius = (h - 2 * padding) / 2;

        // 计算背景圆角半径
        mBackgroundCornerRadius = mRectF.height() / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景
        drawBackground(canvas);

        // 绘制按钮
        drawButton(canvas);
    }

    private void drawBackground(Canvas canvas) {
        // 绘制边框
        mPaint.setColor(mBorderColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderWidth);
        canvas.drawRoundRect(mRectF, mBackgroundCornerRadius, mBackgroundCornerRadius, mPaint);

        // 绘制填充背景
        int backgroundColor = isChecked ? mBackgroundOnColor : mBackgroundOffColor;
        mPaint.setColor(backgroundColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(mRectF, mBackgroundCornerRadius, mBackgroundCornerRadius, mPaint);
    }

    private void drawButton(Canvas canvas) {
        // 计算按钮中心X坐标
        float minX = mRectF.left + mButtonRadius;
        float maxX = mRectF.right - mButtonRadius;
        float buttonCenterX = minX + mButtonCenterXOffset * (maxX - minX);
        float buttonCenterY = mRectF.centerY();

        // 绘制按钮
        mPaint.setColor(mButtonColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(buttonCenterX, buttonCenterY, mButtonRadius, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;

            case MotionEvent.ACTION_UP:
                // 点击切换状态
                toggle();
                return true;

            case MotionEvent.ACTION_CANCEL:
                return true;
        }

        return super.onTouchEvent(event);
    }

    public void toggle() {
        setChecked(!isChecked);
    }

    public void setChecked(boolean checked) {
        if (isChecked != checked) {
            isChecked = checked;
            animateButton();

            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, isChecked);
            }
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    private void animateButton() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        float start = mButtonCenterXOffset;
        float end = isChecked ? 1 : 0;

        mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(200);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mButtonCenterXOffset = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mAnimator.start();
    }

    // 设置颜色方法
    public void setBackgroundOnColor(int color) {
        mBackgroundOnColor = color;
        invalidate();
    }

    public void setBackgroundOffColor(int color) {
        mBackgroundOffColor = color;
        invalidate();
    }

    public void setButtonColor(int color) {
        mButtonColor = color;
        invalidate();
    }

    public void setBorderColor(int color) {
        mBorderColor = color;
        invalidate();
    }

    public void setBorderWidth(float width) {
        mBorderWidth = width;
        invalidate();
    }
}