package com.phaseshifter.canora.service.player.playback;

import android.util.Log;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.model.comparison.AudioDataComparsion;

import java.util.*;

//TODO: Refactor playback controller
public class DefaultPlaybackController implements PlaybackController {
    private final String LOG_TAG = "PlaybackManager";

    private final int historySize = 999;
    private final int forwardHistorySize = 999;

    private final Stack<AudioData> history;
    private final Stack<AudioData> forwardHistory;
    private final List<AudioData> shuffleCache;
    private AudioData currentTrack;
    private int currentIndex;

    private boolean shuffle;
    private boolean repeat;
    private List<AudioData> content;

    private final Random rand = new Random();
    private int nextRandom = -1;

    public DefaultPlaybackController() {
        shuffle = false;
        repeat = false;
        history = new Stack<>();
        forwardHistory = new Stack<>();
        shuffleCache = new ArrayList<>();
        currentIndex = 0;
    }

    public AudioData setNext(UUID id) {
        Log.v(LOG_TAG, "setNext " + id);
        if (content == null || content.size() == 0) {
            Log.e(LOG_TAG, "ERROR: CONTENT EMPTY / INVALID");
            return null;
        }
        history.clear();
        forwardHistory.clear();
        currentIndex = getIndexOfID(id);
        currentTrack = content.get(currentIndex);
        shuffleCache.clear();
        shuffleCache.addAll(content);
        shuffleCache.remove(currentTrack);
        return currentTrack;
    }

    public AudioData getNext() {
        Log.v(LOG_TAG, "getNext");
        if (content == null || content.size() == 0) {
            Log.e(LOG_TAG, "ERROR: CONTENT EMPTY / INVALID");
            return null;
        }
        if (currentTrack != null)
            pushHistory(currentTrack);
        if (forwardHistory.size() > 0) {
            Log.v(LOG_TAG, "FORWARD HISTORY");
            currentTrack = forwardHistory.pop();
            currentIndex = getIndexOfID(currentTrack.getMetadata().getId());
            return currentTrack;
        } else if (shuffle) {
            Log.v(LOG_TAG, "SHUFFLE ON");
            if (!shuffleCache.isEmpty() && (nextRandom < 0 || nextRandom >= shuffleCache.size())) {
                    nextRandom = rand.nextInt(shuffleCache.size());
            }
            if (shuffleCache.isEmpty()) {
                // This path is reached if a playlist with only one track is playing with shuffle enabled
                if (content.isEmpty()){
                    throw new IllegalStateException("getNext called without set content.");
                }
                currentTrack = content.get(0);
            } else {
                currentTrack = shuffleCache.get(nextRandom);
                shuffleCache.remove(nextRandom);
            }
            currentIndex = getIndexOfID(currentTrack.getMetadata().getId());
            if (shuffleCache.size() <= 0) {
                shuffleCache.addAll(content);
                nextRandom = rand.nextInt(shuffleCache.size());
            }
            nextRandom = rand.nextInt(shuffleCache.size());
        } else {
            Log.v(LOG_TAG, "SHUFFLE OFF");
            if (content.size() > 1 && currentIndex < content.size() - 1) {
                currentIndex = currentIndex + 1;
            } else {
                currentIndex = 0;
            }
            currentTrack = content.get(currentIndex);
        }
        return currentTrack;
    }

    public AudioData peekNext() {
        Log.v(LOG_TAG, "getNext");
        if (content == null || content.size() == 0) {
            Log.e(LOG_TAG, "ERROR: CONTENT EMPTY / INVALID");
            return null;
        }
        if (forwardHistory.size() > 0) {
            Log.v(LOG_TAG, "FORWARD HISTORY");
            return forwardHistory.peek();
        } else if (shuffle) {
            Log.v(LOG_TAG, "SHUFFLE ON");
            if (nextRandom < 0) {
                nextRandom = rand.nextInt(shuffleCache.size());
            }
            return shuffleCache.get(nextRandom);
        } else {
            Log.v(LOG_TAG, "SHUFFLE OFF");
            int index = currentIndex;
            if (content.size() > 1 && currentIndex < content.size() - 1) {
                index = currentIndex + 1;
            } else {
                index = 0;
            }
            return content.get(index);
        }
    }

    public AudioData getPrev() {
        Log.v(LOG_TAG, "getPrev");
        if (content == null || content.size() == 0)
            return null;
        if (currentTrack != null) {
            pushForwardHistory(currentTrack);
        }
        if (history.size() > 0) {
            Log.v(LOG_TAG, "HISTORY");
            AudioData p = history.pop();
            for (int i = 0; i < content.size(); i++) {
                if (content.get(i).equals(p)) {
                    currentIndex = i;
                    currentTrack = content.get(i);
                    return content.get(i);
                }
            }
            currentIndex = 0;
            currentTrack = content.get(currentIndex);
            return content.get(currentIndex);
        } else {
            Log.v(LOG_TAG, "PREV");
            if (content.size() > 1 && currentIndex > 0)
                currentIndex--;
            else
                currentIndex = content.size() - 1;
            currentTrack = content.get(currentIndex);
            return content.get(currentIndex);
        }
    }

    public AudioData getCurrentTrack() {
        return currentTrack;
    }

    public boolean setContent(List<AudioData> c) {
        currentIndex = -1;
        boolean found = false;
        if (currentTrack != null) {
            for (int i = 0; i < c.size(); i++) {
                AudioData track = c.get(i);
                if (AudioDataComparsion.isEqual_exclude_UUID(currentTrack, track)) {
                    currentIndex = i;
                    currentTrack = track;
                    found = true;
                    break;
                }
            }
            if (!found)
                currentTrack = null;
        }
        content = new ArrayList<>(c);
        shuffleCache.clear();
        shuffleCache.addAll(content);
        if (currentTrack != null)
            shuffleCache.remove(currentTrack);
        return found;
    }

    public List<AudioData> getContent() {
        return content;
    }

    public void setRepeat(Boolean repeat) {
        this.repeat = repeat;
    }

    public Boolean getRepeat() {
        return repeat;
    }

    public void setShuffle(Boolean shuffle) {
        if (!this.shuffle && shuffle) {
            //Shuffle is getting enabled
            if (content != null) {
                shuffleCache.clear();
                shuffleCache.addAll(content);
                if (currentTrack != null)
                    shuffleCache.remove(currentTrack);
            }
        }
        this.shuffle = shuffle;
    }

    public Boolean getShuffle() {
        return shuffle;
    }

    private int getIndexOfID(UUID id) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).getMetadata().getId() == id)
                return i;
        }
        return 0;
    }

    private void pushHistory(final AudioData item) {
        history.push(item);
        if (history.size() > historySize) {
            history.setSize(historySize);
        }
    }

    private void pushForwardHistory(final AudioData item) {
        forwardHistory.push(item);
        if (forwardHistory.size() > forwardHistorySize) {
            forwardHistory.setSize(forwardHistorySize);
        }
    }
}