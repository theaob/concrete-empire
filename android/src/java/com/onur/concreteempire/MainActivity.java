package com.onur.concreteempire;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Full-screen WebView that runs assets/index.html (the whole game). Saves live in the WebView's localStorage.
 *  window.AndroidGames (PlayGamesBridge) connects the game to Google Play Games Services,
 *  window.AndroidAds (AdsBridge) to AdMob rewarded ads. */
public class MainActivity extends Activity {
    private WebView web;
    private PlayGamesBridge games;
    private AdsBridge ads;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            w.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        // Play Games and AdMob bring network permissions with them; the game page itself stays offline as before
        // (rewarded ads open in AdMob's own full-screen activity, not in this WebView).
        s.setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) { if (games != null) games.refresh(); }
        });
        games = new PlayGamesBridge(this, web);
        web.addJavascriptInterface(games, "AndroidGames");
        ads = new AdsBridge(this, web);
        if (ads.enabled()) web.addJavascriptInterface(ads, "AndroidAds");
        setContentView(web);
        hideBars();
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ (and always from targetSdk 36): onBackPressed is no longer called, back goes through this callback
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                new OnBackInvokedCallback() {
                    @Override public void onBackInvoked() { moveTaskToBack(true); }
                });
        }
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
        ads.start();
    }

    private void hideBars() {
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() { web.onPause(); super.onPause(); }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); if (games != null) games.refresh(); }

    @Override
    public void onBackPressed() {
        // Android 12 and older. The game has no page history; back sends the app to the background instead of killing it
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() { ads.destroy(); web.destroy(); super.onDestroy(); }
}
