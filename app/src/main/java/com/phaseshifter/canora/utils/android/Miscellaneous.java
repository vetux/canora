package com.phaseshifter.canora.utils.android;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.concurrent.TimeUnit;

import static com.phaseshifter.canora.utils.IntegerConversion.safeLongToInt;

/**
 * Temporary class for small functions.
 */
public class Miscellaneous {
    public static Bitmap decodeDrawable(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String digitize(long miliseconds) {
        return leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toMinutes(miliseconds))) + ":" + leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toSeconds(miliseconds - TimeUnit.MILLISECONDS.toMinutes(miliseconds) * 60000)));
    }

    public static String leftpadZero(long val) {
        if ((val - 10) < 0) {
            return "0" + val;
        } else {
            return "" + val;
        }
    }

    public static String trimFileExtension(String input) {
        return input.substring(0, input.lastIndexOf('.'));
    }

    public static void invalidateRecursive(ViewGroup layout) {
        int count = layout.getChildCount();
        View child;
        for (int i = 0; i < count; i++) {
            child = layout.getChildAt(i);
            if (child instanceof ViewGroup)
                invalidateRecursive((ViewGroup) child);
            else
                child.invalidate();
        }
    }

    public static void toggleKeyboardView(Context context, View view, boolean b) {
        if (view == null)
            return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (b)
                imm.showSoftInput(view, 0);
            else
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void expandTouchArea(final View bigView, final View smallView, final int extraPadding) {
        bigView.post(() -> {
            Rect rect = new Rect();
            smallView.getHitRect(rect);
            rect.top -= extraPadding;
            rect.left -= extraPadding;
            rect.right += extraPadding;
            rect.bottom += extraPadding;
            bigView.setTouchDelegate(new TouchDelegate(rect, smallView));
        });
    }

    public static String getRealPathFromURI(Uri contentUri, Activity m) {
        String ret = null;
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = m.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            ret = cursor.getString(column_index);
            cursor.close();
        }
        return ret;
    }
}