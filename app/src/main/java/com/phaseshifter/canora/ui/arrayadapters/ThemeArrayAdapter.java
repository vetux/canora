package com.phaseshifter.canora.ui.arrayadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.theme.AppTheme;

import java.util.List;

public class ThemeArrayAdapter extends ArrayAdapter<AppTheme> {
    private final Context C;
    private final List<AppTheme> contentRef;
    private Integer highlightID = null;

    public ThemeArrayAdapter(Context c, List<AppTheme> list) {
        super(c, 0, list);
        contentRef = list;
        C = c;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void setHighlightedItem(int ID) {
        highlightID = ID;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if ((listItem == null || listItem.findViewById(R.id.themeTitle) == null)) {
            listItem = LayoutInflater.from(C).inflate(R.layout.grid_item_theme, parent, false);
        }

        TextView themeTitle = listItem.findViewById(R.id.themeTitle);
        ImageView image = listItem.findViewById(R.id.themePreview);
        RelativeLayout bg = listItem.findViewById(R.id.themePreviewParent);

        themeTitle.setText(contentRef.get(position).displayNameResID);
        image.setImageResource(contentRef.get(position).previewResID);

        if (highlightID != null && contentRef.get(position).id == highlightID) {
            bg.setBackground(getContext().getDrawable(R.drawable.themepreview_background_highlight));
        } else {
            bg.setBackground(getContext().getDrawable(R.drawable.themepreview_background_normal));
        }

        return listItem;
    }

    public List<AppTheme> getContentRef() {
        return contentRef;
    }
}