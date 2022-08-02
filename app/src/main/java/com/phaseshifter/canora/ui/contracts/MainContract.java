package com.phaseshifter.canora.ui.contracts;

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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

/**
 * As before the basic architecture is MVP.
 * The presenter receives a list of StateListeners which it binds to its store object.
 * <p>
 * The presenter controls the view actions such as showDialog,
 * and the ViewModels are containers for state for the view such as a string.
 * <p>
 * Note that the presenter does not care or know anything about the ViewModels, all it sees is a list of StateListeners.
 * The ViewModels can therefore do whatever they want, they can reference android context, use the repos etc.
 * <p>
 * The actual logic that controls the store is still the presenter, the viewmodels simply
 * serve as a abstraction around the display and conversion of the resulting state.
 * <p>
 * The Presenter store state is therefore separated from the display.
 * We can add and remove state as we please and it is the responsibility of the ViewModels to transform that state into a meaningful representation for the view.
 * <p>
 * With this architecture we gain the stability and testability of MVP,
 * with the flexibility and scalability of MVVM.
 */
public interface MainContract {
    interface View {
        void shutdown();

        void saveState(Serializable state);

        void setTheme(AppTheme theme);

        void setDebugDisplay(boolean debugDisplay);

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

        void showMessage(String title, String text);

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
    }
}