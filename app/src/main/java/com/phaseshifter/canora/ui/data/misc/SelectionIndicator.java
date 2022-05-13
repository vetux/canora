package com.phaseshifter.canora.ui.data.misc;

import com.phaseshifter.canora.ui.data.AudioContentSelector;

import java.io.Serializable;
import java.util.UUID;

public class SelectionIndicator implements Serializable {
    private final AudioContentSelector selector;
    private final UUID uuid;

    public SelectionIndicator(AudioContentSelector selector, UUID uuid) {
        this.selector = selector;
        this.uuid = uuid;
    }

    public SelectionIndicator(SelectionIndicator copy) {
        this(copy.selector, copy.uuid);
    }

    public SelectionIndicator() {
        this(null, null);
    }

    public AudioContentSelector getSelector() {
        return selector;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isSubMenu() {
        return selector != AudioContentSelector.TRACKS && uuid == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SelectionIndicator that = (SelectionIndicator) obj;
        return this.selector == that.selector
                && this.uuid == that.uuid;
    }

    @Override
    public String toString() {
        return "SelectionIndicator{" +
                "selector=" + selector +
                ", uuid=" + uuid +
                '}';
    }
}