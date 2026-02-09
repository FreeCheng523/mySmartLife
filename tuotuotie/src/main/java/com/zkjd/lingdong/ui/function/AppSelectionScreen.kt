package com.zkjd.lingdong.ui.function

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zkjd.lingdong.model.AppInfo
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.FunctionCategory

/**
 * 应用选择屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onDismissRequest: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    viewModel: AppSelectionViewModel = hiltViewModel()
) {
    // 搜索关键字
    var searchQuery by remember { mutableStateOf("") }
    

    // 获取应用列表
    val appList by viewModel.appList.collectAsState()
    
    // 过滤后的应用列表
    val filteredAppList = remember(appList, searchQuery) {
        if (searchQuery.isBlank()) {
            appList
        } else {
            appList.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        }
    }


    

    // 加载应用列表和设置当前按钮类型
    LaunchedEffect(Unit) {
        viewModel.loadAppList()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "选择应用",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.Center)
                            // 可选：根据导航图标的宽度微调偏移量
                            .offset(x = -30.dp),
                        fontSize = 18.sp,// 正值向右偏移，负值向左偏移
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C28),//MaterialTheme.colorScheme.primary,
                    titleContentColor = Color(0xFFFFFFFF),//MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start =30.dp,
                    end = 30.dp,
                    bottom = 16.dp
                )
                .padding(paddingValues)
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("搜索应用") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            // 应用网格列表
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(25.dp),
                verticalArrangement = Arrangement.spacedBy(25.dp)
            ) {
                items(filteredAppList) { appInfo ->
                    AppGridItem(
                        appInfo = appInfo,
                        onClick = {
                            onAppClick(appInfo)

                        }
                    )
                }

                // 如果应用列表为空，显示权限提示
                if (filteredAppList.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "未能获取到应用列表",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "请确保应用已获取查询所有应用的权限",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 应用网格项
 */
@Composable
fun AppGridItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var appIcon by remember { mutableStateOf<Bitmap?>(null) }
    
    // 加载应用图标
    LaunchedEffect(appInfo.packageName) {
        try {
            val icon: Drawable? = packageManager.getApplicationIcon(appInfo.packageName)
            icon?.let {
                appIcon = it.toBitmap(200, 200)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // 如果找不到应用图标，使用默认图标
            appIcon = null
        } catch (e: Exception) {
            // 处理其他异常
            appIcon = null
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
//            .aspectRatio(1.1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
        ) {
            // 应用图标
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    // 显示实际应用图标
                    Image(
                        bitmap = appIcon!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // 使用默认图标
                    Icon(
                        imageVector = Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 应用名称
            Text(
                text = appInfo.appName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
            )

        }
    }
} 