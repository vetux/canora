package com.phaseshifter.canora.ui.activities.editors;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.ImageMetadata;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceByteArray;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.data.theme.AppTheme;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AudioPlaylistEditorActivity extends Activity {
    public static class ActivityBundle {
        public final AppTheme theme;
        public final Playlist data;

        public ActivityBundle(AppTheme theme, Playlist data) {
            this.theme = theme;
            this.data = data;
        }
    }

    public static final String BUNDLE_INPUT = "PlaylistEditorActivity_IN";
    public static final String BUNDLE_OUTPUT = "PlaylistEditorActivity_OUT";

    public static final int RESULTCODE_EDIT = 0;
    public static final int RESULTCODE_DELETE = 1;
    public static final int RESULTCODE_ERROR = 2;
    public static final int RESULTCODE_CANCEL = 3;

    private static final int REQUESTCODE_PICK_IMAGE = 0;

    private final int MAXIMUM_SIZE_PLAYLISTARTWORK = 1_000_000; //In Bytes

    private final String LOG_TAG = "EditorPlaylist";

    private ActivityBundle bundle;

    private Playlist editingPlaylist;

    private byte[] selectedArtwork = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object bundleObject = ((MainApplication) getApplication()).getBundle(BUNDLE_INPUT);

        if (!(bundleObject instanceof ActivityBundle)) {
            onFinishActivity(RESULTCODE_ERROR);
            return;
        }

        bundle = (ActivityBundle) bundleObject;
        editingPlaylist = bundle.data;

        setTheme(bundle.theme.styleResID);
        setContentView(R.layout.activity_editor_playlist);
        setupLayout();
    }

    @Override
    public void onBackPressed() {
        onFinishActivity(RESULTCODE_CANCEL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUESTCODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    if (data != null && data.getData() != null) {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        if (inputStream != null) {
                            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                            compressBitmapToSelectedArtwork(selectedBitmap);
                            inputStream.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onFinishActivity(int resultCode) {
        MainApplication application = (MainApplication) getApplication();
        application.putBundle(BUNDLE_OUTPUT, getUpdatedPlaylist());
        setResult(resultCode);
        finish();
    }

    public Playlist getUpdatedPlaylist() {
        EditText tex = findViewById(R.id.titleedit);
        PlaylistMetadata metadata = editingPlaylist.getMetadata();
        metadata.setTitle(tex.getText().toString());
        if (selectedArtwork != null) {
            ImageDataSourceByteArray imageSource = new ImageDataSourceByteArray(selectedArtwork);
            metadata.setArtwork(new ImageData(new ImageMetadata(UUID.randomUUID(),  0, 0), imageSource));
        } else {
            metadata.setArtwork(null);
        }
        return new Playlist(metadata, editingPlaylist.getTracks());
    }

    private void setupLayout() {
        EditText tex = findViewById(R.id.titleedit);
        ImageView img = findViewById(R.id.playlistImageEditor);
        ImageButton removeButton = findViewById(R.id.buttonRemoveImage);
        ImageButton toolbarNav = findViewById(R.id.toolbar_button_nav);
        ImageButton toolbarApply = findViewById(R.id.toolbar_button_apply);
        TextView toolbarTitle = findViewById(R.id.toolbar_textview_title);

        tex.setText(editingPlaylist.getMetadata().getTitle());
        toolbarTitle.setText(getString(R.string.editor_playlist_toolbar_title0editPlaylist, editingPlaylist.getMetadata().getTitle()));

        img.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUESTCODE_PICK_IMAGE);
        });

        if (editingPlaylist.getMetadata().getArtwork() != null) {
            editingPlaylist.getMetadata().getArtwork().getDataSource().getBitmap(this, (bitmap) -> {
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                img.setImageBitmap(bitmap);
                            } else {
                                img.setImageResource(R.drawable.artwork_unset);
                            }
                        });
                    },
                    (exception) -> {
                        runOnUiThread(() -> {
                            img.setImageResource(R.drawable.artwork_unset);
                        });
                    });
        } else {
            img.setImageResource(R.drawable.artwork_unset);
        }

        removeButton.setOnClickListener((view) -> {
            selectedArtwork = null;
            img.setImageDrawable(getDrawable(R.drawable.artwork_unset));
        });

        toolbarNav.setOnClickListener(v -> onBackPressed());
        toolbarApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinishActivity(RESULTCODE_EDIT);
            }
        });
    }

    private void compressBitmapToSelectedArtwork(Bitmap bitmap) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.editor_playlist_dialog0processingimage));
        dialog.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                    if (dialog.isShowing()) {
                        byte[] compressedArtwork = bos.toByteArray();
                        if (compressedArtwork.length > MAXIMUM_SIZE_PLAYLISTARTWORK) {
                            Log.e(LOG_TAG, "IMAGE TOO LARGE: " + compressedArtwork.length + " BYTES");
                            runOnUiThread(() -> {
                                Toast.makeText(
                                        AudioPlaylistEditorActivity.this,
                                        getString(
                                                R.string.editor_playlist_toast_text0selectedImageTooLarge,
                                                MAXIMUM_SIZE_PLAYLISTARTWORK / 1000000),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        } else {
                            Log.v(LOG_TAG, "NEW ARTWORK SIZE: " + compressedArtwork.length + " BYTES");
                            runOnUiThread(() -> {
                                selectedArtwork = compressedArtwork;
                                ((ImageView) findViewById(R.id.playlistImageEditor)).setImageBitmap(bitmap);
                            });
                        }
                    }
                    bos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        selectedArtwork = null;
                    });
                }
                runOnUiThread(dialog::dismiss);
            }
        }.start();
    }
}