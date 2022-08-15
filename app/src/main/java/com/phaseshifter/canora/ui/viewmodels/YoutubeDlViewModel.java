package com.phaseshifter.canora.ui.viewmodels;

import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadata;
import com.phaseshifter.canora.ui.data.DownloadInfo;
import com.phaseshifter.canora.ui.data.DownloadProgress;
import com.phaseshifter.canora.utils.Observable;

import java.util.ArrayList;
import java.util.List;

public class YoutubeDlViewModel {
    public Observable<String> url = new Observable<>(null);
    public Observable<DownloadInfo> infoForUrl = new Observable<>(null);
    public Observable<List<DownloadProgress>> downloads = new Observable<>(new ArrayList<>());

    public void notifyObservers() {
        url.notifyObservers();
        infoForUrl.notifyObservers();
        downloads.notifyObservers();
    }
}
