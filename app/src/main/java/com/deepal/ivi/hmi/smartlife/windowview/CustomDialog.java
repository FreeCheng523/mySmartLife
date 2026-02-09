package com.deepal.ivi.hmi.smartlife.windowview;



import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class CustomDialog extends Dialog {
    private View contentView;

    public interface OnViewInflatedListener {
        void onViewInflated(View view, CustomDialog customDialog);
    }

    public CustomDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 请求无标题栏
    }

    public CustomDialog setContentView(int layoutResID, OnViewInflatedListener listener) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.contentView = inflater.inflate(layoutResID, (ViewGroup) null);
        if (this.contentView.getParent() != null) {
            ((ViewGroup) this.contentView.getParent()).removeView(this.contentView);
        }
        super.setContentView(this.contentView);
        if (listener != null) {
            listener.onViewInflated(this.contentView, this);
        }
        if (getWindow() != null) {
//            getWindow().setBackgroundDrawableResource(R.drawable.meter_dialog_bg_shape);
        }
        return this;
    }

    public CustomDialog setContentView(View view, OnViewInflatedListener listener) {
        this.contentView = view;
        if (this.contentView.getParent() != null) {
            ((ViewGroup) this.contentView.getParent()).removeView(this.contentView);
        }
        super.setContentView(this.contentView);
        if (listener != null) {
            listener.onViewInflated(this.contentView, this);
        }
        if (getWindow() != null) {
//            getWindow().setBackgroundDrawableResource(R.drawable.meter_dialog_bg_shape);
        }
        return this;
    }

    public CustomDialog setCancelableDialog(boolean cancelable) {
        setCancelable(cancelable);
        return this;
    }

    public CustomDialog setCanceledOnTouchOutsideDialog(boolean cancelable) {
        setCanceledOnTouchOutside(cancelable);
        return this;
    }

    @Override
    public View findViewById(int viewId) {
        return this.contentView.findViewById(viewId);
    }

    public CustomDialog showDialog() {
        show();
        return this;
    }

    public CustomDialog dismissDialog() {
        dismiss();
        return this;
    }

    public CustomDialog onSettingsKey(int offsetX) {
        Log.d("MeterSocketBuild", "CustomDialog offsetX:" + offsetX);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = offsetX;
            window.setAttributes(params);
        }
        return this;
    }

    public void onSettingsKeyChanged(int startWidth, int endWidth) {
        Log.d("MeterSocketBuild", "CustomDialog startWidth:" + startWidth + ">>>endWidth:" + endWidth);
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);
        animator.setDuration(500L);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Window window = CustomDialog.this.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.x = ((Integer) animation.getAnimatedValue()).intValue();
                    window.setAttributes(params);
                }
            }
        });
        animator.start();
    }
}