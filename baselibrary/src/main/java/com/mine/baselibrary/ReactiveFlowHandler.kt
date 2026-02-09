package com.mine.baselibrary

import android.content.Context
import com.mine.baselibrary.window.ToastUtilOverApplication
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 边界检查结果
 */
data class BoundaryCheckResult(
    val shouldAllow: Boolean, // 是否允许执行
    val toastMessage: String? = null // 边界提示消息（如果被拒绝）
)

/**
 * 边界检查接口
 */
interface BoundaryChecker<TAction> {
    /**
     * 检查边界条件
     * @param action 操作类型
     * @param currentValue 当前值
     * @return BoundaryCheckResult 边界检查结果
     */
    suspend fun checkBoundary(action: TAction, currentValue: Int): BoundaryCheckResult
}

/**
 * 执行器接口
 */
interface Executor<TAction> {
    /**
     * 执行实际操作
     * @param action 操作类型
     */
    suspend fun execute(action: TAction)
}

/**
 * 资源检查器接口
 */
interface ResourceChecker {
    /**
     * 检查资源是否可用
     * @return true 如果资源可用，false 否则
     */
    suspend fun check(): Boolean
    
    /**
     * 获取资源名称（用于日志）
     */
    fun getResourceName(): String
}

/**
 * 操作名称提供者接口
 */
interface ActionNameProvider<TAction> {
    /**
     * 获取操作名称（用于日志和Toast）
     * @param action 操作类型
     * @return 操作名称
     */
    fun getActionName(action: TAction): String
}

/**
 * 双向操作接口，用于标识可减少/增加的操作
 */
interface BidirectionalAction {
    /**
     * 判断两个操作是否相反
     */
    fun isOpposite(other: BidirectionalAction): Boolean
}

/**
 * 通用的 Flow 响应式处理工具类
 * 
 * @param TAction 操作枚举类型，必须实现 BidirectionalAction 接口
 * @param context Context 用于显示 Toast
 * @param tag 日志标签
 * @param executor 执行器
 * @param resourceChecker 资源检查器
 * @param actionNameProvider 操作名称提供者
 * @param boundaryChecker 边界检查器（可选，如果启用边界检查则必须提供）
 * @param getCurrentValue 获取当前值的函数（可选，如果启用边界检查则必须提供）
 * @param enableBoundaryCheck 是否启用边界检查，默认 true
 * @param minIntervalMs 最小执行间隔（毫秒），默认 500ms
 */
class ReactiveFlowHandler<TAction : BidirectionalAction>(
    private val context: Context,
    private val tag: String,
    private val executor: Executor<TAction>,
    private val resourceChecker: ResourceChecker,
    private val actionNameProvider: ActionNameProvider<TAction>,
    private val boundaryChecker: BoundaryChecker<TAction>? = null,
    private val getCurrentValue: (suspend () -> Int?)? = null,
    private val enableBoundaryCheck: Boolean = true,
    private val minIntervalMs: Long = 500L,
    private val maxRequestsPerPeriod: Int = 3 // 每个周期最多3个请求

) {
    private val flow = MutableSharedFlow<TAction>(replay = 0, extraBufferCapacity = 64)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 队列管理
    @Volatile
    private var queuedAction: TAction? = null
    private val queuedActionMutex = Mutex()
    private var queuedActionCount: Int = 0
    
    // 执行控制
    private var lastExecuteTime: Long = 0L
    private val executeMutex = Mutex()
    
    // 限流控制（冷却期模式）
    private var requestCount: Int = 0 // 当前周期内的请求计数
    private var lastRequestTime: Long = 0L // 最后一次请求的时间
    private val requestMutex = Mutex()
    private val cooldownPeriodMs: Long = 1000L // 冷却期：1秒内无新请求才重置

    init {
        // 验证参数：如果启用边界检查，则必须提供 boundaryChecker 和 getCurrentValue
        if (enableBoundaryCheck) {
            require(boundaryChecker != null) { "启用边界检查时必须提供 boundaryChecker" }
            require(getCurrentValue != null) { "启用边界检查时必须提供 getCurrentValue" }
        }
        setupFlow()
    }
    
    /**
     * 设置 Flow 处理管道
     */
    private fun setupFlow() {
        flow
            .filter { action ->
                // 检查当前操作是否应该被过滤掉（如果队列状态已被反向操作更新）
                val currentQueued = queuedAction
                Timber.tag(tag).d("filter检查: queuedAction=$currentQueued, 当前事件=$action, count=$queuedActionCount")
                
                if (currentQueued != null && currentQueued != action) {
                    val isOpposite = action.isOpposite(currentQueued)
                    
                    if (isOpposite) {
                        // 这是队列中的反向操作，应该被过滤掉
                        val actionName = actionNameProvider.getActionName(action)
                        Timber.tag(tag).d("队列中的$actionName 操作被反向操作过滤掉")
                        return@filter false
                    }
                }
                
                // 如果是相同操作，减少计数（因为通过filter后会处理）
                if (currentQueued == action && queuedActionCount > 0) {
                    queuedActionCount--
                    Timber.tag(tag).d("相同操作通过filter，减少计数，剩余=$queuedActionCount")
                }
                
                // 检查资源是否可用
                if (!resourceChecker.check()) {
                    Timber.tag(tag).e("${resourceChecker.getResourceName()}为空，无法执行操作")
                    // 减少计数，如果计数为0则清除队列标记
                    if (queuedAction == action) {
                        if (queuedActionCount > 0) {
                            queuedActionCount--
                        }
                        if (queuedActionCount == 0) {
                            queuedAction = null
                        }
                    }
                    return@filter false
                }
                
                // 边界检查（可选）
                if (enableBoundaryCheck) {
                    // 获取当前值并检查边界
                    val currentValue = getCurrentValue?.invoke()
                    if (currentValue == null) {
                        Timber.tag(tag).e("无法获取当前值，跳过操作")
                        // 减少计数，如果计数为0则清除队列标记
                        if (queuedAction == action) {
                            if (queuedActionCount > 0) {
                                queuedActionCount--
                            }
                            if (queuedActionCount == 0) {
                                queuedAction = null
                            }
                        }
                        return@filter false
                    }
                    
                    // 边界检查
                    val boundaryResult = boundaryChecker?.checkBoundary(action, currentValue)
                    if (boundaryResult != null && !boundaryResult.shouldAllow) {
                        // 显示边界提示
                        boundaryResult.toastMessage?.let { message ->
                            ToastUtilOverApplication().showToast(context, message)
                        }
                        Timber.tag(tag).d("边界检查失败，不执行操作")
                        // 减少计数，如果计数为0则清除队列标记
                        if (queuedAction == action) {
                            if (queuedActionCount > 0) {
                                queuedActionCount--
                            }
                            if (queuedActionCount == 0) {
                                queuedAction = null
                            }
                        }
                        return@filter false
                    }
                }
                
                true
            }
            .onEach { action ->
                handleWithDelay(action)
                // 执行完成后，减少计数，如果计数为0则清除队列标记
                if (queuedAction == action) {
                    if (queuedActionCount > 0) {
                        queuedActionCount--
                    }
                    if (queuedActionCount == 0) {
                        queuedAction = null
                        Timber.tag(tag).d("所有操作执行完成，清除队列标记")
                    } else {
                        Timber.tag(tag).d("操作执行完成，队列中还有 $queuedActionCount 个相同操作")
                    }
                }
            }
            .launchIn(coroutineScope)
    }
    
    /**
     * 发送操作到 Flow
     */
    suspend fun emit(action: TAction) {
        // 先检查队列状态，判断是否是反向操作
        val isOppositeAction = queuedActionMutex.withLock {
            val currentQueued = queuedAction
            val isOpposite = currentQueued != null && action.isOpposite(currentQueued)
            
            // 检查是否是反向操作
            if (isOpposite) {
                // 收到反向操作，清除队列中的反向操作，重置为新操作
                val actionName = actionNameProvider.getActionName(action)
                Timber.tag(tag).d("收到反向操作（$actionName），清除队列中的反向操作")
                queuedAction = action
                queuedActionCount = 1 // 重置计数
            } else if (currentQueued == action) {
                // 相同操作，增加计数
                queuedActionCount++
                val actionName = actionNameProvider.getActionName(action)
                Timber.tag(tag).d("相同操作（$actionName），增加计数到 $queuedActionCount")
            } else {
                // 队列为空，初始化
                queuedAction = action
                queuedActionCount = 1
                val actionName = actionNameProvider.getActionName(action)
                Timber.tag(tag).d("初始化队列为$actionName 操作")
            }
            
            isOpposite
        }
        
        // 限流检查：冷却期模式（前3个请求通过，之后拒绝，1秒无新请求后重置）
        requestMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastRequestTime
            
            // 如果距离最后一次请求超过冷却期，重置计数器
            if (timeSinceLastRequest > cooldownPeriodMs && requestCount > 0) {
                Timber.tag(tag).d("冷却期已过（${timeSinceLastRequest}ms），重置限流计数器")
                requestCount = 0
            }
            
            // 如果是反向操作，重置冷静期，允许执行，但仍需计入限流计数
            if (isOppositeAction) {
                // 重置冷静期，让反向操作可以立即执行
                lastRequestTime = currentTime
                // 重置请求计数，开始新的限流周期
                requestCount = 0
                // 反向操作本身也要计入限流计数
                requestCount++
                val actionName = actionNameProvider.getActionName(action)
                Timber.tag(tag).d("检测到反向操作（$actionName），重置冷静期和请求计数，允许反向操作执行，当前周期请求数：${requestCount}/${maxRequestsPerPeriod}")
            } else {
                // 检查是否已达到限流上限
                if (requestCount >= maxRequestsPerPeriod) {
                    val actionName = actionNameProvider.getActionName(action)
                    Timber.tag(tag).w("限流拒绝：当前周期已处理${requestCount}个请求，拒绝$actionName 操作（需等待${cooldownPeriodMs}ms无请求后重置）")
                    // 更新最后请求时间（即使被拒绝，也算一次请求尝试，重置冷却期计时）
                    lastRequestTime = currentTime
                    return
                }
                
                // 请求通过，增加计数并更新时间
                requestCount++
                lastRequestTime = currentTime
                Timber.tag(tag).d("限流检查通过，当前周期请求数：${requestCount}/${maxRequestsPerPeriod}")
            }
        }
        
        // 发送消息到 Flow
        flow.emit(action)
    }
    
    /**
     * 处理延迟响应逻辑：如果距离上次执行不足指定间隔，则延迟执行
     */
    private suspend fun handleWithDelay(action: TAction) {
        executeMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastExecute = currentTime - lastExecuteTime
            
            val actionName = actionNameProvider.getActionName(action)
            
            if (timeSinceLastExecute < minIntervalMs) {
                // 如果距离上次执行不足指定间隔，需要延迟
                val delayTime = minIntervalMs - timeSinceLastExecute
                Timber.tag(tag).d("距离上次执行仅${timeSinceLastExecute}ms，延迟${delayTime}ms后执行$actionName 操作")
                delay(delayTime)
            } else {
                Timber.tag(tag).d("距离上次执行已${timeSinceLastExecute}ms，立即执行$actionName 操作")
            }
            
            // 执行实际操作
            executor.execute(action)
            
            // 更新上次执行时间
            lastExecuteTime = System.currentTimeMillis()
        }
    }
}
