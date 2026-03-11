package com.example.notificationrowwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import android.app.Activity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAccess = findViewById(R.id.btn_notification_access);
        Button btnWidgetSettings = findViewById(R.id.btn_widget_settings);

        btnAccess.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(i);
        });

        btnWidgetSettings.setOnClickListener(v -> {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Nudge widget update when returning (optional)
        Intent intent = new Intent(this, NotificationRowWidgetProvider.class);
        intent.setAction(NotificationRowWidgetProvider.ACTION_REFRESH_WIDGET);

        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(this, NotificationRowWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
}