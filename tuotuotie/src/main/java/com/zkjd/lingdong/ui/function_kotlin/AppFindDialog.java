package com.zkjd.lingdong.ui.function_kotlin;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.zkjd.lingdong.R;
import com.mine.baselibrary.dialog.BaseDialogFragment;
import com.zkjd.lingdong.databinding.AppListBinding;
import com.zkjd.lingdong.model.AppInfo;

public class AppFindDialog extends BaseDialogFragment<AppListBinding> implements AppSelectionFragment.OnAppSelectedListener {

    private ViewPager2 viewPager;
    private Context mContext;

    interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo);
    }

    private  OnAppSelectedListener listener = null;

    public void setListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }

    public AppFindDialog(Context context, OnDialogClickListener listener) {
        super(true);
        this.mContext = context;
        onDialogListener = listener;
    }

    @Override
    protected View getContentLayout() {
        return binding.contentLayout;
    }

    @Override
    protected AppListBinding createViewBinding(LayoutInflater inflater, ViewGroup container) {
        return AppListBinding.inflate(inflater,container,false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    public void init(View view){
        // 初始化视图
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentStateAdapter(getActivity()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                AppSelectionFragment appSelectionFragment = AppSelectionFragment.Companion.newInstance();
                AppSelectionFragment.OnAppSelectedListener onAppSelectedListener = new AppSelectionFragment.OnAppSelectedListener() {
                    @Override
                    public void onAppSelected(@NonNull AppInfo appInfo) {
                        if (listener != null) {
                            listener.onAppSelected(appInfo);
                        }
                    }
                };
                appSelectionFragment.setOnAppSelectedListener(onAppSelectedListener);
                return appSelectionFragment;
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });

        //退出
        binding.linkBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //进入设置页
//        binding.dialogSet.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mCallBack != null) {
//                    mCallBack.onConfirmClick();
//                }
////                dismiss();
//            }
//        });
    }

    private Callback mCallBack;
    public void setCallBack(Callback callback) {
        this.mCallBack = callback;
    }

    @Override
    public void onAppSelected(@NonNull AppInfo appInfo) {

    }

    public interface Callback{
        //        将点击事件传递到root页面
        void onConfirmClick();
    }




        // RecyclerView适配器

        // 网格间距装
}