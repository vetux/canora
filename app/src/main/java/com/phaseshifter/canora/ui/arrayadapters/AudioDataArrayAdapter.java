package com.phaseshifter.canora.ui.arrayadapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.ui.utils.glide.GlideApp;
import com.phaseshifter.canora.ui.utils.glide.target.NonFocusStealingDrawableImageViewTarget;
import com.phaseshifter.canora.utils.Pair;
import com.phaseshifter.canora.utils.android.AttributeConversion;
import com.phaseshifter.canora.utils.android.Miscellaneous;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AudioDataArrayAdapter extends ArrayAdapter<AudioData> implements SectionIndexer {
    private final Context context;
    private final List<AudioData> contentRef;
    private final HashSet<Integer> selection;

    private final Drawable defaultArt;

    private final List<String> sectionChars = new ArrayList<>();
    private final List<Pair<String, Integer>> positionToSection = new ArrayList<>();
    private final List<Integer> sectionToPosition = new ArrayList<>();

    private Integer playingIndex = null;
    private Boolean isSelecting;

    private boolean isEnabled = true;

    public AudioDataArrayAdapter(Context context, List<AudioData> list) {
        super(context, 0, list);
        contentRef = list;
        this.context = context;
        isSelecting = false;
        selection = new HashSet<>();
        defaultArt = this.context.getDrawable(R.drawable.artwork_unset);
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

    public List<AudioData> getContentRef() {
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
        if ((listItem == null || listItem.findViewById(R.id.title) == null)) {
            listItem = LayoutInflater.from(context).inflate(R.layout.list_item_track, parent, false);
        }
        //Get relevant views
        TextView title = listItem.findViewById(R.id.title);
        TextView artist = listItem.findViewById(R.id.artist);
        TextView length = listItem.findViewById(R.id.length);
        CheckBox box = listItem.findViewById(R.id.checkbox);
        ImageView highlight = listItem.findViewById(R.id.highlight);
        ImageView cover = listItem.findViewById(R.id.cover);

        AudioData track = contentRef.get(position);

        //Set values
        title.setText(track.getMetadata().getTitle());
        artist.setText(track.getMetadata().getArtist());
        length.setText(Miscellaneous.digitize(track.getMetadata().getLength()));

        ImageData imageData = track.getMetadata().getArtwork();
        GlideApp.with(context).clear(cover);
        if (imageData != null) {
            GlideApp.with(context)
                    .setDefaultRequestOptions(RequestOptions
                            .diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .load(imageData)
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(defaultArt)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(new NonFocusStealingDrawableImageViewTarget(cover));
        } else {
            cover.setImageResource(R.drawable.artwork_unset);
        }

        if (isSelecting) {
            box.setVisibility(View.VISIBLE);
            box.setChecked(selection.contains(position));
        } else {
            box.setVisibility(View.GONE);
        }

        if (playingIndex != null && playingIndex.equals(position)) {
            //Highlight
            highlight.setVisibility(View.VISIBLE);
            highlight.setColorFilter(AttributeConversion.getColorForAtt(R.attr.colorSecondary, context)); //API 21 COMPAT
        } else {
            highlight.setVisibility(View.INVISIBLE);
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
            AudioData track = contentRef.get(0);
            String c = ("" + track.getMetadata().getTitle().charAt(0)).toUpperCase();
            positionToSection.add(new Pair<>(c, 0));
            sectionToPosition.add(0);
            sectionChars.add(c);
        }
        for (int i = 1; i < contentRef.size(); i++) {
            AudioData track = contentRef.get(i);
            String c = ("" + track.getMetadata().getTitle().charAt(0)).toUpperCase();
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