package com.zkjd.lingdong.ui.function_kotlin

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.ItemFunctionGridBinding
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.FunctionCategory

/**
 * 功能网格适配器
 */
class FunctionGridAdapter(
    private val onFunctionSelected: (ButtonFunction?) -> Unit,
    private val onAppSelectionRequested: () -> Unit
) : RecyclerView.Adapter<FunctionGridAdapter.FunctionGridViewHolder>() {
    
    private var functions = mutableListOf<ButtonFunction>()
    private var selectedFunction: ButtonFunction? = null
    
    fun updateFunctions(newFunctions: List<ButtonFunction>, selectedFunction: ButtonFunction?) {
        this.functions.clear()
        this.functions.addAll(newFunctions)
        this.selectedFunction = selectedFunction
        notifyDataSetChanged()
    }
    
    fun updateSelectedFunction(selectedFunction: ButtonFunction?) {
        val oldSelectedIndex = findSelectedFunctionIndex(this.selectedFunction)
        val newSelectedIndex = findSelectedFunctionIndex(selectedFunction)
        
        this.selectedFunction = selectedFunction
        
        if (oldSelectedIndex != -1) {
            notifyItemChanged(oldSelectedIndex)
        }
        if (newSelectedIndex != -1) {
            notifyItemChanged(newSelectedIndex)
        }
    }
    
    private fun findSelectedFunctionIndex(function: ButtonFunction?): Int {
        if (function == null) return -1
        
        return functions.indexOfFirst { f ->
            if (f.id == "launch_app") {
                function.category == FunctionCategory.APP
            } else {
                function.id == f.id
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionGridViewHolder {
        val binding = ItemFunctionGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FunctionGridViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FunctionGridViewHolder, position: Int) {
        holder.bind(functions[position])
    }
    
    override fun getItemCount(): Int = functions.size
    
    inner class FunctionGridViewHolder(
        private val binding: ItemFunctionGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(function: ButtonFunction) {
            val context = binding.root.context
            val isAppSelector = function.id == "launch_app"
            
            // 判断是否选中
            val isSelected = if (isAppSelector) {
                selectedFunction?.category == FunctionCategory.APP
            } else {
                selectedFunction?.id == function.id
            }
            
            // 设置功能名称
            if (isAppSelector && isSelected && selectedFunction != null) {
//                binding.tvFunctionName.text = "开启应用: ${selectedFunction!!.name}"
                binding.tvFunctionNameLine1.text ="开启应用"
                binding.tvFunctionName.text = "${selectedFunction!!.name}"
            } else {
                // 处理name分割逻辑
                if (function.name.contains("|")) {
                    val parts = function.name.split("|")
                    binding.tvFunctionNameLine1.text = parts[0].trim()
                    binding.tvFunctionName.text = parts[1].trim()
                } else {
                    binding.tvFunctionNameLine1.text = ""
                    binding.tvFunctionName.text = function.name
                }
            }
            
            // 根据功能名称设置tvFunctionNameLine1的marginTop
            val layoutParams = binding.tvFunctionNameLine1.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (function.name.contains("小憩模式")) {
                layoutParams.topMargin = 102
            } else {
                layoutParams.topMargin = 98
            }
            binding.tvFunctionNameLine1.layoutParams = layoutParams
            
            // 设置功能图标
            setFunctionIcon(function, isAppSelector, isSelected)
            
            // 设置选中状态样式
            if (isSelected) {
//                binding.root.setCardBackgroundColor(
//                    ContextCompat.getColor(context, R.color.primary_container)
//                )
//                binding.tvFunctionName.setTextColor(
//                    ContextCompat.getColor(context, R.color.on_primary_container)
//                )
//                binding.viewSelectedIndicator.visibility = View.VISIBLE
                binding.iconBoxBg.setBackgroundResource(R.drawable.icon_choose_true)
                binding.tvFunctionNameLine1.setTextColor(ContextCompat.getColor(context, R.color.icon_choose))
                binding.tvFunctionName.setTextColor(ContextCompat.getColor(context, R.color.icon_choose))
                binding.btnDeleteFunction.visibility = View.VISIBLE
                binding.root.cardElevation = 4f
            } else {
//                binding.root.setCardBackgroundColor(
//                    ContextCompat.getColor(context, R.color.surface)
//                )
//                binding.tvFunctionName.setTextColor(
//                    ContextCompat.getColor(context, R.color.on_surface)
//                )
//                binding.viewSelectedIndicator.visibility = View.GONE
                binding.iconBoxBg.setBackgroundResource(R.drawable.icon_choose_default)
                binding.tvFunctionNameLine1.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                binding.tvFunctionName.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                binding.btnDeleteFunction.visibility = View.GONE
                binding.root.cardElevation = 2f
            }
            
            // 设置点击事件
            binding.root.setOnClickListener {
                if (isAppSelector) {
                    onAppSelectionRequested()
                } else {
                    onFunctionSelected(function)
                }
            }
            
            // 设置删除按钮点击事件
            binding.btnDeleteFunction.setOnClickListener {
                // 清除选中的功能
                updateSelectedFunction(null)
                onFunctionSelected(null)
            }
        }

        private fun setFunctionIcon(function: ButtonFunction, isAppSelector: Boolean, isSelected: Boolean) {
            val context = binding.root.context

            val targetResId = when {
                // launch_app
                isAppSelector &&
                        isSelected &&
                        selectedFunction?.category == FunctionCategory.APP &&
                        function.iconSelectedResId != 0 -> function.iconSelectedResId

                // 其他功能：只要选中且有选中图就用
                isSelected && function.iconSelectedResId != 0 -> function.iconSelectedResId

                else -> function.iconResId
            }
            if (isValidResource(targetResId)) {
                binding.ivFunctionIcon.setImageResource(targetResId)
            } else {
                // 兜底默认图标
                val defaultIcon = if (isAppSelector) R.drawable.ic_function else R.drawable.ic_touch_app
                binding.ivFunctionIcon.setImageResource(defaultIcon)
                binding.ivFunctionIcon.clearColorFilter()
            }
        }
        
//        private fun loadAppIcon(actionCode: String) {
//            val context = binding.root.context
//            val packageName = actionCode.substring(4) // 移除 "APP_" 前缀
//
//            try {
//                val packageManager = context.packageManager
//                val icon: Drawable = packageManager.getApplicationIcon(packageName)
//                binding.ivFunctionIcon.setImageDrawable(icon)
//                binding.ivFunctionIcon.clearColorFilter()
//            } catch (e: PackageManager.NameNotFoundException) {
//                // 如果找不到应用图标，使用默认图标
//                binding.ivFunctionIcon.setImageResource(R.drawable.ic_function)
//                binding.ivFunctionIcon.setColorFilter(
//                    ContextCompat.getColor(context, R.color.on_surface_variant)
//                )
//            }
//        }
        
        private fun isValidResource(resourceId: Int): Boolean {
            return try {
                val context = binding.root.context
                val resourceTypeName = context.resources.getResourceTypeName(resourceId)
                resourceTypeName == "drawable" || resourceTypeName == "mipmap"
            } catch (e: Exception) {
                false
            }
        }
    }
}
