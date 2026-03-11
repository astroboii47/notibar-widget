package com.example.notificationrowwidget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class IconPackPickerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_pack_picker);

        ListView listView = findViewById(R.id.list_icon_packs);

        List<RowItem> rows = new ArrayList<>();
        rows.add(new RowItem("None", "Use each app's own icon", null));
        for (IconRepository.IconPackInfo info : IconRepository.findAvailableIconPacks(this)) {
            rows.add(new RowItem(info.label, info.packageName, info.packageName));
        }

        IconPackRowAdapter adapter = new IconPackRowAdapter(this, rows, IconRepository.getSelectedIconPack(this));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            RowItem item = rows.get(position);
            IconRepository.setSelectedIconPack(this, item.packageName);
            NotificationRowWidgetProvider.requestRefresh(this);
            finish();
        });
    }

    static final class RowItem {
        final String title;
        final String subtitle;
        final String packageName;

        RowItem(String title, String subtitle, String packageName) {
            this.title = title;
            this.subtitle = subtitle;
            this.packageName = packageName;
        }
    }
}
