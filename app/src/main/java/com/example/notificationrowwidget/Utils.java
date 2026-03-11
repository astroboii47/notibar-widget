package com.example.notificationrowwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class Utils {

    public static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Renders ANY Drawable (including AdaptiveIconDrawable with no intrinsic size) into a square bitmap.
     */
    public static Bitmap drawableToBitmapSized(Context context, Drawable drawable, int sizeDp) {
        if (drawable == null) return null;

        int sizePx = dpToPx(context, sizeDp);
        return drawableToBitmapPx(drawable, sizePx);
    }

    public static Bitmap drawableToBitmapPx(Drawable drawable, int sizePx) {
        if (drawable == null || sizePx <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, sizePx, sizePx);
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap fitBitmapToSquare(Bitmap source, int sizePx) {
        if (source == null || sizePx <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float scale = Math.min((float) sizePx / source.getWidth(), (float) sizePx / source.getHeight());
        int dstW = Math.max(1, Math.round(source.getWidth() * scale));
        int dstH = Math.max(1, Math.round(source.getHeight() * scale));
        int left = (sizePx - dstW) / 2;
        int top = (sizePx - dstH) / 2;

        canvas.drawBitmap(source, null, new Rect(left, top, left + dstW, top + dstH), null);
        return bitmap;
    }
}
