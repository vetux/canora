package com.phaseshifter.canora.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.utils.RunnableArg;

import java.util.ArrayList;
import java.util.List;

public class MainDialogFactory {
    public static Dialog getPlaylistCreate(Activity host,
                                           List<AudioData> data,
                                           RunnableArg<String> onCreate,
                                           Runnable onCancel) {
        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_playlist_create);
        if (ret.getWindow() == null)
            throw new NullPointerException("DIALOG WINDOW IS NULL");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView title = ret.findViewById(R.id.title);
        title.setText(R.string.main_dialog_createpl_title0createPlaylist);

        EditText playlistNameInput = ret.findViewById(R.id.plname);
        ret.findViewById(R.id.btnPos).setOnClickListener(v -> {
            ret.dismiss();
            onCreate.run(playlistNameInput.getText().toString());
        });
        ret.findViewById(R.id.btnNeg).setOnClickListener(v -> {
            ret.dismiss();
            onCancel.run();
        });
        ret.setOnShowListener(dialog -> playlistNameInput.requestFocus());
        return ret;
    }

    public static Dialog getPlaylistsDelete(Activity host,
                                            List<AudioPlaylist> playlists,
                                            Runnable onAccept,
                                            Runnable onCancel) {
        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_simple_yn);
        if (ret.getWindow() == null)
            throw new NullPointerException("DIALOG WINDOW IS NULL");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView titleView = ret.findViewById(R.id.title);
        titleView.setText(R.string.main_dialog_deletepl_title0deletePlaylists);

        TextView textView = ret.findViewById(R.id.text);
        textView.setText(host.getString(R.string.main_dialog_deletepl_text0deleteConfirmation, playlists.size()));

        ret.findViewById(R.id.btnPos).setOnClickListener(v -> {
            ret.dismiss();
            onAccept.run();
        });
        ret.findViewById(R.id.btnNeg).setOnClickListener(v -> {
            ret.dismiss();
            onCancel.run();
        });
        return ret;
    }

    public static Dialog getTracksDeleteFromPlaylist(Activity host,
                                                     AudioPlaylist playlist,
                                                     List<AudioData> tracks,
                                                     Runnable onAccept,
                                                     Runnable onCancel) {
        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_simple_yn);
        if (ret.getWindow() == null)
            throw new NullPointerException("DIALOG WINDOW IS NULL");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView titleView = ret.findViewById(R.id.title);
        titleView.setText(R.string.main_dialog_delfpl_title0deleteFilesFromPlaylist);

        TextView textView = ret.findViewById(R.id.text);
        textView.setText(host.getString(R.string.main_dialog_delfpl_text0deleteFilesFromPlaylistConfirmation, tracks.size()));

        ret.findViewById(R.id.btnPos).setOnClickListener(v -> {
            ret.dismiss();
            onAccept.run();
        });
        ret.findViewById(R.id.btnNeg).setOnClickListener(v -> {
            ret.dismiss();
            onCancel.run();
        });
        return ret;
    }

    public static Dialog getSortingOptions(Activity host,
                                           SortingOptions curDef,
                                           RunnableArg<SortingOptions> onAccept) {
        SortingOptions output = new SortingOptions(curDef);

        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_sortoptions);
        if (ret.getWindow() == null)
            throw new NullPointerException("Dialog Window is null");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Spinner sortBySpinner = ret.findViewById(R.id.spinnerSortby);

        List<String> sortByItems = new ArrayList<>();
        sortByItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0title));
        sortByItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0artist));

        ArrayAdapter<String> sortByAdapter = new ArrayAdapter<>(host, R.layout.spinner_item, sortByItems);
        sortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        output.sortby = SortingOptions.SORT_TITLE;
                        break;
                    case 1:
                        output.sortby = SortingOptions.SORT_ARTIST;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sortBySpinner.setAdapter(sortByAdapter);

        Spinner sortDirSpinner = ret.findViewById(R.id.spinnerSortdir);

        List<String> sortDirItems = new ArrayList<>();
        sortDirItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0downwards));
        sortDirItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0upwards));

        ArrayAdapter<String> sortDirAdapter = new ArrayAdapter<>(host, R.layout.spinner_item, sortDirItems);
        sortDirSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        output.sortdir = SortingOptions.SORT_DIR_DOWN;
                        break;
                    case 1:
                        output.sortdir = SortingOptions.SORT_DIR_UP;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sortDirSpinner.setAdapter(sortDirAdapter);

        Spinner sortTechSpinner = ret.findViewById(R.id.spinnerSorttech);

        List<String> sortTechItems = new ArrayList<>();
        sortTechItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0alphabetic));
        sortTechItems.add(host.getString(R.string.main_dialog_sortopt_spinner_item0numeric));

        ArrayAdapter<String> sortTechAdapter = new ArrayAdapter<>(host, R.layout.spinner_item, sortTechItems);
        sortTechSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        output.sorttech = SortingOptions.SORT_TECH_ALPHA;
                        break;
                    case 1:
                        output.sorttech = SortingOptions.SORT_TECH_NUM;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sortTechSpinner.setAdapter(sortTechAdapter);

        switch (curDef.sortby) {
            case SortingOptions.SORT_TITLE:
                sortBySpinner.setSelection(0);
                break;
            case SortingOptions.SORT_ARTIST:
                sortBySpinner.setSelection(1);
                break;
            default:
                throw new RuntimeException("Invalid Setting Value for SORT_BY");
        }
        switch (curDef.sortdir) {
            case SortingOptions.SORT_DIR_DOWN:
                sortDirSpinner.setSelection(0);
                break;
            case SortingOptions.SORT_DIR_UP:
                sortDirSpinner.setSelection(1);
                break;
            default:
                throw new RuntimeException("Invalid Setting Value for SORT_DIR");
        }
        switch (curDef.sorttech) {
            case SortingOptions.SORT_TECH_ALPHA:
                sortTechSpinner.setSelection(0);
                break;
            case SortingOptions.SORT_TECH_NUM:
                sortTechSpinner.setSelection(1);
                break;
            default:
                throw new RuntimeException("Invalid Setting Value for SORT_TECH");
        }

        ret.findViewById(R.id.okbtn).setOnClickListener(v -> {
            ret.dismiss();
            onAccept.run(output);
        });

        return ret;
    }

    public static Dialog getFilterOptions(Activity host,
                                          FilterOptions curDef,
                                          RunnableArg<FilterOptions> onAccept) {
        FilterOptions output = new FilterOptions(curDef);
        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_searchoptions);
        if (ret.getWindow() == null)
            throw new NullPointerException("Dialog Window is null");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Spinner searchBySpinner = ret.findViewById(R.id.spinnersearchby);
        List<String> sortTechItems = new ArrayList<>();
        sortTechItems.add(host.getString(R.string.main_dialog_filteropt_spinner_item0title));
        sortTechItems.add(host.getString(R.string.main_dialog_filteropt_spinner_item0artist));
        sortTechItems.add(host.getString(R.string.main_dialog_filteropt_spinner_item0titleArtist));
        sortTechItems.add(host.getString(R.string.main_dialog_filteropt_spinner_item0any));

        ArrayAdapter<String> sortTechAdapter = new ArrayAdapter<>(host, R.layout.spinner_item, sortTechItems);
        searchBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        output.filterBy = FilterOptions.FILTER_TITLE;
                        break;
                    case 1:
                        output.filterBy = FilterOptions.FILTER_ARTIST;
                        break;
                    case 2:
                        output.filterBy = FilterOptions.FILTER_TITLE_ARTIST;
                        break;
                    case 3:
                        output.filterBy = FilterOptions.FILTER_ANY;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        searchBySpinner.setAdapter(sortTechAdapter);

        switch (curDef.filterBy) {
            case FilterOptions.FILTER_TITLE:
                searchBySpinner.setSelection(0);
                break;
            case FilterOptions.FILTER_ARTIST:
                searchBySpinner.setSelection(1);
                break;
            case FilterOptions.FILTER_TITLE_ARTIST:
                searchBySpinner.setSelection(2);
                break;
            case FilterOptions.FILTER_ANY:
                searchBySpinner.setSelection(3);
                break;
            default:
                throw new RuntimeException("Setting Value for FILTER_BY is invalid.");
        }

        ret.findViewById(R.id.okbtn).setOnClickListener(v -> {
            ret.dismiss();
            onAccept.run(output);
        });

        return ret;
    }

    public static Dialog getVolumeSettings(Activity host, SeekBar.OnSeekBarChangeListener volumeListener, float initalValue) {
        Dialog ret = new Dialog(host);
        ret.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ret.setContentView(R.layout.dialog_volume);
        if (ret.getWindow() == null)
            throw new NullPointerException("Dialog Window is null");
        ret.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        SeekBar volBar = ret.findViewById(R.id.volBar);
        volBar.setOnSeekBarChangeListener(volumeListener);
        volBar.setProgress((int) (volBar.getMax() * initalValue));
        return ret;
    }
}