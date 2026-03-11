package com.example.notificationrowwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class IconRepository {

    public static final String PREFS_NAME = "notif_row_prefs";
    public static final String PREF_SELECTED_ICON_PACK = "selected_icon_pack";
    public static final String PREF_PACK_DISABLED_PKGS = "pack_disabled_pkgs";
    public static final String CUSTOM_ICON_SOURCE = "Custom image";
    public static final String PACK_ICON_SOURCE = "Icon pack";
    public static final String DEFAULT_ICON_SOURCE = "App icon";

    private static final String CUSTOM_ICON_DIR = "custom_icons";
    private static final String WIDGET_CACHE_DIR = "widget_icon_cache";
    private static final Object PACK_LOCK = new Object();
    private static final Map<String, IconPackIndex> PACK_CACHE = new HashMap<>();

    private IconRepository() {}

    public static String getSelectedIconPack(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_SELECTED_ICON_PACK, null);
    }

    public static void setSelectedIconPack(Context context, String packPkg) {
        String current = getSelectedIconPack(context);
        if (TextUtils.equals(current, packPkg)) return;

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_SELECTED_ICON_PACK, packPkg)
                .apply();
        clearWidgetCache(context);
    }

    public static boolean hasCustomIcon(Context context, String pkg) {
        return customIconFile(context, pkg).isFile();
    }

    public static boolean hasPackIcon(Context context, String pkg) {
        if (isPackDisabledForPackage(context, pkg)) return false;
        return canUsePackIcon(context, pkg);
    }

    public static boolean canUsePackIcon(Context context, String pkg) {
        return resolvePackDrawable(context, getSelectedIconPack(context), pkg) != null;
    }

    public static String getIconSourceLabel(Context context, String pkg) {
        if (hasCustomIcon(context, pkg)) return CUSTOM_ICON_SOURCE;
        if (hasPackIcon(context, pkg)) return PACK_ICON_SOURCE;
        return DEFAULT_ICON_SOURCE;
    }

    public static Bitmap getWidgetIconBitmap(Context context, String pkg, int sizeDp) {
        File cached = widgetCacheFile(context, pkg);
        int sizePx = Utils.dpToPx(context, sizeDp);
        Bitmap bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath());
        if (bitmap != null && bitmap.getWidth() == sizePx && bitmap.getHeight() == sizePx) {
            return bitmap;
        }

        bitmap = buildEffectiveIcon(context, pkg, sizePx);
        if (bitmap != null) {
            writeBitmap(bitmap, cached);
        }
        return bitmap;
    }

    public static Bitmap getPreviewIconBitmap(Context context, String pkg, int sizeDp) {
        return buildEffectiveIcon(context, pkg, Utils.dpToPx(context, sizeDp));
    }

    public static boolean saveCustomIcon(Context context, String pkg, Uri uri) {
        if (uri == null) return false;

        Bitmap bitmap = decodeBitmap(context, uri, 256);
        if (bitmap == null) return false;

        boolean ok = writeBitmap(bitmap, customIconFile(context, pkg));
        if (ok) {
            setPackEnabledForPackage(context, pkg, true);
            invalidatePackage(context, pkg);
        }
        return ok;
    }

    public static void clearCustomIcon(Context context, String pkg) {
        File file = customIconFile(context, pkg);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        invalidatePackage(context, pkg);
    }

    public static boolean isPackDisabledForPackage(Context context, String pkg) {
        Set<String> disabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(PREF_PACK_DISABLED_PKGS, null);
        return disabled != null && disabled.contains(pkg);
    }

    public static void setPackEnabledForPackage(Context context, String pkg, boolean enabled) {
        Set<String> disabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(PREF_PACK_DISABLED_PKGS, null);
        Set<String> next = disabled != null ? new HashSet<>(disabled) : new HashSet<>();
        if (enabled) next.remove(pkg);
        else next.add(pkg);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(PREF_PACK_DISABLED_PKGS, next)
                .apply();
        invalidatePackage(context, pkg);
    }

    public static void invalidatePackage(Context context, String pkg) {
        File cached = widgetCacheFile(context, pkg);
        if (cached.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cached.delete();
        }
    }

    public static void clearWidgetCache(Context context) {
        File dir = widgetCacheDir(context);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static CharSequence getIconPackLabel(Context context, String packPkg) {
        if (TextUtils.isEmpty(packPkg)) return "None";
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packPkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            return TextUtils.isEmpty(label) ? packPkg : label;
        } catch (Throwable t) {
            return packPkg;
        }
    }

    public static List<String> previewPackages(Context context, Set<String> preferred, int limit) {
        List<String> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        Collections.sort(apps, Comparator.comparing(a -> appLabel(pm, a).toLowerCase(Locale.getDefault())));

        Set<String> preferredSet = preferred != null ? new HashSet<>(preferred) : null;

        if (preferredSet != null && !preferredSet.isEmpty()) {
            for (ApplicationInfo ai : apps) {
                if (result.size() >= limit) break;
                if (ai == null || TextUtils.equals(ai.packageName, context.getPackageName())) continue;
                if (preferredSet.contains(ai.packageName)) result.add(ai.packageName);
            }
        }

        for (ApplicationInfo ai : apps) {
            if (result.size() >= limit) break;
            if (ai == null || TextUtils.equals(ai.packageName, context.getPackageName())) continue;
            if (!result.contains(ai.packageName)) result.add(ai.packageName);
        }

        return result;
    }

    public static List<IconPackInfo> findAvailableIconPacks(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<IconPackInfo> packs = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if (app == null || TextUtils.equals(app.packageName, context.getPackageName())) continue;
            if (hasAppFilter(context, app.packageName)) {
                packs.add(new IconPackInfo(
                        app.packageName,
                        String.valueOf(getIconPackLabel(context, app.packageName))
                ));
            }
        }

        Collections.sort(packs, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
        return packs;
    }

    private static String appLabel(PackageManager pm, ApplicationInfo info) {
        CharSequence label = pm.getApplicationLabel(info);
        return label != null ? label.toString() : info.packageName;
    }

    private static Bitmap buildEffectiveIcon(Context context, String pkg, int sizePx) {
        Bitmap custom = loadCustomBitmap(context, pkg, sizePx);
        if (custom != null) return custom;

        Drawable packDrawable = isPackDisabledForPackage(context, pkg)
                ? null
                : resolvePackDrawable(context, getSelectedIconPack(context), pkg);
        if (packDrawable != null) {
            return Utils.drawableToBitmapPx(packDrawable, sizePx);
        }

        try {
            Drawable appIcon = context.getPackageManager().getApplicationIcon(pkg);
            return Utils.drawableToBitmapPx(appIcon, sizePx);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap loadCustomBitmap(Context context, String pkg, int sizePx) {
        File file = customIconFile(context, pkg);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) return null;
        return Utils.fitBitmapToSquare(bitmap, sizePx);
    }

    private static Bitmap decodeBitmap(Context context, Uri uri, int maxSizePx) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    int largest = Math.max(width, height);
                    if (largest > maxSizePx) {
                        float scale = (float) maxSizePx / largest;
                        decoder.setTargetSize(
                                Math.max(1, Math.round(width * scale)),
                                Math.max(1, Math.round(height * scale))
                        );
                    }
                });
            }

            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            try {
                return BitmapFactory.decodeStream(in);
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean writeBitmap(Bitmap bitmap, File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Throwable t) {
            return false;
        }
    }

    private static File customIconFile(Context context, String pkg) {
        return new File(customIconDir(context), pkg + ".png");
    }

    private static File widgetCacheFile(Context context, String pkg) {
        return new File(widgetCacheDir(context), pkg + ".png");
    }

    private static File customIconDir(Context context) {
        File dir = new File(context.getFilesDir(), CUSTOM_ICON_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private static File widgetCacheDir(Context context) {
        File dir = new File(context.getFilesDir(), WIDGET_CACHE_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private static Drawable resolvePackDrawable(Context context, String packPkg, String targetPkg) {
        if (TextUtils.isEmpty(packPkg) || TextUtils.isEmpty(targetPkg)) return null;

        IconPackIndex index = loadPackIndex(context, packPkg);
        if (index == null) return null;

        String componentKey = launchComponentKey(context, targetPkg);
        String drawableName = null;

        if (!TextUtils.isEmpty(componentKey)) {
            drawableName = index.byComponent.get(componentKey);
        }
        if (drawableName == null) {
            drawableName = index.byPackage.get(targetPkg);
        }
        if (drawableName == null) return null;

        try {
            PackageManager pm = context.getPackageManager();
            Context packContext = context.createPackageContext(packPkg, 0);
            int resId = packContext.getResources().getIdentifier(drawableName, "drawable", packPkg);
            if (resId == 0) {
                resId = packContext.getResources().getIdentifier(drawableName, "mipmap", packPkg);
            }
            if (resId == 0) return null;
            return pm.getDrawable(packPkg, resId, packContext.getApplicationInfo());
        } catch (Throwable t) {
            return null;
        }
    }

    private static String launchComponentKey(Context context, String targetPkg) {
        try {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(targetPkg);
            ComponentName component = launch != null ? launch.getComponent() : null;
            if (component == null) return null;
            return normalizeComponent(component.getPackageName() + "/" + component.getClassName());
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hasAppFilter(Context context, String packPkg) {
        return loadPackIndex(context, packPkg) != null;
    }

    private static IconPackIndex loadPackIndex(Context context, String packPkg) {
        synchronized (PACK_LOCK) {
            if (PACK_CACHE.containsKey(packPkg)) {
                return PACK_CACHE.get(packPkg);
            }
        }

        IconPackIndex parsed = parsePackIndex(context, packPkg);
        synchronized (PACK_LOCK) {
            PACK_CACHE.put(packPkg, parsed);
        }
        return parsed;
    }

    private static IconPackIndex parsePackIndex(Context context, String packPkg) {
        try {
            Context packContext = context.createPackageContext(packPkg, 0);
            try (InputStream in = packContext.getAssets().open("appfilter.xml")) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(in, "utf-8");

                IconPackIndex index = new IconPackIndex();
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (!TextUtils.isEmpty(component) && !TextUtils.isEmpty(drawable)) {
                            String normalized = normalizeComponent(component);
                            if (!TextUtils.isEmpty(normalized)) {
                                index.byComponent.put(normalized, drawable);
                                String pkg = packageFromComponent(normalized);
                                if (!TextUtils.isEmpty(pkg) && !index.byPackage.containsKey(pkg)) {
                                    index.byPackage.put(pkg, drawable);
                                }
                            }
                        }
                    }
                    eventType = parser.next();
                }

                return index.byComponent.isEmpty() && index.byPackage.isEmpty() ? null : index;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static String normalizeComponent(String raw) {
        if (TextUtils.isEmpty(raw)) return null;

        String value = raw.trim();
        if (value.startsWith("ComponentInfo{")) {
            value = value.substring("ComponentInfo{".length());
        }
        if (value.endsWith("}")) {
            value = value.substring(0, value.length() - 1);
        }

        int slash = value.indexOf('/');
        if (slash <= 0 || slash >= value.length() - 1) return null;

        String pkg = value.substring(0, slash);
        String cls = value.substring(slash + 1);
        if (cls.startsWith(".")) {
            cls = pkg + cls;
        }
        return pkg + "/" + cls;
    }

    private static String packageFromComponent(String normalized) {
        int slash = normalized.indexOf('/');
        return slash > 0 ? normalized.substring(0, slash) : null;
    }

    public static final class IconPackInfo {
        public final String packageName;
        public final String label;

        public IconPackInfo(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }

    private static final class IconPackIndex {
        final Map<String, String> byComponent = new HashMap<>();
        final Map<String, String> byPackage = new HashMap<>();
    }
}
