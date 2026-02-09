package com.zkjd.lingdong.ui.function_kotlin

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.ItemAppGridBinding
import com.zkjd.lingdong.model.AppInfo

/**
 * 应用网格适配器
 */
class AppGridAdapter(
    private val onAppSelected: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppGridViewHolder>() {
    
    private var apps = mutableListOf<AppInfo>()
    
    fun updateApps(newApps: List<AppInfo>) {
        this.apps.clear()
        this.apps.addAll(newApps)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppGridViewHolder {
        val binding = ItemAppGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppGridViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AppGridViewHolder, position: Int) {
        holder.bind(apps[position])
    }
    
    override fun getItemCount(): Int = apps.size
    
    inner class AppGridViewHolder(
        private val binding: ItemAppGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(appInfo: AppInfo) {
            binding.tvAppName.text = appInfo.appName
            
            // 加载应用图标
            loadAppIcon(appInfo.packageName)
            
            // 设置点击事件
            binding.root.setOnClickListener {
                onAppSelected(appInfo)
            }
        }
        
        private fun loadAppIcon(packageName: String) {
            val context = binding.root.context
            
            try {
                val packageManager = context.packageManager
                val icon: Drawable = packageManager.getApplicationIcon(packageName)
                binding.ivAppIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                // 如果找不到应用图标，使用默认图标
                binding.ivAppIcon.setImageResource(R.drawable.ic_launch_app)
                binding.ivAppIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
            } catch (e: Exception) {
                // 处理其他异常
                binding.ivAppIcon.setImageResource(R.drawable.ic_launch_app)
                binding.ivAppIcon.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }
        }
    }
}
