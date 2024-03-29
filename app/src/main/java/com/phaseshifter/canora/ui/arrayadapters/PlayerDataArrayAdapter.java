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
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.ui.utils.glide.GlideApp;
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

public class PlayerDataArrayAdapter extends ArrayAdapter<PlayerData> implements SectionIndexer {
    private final Context context;
    private final List<PlayerData> contentRef;
    private final HashSet<Integer> selection;

    private final Drawable defaultArt;

    private final List<String> sectionChars = new ArrayList<>();
    private final List<Pair<String, Integer>> positionToSection = new ArrayList<>();
    private final List<Integer> sectionToPosition = new ArrayList<>();

    private Integer playingIndex = null;
    private Boolean isSelecting;

    private boolean isEnabled = true;

    public static class ViewHolder {
        public final TextView title;
        public final TextView artist;
        public final TextView length;
        public final CheckBox box;
        public final ImageView highlight;
        public final ImageView cover;

        public ViewHolder(View view) {
            title = view.findViewById(R.id.title);
            artist = view.findViewById(R.id.artist);
            length = view.findViewById(R.id.length);
            box = view.findViewById(R.id.checkbox);
            highlight = view.findViewById(R.id.highlight);
            cover = view.findViewById(R.id.cover);
        }
    }

    public PlayerDataArrayAdapter(Context context, List<PlayerData> list) {
        super(context, 0, list);
        contentRef = list;
        this.context = context;
        isSelecting = false;
        selection = new HashSet<>();
        defaultArt = this.context.getDrawable(R.drawable.artwork_unset);
    }

    public void setSelectionMode(Boolean enable) {
        isSelecting = enable;
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

    public List<PlayerData> getContentRef() {
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
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_track, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else  {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PlayerData track = contentRef.get(position);

        //Set values
        viewHolder.title.setText(track.getMetadata().getTitle());
        viewHolder.artist.setText(track.getMetadata().getArtist());
        viewHolder.length.setText(Miscellaneous.digitize(track.getMetadata().getDuration()));

        ImageData imageData = track.getMetadata().getArtwork();
        GlideApp.with(context).clear(viewHolder.cover);
        if (imageData != null) {
            GlideApp.with(context)
                    .load(imageData)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .fallback(R.drawable.artwork_unset)
                    .error(R.drawable.artwork_unset)
                    .placeholder(R.drawable.artwork_unset)
                    .into(viewHolder.cover);
        } else {
            viewHolder.cover.setImageResource(R.drawable.artwork_unset);
        }

        if (isSelecting) {
            viewHolder.box.setVisibility(View.VISIBLE);
            viewHolder.box.setChecked(selection.contains(position));
        } else {
            viewHolder.box.setVisibility(View.GONE);
        }

        if (playingIndex != null && playingIndex.equals(position)) {
            //Highlight
            viewHolder.highlight.setVisibility(View.VISIBLE);
            viewHolder.highlight.setColorFilter(AttributeConversion.getColorForAtt(R.attr.colorSecondary, context)); //API 21 COMPAT
        } else {
            viewHolder.highlight.setVisibility(View.INVISIBLE);
        }

        return convertView;
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
            PlayerData track = contentRef.get(0);
            String c = ("" + track.getMetadata().getTitle().charAt(0)).toUpperCase();
            positionToSection.add(new Pair<>(c, 0));
            sectionToPosition.add(0);
            sectionChars.add(c);
        }
        for (int i = 1; i < contentRef.size(); i++) {
            PlayerData track = contentRef.get(i);
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
    public int getCount() {
        return contentRef.size();
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