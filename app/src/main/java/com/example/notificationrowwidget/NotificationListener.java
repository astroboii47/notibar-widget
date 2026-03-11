package com.example.notificationrowwidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notification listener that broadcasts a lightweight update (pkg -> count, pkg -> latestPostTime)
 * whenever notifications change. This is more reliable than having the widget query the listener
 * at random times.
 */
public class NotificationListener extends NotificationListenerService {

    private static final String PREFS_NAME = "notif_row_prefs";
    private static final String PREF_FILTER_ENABLED = "filter_enabled";
    private static final String PREF_ALLOWED_PKGS = "allowed_pkgs";


    public static final String EXTRA_COUNTS = "extra_counts";
    public static final String EXTRA_LATEST = "extra_latest";

    private static NotificationListener instance;

    // Keep the latest RankingMap so we can reliably detect "silent" notifications across packages.
    // (NotificationManager#getNotificationChannel() only works for this app's own channels.)
    private volatile RankingMap lastRankingMap;

    @Override
    public void onListenerConnected() {
        instance = this;
        broadcastUpdate(false);
    }

    @Override
    public void onListenerDisconnected() {
        instance = null;
        broadcastUpdate(true);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        broadcastUpdate(false);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        broadcastUpdate(false);
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        lastRankingMap = rankingMap;
        broadcastUpdate(false);
    }

    public static NotificationListener getInstance() {
        return instance;
    }

    public StatusBarNotification[] getActiveNotificationsSafe() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            return active != null ? active : new StatusBarNotification[0];
        } catch (Throwable t) {
            return new StatusBarNotification[0];
        }
    }

    private boolean shouldExclude(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();

        // Avoid double counting summaries.
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return true;

        // Exclude persistent/ongoing notifications (common for Tasker/KWGT, media, VPN, etc).
        if ((n.flags & Notification.FLAG_ONGOING_EVENT) != 0) return true;

        // Exclude "silent" notifications using the system ranking when available.
        RankingMap rm = lastRankingMap;
        if (rm != null) {
            try {
                Ranking r = new Ranking();
                if (rm.getRanking(sbn.getKey(), r)) {
                    int imp = r.getImportance();
                    // Treat LOW/MIN as silent
                    if (imp <= NotificationManager.IMPORTANCE_LOW) return true;
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    /**
     * Build a map of counts and latest post times per package and broadcast it.
     * If empty==true we broadcast empty maps.
     */
    private void broadcastUpdate(boolean empty) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        LinkedHashMap<String, Long> latest = new LinkedHashMap<>();

        if (!empty) {
            boolean filterEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_FILTER_ENABLED, false);
            java.util.Set<String> allowed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getStringSet(PREF_ALLOWED_PKGS, null);

            try {
                StatusBarNotification[] active = getActiveNotificationsSafe();
                for (StatusBarNotification sbn : active) {
                    if (sbn == null) continue;
                    if (shouldExclude(sbn)) continue;

                    String pkg = sbn.getPackageName();
                    if (filterEnabled && allowed != null && !allowed.contains(pkg)) continue;
                    counts.put(pkg, counts.getOrDefault(pkg, 0) + 1);

                    long t = sbn.getPostTime();
                    Long cur = latest.get(pkg);
                    if (cur == null || t > cur) {
                        latest.put(pkg, t);
                    }
                }
            } catch (Throwable ignored) {}
        }

        Intent i = new Intent(NotificationRowWidgetProvider.ACTION_REFRESH_WIDGET);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_COUNTS, (Serializable) new HashMap<>(counts));
        i.putExtra(EXTRA_LATEST, (Serializable) new HashMap<>(latest));
        sendBroadcast(i);
    }

    /**
     * Optional helper similar to bblauncher: if listener permission is enabled but instance is null,
     * request a rebind.
     */
    public static void requestRebindIfNeeded(Context context) {
        if (instance != null) return;

        try {
            String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (enabled == null) return;

            String pkg = context.getPackageName();
            if (enabled.contains(pkg)) {
                NotificationListenerService.requestRebind(new ComponentName(context, NotificationListener.class));
            }
        } catch (Throwable ignored) {}
    }
}
