package com.deepal.ivi.hmi.smartlife.base;

import android.app.WallpaperManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.SkinAppCompatDelegateImpl;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

public abstract class BaseActivity<VB extends ViewBinding,VM extends ViewModel>  extends AppCompatActivity {

    protected VB mBinding;
    protected VM mViewModel;
    protected WallpaperManager mWallpaperManager;
    public static final String KEY_SCENE = "scene";
    public static final String SCENE_BLUR_BG = "blur_bg";//高斯模糊背景场景
    public static final String SCENE_ACTION = "com.mega.carmodel.SCENE_CHANGE";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(getViewModelClass());
        mBinding = getViewBinding();
        setContentView(mBinding.getRoot());
        init();
        initView();
        mWallpaperManager = getSystemService(WallpaperManager.class);
    }

    /**
     * 支持与系统壁纸交互实现模糊背景效果
     **/
    public static void showBlurBg(WallpaperManager wallpaperManager) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SCENE, SCENE_BLUR_BG );
        wallpaperManager.sendWallpaperCommand(null, SCENE_ACTION, 1, 0, 0, bundle);
    }

    protected abstract VB getViewBinding();

    protected abstract Class<VM> getViewModelClass();

    protected abstract void init();

    protected abstract void initView();

    @NonNull
    @Override
    public AppCompatDelegate getDelegate() {
        return SkinAppCompatDelegateImpl.get(this, this);
    }


}