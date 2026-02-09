package com.deepal.ivi.hmi.smartlife;

import android.Manifest;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.deepal.ivi.hmi.instrumentpanel.widget.InstrumentPanelDialog;
import com.deepal.ivi.hmi.ipvehiclecommon.IApplication;
import com.deepal.ivi.hmi.ipvehiclecommon.viewmode.InstrumentPanelViewModel;
import com.deepal.ivi.hmi.smartlife.dialog.PrivacyPolicyDialogFragment;
import com.deepal.ivi.hmi.smartlife.dialog.WebViewDialogFragment;
import com.deepal.ivi.hmi.smartlife.helper.FragranceSettingHelper;
import com.deepal.ivi.hmi.smartlife.helper.LingDongTieSettingHelper;
import com.deepal.ivi.hmi.smartlife.utils.ThrottleClickListener;
import com.adayo.service.utils.MKDisplayStatus;
import com.deepal.ivi.hmi.smartlife.adapter.SmartDeviceAdapter;
import com.deepal.ivi.hmi.smartlife.base.BaseActivity;
import com.deepal.ivi.hmi.smartlife.bean.AgileSmartDevice;
import com.deepal.ivi.hmi.smartlife.bean.SmartDevice;
import com.deepal.ivi.hmi.smartlife.databinding.ActivityMainBinding;
import com.deepal.ivi.hmi.smartlife.databinding.DialogUserAgreementBinding;
import com.deepal.ivi.hmi.smartlife.utils.BlurBackgroundHelper;
import com.mine.baselibrary.constants.CarPlatformConstants;
import com.mine.baselibrary.constants.VehicleTypeConstants;
import com.mine.baselibrary.dialog.BaseDialogFragment;
import com.mine.baselibrary.permission.PermissionUtil;
import com.mine.baselibrary.window.ToastUtilOverApplication;
import com.smartlife.fragrance.data.model.PowerState;
import com.smartlife.fragrance.ui.function.FragranceDialog;
import com.zkjd.lingdong.model.ConnectionState;
import com.zkjd.lingdong.ui.function_kotlin.LightSetDialog;
import com.zkjd.lingdong.lingdongtie_brige.LingDongTieManager;
import com.deepal.ivi.hmi.smartlife.utils.LocalStoreManager;
import com.deepal.ivi.hmi.smartlife.utils.UsbConnectionUtil;
import com.deepal.ivi.hmi.smartlife.viewmodel.MainViewModel;
import com.deepal.ivi.hmi.smartlife.windowview.CustomDialog;
import com.deepal.ivi.hmi.smartlife.dialog.NoPermissionFragmentDialog;
import com.smartlife.fragrance.bridge.FragranceManager;
import com.smartlife.fragrance.data.model.FragranceDevice;
import com.zkjd.lingdong.event.ButtonFunctionEvent;
import com.zkjd.lingdong.model.Device;
import com.zkjd.lingdong.repository.DeviceRepository;
import com.zkjd.lingdong.repository.SettingsRepository;
import com.zkjd.lingdong.ui.function_kotlin.LinkSetDialog;
import com.zkjd.lingdong.ui.pairing.PairingAndConnectViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.fragment.app.DialogFragment;

import dagger.hilt.android.AndroidEntryPoint;
import jakarta.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@AndroidEntryPoint
public class MainActivity extends BaseActivity<ActivityMainBinding, MainViewModel> implements SmartDeviceAdapter.OnDeviceItemClickListener {

    private static final String TAG = "MainActivity";

    private SmartDeviceAdapter smartDeviceAdapter;
    final private List<SmartDevice> smartDeviceList = new ArrayList<>();
    private static final int REQ_BT_CONNECT = 1001;
    private BluetoothCommon bt;
    public CustomDialog dialogUserAgreement;
    public DialogUserAgreementBinding userAgreeViewBind;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Handler mHandler2 = new Handler(Looper.getMainLooper());
    // 轮询搜索USB设备
    private Runnable statusCheckRunnable;
    private Dialog mDialog;
    private boolean mHasReadBottom = false;
    private volatile boolean isSearching = false;
    private Dialog mTuoTuoTieConnectFailDialog; // 全局变量管理连接失败dialog

    private Dialog mFragranceConnectFailDialog; // 全局变量管理连接失败dialog

    private PermissionUtil permissionUtil = new PermissionUtil();
    private HandlerThread mUsbThread; // 移除 final
    private Handler mUsbHandler;

    // 连续点击计数器相关
    private int clickCount = 0;
    private static final int REQUIRED_CLICKS = 10;
    private static final long CLICK_TIMEOUT = 10000; // 2秒内必须完成连续点击
    private Runnable resetClickCountRunnable;


    @Override
    protected ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(LayoutInflater.from(this));
    }

    @Inject
    DeviceRepository mDeviceRepository;

    @Inject
    ButtonFunctionEvent mButtonFunctionEvent;
    @Inject
    SettingsRepository mSettingsRepository;
    private FragranceManager mFragranceManager;
    private LingDongTieManager mLingDongTieManager;
    public static void showBlurDialog(Dialog dialog) {
        BlurBackgroundHelper.applyBlurBehind(dialog);
        dialog.show();
    }

    @Override
    protected Class<MainViewModel> getViewModelClass() {
        return MainViewModel.class;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(VehicleTypeConstants.INSTANCE.isMega()){
            // 设置透明背景
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 设置显示壁纸（API level 5+）
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
            );
        }
        super.onCreate(savedInstanceState);
        // 加载设备列表后立刻打印

        if (VehicleTypeConstants.INSTANCE.isTinnove()) {
            mBinding.getRoot().setBackgroundResource(R.drawable.activity_blur_bg);
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        /***1014新增子线程**/
        mUsbThread = new HandlerThread("UsbBackground");
        mUsbThread.start();
        mUsbHandler = new Handler(mUsbThread.getLooper());

        permissionUtil.init(this, new PermissionUtil.PermissionCallback() {
            @Override
            public void onPermissionGranted(@NonNull String s) {

            }

            @Override
            public void onPermissionDenied(@NonNull String s) {

            }

            @Override
            public void onPermissionPermanentlyDenied(@NonNull String s) {

            }

            @Override
            public void onAllPermissionsResult(@NonNull List<String> list, @NonNull List<String> rationalePermissions, @NonNull List<String> permanentlyDeniedPermissions) {
                if(!rationalePermissions.isEmpty()){
                    requestPermission();
                }else if(!permanentlyDeniedPermissions.isEmpty()){
                    String[] array = permanentlyDeniedPermissions.toArray(new String[0]);
                    // 使用TipsFragmentDialog替换Toast显示权限被拒绝的提示

                    String[] blueToothPermission = PermissionUtil.Companion.getBluetoothScanAndConnectPermissions();

                    // 检查permanentlyDeniedPermissions是否包含blueToothPermission中的元素
                    boolean hasBluetoothPermissionDenied = false;
                    for (String deniedPermission : permanentlyDeniedPermissions) {
                        for (String bluetoothPerm : blueToothPermission) {
                            if (deniedPermission.equals(bluetoothPerm)) {
                                hasBluetoothPermissionDenied = true;
                                break;
                            }
                        }
                        if (hasBluetoothPermissionDenied) {
                            break;
                        }
                    }

                    if (hasBluetoothPermissionDenied) {
                        showPermissionDeniedTips(PermissionUtil.Companion.getPermissionDisplayNames(array, ","));
                        Log.i(TAG, "蓝牙权限被永久拒绝");
                    }
                }
            }
        });

        /**tt新增用户协议**/
        String verify = LocalStoreManager.getInstance().getStoreString("key.store.device.agreed");
        verify = verify.replace("\"", "");
        Log.d(TAG, "onCreate: "+verify );
        if ("1".equals(verify)) {
            // initView();
        }else{
            initUserAgreementDialog();
        }
//        tryOpenBluetooth();
    }

    private void lingDongtieSetting() {
        // 使用辅助类处理灵动贴设备设置，隔离代码逻辑
        mLingDongTieManager = LingDongTieSettingHelper.setupLingDongTie(this);
    }

    private void fragranceSetting() {
        // 使用辅助类处理香薰设备设置，隔离代码逻辑
        mFragranceManager = FragranceSettingHelper.setupFragrance(this);
    }

    //妥妥贴隐藏测试弹框
    private void openTuoWindow(){
    }

    private void showLightSetDialog(String deviceMac){
        LightSetDialog dialog =LightSetDialog.newInstance(this, deviceMac, new BaseDialogFragment.OnDialogClickListener() {
            @Override
            public boolean onClick(DialogFragment dialog, View v, int flag) {
                return false;
            }
        }) ;
        dialog.setCallBack(new LightSetDialog.Callback(){

            @Override
            public void onConfirmClick() {
                dialog.dismiss();
//                showLinkSetDialog();
            }
        });
        dialog.show(getSupportFragmentManager(), "Nap showLightSetDialog");
    }

    private void showLinkSetDialog(String deviceMac){
//        LinkSetDialog dialog = new LinkSetDialog(MainActivity.this, (dialog1, v, flag) -> false);
        // 使用静态工厂方法创建 LinkSetDialog，并传递 deviceMac
        LinkSetDialog dialog = LinkSetDialog.newInstance(this, deviceMac,(dialog1, v, flag) -> false);
        dialog.setCallBack(new LinkSetDialog.Callback() {
            @Override
            public void onConfirmClick() {
//                dialog.dismiss();
                showLightSetDialog(deviceMac);
            }
        });
        dialog.show(getSupportFragmentManager(), "Nap LinkSetDialog");
    }



    private void showFragranceSetDialog(String deviceMac) {
        FragranceDialog dialog = FragranceDialog.newInstance(deviceMac,false,0);
        dialog.setControlListener(new FragranceDialog.OnFragranceControlListener() {
            @Override
            public void onLightLinkChanged(boolean linked) {
                // 处理氛围灯联动状态改变
            }
            @Override
            public void onColorChanged(int colorValue) {
                // 处理颜色改变
            }
            @Override
            public void onDialogDismiss() {
                // 对话框关闭时的处理
            }
            public void onDeviceDisconnected() {
                // 断连处理
            }
        });
        // 显示对话框
        dialog.show(getSupportFragmentManager(), "FragranceDialog");
    }
    @Override
    protected void init() {
        Log.i(TAG, "init start");
        lingDongtieSetting();
        fragranceSetting();
        mViewModel.init();
        initInstrumentPanel();
    }


    public final void initUserAgreementDialog() {

        View inflate = View.inflate(this, R.layout.dialog_user_agreement, null);
        this.userAgreeViewBind = DialogUserAgreementBinding.bind(inflate);

        this.dialogUserAgreement = new CustomDialog(this);
        dialogUserAgreement.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );
        this.dialogUserAgreement.setContentView(inflate);
        this.dialogUserAgreement.setCancelable(false);
        this.dialogUserAgreement.setCanceledOnTouchOutside(false);
        configureDialogViews(userAgreeViewBind, dialogUserAgreement);
        if (this.dialogUserAgreement != null) {
            this.dialogUserAgreement.showDialog();
        }
    }

    @Override
    protected void initView() {
        // 先初始化 Adapter 和 UI（空数据）
        smartDeviceAdapter = new SmartDeviceAdapter(this, smartDeviceList);
        smartDeviceAdapter.setOnDeviceItemClickListener(this);
        mBinding.homeRecyclerview.setLayoutManager(new GridLayoutManager(this, 3));//三个卡片一行
        mBinding.homeRecyclerview.setAdapter(smartDeviceAdapter);
        mBinding.homeRecyclerview.setItemAnimator(null);//关闭卡片闪烁

        // 在子线程中加载设备数据，避免阻塞主线程
        new Thread(() -> {
            try {
                // 1. 数据迁移（首次启动清理SP中的旧数据）
                migrateDataOnFirstLaunch();

                // 2. 从SP加载其他设备（小仪表、中控物理按键）
                loadOtherDevicesFromSP();

                // 3. 从数据库加载妥妥帖和香氛设备
                loadExistingDevicesFromDatabase();
                //smartList 根据时间排序
                smartDeviceList.sort((a, b) -> (int) (a.getAddedTime() - b.getAddedTime()));

                Log.i("SmartLife", "排序后的列表: " + smartDeviceList);
                // 切换到主线程更新UI
                runOnUiThread(() -> {
                    openTuoWindow();
                    Log.i("SmartLife0725", "读取的列表: " + smartDeviceList);

                    // 通知Adapter数据已加载
                    smartDeviceAdapter.notifyDataSetChanged();

                    // 更新UI显示
                    updateEmptyView();
                });
            } catch (Exception e) {
                Log.e(TAG, "加载设备数据失败", e);
                runOnUiThread(() -> {
                    // 加载失败也要显示UI
                    updateEmptyView();
                });
            }
        }, "DeviceLoader").start();

        /**0814 tt新增蓝牙打开提醒**/
        mBinding.addDevice.setOnClickListener(new ThrottleClickListener() {
            @Override
            protected void onThrottleClick(View v) {
                if (BluetoothCommon.isBluetoothEnabled()) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        new ToastUtilOverApplication().showToast(MainActivity.this,
//                                "请先授予蓝牙相关权限");
                        requestPermission();
                        return;
                    }
                    showAddDeviceDialog(true, false);
                    startUsbSearch();
                    mBinding.addDevice.setVisibility(View.GONE);
                    mBinding.tvNoDeviceLin.setVisibility(View.GONE);
                } else {
                    bluetoothDialog();
                }
            }
        });

        mBinding.backImage.setOnClickListener(v -> finish());

        mBinding.addDeviceContainer.setOnClickListener(new ThrottleClickListener() {
            @Override protected void onThrottleClick(View v) {

                if (BluetoothCommon.isBluetoothEnabled()) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        new ToastUtilOverApplication().showToast(MainActivity.this,
//                                "请先授予蓝牙相关权限");
                        requestPermission();
                        return;
                    }
                    showAddDeviceDialog(true, false);
                    startUsbSearch();
                    mBinding.addDevice.setVisibility(View.GONE);
                    mBinding.tvNoDeviceLin.setVisibility(View.GONE);
                } else {
                    bluetoothDialog();
                }
            }
        });
        mBinding.about1.setOnClickListener(new ThrottleClickListener() {
            @Override
            protected void onThrottleClick(View v) {
                PrivacyPolicyDialogFragment.newInstance().show(
                        getSupportFragmentManager(),
                        "privacy_policy_dialog"
                );
            }
        });

        // 连续点击 app_main 15次显示版本名称
        setupVersionClickDetector();
    }


    private void configureDialogViews(DialogUserAgreementBinding bind, CustomDialog customDialog) {

        bind.cbUserAgree.setBackgroundResource(R.drawable.checkbox_gray);
        bind.cbUserAgree.setChecked(false);
        bind.cbUserAgree.setEnabled(true);
        // WebView 加载 + 滚动监听
        bind.wvView.loadUrl("file:///android_asset/service_agreement_black.html");
        bind.wvView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url) {
                bind.wvView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (bind.wvView.getHeight() + scrollY + 20 >=
                            bind.wvView.getContentHeight() * bind.wvView.getScale()) {
                        mHasReadBottom = true;
                        if (!bind.cbUserAgree.isChecked()) {
                            bind.cbUserAgree.setBackgroundResource(R.drawable.checkbox_available);
                        }
                    }
                });
            }

            // 拦截隐私政策链接点击
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("privacy/index_light.html")) {
                    openFullPrivacyPolicy();
                    return true; // 拦截，不加载到当前WebView
                }
                return false; // 其他链接正常处理
            }

            // 兼容旧版本API
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("privacy/index_light.html")) {
                    openFullPrivacyPolicy();
                    return true;
                }
                return false;
            }
        });

        bind.cbUserAgree.setOnClickListener(v -> {
            if (!mHasReadBottom) {
                new ToastUtilOverApplication().showToast(this,
                        getString(R.string.user_agreement_please_agree_check));
                bind.cbUserAgree.setChecked(false);
                return;
            }
            bind.cbUserAgree.setBackgroundResource(
                    bind.cbUserAgree.isChecked() ? R.drawable.checkbox_done
                            : R.drawable.checkbox_available);
            int textColor = bind.cbUserAgree.isChecked()
                    ? Color.parseColor("#FFFFFF")
                    : Color.parseColor("#959595");
            bind.tvAgree.setTextColor(textColor);
        });

        bind.tvExit.setOnClickListener(v -> finish());
        bind.backImage.setOnClickListener(v -> finish());

        bind.tvAgree.setOnClickListener(v -> {
            if (bind.cbUserAgree.isChecked()) {
                LocalStoreManager.getInstance().storeData("key.store.device.agreed", "1");
                customDialog.dismiss();
                //initView();
            } else {
                new ToastUtilOverApplication().showToast(this,
                        getString(R.string.user_agreement_please_agree));
            }
        });

    }

    public void updateDeviceList(SmartDevice device) {
        if (smartDeviceList == null) return;

        int index = -1;
        for (int i = 0; i < smartDeviceList.size(); i++) {
            if (smartDeviceList.get(i).getDeviceName().equals(device.getDeviceName())) {
                index = i;
                break;
            }
        }
        if (index == -1) {          // 列表里根本没有这台设备
            Log.d(TAG, "updateDeviceList: 列表中不存在 " + device.getDeviceName() + "，忽略");
            return;
        }

        // 真正更新
        smartDeviceAdapter.updateItem(index, device);

        // ✅ 只有非数据库管理的设备才写SP（妥妥帖=3, 香氛=4不写SP）
        if (device.getDeviceType() != 3 && device.getDeviceType() != 4) {
            saveOtherDevicesToSP();
        }
    }

    private final Observer<Integer> displayMKStatusObserver = displayMKStatus -> {
        Log.i(TAG, "当前displayMKStatus: " + displayMKStatus);

        SmartDevice existingDevice = findDeviceByType(2);
        if (existingDevice == null) {
            Log.d("LIVE", "列表中无中控物理按键，忽略");
            return;
        }

        int newStatus = (displayMKStatus == 13) ? 3 : 1;

        updateDeviceList(new SmartDevice(
                existingDevice.getDeviceName(),
                2,
                2,
                newStatus
        ));
    };

    private SmartDevice findDeviceByType(int deviceType) {
        if (smartDeviceList == null) return null;
        for (SmartDevice device : smartDeviceList) {
            if (device.getDeviceType() == deviceType) {
                return device;
            }
        }
        return null;
    }

    private static boolean sBlurApplied = false;

    @Override
    protected void onResume() {
        super.onResume();
        if (mUsbThread == null || !mUsbThread.isAlive()) {
            mUsbThread = new HandlerThread("usb_poll");
            mUsbThread.start();
            mUsbHandler = new Handler(mUsbThread.getLooper());
        }
        if(VehicleTypeConstants.INSTANCE.isMega()) {
            /**tt新增毛玻璃**/
            WallpaperManager wm = getSystemService(WallpaperManager.class);
            showBlurBg(wm);

        }
        if(CarPlatformConstants.INSTANCE.isIncludeInstrumentPanel()) {
            //0804:fc:注册USB广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(mUsbReceiver, filter);
        }


        mViewModel.startObserveDisplayMKStatus();
        mViewModel.displayMKStatusLiveData.observe(this, displayMKStatusObserver);

    }


    //具体设备触发点击事件
    @Override
    public void onDeviceItemClick(SmartDevice device,int action) {
        int deviceType = device.getDeviceType();
        Log.i(TAG, "onDeviceItemClick-deviceType: " + deviceType+ "设备名称： " +device.getDeviceName());
        switch (deviceType) {
            case 1: // 小仪表
                showUsbDeviceConnectedWindowView(device,action);
                break;
            case 2: // 中控物理按键
                showPhysicalButtonConnectedWindowView(device);
                break;
            case 3: // 妥妥贴
                showAigileConnectedWindowView(device,action);
                break;
            case 4: // 香薰设备
                showFragranceConnectedWindowView(device,action);
                break;
            default:
                Toast.makeText(this, "未知设备类型", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDeviceRemoved(SmartDevice device,int pos) {
        if(device.getDeviceType()==3) {//灵动帖
            if (mLingDongTieManager != null) {
                mLingDongTieManager.deleteDevice(device.getDeviceId(), new Function1<Boolean, Unit>() {
                    @Override
                    public Unit invoke(Boolean deleted) {
                        if(deleted){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    smartDeviceList.remove(pos);
                                    smartDeviceAdapter.notifyItemRemoved(pos);
                                    if (smartDeviceList.isEmpty()) {
                                        restoreMainView();
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(MainActivity.this, "删除设备失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                        return null;
                    }
                });
            }
            // ✅ 不操作SP，数据库会自动删除
        } else if(device.getDeviceType()==4) {//香薰设备
            // 使用成员变量访问
            if (mFragranceManager != null) {
                mFragranceManager.deleteDevice(device.getDeviceId(), new Function1<Boolean, Unit>() {
                    @Override
                    public Unit invoke(Boolean deleted) {
                        if(deleted){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    smartDeviceList.remove(pos);
                                    smartDeviceAdapter.notifyItemRemoved(pos);
                                    if (smartDeviceList.isEmpty()) {
                                        restoreMainView();
                                    }
                                }
                            });

                        }else{
                            Toast.makeText(MainActivity.this, "删除设备失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                        return null;
                    }
                });
            }
            // ✅ 不操作SP，数据库会自动删除
        } else {
            // ✅ 其他设备（小仪表、中控物理按键）从SP中删除
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    smartDeviceList.remove(pos);
                    smartDeviceAdapter.notifyItemRemoved(pos);
                    saveOtherDevicesToSP();
                    if (smartDeviceList.isEmpty()) {
                        restoreMainView();
                    }
                }
            });
        }


    }

    public void onPowerStateChanged(SmartDevice device, boolean newPowerState) {
        Log.d("PowerState", device.getDeviceName() + " 电源状态改为: " + newPowerState);
        if (mFragranceManager != null) {
            mFragranceManager.setPowerOn(device.getDeviceId(),newPowerState);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.stopObserveDisplayMKStatus();
        mViewModel.displayMKStatusLiveData.removeObserver(displayMKStatusObserver);
        if(ipDialog != null && ipDialog.isVisible()){
            ipDialog.dismiss();
        }
    }
    public void  onDeviceDisconnected(SmartDevice device) {
        PairingAndConnectViewModel viewModel = new ViewModelProvider(this)
                .get(PairingAndConnectViewModel.class);

        // 调用非 suspend 版本的方法
        viewModel.disconnectDevice(device.getDeviceId());
        if(device.getDeviceType()==3 ) {//灵动帖
            //断开连接
            if (mLingDongTieManager != null) {
                String deviceId = device.getDeviceId();
                mLingDongTieManager.disconnected(deviceId);
            }
        }else if(device.getDeviceType()==4){//香氛
            if (mFragranceManager != null) {
                String deviceId = device.getDeviceId();
                mFragranceManager.disconnected(deviceId);
            }
        }
    }


    public void onDeviceRename(SmartDevice device, String newName) {
        Log.d("newName", "设备重命名: " + device.getDeviceName() + " -> " + newName);
        // 更新设备名称
        device.setDeviceName(newName);
        int type = device.getDeviceType();
        switch (type) {
            case 3: // 灵动贴
                mLingDongTieManager.renameDevice(device.getDeviceId(), newName);
                break;
            case 4: // 香氛
                mFragranceManager.renameDevice(device.getDeviceId(), newName);
                break;
            case 1: // 小仪表（或其他）
            default:
                break;
        }
        LocalStoreManager.getInstance().storeData("key.store.device", smartDeviceList);
    }

    @Override
    public void onCancelConnectWhenConnecting(SmartDevice device) {
        if (mLingDongTieManager != null && device.getDeviceType() == 3) {
            mLingDongTieManager.disconnected(device.getDeviceId());
        }
        if (mFragranceManager != null && device.getDeviceType() == 4) {
            mFragranceManager.disconnected(device.getDeviceId());
        }
    }


    public void showPhysicalButtonConnectedWindowView(SmartDevice device){
//           if (device.getConnectStatus() == 3) { //已连接
//            showDeviceConnectedWindow();
//        }
    }

    public void showAigileConnectedWindowView(SmartDevice device,int action){
        Log.i(TAG, "showAigileConnectedWindowView: "+ device.getDeviceId());
        String deviceMac = device.getDeviceId();
        if (action==1&&device.getConnectStatus() == 1) { //未连接
            Log.i(TAG, "showAigileConnectedWindowView: 1");
            //弹框
            if (mLingDongTieManager != null) {
                mLingDongTieManager.connectToDevice(device.getDeviceId());
            }

        } else if (action==3 && device.getConnectStatus() == 3) { //已连接
            Log.i(TAG, "showAigileConnectedWindowView: 3");
            showLinkSetDialog(deviceMac);
        }
    }

    public void showFragranceConnectedWindowView(SmartDevice device, int action) {
        Log.i(TAG, "showFragranceConnectedWindowView: " + device.getDeviceId());
        String deviceMac = device.getDeviceId();
        if (action == 1 && device.getConnectStatus() == 1) { // 未连接
            // 使用单例访问
            if (mFragranceManager != null) {
                mFragranceManager.connectToDevice(device.getDeviceId());
            }
        } else if (action == 3 && device.getConnectStatus() == 3) { // 已连接
            Log.i(TAG, "showFragranceConnectedWindowView: 已连接，显示设置对话框");
            showFragranceSetDialog(deviceMac);
        }
    }

    // 蓝牙确认对话框
    private void bluetoothDialog() {
        Dialog dialog = new Dialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bluetooth, null);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(view);
        showBlurDialog(dialog);

        view.findViewById(R.id.confirm_yes).setOnClickListener(v -> {
            requestPermission();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                new ToastUtilOverApplication().showToast(this, "请先授予蓝牙相关权限");
                requestPermission();
                return;
            }
            dialog.dismiss();
            if (bt == null) bt = new BluetoothCommon(this);
            tryOpenBluetooth();
            showAddDeviceDialog(true, true);
            startUsbSearch();
            mBinding.addDevice.setVisibility(View.GONE);
            mBinding.tvNoDeviceLin.setVisibility(View.GONE);
        });


        view.findViewById(R.id.confirm_no).setOnClickListener(v -> {
            dialog.dismiss();
            showAddDeviceDialog(false, false);
            startUsbSearch();
            mBinding.addDevice.setVisibility(View.GONE);
            mBinding.tvNoDeviceLin.setVisibility(View.GONE);
        });
    }

    private Runnable mSearchTimeoutRunnable = new Runnable() {
        @Override public void run() {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                showNoDeviceDialog();
            }
        }
    };
    private void showAddDeviceDialog(Boolean isWirelessConnection, boolean delayScanBluetoothDevice) {
        //清掉旧延时任务和弹窗
        mHandler.removeCallbacks(mSearchTimeoutRunnable);
        mHandler2.removeCallbacksAndMessages(null);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        mDialog = new Dialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_device, null);
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.setContentView(view);
        showBlurDialog(mDialog);

        if (isWirelessConnection) {
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (mLingDongTieManager != null) {
                        mLingDongTieManager.stopScan();
                    }
                    // 使用成员变量访问
                    if (mFragranceManager != null) {
                        mFragranceManager.stopScan();
                    }
                }
            });

            if(!delayScanBluetoothDevice) {
                // 开始无线扫描
                if (mLingDongTieManager != null) {
                    mLingDongTieManager.startScan();
                }

                // 使用成员变量访问
                if (mFragranceManager != null) {
                    mFragranceManager.startScan();
                }
            }else{
                mHandler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 开始无线扫描
                        if (mLingDongTieManager != null) {
                            mLingDongTieManager.startScan();
                        }

                        // 使用成员变量访问
                        if (mFragranceManager != null) {
                            mFragranceManager.startScan();
                        }
                    }
                },5000);
            }
        } else {
            // 有线连接：设置不同的OnDismissListener或不设置
            mDialog.setOnDismissListener(null);

            // 有线连接：可以执行其他操作，比如搜索有线设备
            // startWiredDeviceSearch();
        }

        // 无论有线还是无线，都设置超时任务
        mHandler.postDelayed(mSearchTimeoutRunnable, 240_000);

        view.findViewById(R.id.toast_device_disconnect_text_bottom).setOnClickListener(v -> {
            mHandler.removeCallbacks(mSearchTimeoutRunnable);
            mDialog.dismiss();
            restoreMainView();
        });
    }


    public void restoreMainView() {
        isSearching = false;
        if (smartDeviceList != null && !smartDeviceList.isEmpty()) {
            mBinding.homeRecyclerview.setVisibility(View.VISIBLE);
            mBinding.addDeviceContainer.setVisibility(View.VISIBLE);
            mBinding.addDevice.setVisibility(View.GONE);
            mBinding.tvNoDeviceLin.setVisibility(View.GONE);
        } else {
            mBinding.homeRecyclerview.setVisibility(View.GONE);
            mBinding.addDeviceContainer.setVisibility(View.GONE);
            mBinding.addDevice.setVisibility(View.VISIBLE);
            mBinding.tvNoDeviceLin.setVisibility(View.VISIBLE);
        }
    }


    private synchronized void addNewDevice(SmartDevice device) {

        // 检查设备是否已存在（通过MAC地址去重）
        String deviceMacAddress = null;
        if (device instanceof AgileSmartDevice) {
            deviceMacAddress = ((AgileSmartDevice) device).getMacAddress();
        } else {
            deviceMacAddress = device.getDeviceId();
        }

        if (deviceMacAddress != null) {
            // 检查内存列表中是否已存在相同MAC地址的设备
            for (SmartDevice existingDevice : smartDeviceList) {
                String existingMacAddress = null;
                if (existingDevice instanceof AgileSmartDevice) {
                    existingMacAddress = ((AgileSmartDevice) existingDevice).getMacAddress();
                } else {
                    existingMacAddress = existingDevice.getDeviceId();
                }

                if (deviceMacAddress.equals(existingMacAddress)) {
                    Log.i(TAG, "设备已存在，跳过添加: " + deviceMacAddress);
                    return; // 设备已存在，不重复添加
                }
            }
        }

        smartDeviceList.add(device);

        // ✅ 只有非数据库管理的设备才写SP（妥妥帖=3, 香氛=4不写SP）
        if (device.getDeviceType() != 3 && device.getDeviceType() != 4) {
            saveOtherDevicesToSP();
        }

        if (smartDeviceAdapter != null) {
            smartDeviceAdapter.notifyItemInserted(smartDeviceList.size() - 1);
        }
        updateEmptyView();
    }

    /**
     * 公共方法：供辅助类调用，添加新设备
     */
    public synchronized void addNewDevicePublic(SmartDevice device) {
        addNewDevice(device);
    }

    /**
     * 公共方法：检查设备是否已在内存列表中
     */
    public boolean isDeviceInMemory(String macAddress) {
        if (smartDeviceList == null) {
            return false;
        }
        synchronized (smartDeviceList) {
            for (SmartDevice existingDevice : smartDeviceList) {
                String existingMacAddress = null;
                if (existingDevice instanceof AgileSmartDevice) {
                    existingMacAddress = ((AgileSmartDevice) existingDevice).getMacAddress();
                } else {
                    existingMacAddress = existingDevice.getDeviceId();
                }

                if (macAddress.equals(existingMacAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 公共方法：显示蓝牙对话框
     */
    public void showBluetoothDialog() {
        bluetoothDialog();
    }

    /**
     * 公共方法：如果搜索对话框正在显示，则关闭它
     */
    public void dismissSearchDialogIfShowing() {
        if (mDialog != null && mDialog.isShowing()) {
            runOnUiThread(() -> {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            });
        }
    }

    /**
     * 公共方法：显示妥妥贴连接失败对话框
     */
    public void showTuoTuoTieConnectFailDialog(String address) {
        if (smartDeviceAdapter != null) {
            runOnUiThread(() -> {
                if (mTuoTuoTieConnectFailDialog != null && mTuoTuoTieConnectFailDialog.isShowing()) {
                    mTuoTuoTieConnectFailDialog.findViewById(R.id.toast_device_connected_text_bottom).setOnClickListener(v -> {
                        mTuoTuoTieConnectFailDialog.dismiss();
                        smartDeviceAdapter.showConnectingDialog(smartDeviceAdapter.findPosition(address));
                    });
                } else {
                    mTuoTuoTieConnectFailDialog = smartDeviceAdapter.showConnectFailDialog(smartDeviceAdapter.findPosition(address));
                }
            });
        }
    }
    /**
     * 公共方法：显示妥妥贴连接失败对话框
     */
    public void showFragranceConnectFailDialog(String address) {
        if (smartDeviceAdapter != null) {
            runOnUiThread(() -> {
                if (mFragranceConnectFailDialog != null && mFragranceConnectFailDialog.isShowing()) {
                    mFragranceConnectFailDialog.findViewById(R.id.toast_device_connected_text_bottom).setOnClickListener(v -> {
                        mFragranceConnectFailDialog.dismiss();
                        smartDeviceAdapter.showConnectingDialog(smartDeviceAdapter.findPosition(address));
                    });
                } else {
                    mFragranceConnectFailDialog = smartDeviceAdapter.showConnectFailDialog(smartDeviceAdapter.findPosition(address));
                }
            });
        }
    }

    /**
     * 公共方法：更新设备状态
     */
    public void updateLingDongTieStatus(Device dataBaseDevice) {
        if (smartDeviceAdapter == null) {
            return;
        }

        String checkedAddress = dataBaseDevice.getMacAddress();
        List<SmartDevice> smartDeviceItems = smartDeviceAdapter.getSmartDeviceItems();

        // 使用迭代器遍历，避免多线程数据不一致
        boolean changed = false;
        java.util.Iterator<SmartDevice> iterator = smartDeviceItems.iterator();
        while (iterator.hasNext()) {
            SmartDevice smartDevice = iterator.next();
            if (checkedAddress.equals(smartDevice.getDeviceId())) {
                if (dataBaseDevice.getLastConnectionState() == ConnectionState.CONNECTED) {
                    if (smartDevice.getConnectStatus() != 3) {
                        smartDevice.setConnectStatus(3);
                        changed = true;
                    }
                } else {
                    if (smartDevice.getConnectStatus() != 1) {
                        smartDevice.setConnectStatus(1);
                        changed = true;
                    }
                }
                if (dataBaseDevice.getBatteryLevel() == null) {
                    if (smartDevice.getDeviceBattery() != 0) {
                        smartDevice.setDeviceBattery(0); // 设置电池电量
                        changed = true;
                    }
                } else {
                    int batteryLevel = dataBaseDevice.getBatteryLevel();
                    if (smartDevice.getDeviceBattery() != batteryLevel) {
                        smartDevice.setDeviceBattery(batteryLevel); // 设置电池电量
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            runOnUiThread(() -> smartDeviceAdapter.notifyDataSetChanged());
        }
    }

    /**
     * 公共方法：更新香薰设备状态
     */
    public void updateFragranceDeviceStatus(FragranceDevice fragranceDevice) {
        if (smartDeviceAdapter == null) {
            return;
        }

        String checkedAddress = fragranceDevice.getMacAddress();
        List<SmartDevice> smartDeviceItems = smartDeviceAdapter.getSmartDeviceItems();

        // 使用迭代器遍历，避免多线程数据不一致
        boolean changed = false;
        java.util.Iterator<SmartDevice> iterator = smartDeviceItems.iterator();
        while (iterator.hasNext()) {
            SmartDevice smartDevice = iterator.next();
            if (checkedAddress.equals(smartDevice.getDeviceId())) {
                // 更新连接状态
                if (fragranceDevice.getConnectionState() == com.smartlife.fragrance.data.model.ConnectionState.CONNECTED) {
                    if (smartDevice.getConnectStatus() != 3) {
                        smartDevice.setConnectStatus(3);
                        changed = true;
                    }
                } else {
                    if (smartDevice.getConnectStatus() != 1) {
                        smartDevice.setConnectStatus(1);
                        changed = true;
                    }
                }
                // 香薰设备通常没有电池，保持为0
                if (fragranceDevice.getBatteryLevel() != smartDevice.getDeviceBattery()) {
                    smartDevice.setDeviceBattery(fragranceDevice.getBatteryLevel());
                    changed = true;
                }
                //香薰开关状态是否改变
                if ((fragranceDevice.getPowerState() == PowerState.ON && !smartDevice.isPowerOn())) {
                    smartDevice.setIsPowerOn(true);
                    changed = true;
                }

                //香薰开关状态是否改变
                if ((fragranceDevice.getPowerState() == PowerState.OFF && smartDevice.isPowerOn())) {
                    smartDevice.setIsPowerOn(false);
                    changed = true;
                }
            }
        }
        if (changed) {
            runOnUiThread(() -> smartDeviceAdapter.notifyDataSetChanged());
        }
    }

    /**
     * 判断某个设备名称是否已经在卡片列表中
     */
    private boolean isDeviceAdded(int deviceType) {
        if (smartDeviceList == null) return false;
        // 使用迭代器遍历，避免多线程数据不一致
        java.util.Iterator<SmartDevice> iterator = smartDeviceList.iterator();
        while (iterator.hasNext()) {
            SmartDevice device = iterator.next();
            if (device.getDeviceType() == deviceType) {
                return true;
            }
        }
        return false;
    }

    private void stopUsbSearch() {
        isSearching = false;
        if (statusCheckRunnable != null) {
            mHandler.removeCallbacks(statusCheckRunnable);
            statusCheckRunnable = null;
        }
    }

    private void startUsbSearch() {
        Log.i(TAG, "开始搜索USB设备，当前的isSearching =" +isSearching);
        isSearching = true;
        if (mUsbThread == null || !mUsbThread.isAlive()) {
            mUsbThread = new HandlerThread("usb_poll");
            mUsbThread.start();
            mUsbHandler = new Handler(mUsbThread.getLooper());
        }

        // 先移除旧的回调再启动新的
        mUsbHandler.removeCallbacks(pollRunnable);
        mUsbHandler.post(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSearching) {
                Log.i(TAG,"不在搜索中，返回");
                mUsbHandler.removeCallbacks(pollRunnable);
                return;
            }

            // 1.子线程
            UsbDevice dev;
            if(CarPlatformConstants.INSTANCE.isIncludeInstrumentPanel()){
                dev = UsbConnectionUtil.getIpDevice(MainActivity.this, 30503, 258);
            }else{
                Log.i(TAG,"不包含小仪表");
                dev = null;
            }

            boolean hasSmall = dev != null && !isDeviceAdded(1);
            boolean hasKey  = MKDisplayStatus.INSTANCE.displayMKStatus(getApplicationContext()) == 13
                    && !isDeviceAdded(2)&&isSearching;

            // 2. 更改UI的回主线程
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {

                    if (hasSmall) addNewDevice(new SmartDevice("小仪表", 1, 0, 1,System.currentTimeMillis(),""));
                    if (hasKey)   addNewDevice(new SmartDevice("中控物理按键", 2, 2, 1,System.currentTimeMillis(),""));
                    updateEmptyView();
                    if ((hasSmall || hasKey) && mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                        isSearching = false;
                    }
                }
            });
            Log.i(TAG, "this 指向：" + this);
            // 3. 循环
            mUsbHandler.postDelayed(this, 5_000);
        }
    };




    /* 统一控制“空页面” vs “列表页面” */
    private void updateEmptyView() {
        boolean hasDevice = smartDeviceList.size() > 0;
        mBinding.homeRecyclerview.setVisibility(hasDevice ? View.VISIBLE : View.GONE);
        mBinding.addDeviceContainer.setVisibility(hasDevice ? View.VISIBLE : View.GONE);
        mBinding.addDevice.setVisibility(hasDevice ? View.GONE : View.VISIBLE);
        mBinding.tvNoDeviceLin.setVisibility(hasDevice ? View.GONE : View.VISIBLE);
    }


    /***扫描结果为空***/
    private void showNoDeviceDialog() {
        isSearching = false;
        Dialog noDeviceDialog = new Dialog(this);
        noDeviceDialog.setContentView(R.layout.null_device_dialog);
        noDeviceDialog.setCancelable(true);
        noDeviceDialog.setCanceledOnTouchOutside(false);
        noDeviceDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        showBlurDialog(noDeviceDialog);

        // 重新搜索
        noDeviceDialog.findViewById(R.id.searching_again).setOnClickListener(v -> {
            noDeviceDialog.dismiss();
            showAddDeviceDialog(true, false);
        });

        // 关闭
        noDeviceDialog.findViewById(R.id.searching_close).setOnClickListener(v -> {
            noDeviceDialog.dismiss();
            restoreMainView();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        isSearching = false;
        stopUsbSearch();
        if (mUsbHandler != null) {
            mUsbHandler.removeCallbacksAndMessages(null);
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler2.removeCallbacksAndMessages(null);
        if (mUsbThread != null) {
            mUsbThread.quitSafely();
            mUsbThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void tryOpenBluetooth() {
        if (bt == null) bt = new BluetoothCommon(this);
        // 如果没有权限，就去申请；有权限就直接开
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bt.openBt()) {
                new ToastUtilOverApplication().showToast(this,
                        getString(R.string.open_bluetooth));
            }
        } else {
            new ToastUtilOverApplication().showToast(this, "没有蓝牙相关权限");
        }

    }


    private void requestPermission() {
        List<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(PermissionUtil.Companion.getBluetoothScanAndConnectPermissions()));
        permissions.add(PermissionUtil.POST_NOTIFICATIONS_PERMISSION);
        permissionUtil.requestPermissions(permissions);
        Log.i(TAG, "申请权限中,需要用户手动确认");
    }

    /**
     * 显示权限被拒绝的提示对话框
     * @param deniedPermissions 被拒绝的权限名称
     */
    private void showPermissionDeniedTips(String deniedPermissions) {
        NoPermissionFragmentDialog dialog = NoPermissionFragmentDialog.newInstance("", "");
        dialog.show(getSupportFragmentManager(), "permission_denied_tips", new NoPermissionFragmentDialog.OnTipsDialogClickListener() {
            @Override
            public void onConfirm() {
                // 用户点击确认，可以在这里添加跳转到设置页面的逻辑
                finish();
                Log.i(TAG, "用户确认了权限提示");
            }

            @Override
            public void onDismiss() {
                Log.i(TAG, "权限提示对话框被关闭");
            }
        });
    }

    /**
     * 从数据库加载已有设备（仅首次加载）
     * 后续的数据库变化通过现有的 handleDeviceChanged 和 handleFragranceDeviceChanged 回调处理
     */
    private void loadExistingDevicesFromDatabase() {
        // 妥妥帖设备 - 使用LingDongTieManager获取
        if (mLingDongTieManager != null) {
            List<Device> devices = mLingDongTieManager.getDevices();
            if (devices != null && !devices.isEmpty()) {
                for (Device device : devices) {
                    if (!isDeviceInMemory(device.getMacAddress())) {
                        SmartDevice smartDevice = convertTuoTuoTieToSmartDevice(device);
                        addNewDevicePublic(smartDevice);
                    }
                }
            }
        }

        // 香氛设备 - 使用FragranceManager获取
        if (mFragranceManager != null) {
            List<FragranceDevice> fragranceDevices = mFragranceManager.getDeviceList();
            if (fragranceDevices != null && !fragranceDevices.isEmpty()) {
                for (FragranceDevice device : fragranceDevices) {
                    if (!isDeviceInMemory(device.getMacAddress())) {
                        SmartDevice smartDevice = convertFragranceToSmartDevice(device);
                        addNewDevicePublic(smartDevice);
                    }
                }
            }
        }
    }

    /**
     * 从SP加载非数据库管理的设备（小仪表、中控物理按键）
     */
    private void loadOtherDevicesFromSP() {
        List<SmartDevice> storeData = LocalStoreManager.getInstance()
                .getStoreData("key.store.device", SmartDevice.class);

        if (storeData != null) {
            for (SmartDevice device : storeData) {
                // 只加载非妥妥帖和香氛的设备
                if (device.getDeviceType() != 3 && device.getDeviceType() != 4) {
                    smartDeviceList.add(device);
                }
            }
        }
    }

    /**
     * 保存非数据库管理的设备到SP
     */
    private void saveOtherDevicesToSP() {
        List<SmartDevice> otherDevices = filterOtherDevices();
        LocalStoreManager.getInstance().storeData("key.store.device", otherDevices);
    }

    /**
     * 过滤获取非数据库管理的设备
     */
    private List<SmartDevice> filterOtherDevices() {
        List<SmartDevice> otherDevices = new ArrayList<>();
        for (SmartDevice device : smartDeviceList) {
            if (device.getDeviceType() != 3 && device.getDeviceType() != 4) {
                otherDevices.add(device);
            }
        }
        return otherDevices;
    }

    /**
     * 妥妥帖数据库设备转换为SmartDevice
     */
    private SmartDevice convertTuoTuoTieToSmartDevice(Device device) {
        int connectStatus = device.getLastConnectionState() == ConnectionState.CONNECTED ? 3 : 1;
        AgileSmartDevice smartDevice = new AgileSmartDevice(device.getName(), 3, 1, connectStatus,device.getCreatedAt(),device.getBleName());
        smartDevice.setMacAddress(device.getMacAddress());
        smartDevice.setBatteryLevel(device.getBatteryLevel() != null ? device.getBatteryLevel() : 0);
        return smartDevice;
    }

    /**
     * 香氛数据库设备转换为SmartDevice
     */
    private SmartDevice convertFragranceToSmartDevice(FragranceDevice device) {
        int connectStatus = device.getConnectionState() ==
                com.smartlife.fragrance.data.model.ConnectionState.CONNECTED ? 3 : 1;
        SmartDevice smartDevice = new SmartDevice(
                device.getDisplayName(),
                4,  // deviceType: 4 表示香薰设备
                1,  // deviceCategory
                connectStatus,
                device.getCreatedAt(),
                ""
        );
        smartDevice.setDeviceId(device.getMacAddress());
        smartDevice.setDeviceBattery(device.getBatteryLevel());
        smartDevice.setIsPowerOn(device.getPowerState() == PowerState.ON);
        return smartDevice;
    }

    /**
     * 数据迁移（首次启动清理SP中的旧数据）
     */
    private void migrateDataOnFirstLaunch() {
        String migratedStr = LocalStoreManager.getInstance().read("key.store.device.migrated.v2", "0");
        boolean migrated = "1".equals(migratedStr);
        if (!migrated) {
            // 从SP中移除妥妥帖和香氛设备
            List<SmartDevice> allDevices = LocalStoreManager.getInstance()
                    .getStoreData("key.store.device", SmartDevice.class);

            if (allDevices != null) {
                List<SmartDevice> otherDevices = new ArrayList<>();
                for (SmartDevice device : allDevices) {
                    if (device.getDeviceType() != 3 && device.getDeviceType() != 4) {
                        otherDevices.add(device);
                    }
                }
                LocalStoreManager.getInstance().storeData("key.store.device", otherDevices);
            }

            // 标记已迁移
            LocalStoreManager.getInstance().write("key.store.device.migrated.v2", "1");

            Log.i(TAG, "数据迁移完成：已清理SP中的妥妥帖和香氛设备");
        }
    }


    private void openFullPrivacyPolicy() {
        String privacyUrl = getString(R.string.privacy_policy_URL);
        WebViewDialogFragment webViewDialog = WebViewDialogFragment.newInstance(privacyUrl);
        webViewDialog.show(getSupportFragmentManager(), "webview_dialog");
    }

    /**
     * 设置连续点击检测器：连续点击 app_main 15次后显示版本名称
     */
    private void setupVersionClickDetector() {
        resetClickCountRunnable = () -> {
            clickCount = 0;
            Log.d(TAG, "点击计数已重置");
        };

        mBinding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 移除之前的重置任务
                mHandler.removeCallbacks(resetClickCountRunnable);

                clickCount++;
                Log.d(TAG, "点击次数: " + clickCount);

                if (clickCount >= REQUIRED_CLICKS) {
                    // 达到15次，显示版本名称
                    showVersionName();
                    clickCount = 0; // 重置计数
                } else {
                    // 设置重置任务，如果2秒内没有继续点击则重置计数
                    mHandler.postDelayed(resetClickCountRunnable, CLICK_TIMEOUT);
                }
            }
        });
    }

    /**
     * 显示版本名称
     */
    private void showVersionName() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;

            Log.i(TAG, "版本名称: " + versionName);

            // 使用 Toast 显示版本名称
            //Toast.makeText(this, "版本号: " + versionName, Toast.LENGTH_LONG).show();
            new ToastUtilOverApplication().showToast(this,"this, \"版本号: \"  "+versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取版本名称失败", e);
            Toast.makeText(this, "获取版本信息失败", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * fc:小仪表部分：1.初始化仪表服务 2.订阅仪表数据 3.显示仪表数据
     */

    private InstrumentPanelViewModel instrumentPanelViewModel;

    private InstrumentPanelDialog ipDialog;


    private void initInstrumentPanel() {
        Log.i(TAG, "初始化小仪表 initInstrumentPanel start");
        instrumentPanelViewModel =  IApplication.getInstrumentPanelViewModel();
        Log.i(TAG, "初始化小仪表 initInstrumentPanel =="+instrumentPanelViewModel);
        instrumentPanelViewModel.init();
        // 观察连接状态变化
        instrumentPanelViewModel.isClientConnected.observe(this,observerIpConnected);
    }

    public void showUsbDeviceConnectedWindowView(SmartDevice device,int action){
        Log.i(TAG, "showUsbDeviceConnectedWindowView");
        boolean ipIsExist = UsbConnectionUtil.checkIpDeviceExist(smartDeviceList);
        if (ipIsExist){
            if(1 == device.getConnectStatus() && 1 == action){//action 1:去连接，
                instrumentPanelViewModel.startServer();
            }else if (device.getConnectStatus() == 3 && action == 3){
                Log.i(TAG, "小仪表已连接,当前名称为："+device.getDeviceName());
                ipDialog = new InstrumentPanelDialog(this,device.getDeviceName());
                ipDialog.show(getSupportFragmentManager(), device.getDeviceName());
            }
        }else {
            Log.i(TAG, "小仪表未添加");
        }

    }

    private Observer<Integer> observerIpConnected = (connecteStatus)-> {
        Log.i(TAG, "小仪表连接状态:" + connecteStatus);
        if (smartDeviceAdapter != null) {
            smartDeviceAdapter.setClientConnected(connecteStatus,instrumentPanelViewModel.showContent.getValue());
            if (connecteStatus == 1){
                Log.i(TAG, "小仪表已连接");
                updateIpStatus(3);
            }
        }
    };

    private void updateIpStatus(int status) {
        List<SmartDevice> devices = LocalStoreManager.getInstance()
                .getStoreData("key.store.device", SmartDevice.class);
        try {
            for (SmartDevice smartDevice : devices){
                if (smartDevice.getDeviceType() == 1){
                    updateDeviceList(new SmartDevice(smartDevice.getDeviceName(), 1, 0, status) );
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "遍历小仪表出错");
        }
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!CarPlatformConstants.INSTANCE.isIncludeInstrumentPanel()){
                Log.i(TAG,"不包含小仪表");
                return;
            }
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // 处理设备连接
                Log.d(TAG, "小仪表设备插入: " + device.getDeviceName());
                List<SmartDevice> tmpDevices = LocalStoreManager.getInstance().getStoreData("key.store.device", SmartDevice.class);
                if (instrumentPanelViewModel != null && UsbConnectionUtil.checkIpDeviceExist(tmpDevices)) {
                    instrumentPanelViewModel.startServer();
                    instrumentPanelViewModel.updateRecoValue(true);
                } else {
                    Log.i(TAG, "小仪表启动条件未满足");
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // 处理设备断开
                Log.d(TAG, "小仪表设备断开: " + device.getDeviceName());
                if (ipDialog != null && ipDialog.isVisible()){
                    ipDialog.dismiss();
                }
                if (instrumentPanelViewModel != null) {
                    instrumentPanelViewModel.stopServer();
                    instrumentPanelViewModel.updateRecoValue(false);
                }
                updateIpStatus(1);
            }
        }
    };
}