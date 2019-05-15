import 'package:flutter/material.dart';
import 'package:drm_wv_fp_player/drm_wv_fp_player.dart';
import 'package:drm_wv_fp_player/model/secured_video_content.dart';
import 'model/media.dart';
class VideoApp extends StatefulWidget {
  Sample sampleVideo;

  VideoApp({@required this.sampleVideo});

  @override
  _VideoAppState createState() => _VideoAppState();
}

class _VideoAppState extends State<VideoApp> {
  VideoPlayerController _controller;

  @override
  void initState() {
    super.initState();

    _controller = VideoPlayerController.exoplayerMeidaFrameWork(MediaContent(
      name: widget.sampleVideo.name,
      uri: widget.sampleVideo.uri,
      extension: widget.sampleVideo.extension,
      drm_scheme: widget.sampleVideo.drm_scheme,
      drm_license_url: widget.sampleVideo.drm_license_url,
      ad_tag_uri: widget.sampleVideo.ad_tag_uri,
      spherical_stereo_mode: widget.sampleVideo.spherical_stereo_mode,
      playlist: widget.sampleVideo.playlist,
    ))
      ..initialize().then((_) {
        // Ensure the first frame is shown after the video is initialized, even before the play button has been pressed.
        setState(() {});
      });

/*    _controller = VideoPlayerController.network(
        widget.sampleVideo.uri)
      ..initialize().then((_) {
        // Ensure the first frame is shown after the video is initialized, even before the play button has been pressed.
        setState(() {});
      });*/
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Video Demo',
      home: Scaffold(
        body: Center(
          child: _controller.value.initialized
              ? AspectRatio(
                  aspectRatio: _controller.value.aspectRatio,
                  child: VideoPlayer(_controller),
                )
              : Container(),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            setState(() {
              _controller.value.isPlaying
                  ? _controller.pause()
                  : _controller.play();
            });
          },
          child: Icon(
            _controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    _controller.dispose();
  }
}
