package com.phaseshifter.canora.model.editor;

public class AudioMetadataMask {
    public final boolean titleEdit;
    public final boolean artistEdit;
    public final boolean albumEdit;
    public final boolean genreEdit;
    public final boolean artworkEdit;

    public AudioMetadataMask(boolean titleEdit, boolean artistEdit, boolean albumEdit, boolean genreEdit, boolean artworkEdit) {
        this.titleEdit = titleEdit;
        this.artistEdit = artistEdit;
        this.albumEdit = albumEdit;
        this.genreEdit = genreEdit;
        this.artworkEdit = artworkEdit;
    }

    public AudioMetadataMask() {
        this(false, false, false, false, false);
    }
}