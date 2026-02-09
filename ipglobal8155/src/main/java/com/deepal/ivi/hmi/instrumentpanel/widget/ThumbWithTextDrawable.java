package com.deepal.ivi.hmi.instrumentpanel.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.deepal.ivi.hmi.instrumentpanel.R;

public class ThumbWithTextDrawable extends Drawable {
    private final View mView;
    private final TextView mTextView;
    private final ImageView mMainIcon;
    private final ImageView mSecondaryIcon;
    public ThumbWithTextDrawable(Context context, int progress) {
        // 加载布局
        mView = LayoutInflater.from(context).inflate(R.layout.seekbar_thumb_with_text, null);
        // 初始化所有视图
        mMainIcon = mView.findViewById(R.id.thumb_main_icon);
        mSecondaryIcon = mView.findViewById(R.id.thumb_selector);
        mTextView = mView.findViewById(R.id.thumb_text);
        setProgress(progress);

        // 测量视图大小
        mView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
    }

    public void setProgress(int progress) {
        mTextView.setText(String.valueOf(progress)+"%");
        // 动态改变图标颜色（示例）
        if(progress > 100) {
            mSecondaryIcon.setColorFilter(Color.RED);
        } else {
            mSecondaryIcon.clearColorFilter();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // 获取系统计算好的thumb位置
        Rect bounds = getBounds();
        canvas.save();
        // 计算 thumb_main_icon 在thumb视图中的中心Y坐标
        int thumbMainIconCenterY = mMainIcon.getTop() + mMainIcon.getHeight() / 2;

        // 计算需要平移的位置
        float translateX = bounds.centerX() - mView.getWidth() / 2f;
        float translateY = bounds.centerY() - thumbMainIconCenterY;
        canvas.translate(translateX, translateY);

        mView.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        mView.setAlpha(alpha / 255f);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // 不再需要实现（保持空方法）
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mView.getMeasuredWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mView.getMeasuredHeight();
    }

}