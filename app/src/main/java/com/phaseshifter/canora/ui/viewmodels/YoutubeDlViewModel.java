package com.phaseshifter.canora.ui.viewmodels;

import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadata;
import com.phaseshifter.canora.ui.data.DownloadProgress;
import com.phaseshifter.canora.utils.Observable;

import java.util.List;

public class YoutubeDlViewModel {
    public Observable<String> url = new Observable<>(null);
    public Observable<AudioMetadata> metadataForUrl = new Observable<>(null);
    public Observable<List<DownloadProgress>> downloads = new Observable<>(null);
}
