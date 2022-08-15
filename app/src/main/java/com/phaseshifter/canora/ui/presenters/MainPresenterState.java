package com.phaseshifter.canora.ui.presenters;

import com.phaseshifter.canora.ui.data.DownloadInfo;
import com.phaseshifter.canora.ui.data.misc.ContentSelector;

import java.io.Serializable;

public class MainPresenterState implements Serializable {
    public ContentSelector uiIndicator;
    public ContentSelector contentIndicator;
    public String url;
    public DownloadInfo info;
    public boolean downloadingAudio;
    public boolean downloadingVideo;
}
