package com.phaseshifter.canora.utils.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.ImageView;

public class AttributeConversion {
    public static int getColorForAtt(int v, Context m) {
        TypedValue tv = new TypedValue();
        Resources.Theme theme = m.getTheme();
        theme.resolveAttribute(v, tv, true);
        return tv.data;
    }

    public static int getColorForAtt(int v, Resources.Theme theme) {
        TypedValue tv = new TypedValue();
        theme.resolveAttribute(v, tv, true);
        return tv.data;
    }

    public static Drawable getDrawableForAtt(int attributeID, Context context, Resources.Theme theme) throws Resources.NotFoundException {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attributeID, typedValue, true);
        switch (typedValue.type) {
            case TypedValue.TYPE_INT_COLOR_ARGB4:
            case TypedValue.TYPE_INT_COLOR_ARGB8:
            case TypedValue.TYPE_INT_COLOR_RGB4:
            case TypedValue.TYPE_INT_COLOR_RGB8:
                //Attribute references color
                return new ColorDrawable(typedValue.data);
            case TypedValue.TYPE_STRING:
            case TypedValue.TYPE_REFERENCE:
                //Attribute references drawable
                return context.getResources().getDrawable(typedValue.resourceId, theme);
            case TypedValue.TYPE_NULL:
                throw new RuntimeException("Attribute invalid data, Have you defined the requested Resource id " + attributeID + " in the theme " + theme.toString() + " ?");
            default:
                throw new RuntimeException("UNABLE TO PARSE TYPE FOR ATTRIBUTE:" + attributeID);
        }
    }

    public static Drawable getDrawableForAtt(int attributeId, Context context) throws Resources.NotFoundException {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        switch (typedValue.type) {
            case TypedValue.TYPE_INT_COLOR_ARGB4:
            case TypedValue.TYPE_INT_COLOR_ARGB8:
            case TypedValue.TYPE_INT_COLOR_RGB4:
            case TypedValue.TYPE_INT_COLOR_RGB8:
                //Attribute references color
                return new ColorDrawable(typedValue.data);
            case TypedValue.TYPE_STRING:
            case TypedValue.TYPE_REFERENCE:
                //Attribute references drawable
                return context.getResources().getDrawable(typedValue.resourceId, context.getTheme());
            case TypedValue.TYPE_NULL:
                throw new RuntimeException("Attribute invalid data");
            default:
                throw new RuntimeException("UNABLE TO PARSE TYPE FOR ATTRIBUTE:" + attributeId);
        }
    }

    public static ImageView.ScaleType getScaleTypeForAtt(int attributeId, Context context) throws Resources.NotFoundException {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        switch (typedValue.type) {
            case TypedValue.TYPE_INT_DEC:
                return ImageView.ScaleType.values()[typedValue.data];
            case TypedValue.TYPE_NULL:
                throw new RuntimeException("Attribute invalid data");
            default:
                throw new RuntimeException("UNABLE TO PARSE TYPE FOR ATTRIBUTE:" + attributeId);
        }
    }

    public static Drawable getStyledDrawable(int drawableID, Context m) {
        return m.getResources().getDrawable(drawableID, m.getTheme());
    }

    public static Drawable getStyledDrawable(int drawableID, Context m, Resources.Theme theme) {
        return m.getResources().getDrawable(drawableID, theme);
    }
}