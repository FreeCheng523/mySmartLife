package com.deepal.ivi.hmi.ipcommon.connection;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.deepal.ivi.hmi.ipcommon.iInterface.ServerCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkServiceManager {
    private static final String TAG = "NetworkServiceManager";
    private final String serverIp;
    private final int serverPort;
    private ServerSocket serverSocket;
    private volatile Socket clientSocket;
    // 用于 Accept 和 Receive 的线程池（固定2个线程）
    private ExecutorService mainExecutor;
    // 优化:发送队列：设置容量上限（例如50），防止无限堆积
    // ArrayBlockingQueue 比 LinkedBlockingQueue 内存更紧凑，适合高频
    private final BlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(86);//3000/35
    // 优化2:专门的发送线程对象，不使用线程池以减少 overhead
    private Thread sendThread;
    //使用AtomicBoolean确保线程安全
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isReconnect = new AtomicBoolean(true);
    private ServerCallback serverCallback;
    private static NetworkServiceManager instance;
    private final Object lock = new Object(); // 用于同步创建和关闭
    // 优化：引入 Handler 处理重连，避免在 Executor 线程内部自杀
    private Handler reconnectHandler;
    private HandlerThread reconnectThread; // 专门用于管理重连倒计时的轻量级线程

    public interface SendCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // 插入设备后允许重连
    public void isToReconnect(boolean isReconnect) {
        Log.d(TAG, "【isToReconnect】是否允许重连: " + isReconnect);
        this.isReconnect.set(isReconnect);
    }

    public static synchronized NetworkServiceManager getInstance(String ip, int port) {
        if (instance == null) {
            instance = new NetworkServiceManager(ip, port);
        } else if (!instance.serverIp.equals(ip) || instance.serverPort != port) {
            // 如果参数变化，重新创建实例
            instance.stopServer();
            instance = new NetworkServiceManager(ip, port);
        }
        return instance;
    }
    public NetworkServiceManager(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        // 初始化一个用于重连调度的后台线程
        reconnectThread = new HandlerThread("Reconnect-Scheduler");
        reconnectThread.start();
        reconnectHandler = new Handler(reconnectThread.getLooper());
    }

    public void setServerCallback(ServerCallback callback) {
        this.serverCallback = callback;
    }

    public void startServer() {
        synchronized (lock) {
            if (isRunning.get()) { // 如果已经在运行，先停止，避免重复启动
                stopServer();
            }
            // 初始化资源
            isRunning.set(true);
            sendQueue.clear(); // 清空历史积压数据
            Log.d(TAG, "正在启动服务...");

            // 1. 初始化接收线程池
            if (mainExecutor == null || mainExecutor.isShutdown()) {
                mainExecutor = Executors.newFixedThreadPool(3);//一个监听、一个接收、一个待用
            }
            // 2. 启动专门的发送线程（只启动这一个，永不销毁重建，直到 stopServer）
            startSendThread();
            // 3. 启动 ServerSocket 监听（这里起1个服务线程了，还有2个可用）
            mainExecutor.execute(this::runServerTask);

        }
    }

    private void runServerTask() {
        try {
            synchronized (lock) {
                if (!isRunning.get()) return;
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(InetAddress.getByName(serverIp), serverPort), 50);
            }
            Log.i(TAG, "服务器启动，监听端口(Server started on port): " + serverPort);
            while (isRunning.get()) {
                Socket newSocket = serverSocket.accept(); // 1、阻塞等待新连接（不加锁，允许 accept 阻塞）
                newSocket.setKeepAlive(true);
                // 设置 TCP 无延迟，对于 35ms 这种小包高频发送很重要，禁用 Nagle 算法
                newSocket.setTcpNoDelay(true);
                Log.i(TAG, "客户端:" + newSocket.getInetAddress().toString()+" 接入");

                // 2. 互斥：如果有旧连接，强制断开！会触发旧线程的 IOException，从而结束旧线程
                disconnectOldSocket();

                // 3. 上位：更新全局引用
                clientSocket = newSocket;
                if (serverCallback != null) serverCallback.connectClientStatus(true, "");

                // 4、启动仪表数据接收任务（这里又启接收线程，还有一个可用）
                if (!mainExecutor.isShutdown()) {
                    mainExecutor.execute(() -> receiveData(newSocket));
                }
            }
        }catch (IOException e) {
            Log.e(TAG, "启动仪表车机服务报错 Server Error", e);
            if (serverCallback != null) serverCallback.connectClientStatus(false, e.getMessage());
            if (isRunning.get() && isReconnect.get()) {
                scheduleReconnect();
            };
        }
    }

    // 辅助方法：安全断开旧连接
    private void disconnectOldSocket() {
        // 使用局部变量引用，防止多线程竞争导致空指针
        Socket old = clientSocket;
        if (old != null && !old.isClosed()) {
            try {
                Log.d(TAG, "关闭旧连接: " + old.getInetAddress());
                old.close();
            } catch (IOException e) {
                // 关闭时的异常通常可以忽略
                Log.w(TAG, "关闭旧连接异常", e);
            }
        }
    }

    private void receiveData(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            while (isRunning.get() && !socket.isClosed()) {
                // Arrays.fill(buffer, (byte) 0); 返回值决定了有效数据长度，无需清零
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    Log.i(TAG, "客户端主动断开");
                    break;
                }
                byte[] receivedData = Arrays.copyOfRange(buffer, 0, bytesRead);
                if (serverCallback != null) {
                    serverCallback.receiveMsgFromClient(receivedData);
                }
            }
        } catch (IOException e) {
            // 当报错的 socket 等于当前最新的 socket 时，通知
            if (socket == clientSocket && isRunning.get()) {
                Log.e(TAG, "数据接收异常: " + e.getMessage());
                if (serverCallback != null) {
                    serverCallback.connectClientStatus(false, e.getMessage());
                }
            } else { // 如果是旧 socket（被 disconnectOldSocket 强制关闭的），则忽略该异常
                Log.w(TAG, e);
            }
        } finally {
            // 确保线程结束时资源被释放
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 优化：发送线程逻辑：从队列取数据 -> 写入 Socket
    private void startSendThread() {
        sendThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    // 阻塞等待数据，最多等待 1 秒检查一次 isRunning 状态
                    byte[] data = sendQueue.poll(1, TimeUnit.SECONDS);
                    if (data == null) continue; // 没数据，继续循环

                    Socket socket = clientSocket;
                    if (socket != null && !socket.isClosed() && socket.isConnected()) {
                        OutputStream out = socket.getOutputStream();
                        out.write(data);
                        out.flush(); // 必须 flush 确保数据立即发出
                    } else {
                        // 如果 Socket 断了，清空队列防止数据堆积
                        sendQueue.clear();
                    }
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }catch (IOException e) {
                    Log.e(TAG, "Send Error: " + e.getMessage());
                    // 发送失败通常意味着连接断开，ServerCallback 在 receiveData 那边会处理断开逻辑
                    // 这里可以选择稍微 sleep 一下防止 CPU 空转
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }
            Log.d(TAG, "发送线程退出 SendThread stopped");
        });
        sendThread.setName("USB-Send-Thread");
        sendThread.start();
    }

    public void sendData(byte[] data, SendCallback callback) {
        try {
            // 如果服务已停止，直接报错
            if (!isRunning.get()) {
                callback.onError(new IllegalStateException("Server stopped"));
                return;
            }
            // 如果队列已满（说明发送线程阻塞或太慢），offer 会立即返回 false
            // 对于 35ms 实时数据，丢弃旧数据通常比造成延迟要好。
            // 这里使用 offer 尝试放入，放不进去就丢弃（或者记录日志）
            boolean success = sendQueue.offer(data);
            if (!success){
                //只有在非常极端的情况下（USB 拔出未检测到或带宽占满）才会走到这里
                String msg = "发送队列已满，数据被丢弃";
                Log.w(TAG, msg);
                callback.onError(new Exception(msg));
            }else {
                callback.onSuccess();//成功进入缓冲区
            }

        }catch (Exception e){
            Log.e(TAG, "发送任务被拒绝 (服务已关闭): " + e.getMessage());
            callback.onError(new IOException("Service is shutting down"));
        }
    }

    private void scheduleReconnect() {
        Log.d(TAG, "1500ms后尝试重连...");
        // 移除可能存在的旧任务，防止多次叠加
        reconnectHandler.removeCallbacksAndMessages(null);
        // 延时执行重启，不阻塞当前线程，也不依赖当前 Executor
        reconnectHandler.postDelayed(() -> {
            if (isReconnect.get()) {
                startServer();
            }
        }, 1500);
    }

    public void stopServer() {
        synchronized (lock) {
            isRunning.set(false);
            // 1. 先关 ServerSocket，停止 accept
            // 取消待执行的重连任务
            if (reconnectHandler != null) {
                reconnectHandler.removeCallbacksAndMessages(null);
            }
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing serverSocket", e);
            }

            // 再关 ClientSocket
            disconnectOldSocket();
            clientSocket = null;


            // 2. 中断发送线程
            if (sendThread != null) {
                sendThread.interrupt();
                sendThread = null;
                Log.i(TAG, "SendThread 已中断");
            }
            sendQueue.clear();

            // 3. 关闭接收线程池
            if (mainExecutor != null && !mainExecutor.isShutdown()) {
                mainExecutor.shutdownNow();
            }
         }
    }
}
