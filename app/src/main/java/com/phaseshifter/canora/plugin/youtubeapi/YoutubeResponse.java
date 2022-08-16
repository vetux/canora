package com.phaseshifter.canora.plugin.youtubeapi;

import java.util.ArrayList;
import java.util.List;

public class YoutubeResponse {
    public String prevPage = null;
    public String nextPage = null;
    public List<YoutubePlaylist> playlists = new ArrayList<>();
    public List<YoutubeChannel> channels = new ArrayList<>();
    public List<YoutubeVideo> videos = new ArrayList<>();
}
