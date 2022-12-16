package com.phaseshifter.canora.utils.android;

import android.content.ContentUris;
import android.net.Uri;

public class ContentUriFactory {
    public Uri withAppendedId(Uri contentUri, long id) {
        return ContentUris.withAppendedId(contentUri, id);
    }
}