package com.phaseshifter.canora.utils.android.bitmap;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.FileNotFoundException;

public class BitmapUtils {

    public static Bitmap getBitmapForResource(Context context, int drawableResourceID) {
        try {
            return BitmapFactory.decodeResource(context.getResources(), drawableResourceID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getBitmapForUri(ContentResolver contentResolver, Uri bitmapUri) {
        if (bitmapUri == null)
            return null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source s = ImageDecoder.createSource(contentResolver, bitmapUri);
                return ImageDecoder.decodeBitmap(s);
            } else {
                return MediaStore.Images.Media.getBitmap(contentResolver, bitmapUri);
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}