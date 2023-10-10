<div>


<div align="right">
  <img alt="CODE_LINES" src="https://img.shields.io/tokei/lines/github/vetux/canora" align="left">
  <img alt="VERSION" src="https://img.shields.io/github/v/release/vetux/canora?include_prereleases" align="left">
  <img alt="LAST_COMMIT" src="https://img.shields.io/github/last-commit/vetux/canora" align="right">
  <img alt="LICENSE" src="https://img.shields.io/github/license/vetux/canora" align="right">
  <img alt="API26+" src="https://img.shields.io/badge/API-26%2B-green.svg?style=flat" align="right">
</div>

<br>

<div align="center">
  <img src="https://github.com/xenotux/canora/blob/master/docs/mainIcon.png" alt="MainIcon" width="300" height="300"/>
</div>

# Canora

</div>

A lightweight fast and customizable music playback and streaming experience for Android.

## Features
- Themes
- Metadata Editor
- Download or stream videos from [1000+ sites](https://ytdl-org.github.io/youtube-dl/supportedsites.html) supported by youtube-dl
- Create Playlists with tracks from any source
- Explore SoundCloud and YouTube content

## Playlist Notes
Currently playlists are serialized using java serialization which causes the serialized data 
to often be incompatible with the app when updating. This will be replaced in the future with a stable custom file format.
Until then please don't become too attached to created playlists as they may disappear with future app updates.

## Credits
- yausername - [youtubedl-android](https://github.com/yausername/youtubedl-android)
- Google - [ExoPlayer](https://github.com/google/ExoPlayer)
- IJabz - [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src)
- bumptech - [glide](https://github.com/bumptech/glide)
- Square, Inc. - [okhttp3](https://square.github.io/okhttp/)