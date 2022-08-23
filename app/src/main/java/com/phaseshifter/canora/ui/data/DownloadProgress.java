package com.phaseshifter.canora.ui.data;

import java.util.UUID;

public class DownloadProgress {
    public final UUID uuid = UUID.randomUUID();
    public String url = null;
    public String outputFile = null;
    public float progress = 0;
    public long etaInSeconds = 0;

    public DownloadProgress(String url, String outputFile) {
        this.url = url;
        this.outputFile = outputFile;
    }
}
