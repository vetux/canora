package com.phaseshifter.canora.ui.arrayadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.theme.AppTheme;

import java.util.List;

public class ThemeStoreArrayAdapter extends ArrayAdapter<AppTheme> {
    private final Context C;
    private final List<AppTheme> contentRef;

    public ThemeStoreArrayAdapter(Context c, List<AppTheme> list) {
        super(c, 0, list);
        contentRef = list;
        C = c;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if ((listItem == null || listItem.findViewById(R.id.themeTitle) == null)) {
            listItem = LayoutInflater.from(C).inflate(R.layout.grid_item_theme_store, parent, false);
        }

        TextView themeTitle = listItem.findViewById(R.id.themeTitle);
        TextView themePrice = listItem.findViewById(R.id.themePrice);
        ImageView image = listItem.findViewById(R.id.themePreview);
        View bg = listItem.findViewById(R.id.themePreviewParent);

        themeTitle.setText(contentRef.get(position).displayNameResID);
        //themePrice.setText(contentRef.get(position).getPrice());
        image.setImageResource(contentRef.get(position).previewResID);

        return listItem;
    }
}