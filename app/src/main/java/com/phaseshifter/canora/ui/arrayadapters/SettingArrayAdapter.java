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

import java.util.List;

public class SettingArrayAdapter extends ArrayAdapter<SettingAdapterItem> {
    private final Context C;
    private final List<SettingAdapterItem> contentRef;

    public SettingArrayAdapter(Context c, List<SettingAdapterItem> list) {
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
        if ((listItem == null || listItem.findViewById(R.id.settingTitle) == null)) {
            listItem = LayoutInflater.from(C).inflate(R.layout.list_item_setting, parent, false);
        }

        TextView titleText = listItem.findViewById(R.id.settingTitle);
        TextView subTitleText = listItem.findViewById(R.id.settingSubTitle);
        ImageView image = listItem.findViewById(R.id.settingIcon);

        titleText.setText(contentRef.get(position).getTitle());
        subTitleText.setText(contentRef.get(position).getSubTitle());
        image.setImageDrawable(contentRef.get(position).getIcon());

        return listItem;
    }
}