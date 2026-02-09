package com.deepal.ivi.hmi.smartlife.dialog;

import static android.view.View.GONE;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.deepal.ivi.hmi.smartlife.databinding.DialogRenameDeviceBinding;
import com.deepal.ivi.hmi.smartlife.utils.BlurBackgroundHelper;

import java.util.HashSet;
import java.util.Set;

public class RenameDeviceDialog extends Dialog {
    private static final String TAG = "RenameDeviceDialog";
    public interface OnRenameConfirmListener {
        void onRenameConfirm(String newName);
    }

    private DialogRenameDeviceBinding binding;
    private OnRenameConfirmListener onRenameConfirmListener;
    private String currentName;
    private Set<String> existingNames; // 已存在的设备名称集合

    // 颜色资源
    private int colorWhite;
    private int colorGray;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardGlobalLayoutListener;

    public RenameDeviceDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
        Log.d(TAG, "【setCurrentName】设置当前名称: '" + currentName + "'");
    }

    public void setExistingNames(Set<String> existingNames) {
        Log.d(TAG, "【setExistingNames】原始集合: " + existingNames + ", 大小: " + (existingNames != null ? existingNames.size() : "null"));

        this.existingNames = existingNames != null ? new HashSet<>(existingNames) : new HashSet<>();
        // 移除当前设备名称，因为当前设备名称是允许的
        if (currentName != null) {
            this.existingNames.remove(currentName);
        }
    }

    public void setOnRenameConfirmListener(OnRenameConfirmListener listener) {
        this.onRenameConfirmListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogRenameDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化颜色
        colorWhite = ContextCompat.getColor(getContext(), com.deepal.ivi.hmi.smartlife.R.color.white);
        colorGray = ContextCompat.getColor(getContext(), com.deepal.ivi.hmi.smartlife.R.color.pop_cancel_text);

        setupWindow();
        initViews();
        setupClickListeners();
        setupTextWatcher();
        setupKeyboardListener();
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            window.setAttributes(params);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }

        }
    }

    private void initViews() {
        // 设置当前设备名称
        if (!TextUtils.isEmpty(currentName)) {
            binding.etDeviceName.setText(currentName);
            binding.etDeviceName.setSelection(currentName.length()); // 光标移到末尾
        }

        // 强制单行模式
        binding.etDeviceName.setSingleLine(true);

        // 初始状态：确认按钮不可点击
        updateConfirmButtonState(true);

    }

    private void setupTextWatcher() {
        binding.etDeviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInput(s.toString());
            }
        });
    }

    private void setupKeyboardListener() {
        keyboardGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View main = findViewById(android.R.id.content);
                View inputView = getInputView();

                if (main == null || inputView == null) {
                    return;
                }

                Rect rect = new Rect();
                main.getWindowVisibleDisplayFrame(rect);
                int screenHeight = main.getRootView().getHeight();
                int keyboardHeight = screenHeight - rect.bottom;

                if (keyboardHeight > screenHeight * 0.15) { // 键盘弹出
                    int[] location = new int[2];
                    inputView.getLocationOnScreen(location);
                    int scrollAmount = location[1] + inputView.getHeight() - rect.bottom;
                    if (scrollAmount > 0) {
                        // 如果需要滚动，调整对话框位置
                        adjustDialogForKeyboard(scrollAmount);
                    }
                } else { // 键盘收起
                    resetDialogPosition();
                }
            }
        };

        // 添加全局布局监听器
        View main = findViewById(android.R.id.content);
        if (main != null) {
            main.getViewTreeObserver().addOnGlobalLayoutListener(keyboardGlobalLayoutListener);
        }
    }

    private View getInputView() {
        return binding.etDeviceName; // 返回输入框视图
    }

    private void adjustDialogForKeyboard(int scrollAmount) {
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            // 向上移动对话框以避免键盘遮挡
            params.y = -Math.min(scrollAmount, getContext().getResources().getDisplayMetrics().heightPixels / 3);
            window.setAttributes(params);
        }
    }

    private void resetDialogPosition() {
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = 0; // 恢复原始位置
            window.setAttributes(params);
        }
    }

    private void removeKeyboardGlobalLayoutListener() {
        View main = findViewById(android.R.id.content);
        if (main != null && keyboardGlobalLayoutListener != null) {
            main.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardGlobalLayoutListener);
            keyboardGlobalLayoutListener = null;
        }
    }



    private void validateInput(String input) {
        String text = input.trim();

        // 隐藏所有提示
        binding.renameTips.setVisibility(GONE);

        if (TextUtils.isEmpty(text)) {
            // 输入为空，确认按钮不可点击
            updateConfirmButtonState(false);
            return;
        }

        if (text.length() > 10) {
            // 超过15个字符
            showErrorTip("请输入1-10位字符");
            updateConfirmButtonState(false);
            return;
        }

        if (existingNames != null && existingNames.contains(text)) {
            // 名称已被占用
            showErrorTip("此名称已被占用，请输入其他名称");
            updateConfirmButtonState(false);
            return;
        }

        // 输入有效
        updateConfirmButtonState(true);
    }

    private void showErrorTip(String message) {
        binding.renameTips.setText(message);
        binding.renameTips.setVisibility(android.view.View.VISIBLE);
    }

    private void updateConfirmButtonState(boolean enabled) {
        binding.confirmYes.setEnabled(enabled);

        if (enabled) {
            // 可点击状态
            binding.confirmYes.setTextColor(colorWhite);
        } else {
            // 不可点击状态
            binding.confirmYes.setTextColor(colorGray);
        }
    }

    private void setupClickListeners() {
        // 取消按钮
        binding.confirmNo.setOnClickListener(v -> {
            dismiss();
        });

        // 确认按钮
        binding.confirmYes.setOnClickListener(v -> {
            if (binding.confirmYes.isEnabled()) {
                confirmRename();
            }
        });

        // 点击输入框外部隐藏键盘
        binding.getRoot().setOnClickListener(v -> {
            hideKeyboard();
        });
    }

    private void confirmRename() {
        String newName = binding.etDeviceName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            return;
        }

        if (newName.length() > 15) {
            return;
        }

        if (existingNames.contains(newName)) {
            return;
        }
        //触发改名回调
        if (onRenameConfirmListener != null) {
            onRenameConfirmListener.onRenameConfirm(newName);
        }

        dismiss();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.etDeviceName.getWindowToken(), 0);
    }
    public void showWithBlur() {
        BlurBackgroundHelper.applyBlurBehind(this);
        show();
    }

    @Override
    public void dismiss() {
        hideKeyboard();
        removeKeyboardGlobalLayoutListener();
        super.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeKeyboardGlobalLayoutListener();
    }
}