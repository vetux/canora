package com.phaseshifter.canora.model.editor;

import android.app.RecoverableSecurityException;
import android.net.Uri;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.metadata.PlayerMetadata;

import java.io.File;
import java.io.IOException;

public interface AudioMetadataEditor {
    /**
     * Attempts to write the supplied metadata to the uri.
     * As "documented" in the android samples this function will throw an RecoverableSecurityException when there are permission issues on android Q and higher.
     * On success the function will return normally, when any errors occur the function throws corresponding exceptions.
     * Notes on Android Q:
     * From the thrown RecoverableSecurityException one can call exception.getUserAction().getActionIntent().getIntentSender()
     * to receive an IntentSender which can be fired to request permissions to the requested file.
     * <p>
     * There is 0 documentation on how and to what exactly the permission is granted when a app has fired the intent sender.
     * We will assume the granted permission is only temporarily granted to the firing Activity.
     * <p>
     * Thus the passed content resolver should be derived from the same Activity which handles the permission request.
     * <p>
     * One, possibly the only, advantage of using the contentResolver method is that the written metadata seems to be updated immediately in the mediastore without calling scanFile,
     * but again there is 0 documentation explaining this behaviour.
     *
     * @param target   The target uri to be edited.
     * @param metadata The updated metadata to be written.
     * @throws IOException                  If the function is unable to write the file.
     * @throws RecoverableSecurityException Only thrown in android Q, indicates an recoverable permissions issue as described above.
     */
    void writeMetadata(Uri target, PlayerMetadata metadata) throws IOException, RecoverableSecurityException;

    /**
     * Write the supplied metadata directly to the file.
     *
     * @param target   The target file to be edited.
     * @param metadata The updated metadata to be written.
     * @throws IOException If the function is unable to write the file.
     */
    void writeMetadata(File target, PlayerMetadata metadata) throws IOException;

    /**
     * Generates a mask which describes which fields are supported for editing.
     *
     * @param data The AudioData object to create a mask of
     * @return The generated mask for the supplied AudioData object. Non null
     */
    AudioMetadataMask getMask(PlayerData data);
}