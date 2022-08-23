package com.phaseshifter.canora.ui.arrayadapters;

import static com.phaseshifter.canora.utils.android.AttributeConversion.getColorForAtt;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.data.DownloadProgress;
import com.phaseshifter.canora.ui.utils.glide.GlideApp;
import com.phaseshifter.canora.ui.utils.glide.target.NonFocusStealingDrawableImageViewTarget;
import com.phaseshifter.canora.ui.widgets.CustomImageView;
import com.phaseshifter.canora.utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DownloadProgressArrayAdapter extends ArrayAdapter<DownloadProgress> {
    private final Context C;
    private final List<DownloadProgress> contentRef;

    public DownloadProgressArrayAdapter(Context c, List<DownloadProgress> cont) {
        super(c, 0, cont);
        contentRef = cont;
        C = c;
    }

    public List<DownloadProgress> getContentRef() {
        return contentRef;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        //Checks
        if (listItem == null || listItem.findViewById(R.id.textView_downloaditem_url) == null)
            listItem = LayoutInflater.from(C).inflate(R.layout.list_item_youtubedl_download, parent, false);

        //Get Relevant Views
        TextView url = listItem.findViewById(R.id.textView_downloaditem_url);
        TextView eta = listItem.findViewById(R.id.textView_downloaditem_eta);
        ProgressBar pb = listItem.findViewById(R.id.progress_downloaditem);

        DownloadProgress progress = contentRef.get(position);
        url.setText(progress.outputFile);
        if (progress.etaInSeconds < 0) {
            eta.setText("-");
        } else {
            eta.setText(C.getString(R.string.main_downloaditem_eta, progress.etaInSeconds));
        }
        pb.setIndeterminate(false);
        pb.setMax(100);
        pb.setProgress((int) progress.progress);
        return listItem;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}
