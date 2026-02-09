package com.deepal.ivi.hmi.smartlife.dialog;


import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.deepal.ivi.hmi.smartlife.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.deepal.ivi.hmi.smartlife.databinding.DialogPrivacyPolicyBinding;
import com.mine.baselibrary.dialog.BaseDialogFragment;

public class PrivacyPolicyDialogFragment extends BaseDialogFragment<DialogPrivacyPolicyBinding> {

    /**
     * 构造函数：启用后方毛玻璃效果
     */
    public PrivacyPolicyDialogFragment() {
        super(true, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true, false);
    }

    @Nullable
    @Override
    protected DialogPrivacyPolicyBinding createViewBinding(@Nullable LayoutInflater inflater, @Nullable ViewGroup container) {
        if (inflater == null) {
            return null;
        }
        return DialogPrivacyPolicyBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding != null) {
            binding.back.setOnClickListener(v -> dismiss());
            // 处理隐私政策链接文本
            setupPrivacyPolicyLink();
        }
    }
    private void setupPrivacyPolicyLink() {
        if (binding == null || binding.contentTextView == null) return;

        String linkText = getString(R.string.privacy_policy_link_text);
        String fullText = getString(R.string.privacy_policy_content, linkText);

        SpannableString spannableString = new SpannableString(fullText);
        // 查找起始和结束位置
        int startIndex = fullText.indexOf(linkText);
        if (startIndex == -1) return; // 未找到链接文本
        int endIndex = startIndex + linkText.length();

        // 设置点击事件
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // 跳转
                openFullPrivacyPolicy();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                // 设置链接样式：蓝色、无下划线
                ds.setColor(Color.parseColor("#007AFF"));
                ds.setUnderlineText(false);
            }
        };

        // 应用Span
        spannableString.setSpan(clickableSpan, startIndex, endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 设置文本
        binding.contentTextView.setText(spannableString);
        // 必须设置movementMethod才能响应点击
        binding.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        // 设置点击后的背景色为透明（可选）
        binding.contentTextView.setHighlightColor(Color.TRANSPARENT);
    }

    private void openFullPrivacyPolicy() {
        String privacyUrl = getString(R.string.privacy_policy_URL);
        WebViewDialogFragment webViewDialog = WebViewDialogFragment.newInstance(privacyUrl);
        webViewDialog.show(getParentFragmentManager(), "webview_dialog");
    }
    @Nullable
    @Override
    protected View getContentLayout() {
        return binding != null ? binding.scrollView : null;
    }

    /**
     * 静态工厂方法创建Dialog实例
     */
    public static PrivacyPolicyDialogFragment newInstance() {
        return new PrivacyPolicyDialogFragment();
    }
}