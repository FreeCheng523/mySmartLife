@file:Suppress("UNREACHABLE_CODE")

package com.zkjd.lingdong.ui.function_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zkjd.lingdong.R
import com.zkjd.lingdong.databinding.ActivityFunctionBinding
import com.zkjd.lingdong.ui.function.FunctionViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 功能配置Activity - 使用XML布局实现，作为FunctionFragment的容器
 */
@AndroidEntryPoint
class FunctionActivity : AppCompatActivity(), FunctionFragment.FunctionFragmentListener {

    private lateinit var binding: ActivityFunctionBinding
    private var deviceMac: String = ""
    private var hasUnsavedChanges = false
    private var functionFragment: FunctionFragment? = null

    companion object {
        const val EXTRA_DEVICE_MAC = "device_mac"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFunctionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceMac = intent.getStringExtra(EXTRA_DEVICE_MAC) ?: ""

        setupToolbar()
        setupFragment()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "按键功能配置"

        binding.toolbar.setNavigationOnClickListener {
            if (hasUnsavedChanges) {
                showSaveDialog()
            } else {
                finish()
            }
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    functionFragment?.saveAllButtonFunctions()
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupFragment() {
        functionFragment = FunctionFragment.newInstance(deviceMac)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, functionFragment!!)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_function, menu)
        return true
    }

    override fun onSaveRequested() {
        functionFragment?.saveAllButtonFunctions()
        finish()
    }

    override fun onHasUnsavedChanges(hasChanges: Boolean) {
        hasUnsavedChanges = hasChanges
    }

    private fun showSaveDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("温馨提醒")
            .setMessage("您更换了功能项，需要保存吗？").setPositiveButton("保存") { _, _ ->
                functionFragment?.saveAllButtonFunctions()
                finish()
            }.setNegativeButton("不需要") { _, _ ->
                finish()
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // 将结果传递给Fragment
        functionFragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges) {
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }

    fun test(view: View) {
        Toast.makeText(this,"test", Toast.LENGTH_SHORT).show()
    }
}