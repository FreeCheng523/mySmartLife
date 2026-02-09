package com.deepal.ivi.hmi.smartlife.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;
import com.deepal.ivi.hmi.smartlife.R;
import com.deepal.ivi.hmi.smartlife.databinding.DialogTipsBinding;
import com.mine.baselibrary.dialog.BaseDialogFragment;

/**
 * 提示对话框
 * 用于显示使用提示信息
 */
public class NoPermissionFragmentDialog extends BaseDialogFragment<ViewBinding> {

    private static final String ARG_TITLE = "title";
    private static final String ARG_CONTENT = "content";
    
    private String title;
    private String content;
    private OnTipsDialogClickListener listener;

    /**
     * 提示对话框点击监听器
     */
    public interface OnTipsDialogClickListener {
        void onConfirm();
        void onDismiss();
    }

    public NoPermissionFragmentDialog() {
        super(false, 1500, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * 创建新的提示对话框实例
     * @param title 标题
     * @param content 内容
     * @return TipsFragmentDialog实例
     */
    public static NoPermissionFragmentDialog newInstance(String title, String content) {
        NoPermissionFragmentDialog dialog = new NoPermissionFragmentDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CONTENT, content);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "使用提示");
            content = getArguments().getString(ARG_CONTENT, "");
        }
    }

    @Override
    protected ViewBinding createViewBinding(LayoutInflater inflater, ViewGroup container) {
        return DialogTipsBinding.inflate(inflater,container,false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        TextView titleText = view.findViewById(R.id.title_text);
        TextView contentText = view.findViewById(R.id.tips_content);
        TextView confirmBtn = view.findViewById(R.id.confirm_btn);
        
        // 设置内容
      /*  if (title != null) {
            titleText.setText(title);
        }
        if (content != null) {
            contentText.setText(content);
        }*/
        
        // 设置确认按钮点击事件
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onConfirm();
                }
                dismiss();
            }
        });
    }

    @Override
    protected View getContentLayout() {
        return getView() != null ? getView().findViewById(R.id.content_layout) : null;
    }

    /**
     * 设置点击监听器
     * @param listener 监听器
     */
    public void setOnTipsDialogClickListener(OnTipsDialogClickListener listener) {
        this.listener = listener;
    }

    /**
     * 显示对话框（带监听器）
     * @param fragmentManager FragmentManager
     * @param tag 标签
     * @param listener 监听器
     */
    public void show(FragmentManager fragmentManager, String tag, OnTipsDialogClickListener listener) {
        setOnTipsDialogClickListener(listener);
        show(fragmentManager, tag);
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.onDismiss();
        }
    }
}
