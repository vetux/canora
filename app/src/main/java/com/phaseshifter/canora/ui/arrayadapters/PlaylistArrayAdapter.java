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

public class PlaylistArrayAdapter extends ArrayAdapter<AudioPlaylist> implements SectionIndexer {
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

    public static class ViewHolder {
        public final TextView subTitle;
        public final TextView subCount;
        public final CustomImageView plImg;
        public final CheckBox box;
        public final ConstraintLayout bg;

        public ViewHolder(View view) {
            subTitle = view.findViewById(R.id.subMenuTitle);
            subCount = view.findViewById(R.id.subMenuTracks);
            plImg = view.findViewById(R.id.playlistImage);
            box = view.findViewById(R.id.checkbox);
            bg = view.findViewById(R.id.textBackground);
        }
    }

    public PlaylistArrayAdapter(Context c, List<AudioPlaylist> cont) {
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
        ViewHolder viewHolder;
        if (convertView == null){
            convertView = LayoutInflater.from(C).inflate(R.layout.grid_item_playlist, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        AudioPlaylist playlist = contentRef.get(position);
        viewHolder.subTitle.setText(playlist.getMetadata().getTitle());
        viewHolder.subCount.setText(C.getString(R.string.arrayadapter_audioplaylist_header0numberOfTracks, playlist.getData().size()));
        if (isSelecting) {
            viewHolder.box.setVisibility(View.VISIBLE);
            viewHolder.box.setChecked(selection.contains(position));
        } else {
            viewHolder.box.setVisibility(View.GONE);
        }

        ImageData imageData = playlist.getMetadata().getArtwork();
        GlideApp.with(C).clear(viewHolder.plImg);
        if (imageData != null) {
            GlideApp.with(C)
                    .setDefaultRequestOptions(RequestOptions
                            .diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .load(imageData)
                    .placeholder(defaultArt)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(viewHolder.plImg);
        } else {
            viewHolder.plImg.setImageResource(R.drawable.artwork_unset);
        }

        if (playingIndex != null && playingIndex.equals(position)) {
            //Highlight
            viewHolder.plImg.setImageTintList(ColorStateList.valueOf(getColorForAtt(R.attr.colorSecondary_20, C)));
            viewHolder.plImg.setImageTintMode(PorterDuff.Mode.ADD);
        } else {
            viewHolder.plImg.setImageTintList(null);
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