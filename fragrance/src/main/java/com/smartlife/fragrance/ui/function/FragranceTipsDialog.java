package com.smartlife.fragrance.ui.function;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mine.baselibrary.dialog.BaseDialogFragment;
import com.smartlife.fragrance.R;
import com.smartlife.fragrance.databinding.FragranceTipsBinding;

/**
 * 香氛机提示对话框
 */
public class FragranceTipsDialog extends BaseDialogFragment<FragranceTipsBinding> {
    public static final String TAG_TIPS = "FragranceTipsDialog";


    public static FragranceTipsDialog newInstance() {
        return new FragranceTipsDialog();
    }

    public FragranceTipsDialog() {
        super(true, 700, 140,false,false);
    }

    @NonNull
    @Override
    public FragranceTipsBinding createViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragranceTipsBinding.inflate(inflater, container, false);
    }

    @Override
    protected View getContentLayout() {
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    private void initViews() {
        // 点击外部关闭
        binding.getRoot().setOnClickListener(v -> dismiss());

        // 点击内容区域不关闭
        binding.fragranceTips.setOnClickListener(v -> {
        });

    }
}