package com.example.notificationrowwidget;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppPickerActivity extends Activity {

    private static final String PREFS_NAME = "notif_row_prefs";
    private static final String PREF_ALLOWED_PKGS = "allowed_pkgs";

    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<AppEntry> filteredApps = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();

    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        RecyclerView rv = findViewById(R.id.recycler_apps);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppAdapter();
        rv.setAdapter(adapter);

        loadSelection();
        loadApps();

        filteredApps.clear();
        filteredApps.addAll(allApps);
        adapter.notifyDataSetChanged();

        EditText search = findViewById(R.id.edit_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        Button btnAll = findViewById(R.id.btn_select_all);
        Button btnNone = findViewById(R.id.btn_clear_all);
        Button btnSave = findViewById(R.id.btn_save_apps);

        btnAll.setOnClickListener(v -> {
            selected.clear();
            for (AppEntry e : allApps) selected.add(e.pkg);
            adapter.notifyDataSetChanged();
        });

        btnNone.setOnClickListener(v -> {
            selected.clear();
            adapter.notifyDataSetChanged();
        });

        btnSave.setOnClickListener(v -> {
            saveSelection();
            NotificationRowWidgetProvider.requestRefresh(this);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadSelection() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> allowed = sp.getStringSet(PREF_ALLOWED_PKGS, null);
        selected.clear();
        if (allowed == null) {
            // Default to "all selected" when no preference exists yet.
            // We keep selected empty for now; after apps load we will treat empty as all.
            return;
        }
        selected.addAll(allowed);
    }

    private void saveSelection() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // If user selected everything, still store it (simple + predictable).
        sp.edit().putStringSet(PREF_ALLOWED_PKGS, new HashSet<>(selected)).apply();
    }

    private void loadApps() {
        allApps.clear();
        PackageManager pm = getPackageManager();

        List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);

        for (android.content.pm.ApplicationInfo ai : apps) {
            if (ai == null) continue;

            String pkg = ai.packageName;
            if (pkg == null) continue;

            // Skip our own widget app
            if (pkg.equals(getPackageName())) continue;

            CharSequence labelCs = pm.getApplicationLabel(ai);
            String label = labelCs != null ? labelCs.toString() : pkg;

            allApps.add(new AppEntry(pkg, label));
        }

        Collections.sort(allApps, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));

        // If no pref exists yet, default-select ALL now that we know the list.
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (sp.getStringSet(PREF_ALLOWED_PKGS, null) == null) {
            selected.clear();
            for (AppEntry e : allApps) selected.add(e.pkg);
        }
    }

    private void applyFilter(String q) {
        String query = q.trim().toLowerCase(Locale.getDefault());
        filteredApps.clear();
        if (query.isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            for (AppEntry e : allApps) {
                if (e.label.toLowerCase(Locale.getDefault()).contains(query) ||
                        e.pkg.toLowerCase(Locale.getDefault()).contains(query)) {
                    filteredApps.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private static class AppEntry {
        final String pkg;
        final String label;
        AppEntry(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private class AppAdapter extends RecyclerView.Adapter<AppVH> {

        @NonNull
        @Override
        public AppVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_checkbox, parent, false);
            return new AppVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AppVH h, int position) {
            AppEntry e = filteredApps.get(position);
            h.icon.setImageBitmap(IconRepository.getPreviewIconBitmap(AppPickerActivity.this, e.pkg, 32));
            h.label.setText(e.label);
            h.pkg.setText(e.pkg);
            h.iconSource.setText(IconRepository.getIconSourceLabel(AppPickerActivity.this, e.pkg));

            boolean checked = selected.contains(e.pkg);
            h.check.setChecked(checked);

            h.itemView.setOnClickListener(v -> {
                toggle(e.pkg);
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) notifyItemChanged(adapterPos);
            });

            h.check.setOnClickListener(v -> {
                toggle(e.pkg);
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) notifyItemChanged(adapterPos);
            });

            h.iconButton.setOnClickListener(v -> {
                Intent intent = new Intent(AppPickerActivity.this, AppIconPickerActivity.class);
                intent.putExtra(AppIconPickerActivity.EXTRA_PACKAGE_NAME, e.pkg);
                intent.putExtra(AppIconPickerActivity.EXTRA_APP_LABEL, e.label);
                startActivity(intent);
            });
        }

        private void toggle(String pkg) {
            if (selected.contains(pkg)) selected.remove(pkg);
            else selected.add(pkg);
        }

        @Override
        public int getItemCount() {
            return filteredApps.size();
        }
    }

    private static class AppVH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView pkg;
        final TextView iconSource;
        final CheckBox check;
        final Button iconButton;
        AppVH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
            pkg = itemView.findViewById(R.id.app_pkg);
            iconSource = itemView.findViewById(R.id.app_icon_source);
            check = itemView.findViewById(R.id.app_check);
            iconButton = itemView.findViewById(R.id.btn_app_icon);
        }
    }
}
