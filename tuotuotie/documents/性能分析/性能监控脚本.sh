#!/bin/bash
# SmartLife 应用性能监控脚本
# 用于持续监控应用的内存和 CPU 使用情况

# 配置
APP_PACKAGE="com.deepal.ivi.hmi.smartlife"
LOG_FILE="smartlife_performance_$(date +%Y%m%d_%H%M%S).log"
INTERVAL=5  # 监控间隔（秒）
DURATION=300  # 监控时长（秒），默认5分钟

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 阈值定义（根据性能分析报告设置）
MEMORY_WARNING=450  # MB
MEMORY_DANGER=500   # MB
CPU_WARNING=5       # %
CPU_DANGER=10       # %

echo "=========================================="
echo "  SmartLife 应用性能监控工具"
echo "=========================================="
echo "应用包名: $APP_PACKAGE"
echo "监控间隔: ${INTERVAL}秒"
echo "监控时长: ${DURATION}秒"
echo "日志文件: $LOG_FILE"
echo "=========================================="
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}错误：未检测到 ADB 设备连接${NC}"
    exit 1
fi

# 检查应用是否运行
if ! adb shell pidof $APP_PACKAGE > /dev/null 2>&1; then
    echo -e "${YELLOW}警告：应用未运行，等待应用启动...${NC}"
    while ! adb shell pidof $APP_PACKAGE > /dev/null 2>&1; do
        sleep 2
    done
    echo -e "${GREEN}应用已启动，开始监控...${NC}"
fi

# 初始化日志文件
echo "SmartLife 性能监控日志 - $(date)" > "$LOG_FILE"
echo "======================================" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# 计数器
count=0
max_count=$((DURATION / INTERVAL))

# 统计变量
total_memory=0
total_cpu=0
peak_memory=0
peak_cpu=0
samples=0

echo -e "${GREEN}开始监控...${NC}"
echo ""

# 主监控循环
while [ $count -lt $max_count ]; do
    timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    # 获取 PID
    pid=$(adb shell pidof $APP_PACKAGE)
    
    if [ -z "$pid" ]; then
        echo -e "${RED}[$timestamp] 应用已停止${NC}"
        echo "[$timestamp] 应用已停止" >> "$LOG_FILE"
        break
    fi
    
    # 获取 top 信息
    top_info=$(adb shell top -n 1 -p $pid 2>/dev/null | tail -1)
    
    # 解析 CPU 和内存信息
    cpu=$(echo "$top_info" | awk '{print $9}' | sed 's/%//')
    memory=$(echo "$top_info" | awk '{print $6}' | sed 's/M//')
    
    # 如果解析失败，使用备用方法
    if [ -z "$cpu" ] || [ -z "$memory" ]; then
        cpu=0
        memory=0
    fi
    
    # 更新统计
    samples=$((samples + 1))
    total_memory=$(echo "$total_memory + $memory" | bc)
    total_cpu=$(echo "$total_cpu + $cpu" | bc)
    
    # 更新峰值
    if [ $(echo "$memory > $peak_memory" | bc) -eq 1 ]; then
        peak_memory=$memory
    fi
    if [ $(echo "$cpu > $peak_cpu" | bc) -eq 1 ]; then
        peak_cpu=$cpu
    fi
    
    # 计算平均值
    avg_memory=$(echo "scale=2; $total_memory / $samples" | bc)
    avg_cpu=$(echo "scale=2; $total_cpu / $samples" | bc)
    
    # 确定内存状态
    memory_status="${GREEN}正常${NC}"
    if [ $(echo "$memory > $MEMORY_DANGER" | bc) -eq 1 ]; then
        memory_status="${RED}危险${NC}"
    elif [ $(echo "$memory > $MEMORY_WARNING" | bc) -eq 1 ]; then
        memory_status="${YELLOW}警告${NC}"
    fi
    
    # 确定 CPU 状态
    cpu_status="${GREEN}正常${NC}"
    if [ $(echo "$cpu > $CPU_DANGER" | bc) -eq 1 ]; then
        cpu_status="${RED}危险${NC}"
    elif [ $(echo "$cpu > $CPU_WARNING" | bc) -eq 1 ]; then
        cpu_status="${YELLOW}警告${NC}"
    fi
    
    # 输出到控制台
    echo -e "${BLUE}[$timestamp]${NC}"
    echo -e "  内存: ${memory}M (平均: ${avg_memory}M, 峰值: ${peak_memory}M) - 状态: $memory_status"
    echo -e "  CPU:  ${cpu}% (平均: ${avg_cpu}%, 峰值: ${peak_cpu}%) - 状态: $cpu_status"
    echo ""
    
    # 写入日志
    echo "[$timestamp]" >> "$LOG_FILE"
    echo "  PID: $pid" >> "$LOG_FILE"
    echo "  内存: ${memory}M (平均: ${avg_memory}M, 峰值: ${peak_memory}M)" >> "$LOG_FILE"
    echo "  CPU:  ${cpu}% (平均: ${avg_cpu}%, 峰值: ${peak_cpu}%)" >> "$LOG_FILE"
    echo "  原始数据: $top_info" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    
    # 如果达到危险阈值，获取详细内存信息
    if [ $(echo "$memory > $MEMORY_DANGER" | bc) -eq 1 ]; then
        echo -e "${RED}!!! 内存超过危险阈值，获取详细信息...${NC}"
        echo "=== 详细内存信息 ===" >> "$LOG_FILE"
        adb shell dumpsys meminfo $APP_PACKAGE >> "$LOG_FILE"
        echo "" >> "$LOG_FILE"
    fi
    
    count=$((count + 1))
    sleep $INTERVAL
done

# 生成总结报告
echo "" >> "$LOG_FILE"
echo "======================================" >> "$LOG_FILE"
echo "监控总结" >> "$LOG_FILE"
echo "======================================" >> "$LOG_FILE"
echo "监控时长: $((samples * INTERVAL))秒 (${samples}个样本)" >> "$LOG_FILE"
echo "内存 - 平均: ${avg_memory}M, 峰值: ${peak_memory}M" >> "$LOG_FILE"
echo "CPU  - 平均: ${avg_cpu}%, 峰值: ${peak_cpu}%" >> "$LOG_FILE"

# 评估
echo "" >> "$LOG_FILE"
echo "性能评估:" >> "$LOG_FILE"
if [ $(echo "$peak_memory > $MEMORY_DANGER" | bc) -eq 1 ]; then
    echo "  [危险] 内存峰值超过危险阈值 (${MEMORY_DANGER}M)" >> "$LOG_FILE"
elif [ $(echo "$peak_memory > $MEMORY_WARNING" | bc) -eq 1 ]; then
    echo "  [警告] 内存峰值超过警告阈值 (${MEMORY_WARNING}M)" >> "$LOG_FILE"
else
    echo "  [正常] 内存使用正常" >> "$LOG_FILE"
fi

if [ $(echo "$peak_cpu > $CPU_DANGER" | bc) -eq 1 ]; then
    echo "  [危险] CPU 峰值超过危险阈值 (${CPU_DANGER}%)" >> "$LOG_FILE"
elif [ $(echo "$peak_cpu > $CPU_WARNING" | bc) -eq 1 ]; then
    echo "  [警告] CPU 峰值超过警告阈值 (${CPU_WARNING}%)" >> "$LOG_FILE"
else
    echo "  [正常] CPU 使用正常" >> "$LOG_FILE"
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  监控完成！${NC}"
echo -e "${GREEN}=========================================${NC}"
echo "监控时长: $((samples * INTERVAL))秒 (${samples}个样本)"
echo "内存 - 平均: ${avg_memory}M, 峰值: ${peak_memory}M"
echo "CPU  - 平均: ${avg_cpu}%, 峰值: ${peak_cpu}%"
echo ""
echo "详细日志已保存到: $LOG_FILE"
echo ""

