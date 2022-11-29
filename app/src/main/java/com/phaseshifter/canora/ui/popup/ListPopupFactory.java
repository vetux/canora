package com.phaseshifter.canora.ui.popup;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListPopupWindow;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.utils.RunnableArg;
import com.phaseshifter.canora.utils.android.AttributeConversion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ListPopupFactory {
    public static ListPopupWindow getOptionsMenu(Context context,
                                                 View anchor,
                                                 int offsetX,
                                                 int offsetY,
                                                 HashSet<OptionsMenu.Action> actions,
                                                 RunnableArg<OptionsMenu.Action> onAction,
                                                 Runnable onCancel) {
        List<OptionsMenu.Action> actionMapping = new ArrayList<>();
        List<ListPopupItem> popupItems = new ArrayList<>();

        if (actions.contains(OptionsMenu.Action.OPEN_SETTINGS)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0settings)));
            actionMapping.add(OptionsMenu.Action.OPEN_SETTINGS);
        }
        if (actions.contains(OptionsMenu.Action.OPEN_SORTOPTIONS)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0sort)));
            actionMapping.add(OptionsMenu.Action.OPEN_SORTOPTIONS);
        }
        if (actions.contains(OptionsMenu.Action.OPEN_FILTEROPTIONS)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0filteroptions)));
            actionMapping.add(OptionsMenu.Action.OPEN_FILTEROPTIONS);
        }
        if (actions.contains(OptionsMenu.Action.EDIT_PLAYLIST)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0editplaylist)));
            actionMapping.add(OptionsMenu.Action.EDIT_PLAYLIST);
        }
        if (actions.contains(OptionsMenu.Action.DELETE)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0delete)));
            actionMapping.add(OptionsMenu.Action.DELETE);
        }
        if (actions.contains(OptionsMenu.Action.ADD_SELECTION)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0addTo)));
            actionMapping.add(OptionsMenu.Action.ADD_SELECTION);
        }
        if (actions.contains(OptionsMenu.Action.SELECT_ALL)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0selectAll)));
            actionMapping.add(OptionsMenu.Action.SELECT_ALL);
        }
        if (actions.contains(OptionsMenu.Action.DESELECT_ALL)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0deselectAll)));
            actionMapping.add(OptionsMenu.Action.DESELECT_ALL);
        }
        if (actions.contains(OptionsMenu.Action.SELECT_STOP)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0cancelSelect)));
            actionMapping.add(OptionsMenu.Action.SELECT_STOP);
        }
        if (actions.contains(OptionsMenu.Action.SELECT_START)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_options_item0select)));
            actionMapping.add(OptionsMenu.Action.SELECT_START);
        }

        int widthDP = 200;
        int marginDP = 10;

        int widthPixels = (int) convertDpToPixels(widthDP, context);
        int offsetx = -(widthPixels + (int) convertDpToPixels(marginDP, context));
        offsetx += offsetX;
        int offsety = (int) convertDpToPixels(marginDP, context);
        offsety += offsetY;

        ListPopupWindow popupWindow = new ListPopupWindow(context);
        popupWindow.setBackgroundDrawable(new ColorDrawable(AttributeConversion.getColorForAtt(R.attr.colorOverlay, context)));
        popupWindow.setAnchorView(anchor);
        popupWindow.setModal(true);
        popupWindow.setWidth(widthPixels);
        popupWindow.setHorizontalOffset(offsetx);
        popupWindow.setVerticalOffset(offsety);
        popupWindow.setAdapter(new ListPopupAdapter(context, popupItems));
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onAction.run(actionMapping.get(position));
                popupWindow.dismiss();
            }
        });
        popupWindow.setOnDismissListener(onCancel::run);
        return popupWindow;
    }

    public static ListPopupWindow getAddToPlaylistMenu(Context context,
                                                       View anchor,
                                                       View prompt,
                                                       int offsetX,
                                                       int offsetY,
                                                       int marginDP,
                                                       List<AudioPlaylist> playlists,
                                                       Runnable onAddToNew,
                                                       RunnableArg<AudioPlaylist> onAddToPlaylist) {
        List<ListPopupItem> popupItems = new ArrayList<>();

        popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_addto_item0newPlaylist)));

        for (AudioPlaylist playlist : playlists) {
            popupItems.add(new ListPopupItem(playlist.getMetadata().getTitle()));
        }

        int widthDP = 200;

        int widthPixels = (int) convertDpToPixels(widthDP, context);
        int combinedOffsetX = -(widthPixels + (int) convertDpToPixels(marginDP, context));
        combinedOffsetX = combinedOffsetX + offsetX;
        int combinedOffsetY = (int) convertDpToPixels(marginDP, context);
        combinedOffsetY = combinedOffsetY + offsetY;

        ListPopupWindow popupWindow = new ListPopupWindow(context);
        popupWindow.setPromptView(prompt);
        popupWindow.setBackgroundDrawable(new ColorDrawable(AttributeConversion.getColorForAtt(R.attr.colorOverlay, context)));
        popupWindow.setAnchorView(anchor);
        popupWindow.setModal(true);
        popupWindow.setWidth(widthPixels);
        popupWindow.setHorizontalOffset(combinedOffsetX);
        popupWindow.setVerticalOffset(combinedOffsetY);
        popupWindow.setAdapter(new ListPopupAdapter(context, popupItems));
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                assert (position >= 0);
                if (position == 0) {
                    onAddToNew.run();
                } else {
                    onAddToPlaylist.run(playlists.get(position - 1));
                }
                smoothHideListPopupWindowWithPrompt(context, parent, popupWindow);
            }
        });
        return popupWindow;
    }

    public static ListPopupWindow getContextMenu(Context context,
                                                 View anchor,
                                                 int offsetx,
                                                 int offsety,
                                                 HashSet<ContextMenu.Action> actions,
                                                 RunnableArg<ContextMenu.Action> onAction,
                                                 Runnable onCancel) {
        List<ContextMenu.Action> actionMapping = new ArrayList<>();
        List<ListPopupItem> popupItems = new ArrayList<>();

        if (actions.contains(ContextMenu.Action.SELECT)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_context_item0select)));
            actionMapping.add(ContextMenu.Action.SELECT);
        }
        if (actions.contains(ContextMenu.Action.EDIT)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_context_item0edit)));
            actionMapping.add(ContextMenu.Action.EDIT);
        }
        if (actions.contains(ContextMenu.Action.DELETE)) {
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_context_item0delete)));
            actionMapping.add(ContextMenu.Action.DELETE);
        }
        if (actions.contains(ContextMenu.Action.DOWNLOAD_AUDIO)){
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_context_item0downloadAudio)));
            actionMapping.add(ContextMenu.Action.DOWNLOAD_AUDIO);
        }
        if (actions.contains(ContextMenu.Action.DOWNLOAD_VIDEO)){
            popupItems.add(new ListPopupItem(context.getString(R.string.main_popup_context_item0downloadVideo)));
            actionMapping.add(ContextMenu.Action.DOWNLOAD_VIDEO);
        }

        int widthDP = 100;

        int widthPixels = (int) convertDpToPixels(widthDP, context);

        ListPopupWindow popupWindow = new ListPopupWindow(context);
        popupWindow.setBackgroundDrawable(new ColorDrawable(AttributeConversion.getColorForAtt(R.attr.colorOverlay, context)));
        popupWindow.setAnchorView(anchor);
        popupWindow.setModal(true);
        popupWindow.setWidth(widthPixels);
        popupWindow.setHorizontalOffset(offsetx);
        popupWindow.setVerticalOffset(offsety);
        popupWindow.setAdapter(new ListPopupAdapter(context, popupItems));
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onAction.run(actionMapping.get(position));
                popupWindow.dismiss();
            }
        });
        popupWindow.setOnDismissListener(onCancel::run);
        return popupWindow;
    }

    /**
     * When calling dismiss on a ListPopupWindow with a prompt set it first visibly removes the prompt and then the whole window.
     * As this looks unacceptable we use this function as a workaround to dismiss all ListPopupWindows containing a prompt.
     *
     * @param context The context used to instantiate the popup
     * @param parent  The AdapterView passed to onItemClick
     * @param popup   The popup to be dismissed
     */
    public static void smoothHideListPopupWindowWithPrompt(Context context, AdapterView<?> parent, ListPopupWindow popup) {
        try {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.popup_fadeout);
            View window = ((View) parent.getParent().getParent());
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    window.setVisibility(View.GONE);
                    popup.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            window.startAnimation(anim);
        } catch (Exception e) {
            e.printStackTrace();
            popup.dismiss();
        }
    }

    private static float convertDpToPixels(float dp, Context context) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}