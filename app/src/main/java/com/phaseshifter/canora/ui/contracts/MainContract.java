package com.phaseshifter.canora.ui.contracts;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.editor.AudioMetadataMask;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;
import com.phaseshifter.canora.ui.data.formatting.SortDef;
import com.phaseshifter.canora.ui.menu.AddToMenuListener;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.ui.utils.dialog.MainDialogFactory;

import java.io.Serializable;
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
        void setTheme(AppTheme theme);

        void setDebugDisplay(boolean debugDisplay);

        void setSearchMax(boolean searchMax);

        void setControlMax(boolean controlMax);

        void setNavigationMax(boolean navigationMax);

        void showTrackContent();

        void showPlaylistContent();

        void showTrackContentDetails(int index);

        void showPlaylistContentDetails(int index);

        void showMenuTrackContent(int index, ContextMenu menu);

        void showMenuPlaylistContent(int index, ContextMenu menu);

        void showMenuOptions(OptionsMenu menu);

        void showMenuAddSelectionToPlaylist(boolean showAddToNew, List<AudioPlaylist> existingPlaylists, AddToMenuListener listener);

        void showMessage(String title, String text);

        void showMessage_createdPlaylist(String playlistTitle, int playlistTracks);

        void showMessage_deletedPlaylist(String title);

        void showMessage_deletedPlaylists(int count);

        void showMessage_deletedTracks(int count);

        void showMessage_deletedTracksFrom(String playlistTitle, int count);

        void showMessage_addedTracks(String playlistTarget, int count);

        void showWarning(String text);

        void showError(String text);

        void showDialog_Exit();

        void showDialog_FilterOptions(FilterDef curDef, MainDialogFactory.FilterOptionsListener listener);

        void showDialog_SortOptions(SortDef curDef, MainDialogFactory.SortingOptionsListener listener);

        void showDialog_CreatePlaylist(List<AudioData> data, MainDialogFactory.PlaylistCreateListener listener);

        void showDialog_DeletePlaylists(List<AudioPlaylist> playlists, MainDialogFactory.DeletePlaylistsListener listener);

        void showDialog_DeleteTracksFromPlaylist(AudioPlaylist playlist, List<AudioData> tracks, MainDialogFactory.DeleteTracksFromPlaylistListener listener);

        void showDialog_error_permissions();

        void showDialog_volume(float currentValue);

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

        void onNavigationButtonClick();

        void onOptionsButtonClick();

        void onSearchButtonClick();

        void onVolumeButtonClick();

        void onBackPress();

        void onPermissionCheckResult(boolean permissionsGranted);

        void onPermissionRequestResult(boolean permissionsGranted);

        void onTrackContentClick(int index);

        void onTrackContentLongClick(int index);

        void onPlaylistContentClick(int index);

        void onPlaylistContentLongClick(int index);

        void onTrackContentScrollToBottom();

        void onMenuAction(OptionsMenu.Action action, OptionsMenu menu);

        void onMenuAction(int index, ContextMenu.Action action, ContextMenu menu);

        void onMediaStoreDataChange();

        void onEditorResult(AudioData data, boolean error, boolean canceled, boolean deleted);

        void onEditorResult(AudioPlaylist data, boolean error, boolean canceled, boolean deleted);

        void onTransportControlChange(boolean controlMax);

        void onSearchChange(boolean searching);

        void onNavigationClick(NavigationItem item);

        Serializable saveState();
    }
}