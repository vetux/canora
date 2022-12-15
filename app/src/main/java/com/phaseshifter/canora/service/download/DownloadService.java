package com.phaseshifter.canora.service.download;

import android.net.Uri;

import com.phaseshifter.canora.utils.RunnableArg;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public interface DownloadService {
    class Download {
        enum Type {
            AUDIO,
            VIDEO
        }
        public Type type;
        public Uri outputUri;
        public String url;
        public String tempFile;
        public float progress;
        public float etaInSeconds;
        public String progressLine;
        public CountDownLatch latch;
    }

    void getDownloads(RunnableArg<HashSet<Download>> callback);

    void downloadAudio(Uri outputUri,
                       OutputStream outputStream,
                       String url,
                       Runnable completionCallback,
                       RunnableArg<Exception> exceptionCallback);

    void downloadVideo(Uri outputUri,
                       OutputStream outputStream,
                       String url,
                       Runnable completionCallback,
                       RunnableArg<Exception> exceptionCallback);
}
