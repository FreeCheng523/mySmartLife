
package com.deepal.ivi.hmi.smartlife.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.deepal.ivi.hmi.smartlife.databinding.DialogWebviewBinding;
import com.mine.baselibrary.dialog.BaseDialogFragment;
import android.util.Log;
public class WebViewDialogFragment extends BaseDialogFragment<DialogWebviewBinding> {

    private static final String ARG_URL = "url";

    public static WebViewDialogFragment newInstance(String url) {
        WebViewDialogFragment fragment = new WebViewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public WebViewDialogFragment() {
        super(true, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true, true);
    }

    @Nullable
    @Override
    protected DialogWebviewBinding createViewBinding(@Nullable LayoutInflater inflater, @Nullable ViewGroup container) {
        if (inflater == null) return null;
        return DialogWebviewBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null || getArguments() == null) return;

        String url = getArguments().getString(ARG_URL);

        // 设置WebView背景透明
        binding.webView.setBackgroundColor(0x00000000);
        // 返回按钮
        binding.back.setOnClickListener(v -> dismiss());

        // 配置WebView,加载网页
        setupWebView(url);
    }

    private void setupWebView(String url) {
        Log.d("WebViewDebug", "开始配置WebView，URL: " + url);

        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false); // 禁用缩放功能
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);

        Log.d("WebViewDebug", "WebSettings配置完成");

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebViewDebug", "页面加载完成");
                injectCustomCSS();//自定义css
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // 显示错误信息
                Log.e("WebViewDialog", "Load error: " + errorCode + ", " + description + ", " + failingUrl);
                binding.webView.loadUrl("about:blank");
                binding.webView.loadData(
                        "<html><body><div style='text-align:center;padding:50px;color:#666;'>加载失败，请检查网络连接</div></body></html>",
                        "text/html",
                        "UTF-8"
                );
            }
        });

        // 加载URL
        binding.webView.loadUrl(url);
    }


    private void injectCustomCSS() {
        String css =
                "javascript:(function() { " +
                        "  var css = '" +
                        /* 1. 所有可能带灰色的标签一次性刷白 */
                        "    html, body, " +
                        "    h1, h2, h3, h4, h5, h6, " +
                        "    p, div, span, a, li, td, th, strong, b, em, i, small, " +
                        "    .title, .headline, .subtitle, .summary, .desc, .caption, .info, .meta, .date { " +
                        "      color: #FFFFFF !important; " +
                        "    } " +
                        /* 2. 基础body样式：可调的左右边距 */
                        "    body { " +
                        "      background-color: transparent !important; " +
                        "      font-family: \\\"deepal_fz_variable_yjh_l2\\\", \\\"PingFang SC\\\", \\\"Microsoft YaHei\\\", sans-serif !important; " +
                        "      font-size: 22px !important; " +
                        "      line-height: 1.6 !important; " +
                        "      margin: 0 auto !important; " +
                        "      padding: 0 4vw !important; " +
                        "      width: 100vw !important; " +
                        "      max-width: none !important; " +
                        "      box-sizing: border-box !important; " +
                        "    } " +
                        "    p, div, span { margin-bottom: 12px !important; } " +
                        "    h1, h2, h3, h4, h5, h6 { margin: 20px 0 12px 0 !important; } " +
                        "    a { color: #FFFFFF !important; } " +
                        "    .container, .content, .main, article, section { " +
                        "      background-color: transparent !important; " +
                        "      color: #FFFFFF !important; " +
                        "      max-width: none !important; " +
                        "      width: 100% !important; " +
                        "      padding-left: 0 !important; " +
                        "      padding-right: 0 !important; " +
                        "      margin-left: 0 !important; " +
                        "      margin-right: 0 !important; " +
                        "    } " +
                        "'; " +
                        "  var style = document.createElement('style'); " +
                        "  style.type = 'text/css'; " +
                        "  style.innerHTML = css; " +
                        "  document.head.appendChild(style); " +
                        "  document.body.style.backgroundColor = 'transparent'; " +
                        "})()";

        binding.webView.loadUrl(css);
    }


    @Nullable
    @Override
    protected View getContentLayout() {
        return binding != null ? binding.getRoot() : null;
    }

    @Override
    public void onDestroyView() {
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.destroy();
        }
        super.onDestroyView();
    }
}