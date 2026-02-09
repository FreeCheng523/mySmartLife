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
import com.zkjd.lingdong.databinding.LinkSetBinding;

public class LinkSetDialog extends BaseDialogFragment<LinkSetBinding> {

    private ViewPager2 viewPager;
    private Context mContext;
    private static final String ARG_DEVICE_MAC = "device_mac";
    private String deviceMac; // 添加成员变量保存 deviceMac

    // 原有的构造函数
    public LinkSetDialog(Context context, OnDialogClickListener listener) {
        super(true,1600,960);
        this.mContext = context;
        this.onDialogListener = listener;
    }

    // 必需的无参构造函数
    public LinkSetDialog() {
        // Fragment 需要公共无参构造函数
    }

    // 静态工厂方法 - 修改为接受 Context 和 listener
    public static LinkSetDialog newInstance(Context context, String deviceMac, OnDialogClickListener listener) {
        LinkSetDialog fragment = new LinkSetDialog(context, listener);
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_MAC, deviceMac);
        fragment.setArguments(args);
        return fragment;
    }

//    public static LinkSetDialog newInstance(String deviceMac) {
//        LinkSetDialog fragment = new LinkSetDialog();
//        Bundle args = new Bundle();
//        args.putString(ARG_DEVICE_MAC, deviceMac);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取传递的参数并保存到成员变量
        if (getArguments() != null) {
            deviceMac = getArguments().getString(ARG_DEVICE_MAC);
            // 在这里可以使用 deviceMac
        }
    }

    @Override
    protected View getContentLayout() {
        return binding.dialogContainer;
    }

    @Override
    protected LinkSetBinding createViewBinding(LayoutInflater inflater, ViewGroup container) {
        return LinkSetBinding.inflate(inflater,container,false);
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
                return  FunctionFragment.Companion.newInstance(deviceMac);
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });

        //退出
        binding.dialogBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //进入设置页
        binding.dialogSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallBack != null) {
                    mCallBack.onConfirmClick();
                }
//                dismiss();
            }
        });
    }

    private Callback mCallBack;
    public void setCallBack(Callback callback) {
        this.mCallBack = callback;
    }

    public interface Callback{
        //        将点击事件传递到root页面
        void onConfirmClick();
    }




        // RecyclerView适配器

        // 网格间距装
}