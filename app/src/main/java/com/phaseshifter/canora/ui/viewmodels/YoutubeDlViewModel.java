package com.phaseshifter.canora.ui.viewmodels;

import com.phaseshifter.canora.ui.data.DownloadInfo;
import com.phaseshifter.canora.utils.Observable;

public class YoutubeDlViewModel {
    public Observable<String> url = new Observable<>(null);
    public Observable<DownloadInfo> infoForUrl = new Observable<>(null);

    public void notifyObservers() {
        url.notifyObservers();
        infoForUrl.notifyObservers();
    }
}
