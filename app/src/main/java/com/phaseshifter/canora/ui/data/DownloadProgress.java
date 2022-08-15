package com.phaseshifter.canora.ui.data;

import java.util.UUID;

public class DownloadProgress {
    public final UUID uuid = UUID.randomUUID();
    public String url = null;
    public float progress = 0;
    public long etaInSeconds = 0;
}
