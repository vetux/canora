package com.phaseshifter.canora.ui.contracts;

import android.net.Uri;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.editor.AudioMetadataMask;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.utils.RunnableArg;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

/**
 * MVP Contract, Actions are methods and additionally there is MVVM style view-models for communicating simple state to the view
 */
public interface MainContract {
    interface View {
        void shutdown();

        void saveState(Serializable state);

        void setTheme(AppTheme theme);

        void showContentContextMenu(int index,
                                    HashSet<ContextMenu.Action> actions,
                                    RunnableArg<ContextMenu.Action> onAction,
                                    Runnable onCancel);

        void showOptionsMenu(HashSet<OptionsMenu.Action> actions,
                             RunnableArg<OptionsMenu.Action> onAction,
                             Runnable onCancel);

        void showAddSelectionMenu(List<AudioPlaylist> existingPlaylists,
                                  Runnable onAddToNew,
                                  RunnableArg<AudioPlaylist> onAddToPlaylist);

        void showMessage(String text);

        void showWarning(String text);

        void showError(String text);

        void showDialog_FilterOptions(FilterOptions curDef,
                                      RunnableArg<FilterOptions> onAccept);

        void showDialog_SortOptions(SortingOptions curDef,
                                    RunnableArg<SortingOptions> onAccept);

        void showDialog_CreatePlaylist(List<AudioData> tracks,
                                       RunnableArg<String> onCreate,
                                       Runnable onCancel);

        void showDialog_DeletePlaylists(List<AudioPlaylist> playlists,
                                        Runnable onAccept,
                                        Runnable onCancel);

        void showDialog_DeleteTracksFromPlaylist(AudioPlaylist playlist,
                                                 List<AudioData> tracks,
                                                 Runnable onAccept,
                                                 Runnable onCancel);

        void startEditor(AudioData data, AudioMetadataMask mask, AppTheme theme);

        void startEditor(AudioPlaylist data, AppTheme theme);

        void startSettings();

        void startInfo();

        void startRate();

        void checkPermissions();

        void requestPermissions();

        void handleSecurityException(SecurityException exception, Runnable onSuccess);

        void setTransportControlMax(boolean maxControls);

        void setNavigationMax(boolean maxNav);

        String getStringResource(int id);

        String getStringResource(int id, Object... formatArgs);

        void createDocument(String mime, String fileName);

        OutputStream openDocument(String uri) throws FileNotFoundException;

        void scanDocument(String uri, Runnable onScanComplete);
    }

    interface Presenter {
        void start();

        void stop();

        void onTrackSeekStart();

        void onTrackSeek(float p);

        void onTrackSeekStop();

        void onPrev();

        void onPlay();

        void onNext();

        void onShuffleSwitch();

        void onRepeatSwitch();

        void onVolumeSeek(float p);

        void onPresetSelected(int preset);

        void onSearchTextChange(String text);

        void onSearchTextEditingFinished();

        void onOptionsButtonClick();

        void onSearchButtonClick();

        void onFloatingAddToButtonClick();

        void onBackPress();

        void onPermissionCheckResult(boolean permissionsGranted);

        void onPermissionRequestResult(boolean permissionsGranted);

        void onTrackContentClick(int index);

        void onTrackContentLongClick(int index);

        void onPlaylistContentClick(int index);

        void onPlaylistContentLongClick(int index);

        void onTrackContentScrollToBottom();

        void onMediaStoreDataChange();

        void onEditorResult(AudioData data, boolean error, boolean canceled, boolean deleted);

        void onEditorResult(AudioPlaylist data, boolean error, boolean canceled, boolean deleted);

        void onTransportControlChange(boolean controlMax);

        void onNavigationClick(NavigationItem item);

        void onUrlTextChange(String text);

        void onCheckUrlClick();

        void onDownloadVideoClick();

        void onDownloadAudioClick();

        void onAddToPlaylistClick();

        void onDocumentCreated(String uri);
    }
}