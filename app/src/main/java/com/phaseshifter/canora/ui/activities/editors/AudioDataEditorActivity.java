package com.phaseshifter.canora.ui.activities.editors;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceFile;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceUri;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.editor.AudioMetadataMask;
import com.phaseshifter.canora.utils.android.Miscellaneous;

public class AudioDataEditorActivity extends Activity {
    public static class ActivityBundle {
        public final AppTheme theme;
        public final AudioData data;
        public final AudioMetadataMask mask;

        public ActivityBundle(AppTheme theme, AudioData data, AudioMetadataMask mask) {
            if (theme == null
                    || data == null
                    || mask == null)
                throw new IllegalArgumentException();
            this.theme = theme;
            this.data = data;
            this.mask = mask;
        }
    }

    //Statics
    public static final String BUNDLE_INPUT = "AudioDataEditorActivity_IN"; //ActivityBundle.class
    public static final String BUNDLE_OUTPUT = "AudioDataEditorActivity_OUT"; //AudioData.class

    public static final int RESULTCODE_EDIT = 0;
    public static final int RESULTCODE_DELETE = 1;
    public static final int RESULTCODE_ERROR = 2;
    public static final int RESULTCODE_CANCEL = 3;

    private final String LOG_TAG = "AudioEditor";

    private AudioData data;
    private AudioMetadataMask mask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainApplication application = (MainApplication) getApplication();
        Object bundleObject = application.getBundle(BUNDLE_INPUT);
        if (!(bundleObject instanceof ActivityBundle)) {
            Log.e(LOG_TAG, "Fatal Error: Received invalid Object Reference from bundle.");
            onFinishActivity(RESULTCODE_ERROR);
            return;
        }
        ActivityBundle bundle = (ActivityBundle) bundleObject;
        data = bundle.data;
        mask = bundle.mask;
        setTheme(bundle.theme.styleResID);
        setContentView(R.layout.activity_editor_audio);
        setupLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        Miscellaneous.toggleKeyboardView(this, findViewById(R.id.root), false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        onFinishActivity(RESULTCODE_CANCEL);
    }

    private void onFinishActivity(int resultCode) {
        MainApplication application = (MainApplication) getApplication();
        application.putBundle(BUNDLE_OUTPUT, getUpdatedData(data));
        setResult(resultCode);
        finish();
    }

    private AudioData getUpdatedData(AudioData data) {
        if (data != null) {
            EditText titleText = findViewById(R.id.title);
            EditText artistText = findViewById(R.id.artist);
            EditText albumText = findViewById(R.id.album);
            AudioMetadataMemory ret = new AudioMetadataMemory(data.getMetadata());
            ret.setTitle(titleText.getText().toString());
            ret.setArtist(artistText.getText().toString());
            ret.setAlbum(albumText.getText().toString());
            return new AudioData(ret, data.getDataSource());
        } else {
            return null;
        }
    }

    private void setupLayout() {
        //Get Views
        TextView toolbarTitle = findViewById(R.id.toolbar_textview_title);
        EditText titleText = findViewById(R.id.title);
        EditText artistText = findViewById(R.id.artist);
        EditText albumText = findViewById(R.id.album);
        EditText genreText = findViewById(R.id.genre);
        EditText fileText = findViewById(R.id.filepath);

        //Fill in data
        toolbarTitle.setText(getString(R.string.editor_audio_toolbar_title0edit, data.getMetadata().getTitle()));
        titleText.setText(data.getMetadata().getTitle());
        artistText.setText(data.getMetadata().getArtist());
        albumText.setText(data.getMetadata().getAlbum());
        StringBuilder sb = new StringBuilder();
        if (data.getMetadata().getGenres() != null) {
            for (String genre : data.getMetadata().getGenres()) {
                sb.append(genre).append(";");
            }
        }
        genreText.setText(sb.toString());

        if (data.getDataSource() instanceof AudioDataSourceUri) {
            fileText.setText(((AudioDataSourceUri) data.getDataSource()).getUri().toString());
        } else if (data.getDataSource() instanceof AudioDataSourceFile) {
            fileText.setText(((AudioDataSourceFile) data.getDataSource()).getFile().getAbsolutePath());
        } else {
            fileText.setText("UNSUPPORTED");
        }

        titleText.setEnabled(mask.titleEdit);
        artistText.setEnabled(mask.artistEdit);
        albumText.setEnabled(mask.albumEdit);
        genreText.setEnabled(mask.genreEdit);
        fileText.setEnabled(false);

        setListeners();
    }

    private void setListeners() {
        ImageButton toolbarNav = findViewById(R.id.toolbar_button_nav);
        ImageButton toolbarApply = findViewById(R.id.toolbar_button_apply);
        toolbarNav.setOnClickListener(v -> onBackPressed());
        toolbarApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinishActivity(RESULTCODE_EDIT);
            }
        });
    }
}