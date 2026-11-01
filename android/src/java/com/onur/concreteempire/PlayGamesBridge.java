package com.onur.concreteempire;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.google.android.gms.games.PlayGames;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * window.AndroidGames in the game. Play Games Services v2 signs the player in automatically at launch; this bridge
 * reports the result to the page (window.__pgsState(signedIn, name)), offers a manual sign-in, unlocks achievements,
 * submits leaderboard scores and opens Google's achievement / leaderboard screens.
 * The page uses short keys ("level_5"); the real ids live in res/values/games-ids.xml as ach_<key> / lb_<key>
 * (a new achievement needs a line there and one in IDS below).
 * Every call is a no-op when Play Games is not configured or the player is not signed in.
 */
final class PlayGamesBridge {
    private static final int RC_UI = 9001;
    private static final Map<String, Integer> IDS = new HashMap<>();
    static {
        IDS.put("ach_first_handover", R.string.ach_first_handover);
        IDS.put("ach_projects_10", R.string.ach_projects_10);
        IDS.put("ach_projects_50", R.string.ach_projects_50);
        IDS.put("ach_level_5", R.string.ach_level_5);
        IDS.put("ach_level_10", R.string.ach_level_10);
        IDS.put("ach_level_20", R.string.ach_level_20);
        IDS.put("ach_level_30", R.string.ach_level_30);
        IDS.put("ach_networth_100m", R.string.ach_networth_100m);
        IDS.put("ach_networth_1b", R.string.ach_networth_1b);
        IDS.put("ach_networth_1t", R.string.ach_networth_1t);
        IDS.put("ach_reputation_90", R.string.ach_reputation_90);
        IDS.put("ach_brand_1", R.string.ach_brand_1);
        IDS.put("ach_streak_7", R.string.ach_streak_7);
        IDS.put("lb_net_worth", R.string.lb_net_worth);
        IDS.put("lb_firm_level", R.string.lb_firm_level);
        IDS.put("lb_lifetime_earnings", R.string.lb_lifetime_earnings);
    }
    private final Activity act;
    private final WebView web;
    private volatile boolean signedIn;

    PlayGamesBridge(Activity act, WebView web) { this.act = act; this.web = web; }

    /** True when this build has a Play Games project id. */
    @JavascriptInterface public boolean available() { return App.gamesOn; }

    @JavascriptInterface public boolean signedIn() { return signedIn; }

    /** Asks Play Games for the current sign-in state and reports it to the page. Called on resume too. */
    @JavascriptInterface public void refresh() {
        if (!App.gamesOn) return;
        act.runOnUiThread(() -> PlayGames.getGamesSignInClient(act).isAuthenticated().addOnCompleteListener(t ->
            onAuth(t.isSuccessful() && t.getResult().isAuthenticated())));
    }

    /** Player tapped "Sign in": shows Google's sign-in sheet. */
    @JavascriptInterface public void signIn() {
        if (!App.gamesOn) return;
        act.runOnUiThread(() -> PlayGames.getGamesSignInClient(act).signIn().addOnCompleteListener(t ->
            onAuth(t.isSuccessful() && t.getResult().isAuthenticated())));
    }

    /** Returns true when the unlock was handed to Play Games (it queues offline and ignores repeats). */
    @JavascriptInterface public boolean unlock(String key) {
        final String id = idFor("ach_", key);
        if (!signedIn || id == null) return false;
        act.runOnUiThread(() -> PlayGames.getAchievementsClient(act).unlock(id));
        return true;
    }

    /** Leaderboards keep each player's best score, so sending a lower one is harmless. */
    @JavascriptInterface public boolean submitScore(String key, double score) {
        final String id = idFor("lb_", key);
        if (!signedIn || id == null || !(score >= 0)) return false;
        final long v = (long) Math.min(score, (double) Long.MAX_VALUE);
        act.runOnUiThread(() -> PlayGames.getLeaderboardsClient(act).submitScore(id, v));
        return true;
    }

    @JavascriptInterface public void showAchievements() {
        if (!signedIn) { signIn(); return; }
        act.runOnUiThread(() -> PlayGames.getAchievementsClient(act).getAchievementsIntent()
            .addOnSuccessListener(this::open));
    }

    @JavascriptInterface public void showLeaderboards() {
        if (!signedIn) { signIn(); return; }
        act.runOnUiThread(() -> PlayGames.getLeaderboardsClient(act).getAllLeaderboardsIntent()
            .addOnSuccessListener(this::open));
    }

    private void open(Intent i) {
        try { act.startActivityForResult(i, RC_UI); } catch (RuntimeException ignored) { }
    }

    private void onAuth(boolean ok) {
        signedIn = ok;
        if (!ok) { push(false, ""); return; }
        PlayGames.getPlayersClient(act).getCurrentPlayer().addOnCompleteListener(t ->
            push(true, t.isSuccessful() && t.getResult() != null ? t.getResult().getDisplayName() : ""));
    }

    private void push(boolean ok, String name) {
        final String js = "window.__pgsState&&window.__pgsState(" + ok + "," + JSONObject.quote(name == null ? "" : name) + ")";
        act.runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    private String idFor(String prefix, String key) {
        Integer res = App.gamesOn && key != null ? IDS.get(prefix + key) : null;
        if (res == null) return null;
        String id = act.getString(res).trim();
        return id.isEmpty() ? null : id;
    }
}
