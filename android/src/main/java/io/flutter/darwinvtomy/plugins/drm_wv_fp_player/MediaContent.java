package io.flutter.darwinvtomy.plugins.drm_wv_fp_player;

import java.util.List;

public class MediaContent {

    final String name;
    final String uri;
    final String extension;
    final String drm_scheme;
    final String drm_license_url;
    final String ad_tag_uri;
    final List<String> playlist;
    final String spherical_stereo_mode;
    final List<String> subtitlesLink;


    public MediaContent(String name, String uri, String extension, String drm_scheme, String drm_license_url, String ad_tag_uri, List<String> playlist, String spherical_stereo_mode, List<String> subtitles) {
        this.name = name;
        this.uri = uri;
        this.extension = extension;
        this.drm_scheme = drm_scheme;
        this.drm_license_url = drm_license_url;
        this.ad_tag_uri = ad_tag_uri;
        this.playlist = playlist;
        this.spherical_stereo_mode = spherical_stereo_mode;
        this.subtitlesLink = subtitles;
    }
}
