package com.onur.concreteempire;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Shows one reminder that NotifyBridge scheduled. Tapping it opens the game. */
public class NotifyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String title = in.getStringExtra("title"), body = in.getStringExtra("body");
        if (title == null || title.isEmpty()) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        NotifyBridge.createChannel(c);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, NotifyBridge.CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_notify)
         .setColor(0xFFF2C100)
         .setContentTitle(title)
         .setContentText(body)
         .setStyle(new Notification.BigTextStyle().bigText(body))
         .setContentIntent(tap)
         .setAutoCancel(true);
        try { nm.notify(in.getIntExtra("slot", 0) + 1, b.build()); } catch (SecurityException e) { /* permission taken away */ }
    }
}
