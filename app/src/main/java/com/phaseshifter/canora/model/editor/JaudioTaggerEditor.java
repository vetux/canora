package com.phaseshifter.canora.model.editor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSourceUri;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.*;

public class JaudioTaggerEditor implements AudioMetadataEditor {
    private final String LOG_TAG = "JaudioTaggerEditor";
    private final ContentResolver contentResolver;
    private final File tempDir;

    public JaudioTaggerEditor(ContentResolver contentResolver, File tempDir) {
        this.contentResolver = contentResolver;
        this.tempDir = tempDir;
    }

    public JaudioTaggerEditor(Context context) {
        this(context.getContentResolver(), new File(context.getFilesDir().getAbsolutePath() + "/tmp"));
        if (!tempDir.exists())
            tempDir.mkdir();
    }

    @Override
    public void writeMetadata(Uri target, PlayerMetadata metadata) throws IOException {
        Log.v(LOG_TAG, "writeMetadata " + target + " " + metadata);
        final byte[] originalBytes = getBytesFromUri(target);

        final String mime = contentResolver.getType(target);
        if (mime == null || !mime.startsWith("audio/"))
            throw new IllegalArgumentException("Invalid URI: " + target + ", Mime Type: " + mime);

        String fileExtension = getFileExtensionForMimeType(mime);
        final byte[] modifiedBytes;
        try {
            File temporaryFile = new File(tempDir.getAbsolutePath() + "/original." + fileExtension);
            writeBytesToFile(originalBytes, temporaryFile);

            writeMetadata(temporaryFile, metadata);

            modifiedBytes = getBytesFromFile(temporaryFile);

            if (!temporaryFile.delete())
                throw new IOException("Failed to delete temporary File " + temporaryFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Fatal Error Processing File.");
        }
        OutputStream outputStream = contentResolver.openOutputStream(target);
        if (outputStream == null)
            throw new IOException("Failed opening output stream for " + target);
        outputStream.write(modifiedBytes);
        outputStream.close();
        notifyMediaStore(target, metadata);
    }

    @Override
    public void writeMetadata(File target, PlayerMetadata metadata) throws IOException {
        Log.v(LOG_TAG, "writeMetadata " + target + " " + metadata);
        try {
            AudioFile jaudioFile = AudioFileIO.read(target);

            Tag audioTag = jaudioFile.getTagOrCreateDefault();//This should throw an exception (library version 2.2.5) if the file is not supported. There is no "checkFileCompatibility" function.
            audioTag.setField(FieldKey.TITLE, metadata.getTitle());
            audioTag.setField(FieldKey.ARTIST, metadata.getArtist());
            audioTag.setField(FieldKey.ALBUM, metadata.getAlbum());

            jaudioFile.setTag(audioTag);
            AudioFileIO.write(jaudioFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed writing file " + target);
        }
    }

    @Override
    public AudioMetadataMask getMask(PlayerData data) {
        //Even though jaudiotagger uses various java.nio classes (Which supposedly should only work with API 26+) the writing / reading of the tag seems to work fine on API 21 in the emulator.
        if (data.getDataSource() instanceof PlayerDataSourceUri) {
            String mime = contentResolver.getType(((PlayerDataSourceUri) data.getDataSource()).getUri());
            if (mime == null)
                return new AudioMetadataMask();
            String extension = getFileExtensionForMimeType(mime);
            boolean found = false;
            for (SupportedFileFormat format : SupportedFileFormat.values()) {
                if (format.getFilesuffix().equals(extension)) {
                    found = true;
                    break;
                }
            }
            return new AudioMetadataMask(found, found, found, false, false);
        } else {
            return new AudioMetadataMask();
        }
    }

    private byte[] getBytesFromUri(Uri target) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(target);
        if (inputStream == null)
            throw new IOException("Failed opening input stream for " + target);
        byte[] ret = getBytesFromStream(inputStream);
        inputStream.close();
        return ret;
    }

    private byte[] getBytesFromFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] ret = getBytesFromStream(inputStream);
        inputStream.close();
        return ret;
    }

    private byte[] getBytesFromStream(InputStream stream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
        }
        byte[] ret = bos.toByteArray();
        stream.close();
        bos.close();
        return ret;
    }

    private void writeBytesToFile(byte[] data, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    private String getFileExtensionForMimeType(String mime) {
        switch (mime) {
            case "audio/mpeg":
                //As MIME types do not differentiate between MPEG versions we will assume mp3
                return "mp3";
            default:
                //Try to use mime type as file extension
                return mime.substring(mime.lastIndexOf("/") + 1);
        }
    }

    private void notifyMediaStore(Uri uri, PlayerMetadata metadata) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.TITLE, metadata.getTitle());
        values.put(MediaStore.Audio.Media.ARTIST, metadata.getArtist());
        values.put(MediaStore.Audio.Media.ALBUM, metadata.getAlbum());
        contentResolver.update(uri, values, null, null);
    }
}