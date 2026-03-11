package com.example.notificationrowwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {

    private static final String PREFS_NAME = "notif_row_prefs";
    private static final String PREF_MAX_VISIBLE = "max_visible";
    private static final String PREF_BADGE_SP = "badge_sp";
    private static final String PREF_OVERLAY_DP = "overlay_dp";
    private static final String PREF_FILTER_ENABLED = "filter_enabled";
    private static final String PREF_ALLOWED_PKGS = "allowed_pkgs";
    private static final int[] PREVIEW_IDS = {
            R.id.preview_icon0, R.id.preview_icon1, R.id.preview_icon2, R.id.preview_icon3
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Spinner countSpinner = findViewById(R.id.spinner_icon_count);
        Spinner sizeSpinner = findViewById(R.id.spinner_font_size);
        Spinner overlaySpinner = findViewById(R.id.spinner_overlay_size);
        Button saveBtn = findViewById(R.id.btn_save);
        Button chooseAppsBtn = findViewById(R.id.btn_choose_apps);
        Button choosePackBtn = findViewById(R.id.btn_choose_icon_pack);
        CheckBox filterCb = findViewById(R.id.checkbox_filter);
        TextView appsSummary = findViewById(R.id.text_apps_summary);
        TextView packSummary = findViewById(R.id.text_icon_pack_summary);


        Integer[] counts = new Integer[]{1,2,3,4,5,6,7,8};
        Integer[] sizes = new Integer[]{12,14,16,18,20,22,24};
        Integer[] overlays = new Integer[]{8,10,12,14,16};

        countSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, counts));
        sizeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sizes));
        overlaySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, overlays));

        int curCount = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(PREF_MAX_VISIBLE, 5);
        float curSize = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getFloat(PREF_BADGE_SP, 14f);
        int curOverlay = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(PREF_OVERLAY_DP, 12);
        boolean curFilter = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_FILTER_ENABLED, false);
        filterCb.setChecked(curFilter);
        updateAppsSummary(appsSummary);

        chooseAppsBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AppPickerActivity.class));
        });

        choosePackBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, IconPackPickerActivity.class));
        });


        for (int i = 0; i < counts.length; i++) if (counts[i] == curCount) { countSpinner.setSelection(i); break; }
        for (int i = 0; i < sizes.length; i++) if (sizes[i].floatValue() == curSize) { sizeSpinner.setSelection(i); break; }
        for (int i = 0; i < overlays.length; i++) if (overlays[i] == curOverlay) { overlaySpinner.setSelection(i); break; }

        saveBtn.setOnClickListener(v -> {
            int selectedCount = (Integer) countSpinner.getSelectedItem();
            int selectedSize = (Integer) sizeSpinner.getSelectedItem();
            int selectedOverlay = (Integer) overlaySpinner.getSelectedItem();

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putInt(PREF_MAX_VISIBLE, selectedCount)
                    .putFloat(PREF_BADGE_SP, (float) selectedSize)
                    .putInt(PREF_OVERLAY_DP, selectedOverlay)
                    .putBoolean(PREF_FILTER_ENABLED, filterCb.isChecked())
                    .apply();

            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(this, NotificationRowWidgetProvider.class));

            Intent refresh = new Intent(this, NotificationRowWidgetProvider.class);
            refresh.setAction(NotificationRowWidgetProvider.ACTION_REFRESH_WIDGET);
            refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(refresh);

            finish();
        });

        updatePackSummary(packSummary);
        updateIconPreview();
    }
@Override
protected void onResume() {
    super.onResume();
    TextView appsSummary = findViewById(R.id.text_apps_summary);
    TextView packSummary = findViewById(R.id.text_icon_pack_summary);
    updateAppsSummary(appsSummary);
    updatePackSummary(packSummary);
    updateIconPreview();
}

private void updateAppsSummary(TextView tv) {
    if (tv == null) return;
    Set<String> allowed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getStringSet(PREF_ALLOWED_PKGS, null);
    if (allowed == null) {
        tv.setText("All apps");
    } else {
        tv.setText(allowed.size() + " selected");
    }
}

private void updatePackSummary(TextView tv) {
    if (tv == null) return;
    String pack = IconRepository.getSelectedIconPack(this);
    tv.setText("Icon pack: " + IconRepository.getIconPackLabel(this, pack));
}

private void updateIconPreview() {
    Set<String> allowed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getStringSet(PREF_ALLOWED_PKGS, null);
    List<String> previewPkgs = IconRepository.previewPackages(this, allowed, PREVIEW_IDS.length);

    for (int i = 0; i < PREVIEW_IDS.length; i++) {
        ImageView imageView = findViewById(PREVIEW_IDS[i]);
        if (imageView == null) continue;

        if (i < previewPkgs.size()) {
            Bitmap bitmap = IconRepository.getPreviewIconBitmap(this, previewPkgs.get(i), 32);
            imageView.setImageBitmap(bitmap);
            imageView.setAlpha(1f);
        } else {
            imageView.setImageDrawable(null);
            imageView.setAlpha(0.25f);
        }
    }
}

}
