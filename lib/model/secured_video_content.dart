import 'package:meta/meta.dart';

class MediaContent {
  final String name;
  final String uri;
  final String extension;
  final String drm_scheme;
  final String drm_license_url;
  final String ad_tag_uri;
  final List<String> playlist;
  final String spherical_stereo_mode;

  MediaContent(
      {@required this.name,
      this.uri,
      this.extension,
      this.drm_scheme,
      this.drm_license_url,
      this.ad_tag_uri,
      this.spherical_stereo_mode,
      this.playlist});

  @override
  String toString() {
    // TODO: implement toString
    return 'Secured Content Name :$name \n'
        'Video Link :$uri \n'
        'Playlist :${playlist == null ? null : playlist.toString()}';
  }
}
