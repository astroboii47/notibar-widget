package com.example.notificationrowwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRowWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_WIDGET = "com.example.notificationrowwidget.ACTION_REFRESH_WIDGET";
    private static final int DEFAULT_MAX_VISIBLE = 5;
    private static final int MAX_SLOTS = 8;
    private static final String PREFS_NAME = "notif_row_prefs";
    private static final String PREF_MAX_VISIBLE = "max_visible";
    private static final String PREF_BADGE_SP = "badge_sp";
    private static final String PREF_OVERLAY_DP = "overlay_dp";
    private static final String PREF_FILTER_ENABLED = "filter_enabled";
    private static final String PREF_ALLOWED_PKGS = "allowed_pkgs";

    private static int prefMaxVisible(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_MAX_VISIBLE, DEFAULT_MAX_VISIBLE);
    }

    private static float prefBadgeSp(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getFloat(PREF_BADGE_SP, 14f);
    }

    public static void requestRefresh(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, NotificationRowWidgetProvider.class));

        Intent refresh = new Intent(context, NotificationRowWidgetProvider.class);
        refresh.setAction(ACTION_REFRESH_WIDGET);
        refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(refresh);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_REFRESH_WIDGET.equals(action) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);

            int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (ids == null) {
                ids = mgr.getAppWidgetIds(new ComponentName(context, NotificationRowWidgetProvider.class));
            }

            // Prefer push-data from listener (more reliable than querying the service here)
            Map<String, Integer> counts = readMap(intent.getSerializableExtra(NotificationListener.EXTRA_COUNTS), Integer.class);
            Map<String, Long> latest = readMap(intent.getSerializableExtra(NotificationListener.EXTRA_LATEST), Long.class);

            updateAll(context, mgr, ids, counts, latest);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // If we're invoked by the system without extras, ask the listener to rebind (if enabled) and
        // do a best-effort render using current listener state (may be empty until it pushes an update).
        NotificationListener.requestRebindIfNeeded(context);
        updateAll(context, appWidgetManager, appWidgetIds, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> readMap(Serializable s, Class<T> cls) {
        if (!(s instanceof HashMap)) return null;
        return (HashMap<String, T>) s;
    }

    
    private static void updateAll(Context context,
                                  AppWidgetManager appWidgetManager,
                                  int[] appWidgetIds,
                                  Map<String, Integer> pushedCounts,
                                  Map<String, Long> pushedLatest) {

        Map<String, Integer> counts = pushedCounts;
        boolean filterEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_FILTER_ENABLED, false);
        java.util.Set<String> allowedPkgs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(PREF_ALLOWED_PKGS, null);

        Map<String, Long> latest = pushedLatest;

        if (counts == null || latest == null) {
            counts = new HashMap<>();
            latest = new HashMap<>();

            NotificationListener listener = NotificationListener.getInstance();
            if (listener != null) {
                try {
                    for (android.service.notification.StatusBarNotification sbn :
                            listener.getActiveNotificationsSafe()) {

                        if (sbn == null) continue;

                        if ((sbn.getNotification().flags & 2) != 0) continue;

                        String pkg = sbn.getPackageName();
                        if (filterEnabled && allowedPkgs != null && !allowedPkgs.contains(pkg)) continue;

                        counts.put(pkg, counts.getOrDefault(pkg, 0) + 1);

                        long postTime = sbn.getPostTime();
                        Long current = latest.get(pkg);
                        if (current == null || postTime > current) {
                            latest.put(pkg, postTime);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (filterEnabled && allowedPkgs != null && counts != null) {
    java.util.ArrayList<String> toRemove = new java.util.ArrayList<>();
    for (String k : counts.keySet()) {
        if (!allowedPkgs.contains(k)) toRemove.add(k);
    }
    for (String k : toRemove) {
        counts.remove(k);
        if (latest != null) latest.remove(k);
    }
}

        final Map<String, Long> latestRef = latest;
        List<String> pkgs = new ArrayList<>(counts.keySet());
        pkgs.sort((a, b) ->
                Long.compare(latestRef.getOrDefault(b, 0L),
                             latestRef.getOrDefault(a, 0L)));

        PackageManager pm = context.getPackageManager();

        int[] itemIds = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7};
        int[] badgeIds = {R.id.badge0, R.id.badge1, R.id.badge2, R.id.badge3, R.id.badge4, R.id.badge5, R.id.badge6, R.id.badge7};
        int[] iconIds  = {R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5, R.id.icon6, R.id.icon7};
        int[] overlayIds = {R.id.overlay0, R.id.overlay1, R.id.overlay2, R.id.overlay3, R.id.overlay4, R.id.overlay5, R.id.overlay6, R.id.overlay7};

        for (int widgetId : appWidgetIds) {

            RemoteViews root =
                    new RemoteViews(context.getPackageName(), R.layout.widget_notification_row);

            // HARD RESET ALL SLOTS FIRST
            for (int i = 0; i < MAX_SLOTS; i++) {
                root.setViewVisibility(itemIds[i], View.GONE);
                root.setViewVisibility(overlayIds[i], View.GONE);
                root.setTextViewText(badgeIds[i], "");
                root.setImageViewBitmap(iconIds[i], null);
            }
            root.setViewVisibility(R.id.extra_text, View.GONE);

            int maxVisible = Math.max(1, Math.min(prefMaxVisible(context), MAX_SLOTS));

            int shown = Math.min(maxVisible, pkgs.size());
            int overlayPx = dpToPx(context, prefOverlayDp(context));

            for (int i = 0; i < shown; i++) {

                String pkg = pkgs.get(i);
                int c = counts.getOrDefault(pkg, 0);

                root.setViewVisibility(itemIds[i], View.VISIBLE);
                root.setViewVisibility(overlayIds[i], View.VISIBLE);
                root.setViewLayoutWidth(overlayIds[i], (float) overlayPx, TypedValue.COMPLEX_UNIT_PX);
                root.setViewLayoutHeight(overlayIds[i], (float) overlayPx, TypedValue.COMPLEX_UNIT_PX);
                root.setViewVisibility(badgeIds[i], View.VISIBLE);
                root.setTextViewTextSize(badgeIds[i], TypedValue.COMPLEX_UNIT_SP, prefBadgeSp(context));
                root.setTextViewText(badgeIds[i],
                        c > 9 ? "9+" : String.valueOf(c));

                try {
                    Bitmap bmp = IconRepository.getWidgetIconBitmap(context, pkg, 24);
                    if (bmp != null) {
                        root.setImageViewBitmap(iconIds[i], bmp);
                    }
                } catch (Throwable ignored) {}

                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    PendingIntent pi = PendingIntent.getActivity(
                            context,
                            (pkg + ":" + i).hashCode(),
                            launch,
                            (Build.VERSION.SDK_INT >= 31)
                                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    : PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    root.setOnClickPendingIntent(iconIds[i], pi);
                }
            }

            int extra = pkgs.size() - shown;
            if (extra > 0) {
                root.setViewVisibility(R.id.extra_text, View.VISIBLE);
                root.setTextViewTextSize(R.id.extra_text, TypedValue.COMPLEX_UNIT_SP, prefBadgeSp(context));
                root.setTextViewText(R.id.extra_text, extra + "+");
            }

            appWidgetManager.updateAppWidget(widgetId, root);
        }
    }

    private static int prefOverlayDp(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_OVERLAY_DP, 10);
    }

    private static int dpToPx(Context c, int dp) {
        float density = c.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

}
