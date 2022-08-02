package com.phaseshifter.canora.ui.popup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.phaseshifter.canora.R;

import java.util.List;

public class ListPopupAdapter extends BaseAdapter {
    private final Context context;
    private final List<ListPopupItem> items;

    public ListPopupAdapter(Context context, List<ListPopupItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ListPopupItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return items.get(i).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.popupitem, null);
        }

        TextView title = convertView.findViewById(R.id.title);
        ImageView arrow = convertView.findViewById(R.id.subArrow);
        ImageView icon = convertView.findViewById(R.id.icon);

        ListPopupItem item = items.get(position);

        title.setText(item.getTitle());

        if (item.isSubMenu()) {
            arrow.setVisibility(View.VISIBLE);
        } else {
            arrow.setVisibility(View.GONE);
        }

        if (item.getIcon() != null)
            icon.setImageDrawable(item.getIcon());

        return convertView;
    }
}