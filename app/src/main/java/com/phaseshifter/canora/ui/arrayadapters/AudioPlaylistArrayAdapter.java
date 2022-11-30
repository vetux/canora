package com.phaseshifter.canora.ui.arrayadapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.utils.glide.GlideApp;
import com.phaseshifter.canora.ui.utils.glide.target.NonFocusStealingDrawableImageViewTarget;
import com.phaseshifter.canora.ui.widgets.CustomImageView;
import com.phaseshifter.canora.utils.Pair;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.phaseshifter.canora.utils.android.AttributeConversion.getColorForAtt;

public class AudioPlaylistArrayAdapter extends ArrayAdapter<AudioPlaylist> implements SectionIndexer {
    private final Context C;
    private final List<AudioPlaylist> contentRef;
    private final HashSet<Integer> selection;

    private final Drawable defaultArt;

    private final List<String> sectionChars = new ArrayList<>();
    private final List<Pair<String, Integer>> positionToSection = new ArrayList<>();
    private final List<Integer> sectionToPosition = new ArrayList<>();

    private Integer playingIndex = null;
    private Boolean isSelecting;

    private boolean isEnabled = true;

    public AudioPlaylistArrayAdapter(Context c, List<AudioPlaylist> cont) {
        super(c, 0, cont);
        contentRef = cont;
        C = c;
        isSelecting = false;
        selection = new HashSet<>();
        defaultArt = C.getDrawable(R.drawable.artwork_unset);
    }

    public void setSelectionMode(Boolean enable) {
        isSelecting = enable;
        selection.clear();
    }

    public boolean getSelectionMode() {
        return isSelecting;
    }

    public void setSelectionIndex(HashSet<Integer> selection) {
        this.selection.clear();
        if (selection != null) {
            this.selection.addAll(selection);
        }
    }

    public HashSet<Integer> getSelection() {
        return selection;
    }

    public List<AudioPlaylist> getContentRef() {
        return contentRef;
    }

    public void setHighlightedIndex(Integer index) {
        this.playingIndex = index;
    }

    public Integer getHighlightedIndex() {
        return playingIndex;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        //Checks
        if (listItem == null || listItem.findViewById(R.id.subMenuTitle) == null)
            listItem = LayoutInflater.from(C).inflate(R.layout.grid_item_playlist, parent, false);
        //Get Relevant Views
        TextView subTitle = listItem.findViewById(R.id.subMenuTitle);
        TextView subCount = listItem.findViewById(R.id.subMenuTracks);
        CustomImageView plImg = listItem.findViewById(R.id.playlistImage);
        CheckBox box = listItem.findViewById(R.id.checkbox);
        ConstraintLayout bg = listItem.findViewById(R.id.textBackground);

        AudioPlaylist playlist = contentRef.get(position);
        //SetValues
        subTitle.setText(playlist.getMetadata().getTitle());
        subCount.setText(C.getString(R.string.arrayadapter_audioplaylist_header0numberOfTracks, playlist.getData().size()));
        if (isSelecting) {
            box.setVisibility(View.VISIBLE);
            box.setChecked(selection.contains(position));
        } else {
            box.setVisibility(View.GONE);
        }

        ImageData imageData = playlist.getMetadata().getArtwork();
        GlideApp.with(C).clear(plImg);
        if (imageData != null) {
            GlideApp.with(C)
                    .setDefaultRequestOptions(RequestOptions
                            .diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .load(imageData)
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(defaultArt)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(new NonFocusStealingDrawableImageViewTarget(plImg));
        } else {
            plImg.setImageResource(R.drawable.artwork_unset);
        }

        if (playingIndex != null && playingIndex.equals(position)) {
            //Highlight
            plImg.setImageTintList(ColorStateList.valueOf(getColorForAtt(R.attr.colorSecondary_20, C)));
            plImg.setImageTintMode(PorterDuff.Mode.ADD);
        } else {
            plImg.setImageTintList(null);
        }
        return listItem;
    }

    @Override
    public boolean isEnabled(int position) {
        return isEnabled;
    }

    @Override
    public void notifyDataSetChanged() {
        positionToSection.clear();
        sectionToPosition.clear();
        sectionChars.clear();
        if (contentRef.size() > 0) {
            AudioPlaylist playlist = contentRef.get(0);
            String title = playlist.getMetadata().getTitle();
            String c = "";
            if (title != null)
                c = ("" + title.charAt(0)).toUpperCase();
            positionToSection.add(new Pair<>(c, 0));
            sectionToPosition.add(0);
            sectionChars.add(c);
        }
        for (int i = 1; i < contentRef.size(); i++) {
            AudioPlaylist playlist = contentRef.get(i);
            String c = ("" + playlist.getMetadata().getTitle().charAt(0)).toUpperCase();
            int sectionIndex = positionToSection.get(positionToSection.size() - 1).second;
            if (sectionChars.contains(c)) {
                positionToSection.add(new Pair<>(c, sectionIndex));
            } else {
                sectionChars.add(c);
                positionToSection.add(new Pair<>(c, ++sectionIndex));
            }
            if (sectionToPosition.size() <= sectionIndex) {
                sectionToPosition.add(i);
            }
        }
        super.notifyDataSetChanged();
    }

    @Override
    public Object[] getSections() {
        return sectionChars.toArray();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return sectionToPosition.get(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        return positionToSection.get(position).second;
    }
}