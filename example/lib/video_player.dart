import 'package:flutter/material.dart';
import 'package:drm_wv_fp_player/drm_wv_fp_player.dart';
import 'package:drm_wv_fp_player/model/secured_video_content.dart';
import 'package:flutter/services.dart';
import 'model/media.dart';

class VideoApp extends StatefulWidget {
  Sample sampleVideo;

  VideoApp({@required this.sampleVideo});

  @override
  _VideoAppState createState() => _VideoAppState();
}

class _VideoAppState extends State<VideoApp> {
  VideoPlayerController _controller;
  bool _controllerWasPlaying = false;

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
    SystemChrome.setEnabledSystemUIOverlays([]);
    SystemChrome.setPreferredOrientations(
        [DeviceOrientation.landscapeLeft, DeviceOrientation.landscapeRight]);
  }

  @override
  Widget build(BuildContext context) {

    Widget progressIndicator;
    if (_controller.value.initialized) {
      final int duration = _controller.value.duration.inMilliseconds;
      final int position = _controller.value.position.inMilliseconds;

      int maxBuffering = 0;
      for (DurationRange range in _controller.value.buffered) {
        final int end = range.end.inMilliseconds;
        if (end > maxBuffering) {
          maxBuffering = end;
        }
      }

      double val = position / duration;
      progressIndicator = Stack(
        fit: StackFit.passthrough,
        children: <Widget>[
          LinearProgressIndicator(
            value: maxBuffering / duration,
            valueColor: AlwaysStoppedAnimation<Color>(Colors.red),
            backgroundColor: Colors.grey,
          ),

          Slider(
            value: val,
            min: 0.0,
            max: 1.0,
            activeColor: Colors.grey,
            inactiveColor: Colors.white,
            onChanged: (double value) {
              setState(() {
                print(value);
                val = value;
                if (!_controller.value.initialized) {
                  return;
                }
                final Duration position = _controller.value.duration * value;
                _controller.seekTo(position);
              });
            },
            onChangeStart: (double value) {
              if (!_controller.value.initialized) {
                return;
              }
              _controllerWasPlaying = _controller.value.isPlaying;
              if (_controllerWasPlaying) {
                _controller.pause();
              }
            },

            onChangeEnd: (double value){
              if (_controllerWasPlaying) {
                _controller.play();
              }
            },
          ),

          /* LinearProgressIndicator(
            value: position / duration,
            valueColor: AlwaysStoppedAnimation<Color>(colors.playedColor),
            backgroundColor: Colors.transparent,
          ),*/
        ],
      );
    } else {
      progressIndicator = LinearProgressIndicator(
        value: null,
        valueColor: AlwaysStoppedAnimation<Color>(Colors.green),
        backgroundColor: Colors.red,
      );
    }
    final Widget paddedProgressIndicator = Padding(
      padding: EdgeInsets.all(10),
      child: progressIndicator,
    );
 /*   if (widget.allowScrubbing) {
      return _VideoScrubber(
        child: paddedProgressIndicator,
        controller: _controller,
      );
    } else {*/
//      return paddedProgressIndicator;

    return MaterialApp(
      title: 'Video Demo',
      home: Scaffold(
        body: Stack(children: [
          Center(
            child: _controller.value.initialized
                ? AspectRatio(
                    aspectRatio: _controller.value.aspectRatio,
                    child: VideoPlayer(_controller),
                  )
                : Container(),
          ),
          MediaVolumeSeekBar(_controller),
          paddedProgressIndicator
        ]),
        /*floatingActionButton: FloatingActionButton(
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
        ),*/
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    _controller.dispose();
  }
}
