package com.example.notificationrowwidget;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class IconPackRowAdapter extends ArrayAdapter<IconPackPickerActivity.RowItem> {

    private final String selectedPackage;

    public IconPackRowAdapter(Context context,
                              List<IconPackPickerActivity.RowItem> items,
                              String selectedPackage) {
        super(context, 0, items);
        this.selectedPackage = selectedPackage;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_icon_pack, parent, false);
        }

        IconPackPickerActivity.RowItem item = getItem(position);
        if (item == null) return view;

        TextView title = view.findViewById(R.id.pack_title);
        TextView subtitle = view.findViewById(R.id.pack_subtitle);
        ImageView check = view.findViewById(R.id.pack_selected);

        title.setText(item.title);
        subtitle.setText(item.subtitle);

        boolean selected = TextUtils.equals(selectedPackage, item.packageName)
                || (TextUtils.isEmpty(selectedPackage) && item.packageName == null);
        check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

        return view;
    }
}
