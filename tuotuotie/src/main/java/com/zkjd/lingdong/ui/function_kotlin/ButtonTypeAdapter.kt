package com.zkjd.lingdong.ui.function_kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.ItemButtonTypeBinding
import com.zkjd.lingdong.model.ButtonType

/**
 * 按钮类型适配器
 */
class ButtonTypeAdapter(
    private val onItemClick: (ButtonType) -> Unit
) : RecyclerView.Adapter<ButtonTypeAdapter.ButtonTypeViewHolder>() {
    
    private var buttonTypes = mutableListOf<ButtonTypeItem>()
    private var selectedType: ButtonType = ButtonType.SHORT_PRESS
    private var isRoty: Boolean = false
    
    data class ButtonTypeItem(
        val type: ButtonType,
        val name: String,
        val iconResId: Int
    )
    
    init {
        updateButtonTypes()
    }
    
    fun updateSelectedType(selectedType: ButtonType) {
        val oldSelectedIndex = buttonTypes.indexOfFirst { it.type == this.selectedType }
        val newSelectedIndex = buttonTypes.indexOfFirst { it.type == selectedType }
        
        this.selectedType = selectedType
        
        if (oldSelectedIndex != -1) {
            notifyItemChanged(oldSelectedIndex)
        }
        if (newSelectedIndex != -1) {
            notifyItemChanged(newSelectedIndex)
        }
    }
    
    fun updateIsRoty(isRoty: Boolean) {
        if (this.isRoty != isRoty) {
            this.isRoty = isRoty
            updateButtonTypes()
            notifyDataSetChanged()
        }
    }
    
    private fun updateButtonTypes() {
        buttonTypes.clear()
        
        // 添加基础按钮类型
        buttonTypes.add(ButtonTypeItem(ButtonType.SHORT_PRESS, "短按", R.drawable.ic_touch_app))
        buttonTypes.add(ButtonTypeItem(ButtonType.DOUBLE_CLICK, "双击", R.drawable.ic_touch_app))
        
        // 如果是旋钮设备，添加旋转功能
        if (isRoty) {
            buttonTypes.add(ButtonTypeItem(ButtonType.LEFT_ROTATE, "旋转", R.drawable.ic_rotate_left))
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonTypeViewHolder {
        val binding = ItemButtonTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ButtonTypeViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ButtonTypeViewHolder, position: Int) {
        holder.bind(buttonTypes[position])
    }
    
    override fun getItemCount(): Int = buttonTypes.size
    
    inner class ButtonTypeViewHolder(
        private val binding: ItemButtonTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ButtonTypeItem) {
            val isSelected = item.type == selectedType
            val context = binding.root.context
            
            binding.tvButtonTypeName.text = item.name
            binding.ivButtonTypeIcon.setImageResource(item.iconResId)
            
            // 设置选中状态的样式
            if (isSelected) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary_container)
                )
                binding.tvButtonTypeName.setTextColor(
                    ContextCompat.getColor(context, R.color.on_primary_container)
                )
                binding.ivButtonTypeIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
                binding.root.cardElevation = 4f
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface)
                )
                binding.tvButtonTypeName.setTextColor(
                    ContextCompat.getColor(context, R.color.on_surface)
                )
                binding.ivButtonTypeIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.on_surface_variant)
                )
                binding.root.cardElevation = 2f
            }
            
            binding.root.setOnClickListener {
                onItemClick(item.type)
            }
        }
    }
}
