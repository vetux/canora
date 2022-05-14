package com.phaseshifter.canora.soundcloud.api.client;

import com.phaseshifter.canora.soundcloud.api.data.SCConstants;
import com.phaseshifter.canora.soundcloud.api.data.SCTrack;
import com.phaseshifter.canora.soundcloud.api.exceptions.SCException;
import com.phaseshifter.canora.soundcloud.api.json.SCJsonParser;
import com.phaseshifter.canora.net.http.*;
import com.phaseshifter.canora.soundcloud.util.LogKeeper;
import com.phaseshifter.canora.soundcloud.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.phaseshifter.canora.soundcloud.api.data.SCConstants.SUFFIX_TRACKS;
import static com.phaseshifter.canora.soundcloud.api.data.SCConstants.URL_API_BASE;

/**
 * Soundcloud Client interface
 */
public class SCClient {
    private final String LOG_TAG = "V1";

    private final String clientID;
    private final HttpClient client;
    private final LogKeeper logger;

    /**
     * @param clientID The client id to use for the communication with the soundcloud api.
     */
    public SCClient(String clientID) {
        this.clientID = clientID;
        logger = new LogKeeper();
        client = HttpClientBuilder.create().setLogger(logger).build();
    }

    /**
     * @param clientID The client id to use for the communication with the soundcloud api.
     * @param logger   The LogKeeper instance to use for logging.
     */
    public SCClient(String clientID, LogKeeper logger) {
        this.clientID = clientID;
        this.logger = logger;
        client = HttpClientBuilder.create().setLogger(logger).build();
    }

    /**
     * @param clientID The client id to use for the communication with the soundcloud api.
     * @param logSize  The maximum log size in bytes. -1 for unlimited size.
     */
    public SCClient(String clientID, int logSize) {
        this.clientID = clientID;
        this.logger = new LogKeeper(logSize);
        client = HttpClientBuilder.create().setLogger(logger).build();
    }

    /**
     * Queries the soundcloud database for track titles matching the supplied text.
     *
     * @param q             Term to search for
     * @param tracksPerPage Number of tracks per page
     * @param pageNumber    0 Equals the first page, 1 equals the second page and so on
     * @return A List of SCTrack objects returned from soundcloud.
     * @throws IOException When the HTTP Request could not be made.
     * @throws SCException When there was any unrecoverable internal error. (Indicates a change in the api or connectivity problems)
     */
    public List<SCTrack> getTracksContainingString(String q, int tracksPerPage, int pageNumber) throws IOException, SCException {
        logger.log(LOG_TAG, "Get tracks containing string: \"" + q + "\", Tracks per page: " + tracksPerPage + ", Pagenumber: " + pageNumber);
        int queryOffset = pageNumber + (tracksPerPage * pageNumber);

        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>(SCConstants.PARAMETER_GET_CLIENTID, clientID));
        parameters.add(new Pair<>(SCConstants.PARAMETER_GET_SEARCH, q));
        parameters.add(new Pair<>(SCConstants.PARAMETER_GET_LIMIT, Integer.toString(tracksPerPage)));
        parameters.add(new Pair<>(SCConstants.PARAMETER_GET_OFFSET, Integer.toString(queryOffset)));
        parameters.add(new Pair<>(SCConstants.PARAMETER_GET_LINKEDPARTITIONING, "1"));

        HttpRequest request = new HttpRequest(HttpMethod.GET, URL_API_BASE + SUFFIX_TRACKS, parameters);

        List<SCTrack> ret;
        HttpResponse response = client.doRequest(request);
        ret = new SCJsonParser().parseTracks(response.getBodyString());
        response.close();
        return ret;
    }

    public void shutdown() {
        client.shutdown();
    }

    public LogKeeper getLogger() {
        return logger;
    }
}
