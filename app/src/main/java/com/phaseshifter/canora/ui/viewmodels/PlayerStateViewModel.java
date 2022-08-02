package com.phaseshifter.canora.ui.viewmodels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadata;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.service.state.PlaybackState;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.utils.Observable;

import java.util.Objects;

public class PlayerStateViewModel {
    public final Observable<Boolean> buffering = new Observable<>();

    public final Observable<String> trackText = new Observable<>();
    public final Observable<String> trackTitle = new Observable<>();
    public final Observable<String> trackArtist = new Observable<>();
    public final Observable<Bitmap> trackArtwork = new Observable<>();
    public final Observable<Long> trackLength = new Observable<>(0L);
    public final Observable<Long> trackPosition = new Observable<>(0L);

    public final Observable<Boolean> isPlaying = new Observable<>(false);
    public final Observable<Boolean> isShuffling = new Observable<>(false);
    public final Observable<Boolean> isRepeating = new Observable<>(false);
    public final Observable<Float> volume = new Observable<>(0f);
}