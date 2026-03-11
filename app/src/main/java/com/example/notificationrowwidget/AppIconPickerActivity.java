package com.example.notificationrowwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppIconPickerActivity extends Activity {

    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_APP_LABEL = "app_label";

    private static final int REQ_PICK_IMAGE = 1001;

    private String packageName;
    private String appLabel;

    private ImageView preview;
    private TextView source;
    private Button usePackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_icon_picker);

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        appLabel = getIntent().getStringExtra(EXTRA_APP_LABEL);

        if (TextUtils.isEmpty(packageName)) {
            finish();
            return;
        }

        preview = findViewById(R.id.icon_preview);
        source = findViewById(R.id.text_icon_source);
        usePackButton = findViewById(R.id.btn_use_pack_icon);

        TextView title = findViewById(R.id.text_app_title);
        TextView subtitle = findViewById(R.id.text_app_subtitle);

        title.setText(!TextUtils.isEmpty(appLabel) ? appLabel : packageName);
        subtitle.setText(packageName);

        Button useDefaultButton = findViewById(R.id.btn_use_app_icon);
        Button chooseCustomButton = findViewById(R.id.btn_choose_custom_icon);
        Button removeCustomButton = findViewById(R.id.btn_remove_custom_icon);

        useDefaultButton.setOnClickListener(v -> {
            IconRepository.clearCustomIcon(this, packageName);
            IconRepository.setPackEnabledForPackage(this, packageName, false);
            refreshAndClose();
        });

        usePackButton.setOnClickListener(v -> {
            IconRepository.clearCustomIcon(this, packageName);
            IconRepository.setPackEnabledForPackage(this, packageName, true);
            refreshAndClose();
        });

        chooseCustomButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK_IMAGE);
        });

        removeCustomButton.setOnClickListener(v -> {
            IconRepository.clearCustomIcon(this, packageName);
            refreshAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPreview();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {}

        if (IconRepository.saveCustomIcon(this, packageName, uri)) {
            refreshAndClose();
        }
    }

    private void bindPreview() {
        Bitmap bitmap = IconRepository.getPreviewIconBitmap(this, packageName, 72);
        preview.setImageBitmap(bitmap);
        source.setText("Current source: " + IconRepository.getIconSourceLabel(this, packageName));
        usePackButton.setEnabled(IconRepository.canUsePackIcon(this, packageName));
    }

    private void refreshAndClose() {
        NotificationRowWidgetProvider.requestRefresh(this);
        setResult(RESULT_OK);
        finish();
    }
}
