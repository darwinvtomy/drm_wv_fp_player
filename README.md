
# drm_wv_fp_player (Beta Version)

An awesome  Flutter plugin for Playing Widevine and Fairplay(Incomplete still) DRM Contents For Android and IOS Phones

## Getting Started

This project is a starting point for a Flutter app for VOD based Apps
We have Integrated DRM for Flutter based VOD App 
The development is Premature stage and I need volunteers to work on the IOS counter part
We used Android's [Exoplayer](https://github.com/google/ExoPlayer),Framework to integrate in Native Android
And [video_player](https://github.com/flutter/plugins/tree/master/packages/video_player), in flutter
the following project is the combination of both Exoplayer and video_player (I don't have Mac-System to work on IOS part)
The project is still in beta stage. And I am trying to get all help i can get
I have completed 30% of the project.I will complete the remaining, once I get the time or resources

## Flutter Plugin
[plug-in package](https://flutter.io/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.


For help getting started with Flutter, view Google's 
[online documentation](https://flutter.io/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.




## Installation

First, add `drm_wv_fp_player` as a [dependency in your pubspec.yaml file](https://flutter.io/using-packages/).

### iOS

Warning: Still have to wait


### Android

Ensure the following permission is present in your Android Manifest file, located in `<project root>/android/app/src/main/AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

The Flutter project template adds it, so it may already be there.


### Supported Formats

- On Android, the backing player is [ExoPlayer](https://google.github.io/ExoPlayer/),
  please refer [here](https://google.github.io/ExoPlayer/supported-formats.html) for list of supported formats.

### DRM

* Widevine is currently supported
* HDCP Compilence is seems to be working
* HLS has some bugs. Will be solved soon
* Playready HSS is been solved a new patch will be committed after the testing 
* Fairplay I need a MAC book and Apple Account 
* DASH is supported with adaptive streaming
* MULTI-DRM Sample will be added once Fairplay is Fixed
* All Functionality of Exoplayer will be replicated. 
* Contents like CENC MULTI-DRM contents to support multiple platforms will be added later


### Example

```dart
import 'package:drm_wv_fp_player/drm_wv_fp_player.dart';
import 'package:drm_wv_fp_player/model/secured_video_content.dart';

void main() => runApp(VideoApp());

class VideoApp extends StatefulWidget {
  @override
  _VideoAppState createState() => _VideoAppState();
}

class _VideoAppState extends State<VideoApp> {
  VideoPlayerController _controller;

  @override
  void initState() {
    super.initState();
      _controller = VideoPlayerController.exoplayerMeidaFrameWork(MediaContent(
        name: "WV: Secure SD (cenc,MP4,H264)",//Can be null
        uri: 'https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd',//Google Test Content
        extension: null,//Pending Work
        drm_scheme: 'widevine',
        drm_license_url: 'https://proxy.uat.widevine.com/proxy?provider=widevine_test', //Google Test License
        ad_tag_uri: null,//Pending work
        spherical_stereo_mode: null, //Pending Work
        playlist: null, //Pending Work
      ))
        ..initialize().then((_) {
          // Ensure the first frame is shown after the video is initialized, even before the play button has been pressed.
          setState(() {});
        });
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
```


## Contributors

1. [DARWIN V TOMY](https://github.com/darwinvtomy), --Yep that's meee
2. Imiginary Friend
3. Motivatiors
4. And My Computer which keeps hanging while i work on Android Studio
5. Google Search


[LinkdIn](https://www.linkedin.com/in/darwin-v-tomy-15177711/), If any one wants Connect me. I am happy to help
[PayPal](https://paypal.me/darwinvtomy?locale.x=en_GB), In case If anyone wants to buy me a :coffee:
