package com.tiktok.tvweb;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TikTokTV";
    private static final String TIKTOK_URL = "https://www.tiktok.com";

    // Clean Chrome Android TV User Agent: no "wv" token so TikTok won't flag as WebView bot
    private static final String CLEAN_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Mi Box 4K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    // Video switch debounce threshold (prevents media decoder freeze and audio stutter)
    private static final long SWITCH_DEBOUNCE_MS = 350;
    private long lastVideoSwitchTime = 0;

    // UI elements
    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private View btnRetry;
    private ImageView mouseCursor;
    private TextView tvStatusIndicator;

    // Virtual Mouse State
    private boolean isMouseMode = false;
    private float cursorX = -1;
    private float cursorY = -1;
    private float cursorSpeed = 30f;
    private long lastCursorMoveTime = 0;

    // Track long press on OK button so all remotes can toggle mouse mode
    private boolean isLongPressHandled = false;

    // Audio Focus Manager
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private Object audioFocusRequestObj;

    // Back press tracker
    private long lastBackPressTime = 0;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvStatusIndicator != null) {
                tvStatusIndicator.setVisibility(View.GONE);
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Keep TV screen awake during video playback
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.e(TAG, "Error setting FLAG_KEEP_SCREEN_ON", e);
        }

        setContentView(R.layout.activity_main);
        hideSystemUI();

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        btnRetry = findViewById(R.id.btnRetry);
        mouseCursor = findViewById(R.id.mouseCursor);
        tvStatusIndicator = findViewById(R.id.tvStatusIndicator);

        setupAudioManager();
        setupWebView();

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                if (errorLayout != null) errorLayout.setVisibility(View.GONE);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (webView != null) webView.reload();
            });
        }

        if (webView != null) {
            webView.loadUrl(TIKTOK_URL);
        }
    }

    private void setupAudioManager() {
        try {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            audioFocusChangeListener = focusChange -> {
                Log.d(TAG, "Audio focus changed: " + focusChange);
            };
        } catch (Exception e) {
            Log.e(TAG, "Error setting up AudioManager", e);
        }
    }

    private void requestTVAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestAudioFocusOreo();
            } else {
                audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not request audio focus", e);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void requestAudioFocusOreo() {
        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build();
        AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
        audioFocusRequestObj = focusRequest;
        audioManager.requestAudioFocus(focusRequest);
    }

    private void abandonTVAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequestObj instanceof AudioFocusRequest) {
                abandonAudioFocusOreo();
            } else if (audioFocusChangeListener != null) {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not abandon audio focus", e);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void abandonAudioFocusOreo() {
        audioManager.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequestObj);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        requestTVAudioFocus();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        abandonTVAudioFocus();
        if (webView != null) {
            webView.onPause();
        }
        flushCookies();
    }

    private void flushCookies() {
        try {
            CookieManager.getInstance().flush();
        } catch (Exception e) {
            Log.w(TAG, "Cookie flush error", e);
        }
    }

    private void hideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hide system UI", e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webView == null) return;

        try {
            // Hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);

            // Clean User Agent without "wv" token
            settings.setUserAgentString(CLEAN_USER_AGENT);

            // Allow video autoplay without user gesture on TV
            settings.setMediaPlaybackRequiresUserGesture(false);

            // Performance cache
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setEnableSmoothTransition(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            }
            CookieManager.getInstance().setAcceptCookie(true);
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebSettings", e);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null && newProgress >= 65) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (errorLayout != null) errorLayout.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                injectCustomScripts();
                flushCookies();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Block external custom schemes like snssdk1233:// or intent:// that crash WebView
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                // Intercept & block heavy telemetry and ad trackers that cause 100% CPU usage
                if (url.contains("slardar") || url.contains("/telemetry/") || url.contains("byteoversea.com")
                        || url.contains("mon.tiktokv.com") || url.contains("mcs.tiktokv.com")
                        || url.contains("/log/") || url.contains("/beacon/")
                        || url.contains("google-analytics") || url.contains("doubleclick")) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Injects lightweight CSS and DOM helpers:
     * - Disables heavy filters
     * - Auto mutes inactive videos
     * - Maps next/prev functions
     */
    private void injectCustomScripts() {
        if (webView == null) return;
        try {
            String script =
                    "(function() {" +
                    "  if (window.__tiktokTVInjected) return;" +
                    "  window.__tiktokTVInjected = true;" +

                    // Lightweight TV CSS
                    "  var style = document.createElement('style');" +
                    "  style.id = 'tiktok-tv-optimized-css';" +
                    "  style.innerHTML = '" +
                    "    * { backdrop-filter: none !important; -webkit-backdrop-filter: none !important; box-shadow: none !important; text-shadow: none !important; } " +
                    "    video { will-change: transform; transform: translateZ(0); object-fit: contain !important; } " +
                    "    [class*=\"banner\"], [id*=\"banner\"], [data-e2e*=\"download-app\"], [class*=\"download-app\"], " +
                    "    [class*=\"DivToastContainer\"], [class*=\"BottomBanner\"] { display: none !important; } " +
                    "    [data-e2e=\"qr-code\"], [class*=\"QRCodeContainer\"], [class*=\"DivQRCode\"] { transform: scale(1.15) !important; margin: 10px auto !important; } " +
                    "  ';" +
                    "  document.head.appendChild(style);" +

                    // Mute inactive background videos
                    "  function muteOtherVideos(activeVid) {" +
                    "    try {" +
                    "      var allVids = document.querySelectorAll('video');" +
                    "      for (var i = 0; i < allVids.length; i++) {" +
                    "        var v = allVids[i];" +
                    "        if (v !== activeVid) {" +
                    "          v.pause();" +
                    "          v.muted = true;" +
                    "        }" +
                    "      }" +
                    "      if (activeVid) {" +
                    "        activeVid.muted = false;" +
                    "      }" +
                    "    } catch(e) {}" +
                    "  }" +

                    "  document.addEventListener('play', function(e) {" +
                    "    if (e.target && e.target.tagName === 'VIDEO') { muteOtherVideos(e.target); }" +
                    "  }, true);" +

                    // Navigation Helpers
                    "  window.__tiktokTVNextVideo = function() {" +
                    "    var btn = document.querySelector('[data-e2e=\"arrow-down\"]') || " +
                    "              document.querySelector('button[aria-label*=\"Next\"]') || " +
                    "              document.querySelector('button[aria-label*=\"next\"]') || " +
                    "              document.querySelector('button[aria-label*=\"xuống\"]') || " +
                    "              document.querySelector('button[aria-label*=\"tiếp\"]');" +
                    "    if (btn) {" +
                    "      btn.click();" +
                    "    } else {" +
                    "      var activeVideo = document.querySelector('video');" +
                    "      var target = activeVideo ? (activeVideo.closest('[data-e2e=\"recommend-list-item-container\"]') || activeVideo) : document.body;" +
                    "      var evt = new KeyboardEvent('keydown', {key: 'ArrowDown', code: 'ArrowDown', keyCode: 40, which: 40, bubbles: true});" +
                    "      target.dispatchEvent(evt);" +
                    "      window.dispatchEvent(evt);" +
                    "    }" +
                    "  };" +

                    "  window.__tiktokTVPrevVideo = function() {" +
                    "    var btn = document.querySelector('[data-e2e=\"arrow-up\"]') || " +
                    "              document.querySelector('button[aria-label*=\"Previous\"]') || " +
                    "              document.querySelector('button[aria-label*=\"previous\"]') || " +
                    "              document.querySelector('button[aria-label*=\"lên\"]');" +
                    "    if (btn) {" +
                    "      btn.click();" +
                    "    } else {" +
                    "      var activeVideo = document.querySelector('video');" +
                    "      var target = activeVideo ? (activeVideo.closest('[data-e2e=\"recommend-list-item-container\"]') || activeVideo) : document.body;" +
                    "      var evt = new KeyboardEvent('keydown', {key: 'ArrowUp', code: 'ArrowUp', keyCode: 38, which: 38, bubbles: true});" +
                    "      target.dispatchEvent(evt);" +
                    "      window.dispatchEvent(evt);" +
                    "    }" +
                    "  };" +

                    "  window.__tiktokTVTogglePlay = function() {" +
                    "    var v = document.querySelector('video');" +
                    "    if (v) {" +
                    "      if (v.paused) { v.play(); } else { v.pause(); }" +
                    "    } else {" +
                    "      var evt = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, which: 32, bubbles: true});" +
                    "      window.dispatchEvent(evt);" +
                    "    }" +
                    "  };" +

                    "  window.__tiktokTVSeek = function(seconds) {" +
                    "    var v = document.querySelector('video');" +
                    "    if (v && v.duration) {" +
                    "      v.currentTime = Math.max(0, Math.min(v.duration, v.currentTime + seconds));" +
                    "    } else {" +
                    "      var key = seconds < 0 ? 'ArrowLeft' : 'ArrowRight';" +
                    "      var code = seconds < 0 ? 37 : 39;" +
                    "      var evt = new KeyboardEvent('keydown', {key: key, code: key, keyCode: code, which: code, bubbles: true});" +
                    "      window.dispatchEvent(evt);" +
                    "    }" +
                    "  };" +

                    "  window.__tiktokTVCloseModal = function() {" +
                    "    var closeBtn = document.querySelector('[data-e2e=\"modal-close-inner-button\"], button[aria-label*=\"Close\"], [class*=\"CloseButton\"], [class*=\"ModalClose\"]');" +
                    "    if (closeBtn) {" +
                    "      closeBtn.click();" +
                    "      return true;" +
                    "    }" +
                    "    var modal = document.querySelector('[class*=\"DivModalContainer\"], [class*=\"DivLoginContainer\"]');" +
                    "    if (modal) {" +
                    "      modal.remove();" +
                    "      return true;" +
                    "    }" +
                    "    return false;" +
                    "  };" +

                    "})();";

            webView.evaluateJavascript(script, null);
        } catch (Exception e) {
            Log.w(TAG, "Error injecting scripts", e);
        }
    }

    private void showStatusIndicator(String message) {
        if (tvStatusIndicator == null) return;
        uiHandler.removeCallbacks(hideIndicatorRunnable);
        tvStatusIndicator.setText(message);
        tvStatusIndicator.setVisibility(View.VISIBLE);
        uiHandler.postDelayed(hideIndicatorRunnable, 3000);
    }

    private void toggleMouseMode() {
        isMouseMode = !isMouseMode;
        if (isMouseMode) {
            if (cursorX < 0 || cursorY < 0) {
                if (webView != null && webView.getWidth() > 0 && webView.getHeight() > 0) {
                    cursorX = webView.getWidth() / 2f;
                    cursorY = webView.getHeight() / 2f;
                } else {
                    cursorX = 640f;
                    cursorY = 360f;
                }
            }
            updateCursorPosition();
            if (mouseCursor != null) mouseCursor.setVisibility(View.VISIBLE);
            showStatusIndicator(getString(R.string.mouse_mode_on));
        } else {
            if (mouseCursor != null) mouseCursor.setVisibility(View.GONE);
            showStatusIndicator(getString(R.string.mouse_mode_off));
        }
    }

    private void updateCursorPosition() {
        if (mouseCursor == null || webView == null) return;
        int maxX = Math.max(0, webView.getWidth() - 32);
        int maxY = Math.max(0, webView.getHeight() - 32);
        cursorX = Math.max(0, Math.min(cursorX, maxX));
        cursorY = Math.max(0, Math.min(cursorY, maxY));
        mouseCursor.setX(cursorX);
        mouseCursor.setY(cursorY);
    }

    private void moveCursor(float dx, float dy) {
        long now = System.currentTimeMillis();
        // Dynamic acceleration when holding D-Pad
        if (now - lastCursorMoveTime < 120) {
            cursorSpeed = Math.min(65f, cursorSpeed + 4f);
        } else {
            cursorSpeed = 26f;
        }
        lastCursorMoveTime = now;

        cursorX += dx * cursorSpeed;
        cursorY += dy * cursorSpeed;
        updateCursorPosition();
    }

    private void dispatchMouseClick() {
        if (webView == null || cursorX < 0 || cursorY < 0) return;
        try {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();
            MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
            MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime + 40, MotionEvent.ACTION_UP, cursorX, cursorY, 0);
            webView.dispatchTouchEvent(downEvent);
            webView.dispatchTouchEvent(upEvent);
            downEvent.recycle();
            upEvent.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error dispatching mouse click", e);
        }
    }

    /**
     * Map Remote Control keys:
     * - Press MENU, INFO, GUIDE, 0: Toggle Virtual Mouse Cursor
     * - Long press OK / Enter: Toggle Virtual Mouse Cursor (for remotes without MENU key)
     * - When Mouse Mode is ON: D-Pad moves cursor, OK clicks
     * - When Mouse Mode is OFF: D-Pad switches video smoothly (debounced), OK toggles Play/Pause
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Track OK / Enter for long press
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            event.startTracking();
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_INFO:
            case KeyEvent.KEYCODE_GUIDE:
            case KeyEvent.KEYCODE_SETTINGS:
            case KeyEvent.KEYCODE_0:
                toggleMouseMode();
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isMouseMode) {
                    moveCursor(0, 1f);
                } else {
                    nextVideo();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (isMouseMode) {
                    moveCursor(0, -1f);
                } else {
                    prevVideo();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isMouseMode) {
                    moveCursor(-1f, 0);
                } else {
                    seekVideo(-5);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (isMouseMode) {
                    moveCursor(1f, 0);
                } else {
                    seekVideo(5);
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                return true;

            case KeyEvent.KEYCODE_BACK:
                return handleBackPressed();

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            isLongPressHandled = true;
            toggleMouseMode();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (isLongPressHandled) {
                isLongPressHandled = false;
                return true;
            }
            if (isMouseMode) {
                dispatchMouseClick();
            } else {
                togglePlayPause();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void nextVideo() {
        long now = System.currentTimeMillis();
        if (now - lastVideoSwitchTime < SWITCH_DEBOUNCE_MS) return;
        lastVideoSwitchTime = now;
        executeJs("window.__tiktokTVNextVideo && window.__tiktokTVNextVideo();");
    }

    private void prevVideo() {
        long now = System.currentTimeMillis();
        if (now - lastVideoSwitchTime < SWITCH_DEBOUNCE_MS) return;
        lastVideoSwitchTime = now;
        executeJs("window.__tiktokTVPrevVideo && window.__tiktokTVPrevVideo();");
    }

    private void togglePlayPause() {
        executeJs("window.__tiktokTVTogglePlay && window.__tiktokTVTogglePlay();");
    }

    private void seekVideo(int seconds) {
        executeJs("window.__tiktokTVSeek && window.__tiktokTVSeek(" + seconds + ");");
    }

    private boolean handleBackPressed() {
        if (isMouseMode) {
            toggleMouseMode();
            return true;
        }

        // Try closing login modal if open
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function() { return window.__tiktokTVCloseModal ? window.__tiktokTVCloseModal() : false; })();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if ("true".equalsIgnoreCase(value)) {
                                showStatusIndicator(getString(R.string.modal_closed));
                            } else {
                                runOnUiThread(() -> {
                                    if (webView != null && webView.canGoBack()) {
                                        webView.goBack();
                                    } else {
                                        checkExit();
                                    }
                                });
                            }
                        }
                    }
            );
            return true;
        }

        checkExit();
        return true;
    }

    private void checkExit() {
        if (System.currentTimeMillis() - lastBackPressTime < 2000) {
            finish();
        } else {
            lastBackPressTime = System.currentTimeMillis();
            Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();
        }
    }

    private void executeJs(String script) {
        if (webView != null) {
            webView.evaluateJavascript(script, null);
        }
    }
}
