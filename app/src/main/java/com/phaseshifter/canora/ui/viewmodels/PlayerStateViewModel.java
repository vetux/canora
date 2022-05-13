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
import com.phaseshifter.canora.ui.redux.core.StateListener;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;
import com.phaseshifter.canora.utils.Observable;

import java.util.Objects;

public class PlayerStateViewModel implements StateListener<MainStateImmutable> {
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

    private final Context context;
    private final Bitmap defaultArt;

    private MainStateImmutable previousState;

    public PlayerStateViewModel(Context context) {
        this.context = context;
        defaultArt = BitmapFactory.decodeResource(context.getResources(), R.drawable.artwork_unset);
    }

    @Override
    public void update(MainStateImmutable updatedState) {
        if (previousState != null
                && !Objects.equals(updatedState.getTheme(), previousState.getTheme()))
            previousState = null;

        PlayerState playerState = updatedState.getPlayerState();
        if (previousState == null
                || !Objects.equals(updatedState.getTheme(), previousState.getTheme())
                || !Objects.equals(playerState, previousState.getPlayerState())) {
            if (playerState != null) {
                buffering.set(playerState.getPlaybackState() == PlaybackState.STATE_BUFFERING);
                AudioData currentTrack = playerState.getCurrentTrack();
                if (currentTrack != null) {
                    AudioMetadata metadata = currentTrack.getMetadata();
                    trackText.set(context.getString(R.string.main_text0controlsBy, metadata.getTitle(), metadata.getArtist()));
                    trackTitle.set(metadata.getTitle());
                    trackArtist.set(metadata.getArtist());
                    ImageData artwork = metadata.getArtwork();
                    if (artwork != null)
                        try {
                            trackArtwork.set(artwork.getDataSource().getBitmap(context));
                        } catch (Exception e) {
                            e.printStackTrace();
                            trackArtwork.set(defaultArt);
                        }
                    if (previousState == null
                            || !trackLength.get().equals(metadata.getLength()))
                        trackLength.set(metadata.getLength());
                } else {
                    trackText.set("");
                    trackTitle.set("");
                    trackArtist.set("");
                    trackArtwork.set(defaultArt);
                    trackLength.set(0L);
                }
                if (previousState == null
                        || trackPosition.get() != playerState.getPlayerPosition())
                    trackPosition.set(playerState.getPlayerPosition());
                if (previousState == null
                        || isPlaying.get() != playerState.isPlaying())
                    isPlaying.set(playerState.isPlaying());
                isShuffling.set(playerState.isShuffling());
                isRepeating.set(playerState.isRepeating());
                volume.set(playerState.getVolume());
            } else {
                buffering.set(true);
                trackText.set("");
                trackTitle.set("");
                trackArtist.set("");
                trackArtwork.set(defaultArt);
                trackLength.set(0L);
                trackPosition.set(0L);
                isPlaying.set(false);
                isShuffling.set(false);
                isRepeating.set(false);
                volume.set(0f);
            }
        }
        previousState = updatedState;
    }
}