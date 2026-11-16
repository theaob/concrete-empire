package com.onur.concreteempire;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * window.AndroidNotify in the game: local reminders shown while the game is closed. Nothing goes over the network.
 *
 * The page keeps a small plan up to date with setPlan(json): a list of {id, title, body} items, each with either
 * "after" (seconds after the game goes to the background) or "at" (wall-clock epoch ms), and "quiet": true to keep it
 * out of the night (22:00–09:00 moves to 09:00). Two reminders are never scheduled within two hours of each other;
 * the earlier item in the list wins. When the activity pauses
 * the plan is turned into inexact alarms (no exact-alarm permission needed); when it resumes they are cancelled and
 * any shown reminder is cleared, so a reminder never shows while the game is open. The game decides what to plan,
 * including the player's on/off setting (an empty plan schedules nothing).
 *
 * Android 13+ needs the POST_NOTIFICATIONS runtime permission: the page asks for it through requestPermission() at a
 * moment it chooses, and permission() tells it "granted", "ask" (not asked yet / can ask again) or "blocked".
 */
final class NotifyBridge {
    static final String CHANNEL = "reminders";
    private static final String PREFS = "notify";
    private static final String[] IDS = {"away", "daily"};   // fixed slots: one alarm per kind
    static final int REQ_PERMISSION = 7301;

    private final Activity act;
    private final WebView web;

    NotifyBridge(Activity act, WebView web) { this.act = act; this.web = web; }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    @JavascriptInterface
    public String permission() {
        NotificationManager nm = act.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 33 && act.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Never asked, or the system will still show the dialog: "ask". Asked and turned down for good: "blocked".
            boolean asked = prefs(act).getBoolean("asked", false);
            return !asked || act.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ? "ask" : "blocked";
        }
        return nm != null && nm.areNotificationsEnabled() ? "granted" : "blocked";
    }

    @JavascriptInterface
    public void requestPermission() {
        if (Build.VERSION.SDK_INT < 33) { pushState(); return; }
        act.runOnUiThread(() -> {
            prefs(act).edit().putBoolean("asked", true).apply();
            act.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PERMISSION);
        });
    }

    /** The app's notification page in the phone's settings, for when the permission is blocked. */
    @JavascriptInterface
    public void openSettings() {
        act.runOnUiThread(() -> {
            Intent i = Build.VERSION.SDK_INT >= 26
                ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, act.getPackageName())
                : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + act.getPackageName()));
            try { act.startActivity(i); } catch (Exception e) { /* no settings screen: nothing to do */ }
        });
    }

    @JavascriptInterface
    public void setPlan(String json) { prefs(act).edit().putString("plan", json == null ? "[]" : json).apply(); }

    /** From MainActivity after the permission dialog closes and on resume (the player may have changed it in settings). */
    void pushState() { act.runOnUiThread(() -> web.evaluateJavascript("window.__notifyState&&window.__notifyState()", null)); }

    /** MainActivity.onPause: turn the stored plan into alarms. */
    void schedule() {
        cancel();
        if (!"granted".equals(permission())) return;
        AlarmManager am = act.getSystemService(AlarmManager.class);
        if (am == null) return;
        long now = System.currentTimeMillis();
        try {
            JSONArray plan = new JSONArray(prefs(act).getString("plan", "[]"));
            long kept = -1;
            for (int i = 0; i < plan.length(); i++) {   // the page lists the most important reminder first
                JSONObject n = plan.getJSONObject(i);
                int slot = slot(n.optString("id"));
                if (slot < 0) continue;
                long at = n.has("at") ? n.getLong("at") : now + Math.round(n.optDouble("after", 0) * 1000);
                if (n.optBoolean("quiet")) at = outOfNight(at);
                if (at <= now + 60_000) continue;                               // too soon to be worth a reminder
                if (kept > 0 && Math.abs(at - kept) < 2 * 3600_000L) continue;  // never two reminders close together
                kept = at;
                PendingIntent pi = pending(act, slot, n.optString("title"), n.optString("body"));
                // Inexact and Doze-friendly; a few minutes late is fine for these reminders.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception e) { /* a bad plan schedules nothing */ }
    }

    /** Moves a time between 22:00 and 09:00 (phone's time zone) to 09:00, so reminders don't wake anyone up. */
    static long outOfNight(long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        int h = c.get(Calendar.HOUR_OF_DAY);
        if (h >= 9 && h < 22) return at;
        if (h >= 22) c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** MainActivity.onResume: the player is back, drop pending and shown reminders. */
    void cancel() {
        AlarmManager am = act.getSystemService(AlarmManager.class);
        NotificationManager nm = act.getSystemService(NotificationManager.class);
        for (int s = 0; s < IDS.length; s++) {
            if (am != null) am.cancel(pending(act, s, "", ""));
            if (nm != null) nm.cancel(s + 1);
        }
    }

    private static int slot(String id) { for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i; return -1; }

    private static PendingIntent pending(Context c, int slot, String title, String body) {
        Intent i = new Intent(c, NotifyReceiver.class).putExtra("slot", slot).putExtra("title", title).putExtra("body", body);
        return PendingIntent.getBroadcast(c, slot, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Called from App.onCreate. The channel name shows in the phone's notification settings, so it follows the phone's language. */
    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, c.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(c.getString(R.string.notify_channel_desc));
        nm.createNotificationChannel(ch);
    }
}
