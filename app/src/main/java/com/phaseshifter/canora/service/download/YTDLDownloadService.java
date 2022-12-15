package com.phaseshifter.canora.service.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.ui.activities.MainActivity;
import com.phaseshifter.canora.utils.RunnableArg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class YTDLDownloadService extends Service implements DownloadService {
    private static final String LOG_TAG = "DownloadService";

    private static final String NOTIFICATION_CHANNEL = "DownloadChannel";

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(4,
            4,
            0,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    private final YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
    private final HashSet<Download> downloads = new HashSet<>();
    private final Lock downloadsLock = new ReentrantLock();

    private final IBinder mBinder = new YTDLDownloadService.LocalBinder();

    private int notificationIdCounter = 0;
    private ArrayList<Integer> notificationIdCache = new ArrayList<Integer>();

    public class LocalBinder extends Binder {
        public YTDLDownloadService getService() {
            return YTDLDownloadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public YTDLDownloadService() {

    }

    public void getDownloads(RunnableArg<HashSet<Download>> callback) {
        downloadsLock.lock();
        HashSet<Download> ret = new HashSet<>(downloads);
        downloadsLock.unlock();
        callback.run(ret);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void downloadAudio(Uri outputUri,
                              OutputStream outputStream,
                              String url,
                              Runnable completionCallback,
                              RunnableArg<Exception> exceptionCallback) {
        int id = createNotificationID();

        pool.submit(() -> {
            Download download = new Download();
            download.type = Download.Type.AUDIO;
            download.latch = new CountDownLatch(1);
            download.outputUri = outputUri;
            download.url = url;

            new Handler(getMainLooper()).post(() -> {
                updateNotification(id, download);
            });

            downloadsLock.lock();

            // Wait for download with identical output uri
            for (Download d : downloads) {
                if (d.outputUri.equals(outputUri)) {
                    downloadsLock.unlock();
                    while (d.latch.getCount() != 0) {
                        try {
                            d.latch.await();
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }
                    downloadsLock.lock();
                }
            }

            downloads.add(download);
            downloadsLock.unlock();

            new Handler(getMainLooper()).post(() -> {
                updateNotification(id, download);
            });

            File youtubeDLDir = new File(getFilesDir(), "download_cache");
            String tempFile = new String(youtubeDLDir.getAbsolutePath() + getTempFile(".mp3"));

            new File(tempFile).delete();

            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
            request.addOption("-f", "mp4");
            request.addOption("--no-playlist");
            request.addOption("--extract-audio");
            request.addOption("--add-metadata");
            request.addOption("--embed-thumbnail");
            request.addOption("--audio-format", "mp3");
            request.addOption("--output", tempFile);
            try {
                ytdl.execute(request, (progress, etaInSeconds, line) -> {
                    if (progress > download.progress)
                        download.progress = progress;
                    download.etaInSeconds = etaInSeconds;
                    download.progressLine = line;
                    new Handler(getMainLooper()).post(() -> {
                        updateNotification(id, download);
                    });
                });
                new Handler(getMainLooper()).post(() -> {
                    updateNotification(id, download);
                });
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                fis.close();
                new Handler(getMainLooper()).post(() -> {
                    updateNotification(id, download);
                });
            } catch (Exception e) {
                exceptionCallback.run(e);

                download.latch.countDown();

                downloadsLock.lock();
                downloads.remove(download);
                downloadsLock.unlock();

                return;
            }

            downloadsLock.lock();
            downloads.remove(download);
            downloadsLock.unlock();

            new Handler(getMainLooper()).post(() -> {
                revokeUriPermission(outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                closeNotification(id);
                destroyNotificationId(id);
            });

            completionCallback.run();
        });
    }

    @Override
    public void downloadVideo(Uri outputUri,
                              OutputStream outputStream,
                              String url,
                              Runnable completionCallback,
                              RunnableArg<Exception> exceptionCallback) {
        int id = createNotificationID();

        pool.submit(() -> {
            Download download = new Download();
            download.type = Download.Type.VIDEO;
            download.latch = new CountDownLatch(1);
            download.outputUri = outputUri;
            download.url = url;

            new Handler(getMainLooper()).post(() -> {
                updateNotification(id, download);
            });

            downloadsLock.lock();

            // Wait for download with identical output uri
            for (Download d : downloads) {
                if (d.outputUri.equals(outputUri)) {
                    downloadsLock.unlock();
                    while (d.latch.getCount() != 0) {
                        try {
                            d.latch.await();
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }
                    downloadsLock.lock();
                }
            }

            downloads.add(download);
            downloadsLock.unlock();

            new Handler(getMainLooper()).post(() -> {
                updateNotification(id, download);
            });

            YoutubeDLRequest request = new YoutubeDLRequest(url);
            File youtubeDLDir = new File(getFilesDir(), "download_cache");
            String tempFile = new String(youtubeDLDir.getAbsolutePath() + getTempFile(".mp4"));

            new File(tempFile).delete();

            request.addOption("-o", tempFile);
            request.addOption("-f", "mp4");
            request.addOption("--no-playlist");

            try {
                ytdl.execute(request, (progress, etaInSeconds, line) -> {
                    if (progress > download.progress)
                        download.progress = progress;
                    download.etaInSeconds = etaInSeconds;
                    download.progressLine = line;
                    new Handler(getMainLooper()).post(() -> {
                        updateNotification(id, download);
                    });
                });
                new Handler(getMainLooper()).post(() -> {
                    updateNotification(id, download);
                });
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                fis.close();
                new Handler(getMainLooper()).post(() -> {
                    updateNotification(id, download);
                });
            } catch (Exception e) {
                exceptionCallback.run(e);

                download.latch.countDown();

                downloadsLock.lock();
                downloads.remove(download);
                downloadsLock.unlock();

                return;
            }

            download.latch.countDown();

            downloadsLock.lock();
            downloads.remove(download);
            downloadsLock.unlock();

            new Handler(getMainLooper()).post(() -> {
                revokeUriPermission(outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                closeNotification(id);
                destroyNotificationId(id);
            });

            completionCallback.run();
        });
    }

    private String getTempFile(String extension) {
        String ret = "/temp_" + UUID.randomUUID().toString();
        ret += extension;
        return ret;
    }

    private void updateNotification(int id, Download d) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.notification_smallicon)
                .setContentTitle(getString(R.string.service_download_title, d.url))
                .setContentText(d.progressLine)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setProgress(100, (int) d.progress, false)
                .setAutoCancel(true);

        String channelId = NOTIFICATION_CHANNEL;
        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Canora Downloads",
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
        builder.setChannelId(channelId);

        notificationManager.notify(id, builder.build());
    }

    private void closeNotification(int id) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.cancel(id);
    }

    private int createNotificationID() {
        if (notificationIdCache.isEmpty()) {
            if (notificationIdCounter == Integer.MAX_VALUE) {
                throw new ArithmeticException("No more notification ids available.");
            } else {
                return notificationIdCounter++;
            }
        } else {
            int ret = notificationIdCache.get(0);
            notificationIdCache.remove(0);
            return ret;
        }
    }

    private void destroyNotificationId(int id) {
        notificationIdCache.add(id);
    }
}