package com.phaseshifter.canora.plugin.youtubeapi;

import com.google.common.primitives.UnsignedInteger;

public class YoutubeRequest {
    public String key;
    public String searchText = null;
    public String pageToken= null;
    public String channelId= null;
    public String regionCode = null;
    public UnsignedInteger maxResults= null;
    public YoutubeOrder order= null;
    public YoutubeResource type= null;
    public YoutubeVideoDuration videoDuration= null;

    public YoutubeRequest(String key) {
        this.key = key;
    }
}
