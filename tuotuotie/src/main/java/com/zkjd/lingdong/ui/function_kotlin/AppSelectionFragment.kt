package com.zkjd.lingdong.ui.function_kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.zkjd.lingdong.databinding.FragmentAppSelectionBinding
import com.zkjd.lingdong.model.AppInfo
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.FunctionCategory
import com.zkjd.lingdong.ui.function.AppSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 应用选择Fragment - 包含主要的应用选择逻辑
 */
@AndroidEntryPoint
class AppSelectionFragment : Fragment() {
    
    private var _binding: FragmentAppSelectionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AppSelectionViewModel by viewModels()
    
    private lateinit var appGridAdapter: AppGridAdapter
    private var allApps = listOf<AppInfo>()
    
    // 应用选择监听器
    interface OnAppSelectedListener {
        fun onAppSelected(appInfo: AppInfo)
    }
    
    private var listener: OnAppSelectedListener? = null
    
    fun setOnAppSelectedListener(listener: OnAppSelectedListener) {
        this.listener = listener
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =  FragmentAppSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupObservers()
        
        // 加载应用列表
        viewModel.loadAppList()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        appGridAdapter = AppGridAdapter { appInfo ->
            onAppSelected(appInfo)
        }
        
        binding.rvAppList.apply {
            layoutManager = GridLayoutManager(requireContext(), 6)
            adapter = appGridAdapter
        }
    }
    
    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appList.collect { apps ->
                    allApps = apps
                    updateAppList(apps)
                }
            }
        }
    }
    
    private fun filterApps(query: String) {
        val filteredApps = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.appName.contains(query, ignoreCase = true) }
        }
        updateAppList(filteredApps)
    }
    
    private fun updateAppList(apps: List<AppInfo>) {
        if (apps.isEmpty()) {
            binding.rvAppList.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvAppList.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            appGridAdapter.updateApps(apps)
        }
    }
    
    private fun onAppSelected(appInfo: AppInfo) {
        // 如果设置了监听器，优先使用监听器
        listener?.onAppSelected(appInfo)
        
        // 如果没有监听器，则使用传统的Activity结果返回方式
        if (listener == null) {
            // 创建应用功能对象
            val function = ButtonFunction(
                id = "app_${appInfo.packageName}",
                category = FunctionCategory.APP,
                name = appInfo.appName,
                actionCode = "APP_${appInfo.packageName}",
                iconResId = 0,
                configWords = ""
            )
            
            // 返回选择的应用信息
            val resultIntent = Intent().apply {
                putExtra("selected_app_id", function.id)
                putExtra("selected_app_name", function.name)
                putExtra("selected_app_action_code", function.actionCode)
            }
            
            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
            requireActivity().finish()
        }
    }
    
    companion object {
        /**
         * 创建新的Fragment实例
         */
        fun newInstance(): AppSelectionFragment {
            return AppSelectionFragment()
        }
    }
}
