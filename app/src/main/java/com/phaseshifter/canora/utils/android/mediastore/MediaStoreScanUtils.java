package com.phaseshifter.canora.utils.android.mediastore;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;

public class MediaStoreScanUtils {
    public static void updateAudioUri(ContentResolver contentResolver, Uri file, AudioMetadataMemory data) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.AudioColumns.TITLE, data.getTitle());
        values.put(MediaStore.Audio.AudioColumns.ARTIST, data.getArtist());
        values.put(MediaStore.Audio.AudioColumns.ALBUM, data.getAlbum());
        contentResolver.update(file, values, null, null);
    }
}