@file:Suppress("UNREACHABLE_CODE")

package com.zkjd.lingdong.ui.function_kotlin

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.FragmentFunctionBinding
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.ButtonType
import com.zkjd.lingdong.model.FunctionCategory
import com.zkjd.lingdong.ui.function.FunctionViewModel
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.model.AppInfo
import com.zkjd.lingdong.repository.DeviceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.log

/**
 * 功能配置Fragment - 使用XML布局实现
 */
@AndroidEntryPoint
open class FunctionFragment : Fragment() {

    private var _binding: FragmentFunctionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FunctionViewModel by viewModels()

    private lateinit var buttonTypeAdapter: ButtonTypeAdapter
    private lateinit var functionGridAdapter: FunctionGridAdapter

    private lateinit var tabs: List<TextView>

    private var deviceMac: String = ""
    private var selectedFunction: ButtonFunction? = null

    @Inject
    lateinit var deviceRepository: DeviceRepository

    // Fragment与Activity通信的接口
    interface FunctionFragmentListener {
        fun onSaveRequested()
        fun onHasUnsavedChanges(hasChanges: Boolean)
    }

    private var listener: FunctionFragmentListener? = null

    companion object {
        private const val ARG_DEVICE_MAC = "device_mac"

        fun newInstance(deviceMac: String): FunctionFragment {
            val fragment = FunctionFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_MAC, deviceMac)
            fragment.arguments = args
            return fragment
        }
    }

    private fun selectTab(selectedIndex: Int) {
        // 重置所有选项卡状态并设置选中状态
        tabs.forEachIndexed { index, tab ->
            tab.isSelected = index == selectedIndex
        }

        viewModel.setSelectedButtonType(
            when (selectedIndex)  {
                0->ButtonType.SHORT_PRESS
                1->ButtonType.DOUBLE_CLICK
                2->ButtonType.LEFT_ROTATE
                else -> ButtonType.SHORT_PRESS
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceMac = it.getString(ARG_DEVICE_MAC) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFunctionBinding.inflate(inflater, container, false)

        viewModel.viewModelScope.launch {
            val device = deviceRepository.getDeviceByMacAddress(deviceMac)
            launch(Dispatchers.Main) {
                _binding?.titleCenter?.text = "${device?.name?:""}"
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化选项卡列表
        tabs = listOf(binding.tab1, binding.tab2, binding.tab3)

        // 设置默认选中第一个选项卡
        selectTab(0)

        // 设置选项卡点击监听
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener { selectTab(index) }
        }
        
        // 设置监听器
        if (activity is FunctionFragmentListener) {
            listener = activity as FunctionFragmentListener
        }
        
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()

        // 加载设备功能配置
        viewModel.loadDeviceFunctions(deviceMac)
    }

    private fun setupRecyclerViews() {
        // 设置按钮类型RecyclerView
        buttonTypeAdapter = ButtonTypeAdapter { buttonType ->
            viewModel.setSelectedButtonType(buttonType)
        }
        binding.rvButtonTypes.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = buttonTypeAdapter
        }

        // 设置功能网格RecyclerView
        functionGridAdapter = FunctionGridAdapter(onFunctionSelected = { function ->
            selectedFunction = function
            viewModel.tempSaveButtonFunction(viewModel.selectedButtonType.value, function)
            viewModel.saveAllButtonFunctions(deviceMac)
        }, onAppSelectionRequested = {
            showAppSelectionDialog()
        })
        binding.rvFunctionGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 6)
            adapter = functionGridAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听UI事件
                launch {
                    viewModel.uiEvents.collect { event ->
                        when (event) {
                            is FunctionViewModel.UiEvent.ShowToast -> {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        requireContext(),
                                        event.message + "2222",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            is FunctionViewModel.UiEvent.Navigate -> {
                                // Handle navigation if needed
                            }
                        }
                    }
                }

                // 监听按钮类型变化
                launch {
                    viewModel.selectedButtonType.collect { buttonType ->
                        buttonTypeAdapter.updateSelectedType(buttonType)
                        // 先更新选中的功能为当前按钮类型对应的功能
                        selectedFunction = viewModel.tempButtonFunctions.value[buttonType]
                        updateFunctionsList()
                        updateSelectedFunctionCard()
                    }
                }

                // 监听是否为旋钮设备
                launch {
                    viewModel.isRoty.collect { isRoty ->
                        buttonTypeAdapter.updateIsRoty(isRoty)
                        tabs[2].visibility =  if (isRoty) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        binding.switchPreventAccidental.visibility = if (isRoty) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }



                // 监听临时按钮功能
                launch {
                    viewModel.tempButtonFunctions.collect { tempFunctions ->
                        selectedFunction = tempFunctions[viewModel.selectedButtonType.value]
                        updateSelectedFunctionCard()
                    }
                }

                launch {
                    viewModel.useCarTypeName.collect {
                        updateFunctionsList()
                    }
                }

                // 监听是否有未保存的更改
                launch {
                    viewModel.isSave.collect { hasUnsavedChanges ->
                        listener?.onHasUnsavedChanges(hasUnsavedChanges)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {


        // 清除功能按钮
        binding.btnClearFunction.setOnClickListener {
            selectedFunction = null
            viewModel.tempSaveButtonFunction(viewModel.selectedButtonType.value, null)
            updateSelectedFunctionCard()
        }
    }

    private fun updateFunctionsList() {
        val buttonType = viewModel.selectedButtonType.value
        val carTypeName = viewModel.useCarTypeName.value
        val functionsConfig = FunctionsConfig.getInstance(requireContext())

        val functions = when {
            buttonType in ButtonType.ROTATE_TYPES -> functionsConfig.getRotaryFunctions(carTypeName)
            else -> functionsConfig.getNonRotaryFunctions(carTypeName)
        }

        functionGridAdapter.updateFunctions(functions, selectedFunction)
    }

    private fun updateSelectedFunctionCard() {
        val buttonType = viewModel.selectedButtonType.value

        // 更新功能卡片显示
        binding.tvButtonTypeTitle.text = when (buttonType) {
            ButtonType.SHORT_PRESS -> "短按功能"
            ButtonType.LONG_PRESS -> "长按功能"
            ButtonType.DOUBLE_CLICK -> "双击功能"
            ButtonType.LEFT_ROTATE -> "旋转功能 (左旋)"
            ButtonType.RIGHT_ROTATE -> "旋转功能 (右旋)"
            else -> ""
        }

        if (selectedFunction != null) {
            binding.tvFunctionName.text = when {
                selectedFunction?.category == FunctionCategory.APP -> "${selectedFunction?.name}"
                else -> selectedFunction?.name ?: "无"
            }
            binding.btnClearFunction.visibility = View.VISIBLE
            // 这里可以设置功能图标
        } else {
            binding.tvFunctionName.text = "无"
            binding.btnClearFunction.visibility = View.GONE
        }

        //新增功能显示按钮
        viewModel.tempButtonFunctions.value.forEach { elemnt ->
            val (buttonTypn, function) = elemnt
            var preButtonStr = "";

            val name = when (buttonTypn){
                ButtonType.SHORT_PRESS -> "短按"
                ButtonType.DOUBLE_CLICK -> "双击"
                ButtonType.LEFT_ROTATE -> "旋转"
                else -> null
            }

            val btn = when (buttonTypn){
                ButtonType.SHORT_PRESS -> binding.buttonPoint1
                ButtonType.DOUBLE_CLICK -> binding.buttonPoint2
                ButtonType.LEFT_ROTATE -> binding.buttonPoint3
                else -> null
            }

            if(btn!=null) {
                if (function != null) {
                    btn.text = when {
                        function?.category == FunctionCategory.APP -> name + ": ${function?.name}"
                        else -> name + ": "+ (function?.name ?: "无");
                    }
                    btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    btn.text = name + ":无";
                    btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white99))
                }
            }
        }

        // 更新功能网格适配器
        functionGridAdapter.updateSelectedFunction(selectedFunction)
    }

    private fun showAppSelectionDialog() {

        val dialog = AppFindDialog(activity,null)
        dialog.setListener(object :AppFindDialog.OnAppSelectedListener{
            override fun onAppSelected(appInfo: AppInfo) {
                val function = ButtonFunction(
                    id = "app_${appInfo.packageName}",
                    category = FunctionCategory.APP,
                    name = appInfo.appName,
                    actionCode = "APP_${appInfo.packageName}",
                    iconResId = 0,
                    configWords = ""
                )
                selectedFunction = function
                viewModel.tempSaveButtonFunction(viewModel.selectedButtonType.value, function)
                viewModel.saveAllButtonFunctions(deviceMac)
                updateSelectedFunctionCard()
                viewModel.saveAllButtonFunctions(deviceMac)
                dialog.dismiss();
            }
        })
        dialog.show(childFragmentManager, "app_selection_dialog")
    }

    /**
     * 保存所有按钮功能配置
     */
    fun saveAllButtonFunctions() {
        viewModel.saveAllButtonFunctions(deviceMac)
    }

    /**
     * 检查是否有未保存的更改
     */
    fun hasUnsavedChanges(): Boolean {
        return viewModel.isSave.value == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
