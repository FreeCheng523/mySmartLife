package com.zkjd.lingdong.ui.function_kotlin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.DialogAppSelectionBinding
import com.zkjd.lingdong.model.AppInfo
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用选择对话框Fragment
 * 包装 AppSelectionFragment 实现对话框形式的应用选择
 */
@AndroidEntryPoint
class AppSelectionDialogFragment : DialogFragment(), AppSelectionFragment.OnAppSelectedListener {
    
    private var _binding: DialogAppSelectionBinding? = null
    private val binding get() = _binding!!
    
    private var appSelectionFragment: AppSelectionFragment? = null
    private var selectedApp: AppInfo? = null
    
    // 应用选择监听器
    interface OnAppSelectedListener {
        fun onAppSelected(appInfo: AppInfo)
        fun onDialogCancelled()
    }
    
    private var listener: OnAppSelectedListener? = null
    
    fun setOnAppSelectedListener(listener: OnAppSelectedListener) {
        this.listener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置对话框样式
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // 去除标题栏
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAppSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDialog()
        setupAppSelectionFragment()
        setupClickListeners()
        
        // 处理参数
        val showBottomButtons = arguments?.getBoolean("show_bottom_buttons", false) ?: false
        setShowBottomButtons(showBottomButtons)
    }
    
    override fun onStart() {
        super.onStart()
        // 设置对话框大小
//        dialog?.window?.setLayout(
//            (resources.displayMetrics.widthPixels * 0.9).toInt(),
//            (resources.displayMetrics.heightPixels * 0.8).toInt()
//        )
        dialog?.window?.setLayout(1600, 960)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupDialog() {
        // 设置对话框可取消
        isCancelable = true
        
        // 监听对话框取消事件
        dialog?.setOnCancelListener {
            listener?.onDialogCancelled()
        }
    }
    
    private fun setupAppSelectionFragment() {
            appSelectionFragment = AppSelectionFragment.newInstance().apply {
                setOnAppSelectedListener(this@AppSelectionDialogFragment)
            }
            
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, appSelectionFragment!!)
                .commit()
    }
    
    private fun setupClickListeners() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            dismiss()
            listener?.onDialogCancelled()
        }
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
            listener?.onDialogCancelled()
        }
        
        // 确定按钮
        binding.btnConfirm.setOnClickListener {
            selectedApp?.let { app ->
                listener?.onAppSelected(app)
                dismiss()
            }
        }
    }
    
    // 实现 AppSelectionFragment.OnAppSelectedListener
    override fun onAppSelected(appInfo: AppInfo) {
        // 根据使用模式决定行为
        if (binding.layoutBottomButtons.visibility == View.VISIBLE) {
            // 如果显示了底部按钮，则先保存选择，等待用户确认
            selectedApp = appInfo
            binding.btnConfirm.isEnabled = true
        } else {
            // 如果没有底部按钮，直接返回结果并关闭对话框
            listener?.onAppSelected(appInfo)
            dismiss()
        }
    }
    
    /**
     * 设置是否显示底部确认按钮
     * @param showButtons true 显示取消/确定按钮，false 直接选择后关闭
     */
    fun setShowBottomButtons(showButtons: Boolean) {
        if (_binding != null) {
            binding.layoutBottomButtons.visibility = if (showButtons) View.VISIBLE else View.GONE
        }
    }
    
    /**
     * 设置对话框标题
     */
    fun setDialogTitle(title: String) {
        // 可以通过修改布局来支持动态标题
        // 这里暂时使用固定标题
    }
    
    companion object {
        private const val TAG = "AppSelectionDialogFragment"
        
        /**
         * 创建新的对话框实例
         * @param showBottomButtons 是否显示底部确认按钮
         */
        fun newInstance(showBottomButtons: Boolean = false): AppSelectionDialogFragment {
            return AppSelectionDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("show_bottom_buttons", showBottomButtons)
                }
            }
        }
        
        /**
         * 显示对话框
         * @param fragment 调用的Fragment
         * @param listener 选择监听器
         * @param showBottomButtons 是否显示底部按钮
         */
        fun show(
            fragment: Fragment,
            listener: OnAppSelectedListener,
            showBottomButtons: Boolean = false
        ) {
            val dialog = newInstance(showBottomButtons)
            dialog.setOnAppSelectedListener(listener)
            dialog.show(fragment.childFragmentManager, TAG)
        }
    }
}
