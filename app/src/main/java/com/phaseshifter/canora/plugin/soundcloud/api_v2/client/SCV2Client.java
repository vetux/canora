package com.phaseshifter.canora.plugin.soundcloud.api_v2.client;

import com.phaseshifter.canora.plugin.soundcloud.api.data.SCConstants;
import com.phaseshifter.canora.plugin.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCConnectionException;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.*;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.json.SCV2JsonParser;
import com.phaseshifter.canora.net.http.*;
import com.phaseshifter.canora.utils.LogKeeper;
import com.phaseshifter.canora.utils.Pair;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for the undocumented api-v2
 */
public class SCV2Client {
    private final String LOG_TAG = "V2";

    private final HttpClient client;
    private final LogKeeper logger;

    private String clientID;

    /**
     * @param clientID The client id to use for the communication with the soundcloud api.
     * @param logger   The LogKeeper instance to use for logging.
     * @param client   The HttpClient instance to use
     */
    public SCV2Client(String clientID, LogKeeper logger, HttpClient client) {
        this.clientID = clientID;
        this.logger = logger;
        this.client = client;
    }

    public SCV2Client(String clientID, LogKeeper logger) {
        this(clientID, logger, HttpClientBuilder.create().setLogger(logger).build());
    }

    public SCV2Client(String clientID) {
        this(clientID, new LogKeeper());
    }

    public SCV2Client(LogKeeper logger) {
        this(null, logger);
    }

    public SCV2Client() {
        this(null, new LogKeeper());
    }

    /**
     * Gathers available tracks matching a specific string from the api-v2 endpoint.
     * In order to be able to stream one of the tracks pass it to getTemporaryStreamUrl() to obtain a temporary streamable url.
     *
     * @param q             Term to search for
     * @param tracksPerPage Number of tracks per page
     * @param pageNumber    0 Equals the first page, 1 equals the second page and so on
     * @return A List of SCTrack objects returned from soundcloud or null if no results exist for the given pageNumber
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public List<SCV2Track> getTracksContainingString(String q, int tracksPerPage, int pageNumber) throws IOException, SCConnectionException, SCParsingException {
        logger.log(LOG_TAG, "Get tracks containing string: \"" + q + "\", Tracks per page: " + tracksPerPage + ", Pagenumber: " + pageNumber);
        int queryOffset = pageNumber + (tracksPerPage * pageNumber);
        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_CLIENTID, clientID));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_SEARCH, q));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_LIMIT, Integer.toString(tracksPerPage)));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_OFFSET, Integer.toString(queryOffset)));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_LINKEDPARTITIONING, "1"));
        HttpRequest request = new HttpRequest(HttpMethod.GET, SCV2Constants.URL_APIV2_BASE + SCV2Constants.SUFFIX_SEARCH + SCV2Constants.SUFFIX_TRACKS, parameters);
        HttpResponse response = client.doRequest(request);
        if (response.getStatusCode() == HttpStatusCode.NOT_FOUND.code) {
            response.close();
            return null;
        } else if (response.getStatusCode() == HttpStatusCode.OK.code) {
            String json = response.getBodyString();
            response.close();
            return new SCV2JsonParser().getTracks(json);
        } else {
            response.close();
            throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
        }
    }

    /**
     * Returns a SCV2Track object representing the track with the id trackID.
     *
     * @param trackID The id of the track to get the SCV2Track object of.
     * @return The SCV2Track representing the track with the trackID.
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public SCV2Track getTrackForID(int trackID) throws IOException, SCParsingException, SCConnectionException {
        logger.log(LOG_TAG, "Get Track for ID: " + trackID);
        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_CLIENTID, clientID));
        HttpRequest request = new HttpRequest(HttpMethod.GET, SCV2Constants.URL_APIV2_BASE + SCV2Constants.SUFFIX_TRACKS + "/" + trackID, parameters);
        String json;
        HttpResponse response = client.doRequest(request);
        if (response.getStatusCode() != HttpStatusCode.OK.code) {
            response.close();
            throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
        }
        json = response.getBodyString();
        response.close();
        return new SCV2JsonParser().getTrack(json);
    }

    /**
     * @param genre         The SCGenre to get the charts of
     * @param tracksPerPage The number of tracks returned per request.
     * @param pageNumber    0 Equals the first page, 1 equals the second page and so on
     * @return The SCV2Charts object representing the charts for the given genre or null if no results exist for the given pageNumber
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public SCV2Charts getCharts(SCGenre genre, int tracksPerPage, int pageNumber) throws IOException, SCParsingException, SCConnectionException {
        logger.log(LOG_TAG, "Get Charts: " + genre.parameterValue + " , Tracks per page: " + tracksPerPage + ", Pagenumber: " + pageNumber);
        int queryOffset = pageNumber + (tracksPerPage * pageNumber);
        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_CLIENTID, clientID));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_GENRE, genre.parameterValue));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_KIND, "top"));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_LIMIT, Integer.toString(tracksPerPage)));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_OFFSET, Integer.toString(queryOffset)));
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_LINKEDPARTITIONING, "1"));
        HttpRequest request = new HttpRequest(HttpMethod.GET, SCV2Constants.URL_APIV2_BASE + SCV2Constants.SUFFIX_CHARTS, parameters);
        HttpResponse response = client.doRequest(request);
        if (response.getStatusCode() == HttpStatusCode.NOT_FOUND.code) {
            response.close();
            return null;
        } else if (response.getStatusCode() == HttpStatusCode.OK.code) {
            String json = response.getBodyString();
            response.close();
            return new SCV2JsonParser().getCharts(json);
        } else {
            response.close();
            throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
        }
    }

    /**
     * Returns a list of SCV2TrackStreamData objects containing the temporarily streamable url and the corresponding protocol.
     *
     * @param codings The track to obtain the streamable urls of.
     * @return The SCV2TrackStreamData object containing the url, or null if not available.
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public List<SCV2TrackStreamData> getTemporaryStreamUrls(List<SCV2Track.MediaTranscoding> codings) throws SCParsingException, IOException, SCConnectionException, JSONException {
        logger.log(LOG_TAG, "Get temporary stream url: " + codings);
        if (codings == null)
            return null;

        List<SCV2TrackStreamData> ret = new ArrayList<>();
        for (SCV2Track.MediaTranscoding coding : codings) {
            String stagerUrl = null;
            SCV2StreamProtocol protocol = null;
            if (coding.getProtocol().equals("progressive")) {
                stagerUrl = coding.getUrl();
                protocol = SCV2StreamProtocol.PROGRESSIVE;
            } else if (coding.getProtocol().equals("hls")) {
                stagerUrl = coding.getUrl();
                protocol = SCV2StreamProtocol.HLS;
            }
            if (stagerUrl == null) {
                return null;
            }
            List<Pair<String, String>> parameters = new ArrayList<>();
            parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_CLIENTID, clientID));
            HttpRequest request = new HttpRequest(HttpMethod.GET, stagerUrl, parameters);
            HttpResponse response = client.doRequest(request);
            if (response.getStatusCode() != HttpStatusCode.OK.code) {
                response.close();
                throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
            }
            String json = response.getBodyString();
            response.close();
            String url = new SCV2JsonParser().getStreamUrlFromStager(json);
            ret.add(new SCV2TrackStreamData(url, protocol));
        }
        return ret;
    }

    /**
     * Returns a temporarily streamable url matching the specified protocol.
     *
     * @param track    The track to get a streamable url of.
     * @param protocol The protocol of the stream to be returned.
     * @return The SCV2TrackStreamData object containing the url, or null if not available.
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public SCV2TrackStreamData getTemporaryStreamUrl(SCV2Track track, SCV2StreamProtocol protocol) throws IOException, SCConnectionException, SCParsingException, JSONException {
        logger.log(LOG_TAG, "Get temporary stream url: " + track + " " + protocol);
        if (track.getCodings() == null)
            return null;
        String stagerUrl = null;
        for (SCV2Track.MediaTranscoding coding : track.getCodings()) {
            if (protocol == SCV2StreamProtocol.PROGRESSIVE && coding.getProtocol().equals("progressive")) {
                stagerUrl = coding.getUrl();
                break;
            } else if (protocol == SCV2StreamProtocol.HLS && coding.getProtocol().equals("hls")) {
                stagerUrl = coding.getUrl();
                break;
            }
        }
        if (stagerUrl == null) {
            return null;
        }
        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>(SCV2Constants.PARAMETER_GET_CLIENTID, clientID));
        HttpRequest request = new HttpRequest(HttpMethod.GET, stagerUrl, parameters);
        HttpResponse response = client.doRequest(request);
        if (response.getStatusCode() != HttpStatusCode.OK.code) {
            response.close();
            throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
        }
        String json = response.getBodyString();
        response.close();
        String url = new SCV2JsonParser().getStreamUrlFromStager(json);
        return new SCV2TrackStreamData(url, protocol);
    }

    /**
     * Obtains a new clientID by extracting the js sources and then checking each script for a clientID.
     * <p>
     * Ported to java from:
     * https://github.com/ytdl-org/youtube-dl/blob/3bed621750b7fe25afc04a0131664bbbc610c563/youtube_dl/extractor/soundcloud.py#L275
     *
     * @return The extracted client_id
     * @throws IOException           Thrown when the HTTP connection could not be made. Indicates a fatal local error.
     * @throws SCConnectionException Thrown when the request was rejected by the server. Indicates an invalid clientID or change in api.
     * @throws SCParsingException    Reports a fatal error in the parsing of the returned html / json. Indicates a change in the api.
     */
    public String getNewClientID() throws IOException, SCParsingException, SCConnectionException {
        logger.log(LOG_TAG, "Get new clientID");
        HttpRequest request = new HttpRequest(HttpMethod.GET, SCConstants.URL_BASE, null);
        HttpResponse response = client.doRequest(request);
        String text = response.getBodyString();
        response.close();
        List<String> jsUrls = new ArrayList<>();
        Matcher matcher = Pattern.compile("<script[^>]+src=\"([^\"]+)\"").matcher(text);
        while (matcher.find()) {
            jsUrls.add(0, matcher.group(1));
        }
        for (String jsUrl : jsUrls) {
            request = new HttpRequest(HttpMethod.GET, jsUrl, null);
            response = client.doRequest(request);
            if (response.getStatusCode() != HttpStatusCode.OK.code) {
                response.close();
                throw new SCConnectionException("Request to api-v2 endpoint rejected. Status Code: " + response.getStatusCode());
            }
            text = response.getBodyString();
            response.close();
            Matcher jsmatcher = Pattern.compile("client_id\\s*:\\s*\"([0-9a-zA-Z]{32})\"").matcher(text);
            if (jsmatcher.find()) {
                logger.log(LOG_TAG, "Obtained clientID: " + jsmatcher.group(1));
                return jsmatcher.group(1);
            }
        }
        throw new SCParsingException("Failed to get new Client ID");
    }

    /**
     * Free used resources by the http client.
     */
    public void shutdown() {
        client.shutdown();
    }

    public LogKeeper getLogger() {
        return logger;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }
}
