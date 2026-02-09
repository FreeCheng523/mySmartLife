package com.mine.baselibrary.dialog

import android.content.DialogInterface
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.mine.baselibrary.R


/**
 * 弹窗基类
 * ref:https://source.android.com/docs/core/display/window-blurs?hl=zh-cn
 * @param <VB>
</VB> */
abstract class BaseDialogFragment<VB : ViewBinding?>() : DialogFragment() {
    
    companion object {
        internal const val ARG_GRAVITY = "arg_gravity"
        internal const val ARG_X = "arg_x"
        internal const val ARG_Y = "arg_y"
        
        /**
         * 创建包含窗口位置参数的Bundle
         * @param gravity 窗口对齐方式，默认为Gravity.CENTER
         * @param x 窗口X偏移量，单位：像素
         * @param y 窗口Y偏移量，单位：像素
         */
        @JvmStatic
        fun createPositionBundle(gravity: Int = Gravity.CENTER, x: Int = 0, y: Int = 0): Bundle {
            return Bundle().apply {
                putInt(ARG_GRAVITY, gravity)
                putInt(ARG_X, x)
                putInt(ARG_Y, y)
            }
        }
    }
    @JvmField
    protected var binding: VB? = null

    @JvmField
    protected var onDialogListener: OnDialogClickListener? = null
    private var isOnTouchOutSide = false
    private var winWidth = ViewGroup.LayoutParams.MATCH_PARENT
    private var winHeight = ViewGroup.LayoutParams.MATCH_PARENT
    private var blurBehindRadius = 50 // 后方屏幕模糊半径，单位：dp
    private var backgroundBlurRadius = 30 // 窗口背景模糊半径，单位：dp
    private var _enableBlurBehind = true // 是否启用后方屏幕模糊
    private var _enableBackgroundBlur = false // 是否启用窗口背景模糊
    private var bgBlurWindowBackGroundDrawable = R.drawable.window_background_blur

    @JvmField
    val TAG: String? = this.javaClass.getSimpleName()

    constructor(isOnTouchOutSide: Boolean) : this() {
        this.isOnTouchOutSide = isOnTouchOutSide
    }

    constructor(isOnTouchOutSide: Boolean, winWidth: Int, winHeight: Int) : this(isOnTouchOutSide) {
        this.winHeight = winHeight
        this.winWidth = winWidth
    }

    constructor(isOnTouchOutSide: Boolean, winWidth: Int, winHeight: Int, enableBlurBehind: Boolean, enableBackgroundBlur: Boolean) : this(isOnTouchOutSide, winWidth, winHeight) {
        this._enableBlurBehind = enableBlurBehind
        this._enableBackgroundBlur = enableBackgroundBlur
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 根据是否启用窗口背景模糊选择不同的样式
        val styleRes = if (_enableBackgroundBlur) {
            R.style.BaseLib_BaseDialogStyle_Blur
        } else {
            R.style.BaseLib_BaseDialogStyle
        }
        setStyle(STYLE_NORMAL, styleRes)
    }

    protected abstract fun createViewBinding(inflater: LayoutInflater?, container: ViewGroup?): VB?

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val dialog = getDialog()
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(isOnTouchOutSide)
            dialog.setCancelable(isOnTouchOutSide)
            //去除蒙板
            getDialog()!!.getWindow()!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            //设置窗口后方屏幕模糊
            setupWindowBehindBlur()
        }
        binding = createViewBinding(inflater, container)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated:")
        binding!!.getRoot().setOnClickListener(View.OnClickListener { v: View? ->
            if (isOnTouchOutSide) {
                dismiss()
            }
            Log.i(TAG, "onClick: root was clicked! isOnTouchOutSide >>> " + isOnTouchOutSide)
            onRootClick(v)
        })
        val contentLayout = this.contentLayout
        if (contentLayout != null) {
            contentLayout.setOnClickListener(View.OnClickListener { v: View? ->
                Log.i(
                    TAG,
                    "onClick: contentLayout was clicked"
                )
            }) //用来消费点击事件，防止事件透传
        }
    }

    protected abstract val contentLayout: View?

    protected fun onRootClick(v: View?) {
    }

    /**
     * 设置窗口后方屏幕模糊效果
     * 根据Android版本使用不同的API
     */
    private fun setupWindowBehindBlur() {
        val window = getDialog()?.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 设置后方屏幕模糊标志 - 这是后方屏幕模糊生效的必要条件
            if (_enableBlurBehind) {
                try {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes.setBlurBehindRadius(blurBehindRadius)
                    Log.i(TAG, "setupWindowBlur: FLAG_BLUR_BEHIND 已设置")
                } catch (e: Exception) {
                    Log.w(TAG, "setupWindowBlur: 设置 FLAG_BLUR_BEHIND 失败", e)
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        setupWindowBackgroundBlurListener()
        Log.i(TAG, "onStart: winWidth:" + winWidth + ",winHeight:" + winHeight)
        val window = getDialog()!!.getWindow()

        val attributes = window!!.getAttributes()
        // 根据是否启用窗口背景模糊设置不同的像素格式
        if (_enableBackgroundBlur) {
            // 窗口背景模糊需要不透明的像素格式
            attributes.format = PixelFormat.OPAQUE
            Log.i(TAG, "onStart: 窗口背景模糊模式，使用OPAQUE格式")
        } else {
            // 普通模式使用透明格式
            attributes.format = PixelFormat.TRANSPARENT
            Log.i(TAG, "onStart: 普通模式，使用TRANSPARENT格式")
        }
        attributes.format = PixelFormat.TRANSPARENT
        attributes.width = winWidth
        attributes.height = winHeight
        
        // 从arguments中读取窗口位置参数
        arguments?.let { args ->
            val gravity = args.getInt(ARG_GRAVITY, Gravity.CENTER)
            val x = args.getInt(ARG_X, 0)
            val y = args.getInt(ARG_Y, 0)
            
            attributes.gravity = gravity
            attributes.x = x
            attributes.y = y
            
            Log.i(TAG, "onStart: 窗口位置参数 - gravity:$gravity, x:$x, y:$y")
        }

        window.setAttributes(attributes)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    /**
     * dialog显示
     * 防止重复显示
     * @param manager The FragmentManager this fragment will be added to.
     * @param tag The tag for this fragment, as per
     */
    override fun show(manager: FragmentManager, tag: String?) {
        val fragmentByTag = manager.findFragmentByTag(tag)
        if (fragmentByTag == null) {
            super.show(manager, tag)
        } else {
            Log.i(TAG, "show: the tag of fragment is already shown! the tag is " + tag)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    fun setOnDialogListener(listener: OnDialogClickListener?) {
        onDialogListener = listener
    }

    /**
     * 设置窗口背景模糊
     * 如果是代码设置，则需要监听到onViewAttachedToWindow之后才能设置
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun setupWindowBackgroundBlurListener() {
        dialog?.window?.getDecorView()?.addOnAttachStateChangeListener(object :View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(v: View) {
                dialog?.window?.windowManager?.addCrossWindowBlurEnabledListener {
                    if (_enableBackgroundBlur) {
                        // 设置窗口背景模糊半径
                        dialog?.window?.setBackgroundBlurRadius(backgroundBlurRadius)
                        dialog?.window?.setBackgroundDrawableResource(bgBlurWindowBackGroundDrawable)
                     /*   getDialog()?.getWindow()?.setFlags(
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        )*/
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {

            }

        })
    }


    /**
     * 创建圆角背景drawable
     * @param radiusDp 圆角半径，单位：dp
     */
    private fun createCornerRadiusDrawable(radiusDp: Int): android.graphics.drawable.GradientDrawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        drawable.cornerRadius = radiusDp * resources.displayMetrics.density
        drawable.setColor(0xE6FFFFFF.toInt()) // 半透明白色
        return drawable
    }

    interface OnDialogClickListener {
        fun onClick(dialog: DialogFragment?, v: View?, flag: Int): Boolean
    }


}