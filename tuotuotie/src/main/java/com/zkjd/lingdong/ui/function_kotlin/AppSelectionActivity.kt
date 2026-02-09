package com.zkjd.lingdong.ui.function_kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zkjd.lingdong.databinding.ActivityAppSelectionBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用选择Activity - 托管 AppSelectionFragment
 */
@AndroidEntryPoint
class AppSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAppSelectionBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        // Fragment 会通过 FragmentContainerView 自动加载
        // 如果需要动态添加 Fragment，可以使用以下代码：
        // if (savedInstanceState == null) {
        //     supportFragmentManager.beginTransaction()
        //         .replace(R.id.fragment_container, AppSelectionFragment.newInstance())
        //         .commit()
        // }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "选择应用"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}
