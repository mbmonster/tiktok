package com.tiktok.tvweb;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TIKTOK_URL = "https://www.tiktok.com";
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private Button btnRetry;
    private long lastBackPressTime = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake while watching TV
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        btnRetry = findViewById(R.id.btnRetry);

        setupWebView();

        btnRetry.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            webView.reload();
        });

        webView.loadUrl(TIKTOK_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // Enable essential web features
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Set Desktop User-Agent so TikTok renders full desktop layout
        settings.setUserAgentString(DESKTOP_USER_AGENT);

        // Allow media playback without user touch gesture (Crucial for TV Autoplay)
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Cache & Rendering performance
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 80) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                injectCustomScripts();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    errorLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Injects CSS and JS to hide mobile install banners and ensure smooth TV experience
     */
    private void injectCustomScripts() {
        // Inject CSS to hide banners and fix scroll containers
        String hideBannersCSS = "(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                "[class*=\"banner\"], [id*=\"banner\"], " +
                "[data-e2e*=\"download-app\"], [class*=\"download-app\"], " +
                "[class*=\"DivBannerContainer\"], [class*=\"DivDownloadAppContainer\"] " +
                "{ display: none !important; } " +
                "body { overflow: hidden !important; }';" +
                "document.head.appendChild(style);" +
                "})();";
        webView.evaluateJavascript(hideBannersCSS, null);
    }

    /**
     * Map Mi Box Remote Control D-Pad keys to TikTok navigation
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Scroll to next video
                executeJs(
                        "(function() {" +
                                "var evt = new KeyboardEvent('keydown', {key: 'ArrowDown', code: 'ArrowDown', keyCode: 40, which: 40, bubbles: true});" +
                                "window.dispatchEvent(evt);" +
                                "document.dispatchEvent(evt);" +
                                "setTimeout(function() {" +
                                "  var btn = document.querySelector('[data-e2e=\"arrow-down\"]');" +
                                "  if (btn) { btn.click(); }" +
                                "  else { window.scrollBy({top: window.innerHeight * 0.88, behavior: 'smooth'}); }" +
                                "}, 40);" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                // Scroll to previous video
                executeJs(
                        "(function() {" +
                                "var evt = new KeyboardEvent('keydown', {key: 'ArrowUp', code: 'ArrowUp', keyCode: 38, which: 38, bubbles: true});" +
                                "window.dispatchEvent(evt);" +
                                "document.dispatchEvent(evt);" +
                                "setTimeout(function() {" +
                                "  var btn = document.querySelector('[data-e2e=\"arrow-up\"]');" +
                                "  if (btn) { btn.click(); }" +
                                "  else { window.scrollBy({top: -window.innerHeight * 0.88, behavior: 'smooth'}); }" +
                                "}, 40);" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // Play / Pause toggle
                executeJs(
                        "(function() {" +
                                "var v = document.querySelector('video');" +
                                "if (v) {" +
                                "  if (v.paused) { v.play(); } else { v.pause(); }" +
                                "} else {" +
                                "  var evt = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, which: 32, bubbles: true});" +
                                "  window.dispatchEvent(evt);" +
                                "}" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Rewind 5 seconds
                executeJs(
                        "(function() {" +
                                "var v = document.querySelector('video');" +
                                "if (v) { v.currentTime = Math.max(0, v.currentTime - 5); }" +
                                "else {" +
                                "  var evt = new KeyboardEvent('keydown', {key: 'ArrowLeft', code: 'ArrowLeft', keyCode: 37, which: 37, bubbles: true});" +
                                "  window.dispatchEvent(evt);" +
                                "}" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Fast forward 5 seconds
                executeJs(
                        "(function() {" +
                                "var v = document.querySelector('video');" +
                                "if (v) { v.currentTime = Math.min(v.duration, v.currentTime + 5); }" +
                                "else {" +
                                "  var evt = new KeyboardEvent('keydown', {key: 'ArrowRight', code: 'ArrowRight', keyCode: 39, which: 39, bubbles: true});" +
                                "  window.dispatchEvent(evt);" +
                                "}" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_MENU:
                // Menu key toggles mute (m) or like (l)
                executeJs(
                        "(function() {" +
                                "var evt = new KeyboardEvent('keydown', {key: 'm', code: 'KeyM', keyCode: 77, which: 77, bubbles: true});" +
                                "window.dispatchEvent(evt);" +
                                "})();"
                );
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                if (System.currentTimeMillis() - lastBackPressTime < 2000) {
                    finish();
                } else {
                    lastBackPressTime = System.currentTimeMillis();
                    Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void executeJs(String script) {
        if (webView != null) {
            webView.evaluateJavascript(script, null);
        }
    }
}
