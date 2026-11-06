package com.onur.concreteempire;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import org.json.JSONObject;

/**
 * window.AndroidAds in the game: Google AdMob rewarded ads.
 * The page calls showRewarded(placement, id); when the ad closes this reports back with
 * window.__adResult(id, watchedToEnd, reason), reason being "" (watched), "skipped", "noad" or "busy".
 * The game only pays a grant / boost for watchedToEnd == true.
 *
 * Start-up order follows Google's guidance: consent first (User Messaging Platform; shows the GDPR form in the
 * EEA/UK when an AdMob privacy message is set up), then the Mobile Ads SDK, then one ad is preloaded so the next
 * tap shows it at once. Ids live in res/values/admob-ids.xml; an empty rewarded unit id switches ads off
 * (MainActivity then doesn't add this bridge and the game uses its placeholder ad).
 */
final class AdsBridge {
    /** How long a tap waits for an ad that is still loading before giving up. */
    private static final long LOAD_WAIT_MS = 8000;

    private final Activity act;
    private final WebView web;
    private final String unitId;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile ConsentInformation consent;

    // main thread only
    private boolean initStarted, sdkReady, loading, showing;
    private RewardedAd ad;
    private String waitingId;
    private final Runnable waitTimeout = () -> finishWaiting(false, "noad");

    AdsBridge(Activity act, WebView web) {
        this.act = act;
        this.web = web;
        this.unitId = act.getString(R.string.admob_rewarded_unit_id).trim();
    }

    boolean enabled() { return !unitId.isEmpty(); }

    /** From MainActivity.onCreate. */
    void start() {
        if (!enabled()) return;
        consent = UserMessagingPlatform.getConsentInformation(act);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(act, params,
            () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(act, err -> { initAds(); pushState(); }),
            err -> initAds());
        // Consent given in an earlier session already allows requests; no need to wait for the update.
        initAds();
    }

    void destroy() { ui.removeCallbacksAndMessages(null); }

    private void initAds() {
        if (initStarted || consent == null || !consent.canRequestAds()) return;
        initStarted = true;
        new Thread(() -> MobileAds.initialize(act, status -> ui.post(() -> { sdkReady = true; load(); }))).start();
    }

    private void load() {
        if (!sdkReady || loading || ad != null) return;
        loading = true;
        RewardedAd.load(act, unitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd a) {
                loading = false;
                ad = a;
                if (waitingId != null) {
                    String id = waitingId;
                    waitingId = null;
                    ui.removeCallbacks(waitTimeout);
                    show(id);
                }
            }
            @Override public void onAdFailedToLoad(LoadAdError e) {
                loading = false;
                // No retry loop here: the next tap tries again (keeps idle players from burning requests).
                finishWaiting(false, "noad");
            }
        });
    }

    /** Player tapped an ad button. Always answers through window.__adResult exactly once. */
    @JavascriptInterface public void showRewarded(String placement, final String id) {
        ui.post(() -> {
            if (showing || waitingId != null) { result(id, false, "busy"); return; }
            if (consent == null || !consent.canRequestAds()) { result(id, false, "noad"); return; }
            if (ad != null) { show(id); return; }
            waitingId = id;
            ui.postDelayed(waitTimeout, LOAD_WAIT_MS);
            initAds();
            load();
        });
    }

    /** GDPR: true when the player must be able to reopen the consent form (Settings shows a button then). */
    @JavascriptInterface public boolean privacyOptionsRequired() {
        ConsentInformation c = consent;
        return c != null && c.getPrivacyOptionsRequirementStatus()
            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @JavascriptInterface public void showPrivacyOptions() {
        ui.post(() -> UserMessagingPlatform.showPrivacyOptionsForm(act, err -> { initAds(); pushState(); }));
    }

    private void show(final String id) {
        RewardedAd a = ad;
        ad = null;
        showing = true;
        final boolean[] earned = {false};
        a.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                showing = false;
                result(id, earned[0], earned[0] ? "" : "skipped");
                load();
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError e) {
                showing = false;
                result(id, false, "noad");
                load();
            }
        });
        a.show(act, reward -> earned[0] = true);
    }

    private void finishWaiting(boolean ok, String why) {
        if (waitingId == null) return;
        String id = waitingId;
        waitingId = null;
        ui.removeCallbacks(waitTimeout);
        result(id, ok, why);
    }

    private void result(String id, boolean ok, String why) {
        final String js = "window.__adResult&&window.__adResult(" + JSONObject.quote(id == null ? "" : id) + "," + ok
            + "," + JSONObject.quote(why) + ")";
        act.runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    /** Consent changed: let the page redraw Settings (privacy options button). */
    private void pushState() {
        act.runOnUiThread(() -> web.evaluateJavascript("window.__adsState&&window.__adsState()", null));
    }
}
