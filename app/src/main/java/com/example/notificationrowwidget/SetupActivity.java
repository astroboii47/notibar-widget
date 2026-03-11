package com.example.notificationrowwidget;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SetupActivity extends android.app.Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Notification Row Widget - Setup");
        title.setTextSize(18f);

        status = new TextView(this);
        status.setTextSize(16f);

        Button btn = new Button(this);
        btn.setText("Open Notification Access Settings");
        btn.setOnClickListener(v -> openNotificationAccess());

        TextView help = new TextView(this);
        help.setTextSize(14f);
        help.setText("After enabling access, add the widget:\nLong press home screen → Widgets → Notification Row Widget.");

        root.addView(title);
        root.addView(status);
        root.addView(btn);
Button settingsBtn = new Button(this);
        settingsBtn.setText("Widget display settings");
        settingsBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        });
        root.addView(settingsBtn);
        root.addView(help);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status.setText(isListenerEnabled()
                ? "Status: ENABLED ✅"
                : "Status: NOT enabled yet ❌\nEnable \"Allow notification access\" for this app.");
    }

    private void openNotificationAccess() {
        ComponentName cn = new ComponentName(this, NotificationListener.class);
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, cn.flattenToString());
        } else {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    private boolean isListenerEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName cn = new ComponentName(this, NotificationListener.class);
        return enabled.contains(cn.flattenToString());
    }
}
