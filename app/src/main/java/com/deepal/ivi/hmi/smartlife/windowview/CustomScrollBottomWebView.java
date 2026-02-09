package com.deepal.ivi.hmi.smartlife.windowview;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CustomScrollBottomWebView extends WebView {
    private boolean isScrollBottom;

    public CustomScrollBottomWebView(Context context) {
        this(context, null);
    }

    public CustomScrollBottomWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomScrollBottomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置背景颜色为透明
        setBackgroundColor(0);
        // 设置图层类型为硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null);
        // 获取WebSettings并进行配置
        WebSettings settings = getSettings();
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // 缓存模式
        settings.setDomStorageEnabled(true); // 启用DOM存储
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH); // 设置高渲染优先级
        settings.setUseWideViewPort(true); // 启用宽视口模式
        settings.setLoadWithOverviewMode(true); // 启用概览模式
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING); // 设置布局算法为文本自动调整大小

        // 设置WebViewClient
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // 检测是否滚动到底部
        if (getHeight() + t + 20 >= getContentHeight() * getScale()) {
            isScrollBottom = true;
        }
    }

    public boolean isScrollBottom() {
        return isScrollBottom;
    }

    public void setScrollBottom(boolean isScrollBottom) {
        this.isScrollBottom = isScrollBottom;
    }
}