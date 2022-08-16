package com.phaseshifter.canora.model.repo;

import com.google.common.primitives.UnsignedInteger;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeApiClient;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeOrder;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeRequest;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeResource;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeResponse;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeVideo;
import com.phaseshifter.canora.utils.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class YoutubeSearchRepository {
    public Observable<List<AudioData>> results = new Observable<>(new ArrayList<>());

    public YoutubeSearchRepository(YoutubeApiClient client) {
        this.client = client;
    }

    public void setApiKey(String key) {
        syncWithPool();
        this.key = key;
    }

    public void loadNextPage() {
        if (searchText == null) {
            throw new IllegalStateException("Next page called while no search text was set");
        }
        syncWithPool();
        if (!(!results.get().isEmpty()
                && nextPage == null)) {
            runTaskOnPool(() -> {
                YoutubeRequest request = new YoutubeRequest(key);
                request.order = order;
                request.searchText = searchText;
                request.pageToken = nextPage;
                request.maxResults = UnsignedInteger.valueOf(25);
                request.type = YoutubeResource.YOUTUBE_VIDEO;
                try {
                    YoutubeResponse response = client.execute(request);
                    if (response != null) {
                        nextPage = response.nextPage;
                        for (YoutubeVideo video : response.videos) {
                            results.get().add(client.getAudioData(key, video));
                        }
                        results.notifyObservers();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void setSearchText(String text) {
        if (!Objects.equals(searchText, text)) {
            syncWithPool();
            this.searchText = text;
            this.pages.clear();
            this.results.get().clear();
            this.results.notifyObservers();
            nextPage = null;
        }
    }

    public void setOrder(YoutubeOrder order) {
        syncWithPool();
        this.order = order;
    }

    private final YoutubeApiClient client;

    private String key = null;

    private String searchText = null;
    private YoutubeOrder order = YoutubeOrder.YOUTUBE_RELEVANCE;

    String nextPage = null;
    private List<String> pages = new ArrayList<>();

    private transient ExecutorService pool = Executors.newSingleThreadExecutor();
    private transient Semaphore semaphore = new Semaphore(1);

    private void syncWithPool() {
        semaphore.acquireUninterruptibly();
        semaphore.release();
    }

    private void runTaskOnPool(Runnable task) {
        semaphore.acquireUninterruptibly();
        pool.submit(() -> {
            task.run();
            semaphore.release();
        });
    }
}
