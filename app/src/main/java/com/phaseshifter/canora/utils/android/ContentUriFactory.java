package com.phaseshifter.canora.utils.android;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

public class ContentUriFactory {
    public Uri withAppendedId(Uri contentUri, long id) {
        return ContentUris.withAppendedId(contentUri, id);
    }

    public Uri getGenreContentUriFromAudioId(int audioID) {
        return MediaStore.Audio.Genres.getContentUriForAudioId("external", audioID);
    }

    public Uri getAlbumArtworkUriFromAlbumId(long albumID) {
        final Uri magicUri = Uri.parse("content://media/external/audio/albumart");
        return withAppendedId(magicUri, albumID);
    }
}