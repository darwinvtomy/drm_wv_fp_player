package io.flutter.darwinvtomy.plugins.drm_wv_fp_player;

// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TrackNameProvider;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.TextureRegistry;

import static android.content.Context.MODE_PRIVATE;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

public class DrmWvFpPlayerPlugin implements MethodCallHandler {
    private static final String TAG = "VideoPlayerPlugin";
    private static FrameworkMediaDrm mediaDrm;
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private final static String PREF_NAME = "MOVIDONE_EXOPLAYER";
    private final static String OFFLINE_KEY_ID = "OFFLINE_KEY_ID";
    private static OfflineLicenseHelper<FrameworkMediaCrypto> mOfflineLicenseHelper;


    private static class VideoPlayer {

        private SimpleExoPlayer exoPlayer;

        private Surface surface;

        private final TextureRegistry.SurfaceTextureEntry textureEntry;

        private QueuingEventSink eventSink = new QueuingEventSink();

        private final EventChannel eventChannel;

        private boolean isInitialized = false;
        private DefaultTrackSelector trackSelector;


        VideoPlayer(
                Context context,
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                String dataSource,
                Result result) {
            this.eventChannel = eventChannel;
            this.textureEntry = textureEntry;

            trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

            Uri uri = Uri.parse(dataSource);

            DataSource.Factory dataSourceFactory;
            if (isFileOrAsset(uri)) {
                dataSourceFactory = new DefaultDataSourceFactory(context, "ExoPlayer");
            } else {
                dataSourceFactory =
                        new DefaultHttpDataSourceFactory(
                                "ExoPlayer",
                                null,
                                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                                true);
            }
            MediaSource mediaSource = buildMediaSource(uri, null, dataSourceFactory, context);
//            Log.e(TAG, "VideoPlayer: URI LINK "+uri.toString() );
            exoPlayer.prepare(mediaSource);

            setupVideoPlayer(eventChannel, textureEntry, result, context);
        }

        VideoPlayer(
                Context context,
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                MediaContent mediaContent,
                Result result) {
            this.eventChannel = eventChannel;
            this.textureEntry = textureEntry;
            DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
            Uri uri = Uri.parse(mediaContent.uri);
            //Add Custom DRM Management

            if (mediaContent.drm_scheme != null) {
                String drmLicenseUrl = mediaContent.drm_license_url;//WIDEVINE EXAMPLE
                String[] keyRequestPropertiesArray =
                        null;
                boolean multiSession = true;
                String errorStringId = "An unknown DRM error occurred";
                if (Util.SDK_INT < 18) {
                    errorStringId = "Protected content not supported on API levels below 18";
                } else {
                    try {
                        UUID drmSchemeUuid = Util.getDrmUuid("widevine");

                        if (drmSchemeUuid == null) {
                            errorStringId = "This device does not support the required DRM scheme";
                        } else {
                            drmSessionManager =
                                    buildDrmSessionManagerV18(
                                            drmSchemeUuid, drmLicenseUrl, keyRequestPropertiesArray, multiSession, uri,context);
                        }
                    } catch (UnsupportedDrmException | IOException | InterruptedException e) {
                        /*errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                                ? "This device does not support the required DRM scheme" : "An unknown DRM error occurred";*/
                    }
                }
                if (drmSessionManager == null) {
//                    Log.e(TAG, "VideoPlayer: DRM ERROR "+errorStringId );
                    return;
                }
            }

            DefaultRenderersFactory renderersFactory =
                    new DefaultRenderersFactory(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
            TrackSelection.Factory trackSelectionFactory;
            DefaultTrackSelector.Parameters trackSelectorParameters;
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);

            trackSelector = new DefaultTrackSelector(trackSelectionFactory);
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
            trackSelector.setParameters(trackSelectorParameters);
            exoPlayer =
                    ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector, drmSessionManager);
            // exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);


            DataSource.Factory dataSourceFactory;
            if (isFileOrAsset(uri)) {
                dataSourceFactory = new DefaultDataSourceFactory(context, "ExoPlayer");
            } else {
                dataSourceFactory =
                        new DefaultHttpDataSourceFactory(
                                "ExoPlayer",
                                null,
                                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                                true);
            }
            MediaSource[] mediaSources = new MediaSource[1 + mediaContent.subtitlesLink.size()];
            mediaSources[0] = buildMediaSource(uri, mediaContent.extension, dataSourceFactory, context);
            for (int i = 0; i < mediaContent.subtitlesLink.size(); i++) {
                Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                        null, Format.NO_VALUE, Format.NO_VALUE, "en", null, Format.OFFSET_SAMPLE_RELATIVE);
                Log.e("srt link is", mediaContent.subtitlesLink.get(i));
                Uri uriSubtitle = Uri.parse(mediaContent.subtitlesLink.get(i));
                MediaSource subtitleSource = (new SingleSampleMediaSource.Factory(dataSourceFactory)).createMediaSource(uriSubtitle, textFormat, C.TIME_UNSET);
                mediaSources[1 + i] = subtitleSource;
            }
            MediaSource mediaSource;
            mediaSource = mediaSources.length == 1 ? mediaSources[0] : new MergingMediaSource(mediaSources);
            exoPlayer.prepare(mediaSource);
            setupVideoPlayer(eventChannel, textureEntry, result, context);
        }

        private static boolean isFileOrAsset(Uri uri) {
            if (uri == null || uri.getScheme() == null) {
                return false;
            }
            String scheme = uri.getScheme();
            return scheme.equals("file") || scheme.equals("asset");
        }

        private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter, Context context) {
            return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null, context);
        }

        public DataSource.Factory buildDataSourceFactory(TransferListener listener, Context context) {
            DefaultDataSourceFactory upstreamFactory =
                    new DefaultDataSourceFactory(context, listener, buildHttpDataSourceFactory(listener, context));
            return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
        }

        /**
         * Returns a {@link HttpDataSource.Factory}.
         */
        public HttpDataSource.Factory buildHttpDataSourceFactory(
                TransferListener listener, Context context) {
            String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
            return new DefaultHttpDataSourceFactory(userAgent, listener);
        }

        private synchronized Cache getDownloadCache() {
            Cache downloadCache;
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor());
            return downloadCache;
        }

        private File getDownloadDirectory() {
            File downloadDirectory = new File("");
            return downloadDirectory;
        }


        private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
                DefaultDataSourceFactory upstreamFactory, Cache cache) {
            return new CacheDataSourceFactory(
                    cache,
                    upstreamFactory,
                    new FileDataSourceFactory(),
                    /* cacheWriteDataSinkFactory= */ null,
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                    /* eventListener= */ null);
        }


        private MediaSource buildMediaSource(
                Uri uri, String extension, DataSource.Factory mediaDataSourceFactory, Context context) {
            @C.ContentType int contenttype = Util.inferContentType(uri, extension);
//            Log.e(TAG, "buildMediaSource: CONTENT TYPE "+contenttype  );
            int type = Util.inferContentType(uri.getLastPathSegment());
//            Log.e(TAG, "buildMediaSource: THe  CONTENT TYPE "+type  );
            switch (contenttype) {
                case C.TYPE_SS:
                    return new SsMediaSource.Factory(
                            new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                            .createMediaSource(uri);
                case C.TYPE_DASH:
                    return new DashMediaSource.Factory(
                            new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
                            .createMediaSource(uri);
                case C.TYPE_HLS:
                    return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
                case C.TYPE_OTHER:
                    return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                            .setExtractorsFactory(new DefaultExtractorsFactory())
                            .createMediaSource(uri);
                default: {
                    throw new IllegalStateException("Unsupported type: " + type);
                }
            }
        }

        private void setupVideoPlayer(
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                Result result, Context context) {

            eventChannel.setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object o, EventChannel.EventSink sink) {
                            eventSink.setDelegate(sink);
                        }

                        @Override
                        public void onCancel(Object o) {
                            eventSink.setDelegate(null);
                        }
                    });

            surface = new Surface(textureEntry.surfaceTexture());

            SurfaceView view = new SurfaceView(context);
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            PlayerView player = new PlayerView(context);
            player.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            player.setPlayer(exoPlayer);
            player.getSubtitleView().setBackgroundColor(0xFFFFFFFF);
            exoPlayer.setVideoSurfaceHolder(view.getHolder());
            exoPlayer.setVideoSurface(surface);
//            exoPlayer.setVideoSurfaceView(view);
            setAudioAttributes(exoPlayer);

            exoPlayer.addListener(
                    new EventListener() {

                        @Override
                        public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
                            if (playbackState == Player.STATE_BUFFERING) {
                                sendBufferingUpdate();
                            } else if (playbackState == Player.STATE_READY) {
                                if (!isInitialized) {
                                    isInitialized = true;
                                    getDefaultAudioAndVideo(context);
//                                    sendInitialized(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

                                }
                            } else if (playbackState == Player.STATE_ENDED) {
                                Map<String, Object> event = new HashMap<>();
                                event.put("event", "completed");
                                eventSink.success(event);
                            }
                        }

                        @Override
                        public void onPlayerError(final ExoPlaybackException error) {
                            if (eventSink != null) {
                                eventSink.error("VideoError", "Video player had error " + error, null);
                            }
                        }
                    });

            Map<String, Object> reply = new HashMap<>();
            reply.put("textureId", textureEntry.id());
            result.success(reply);
        }

        private void getDefaultAudioAndVideo(Context context) {
            ArrayList<String> AudioNew = new ArrayList<>();
            ArrayList<String> ResolutionChange = new ArrayList<>();
            ArrayList<String> SubtitleNew = new ArrayList<>();
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                    if (trackGroups.length != 0) {
                        switch (exoPlayer.getRendererType(i)) {
                            case C.TRACK_TYPE_AUDIO:
                                ArrayList<String> unq = new ArrayList<>();
                                String name = "DEFAULT", code = "def";
                                for (int j = 0; j < trackGroups.length; j++) {
                                    TrackGroup group = trackGroups.get(j);
                                    if (group.length > 0) {
                                        for (int k = 0; k < group.length; k++) {
                                            com.google.android.exoplayer2.Format format = group.getFormat(k);
                                            Log.v("kushal audio", format.toString());
                                            TrackNameProvider trackNameProvider = new DefaultTrackNameProvider(context.getResources());
                                            trackNameProvider = Assertions.checkNotNull(trackNameProvider);
                                            Log.v("kushal audio", trackNameProvider.getTrackName(format) + ":" + format.language);
                                            unq.add(trackNameProvider.getTrackName(format) + ":" + format.language);

                                        }
                                    }
                                }
                                AudioNew = new ArrayList<String>(new LinkedHashSet<String>(unq));
                                if (AudioNew.size() > 0)
                                    trackSelector.setParameters(
                                            trackSelector.buildUponParameters()
                                                    .setPreferredAudioLanguage(AudioNew.get(0).split(":")[1]));
                                break;
                            case C.TRACK_TYPE_VIDEO:
                                ArrayList<Integer> unq1 = new ArrayList<>();
                                ArrayList<String> unqBitrate = new ArrayList<>();
                                for (int j = 0; j < trackGroups.length; j++) {
                                    TrackGroup group = trackGroups.get(j);
                                    if (group.length > 0) {
                                        for (int k = 0; k < group.length; k++) {
                                            com.google.android.exoplayer2.Format format = group.getFormat(k);
                                            unqBitrate.add(format.width + " X " + format.height + "p");
                                            unq1.add(format.height);
                                            Log.v("kushal video", format.width + " X " + format.height);
                                        }
                                    }
                                }
                                ResolutionChange = unqBitrate;
                                trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                                .setMaxVideoSize(Integer.parseInt(ResolutionChange.get(0).split(" X ")[0]), Integer.parseInt((ResolutionChange.get(0).split(" X ")[0]).replace("p", ""))));
                                break;
                            case C.TRACK_TYPE_TEXT:
                                ArrayList<String> vtt = new ArrayList<>();
                                String nameVTT = "DEFAULT", codeVTT = "def";
                                for (int j = 0; j < trackGroups.length; j++) {
                                    TrackGroup group = trackGroups.get(j);
                                    if (group.length > 0) {
                                        for (int k = 0; k < group.length; k++) {
                                            com.google.android.exoplayer2.Format format = group.getFormat(k);
                                            TrackNameProvider trackNameProvider = new DefaultTrackNameProvider(context.getResources());
                                            trackNameProvider = Assertions.checkNotNull(trackNameProvider);
                                            Log.v("kushal subtitle", trackNameProvider.getTrackName(format));
                                            vtt.add(trackNameProvider.getTrackName(format) + ":" + format.language);
                                            if (k == 0) {
                                                String part[] = trackNameProvider.getTrackName(format).split(",");
                                                name = part[0];
                                                code = format.language;
                                            }
                                        }
                                    }
                                }
                                SubtitleNew = new ArrayList<String>(new LinkedHashSet<String>(vtt));
                                if (SubtitleNew.size() > 0)
                                    trackSelector.setParameters(
                                            trackSelector.buildUponParameters()
                                                    .setPreferredTextLanguage(SubtitleNew.get(0).split(":")[1]));

                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            sendInitialized(AudioNew, ResolutionChange, SubtitleNew);
        }

        private void sendBufferingUpdate() {
            Map<String, Object> event = new HashMap<>();
            event.put("event", "bufferingUpdate");
            List<? extends Number> range = Arrays.asList(0, exoPlayer.getBufferedPosition());
            // iOS supports a list of buffered ranges, so here is a list with a single range.
            event.put("values", Collections.singletonList(range));
            eventSink.success(event);
        }

        @SuppressWarnings("deprecation")
        private static void setAudioAttributes(SimpleExoPlayer exoPlayer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                exoPlayer.setAudioAttributes(
                        new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).build());
            } else {
                exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);
            }
        }


        void pause() {
            exoPlayer.setPlayWhenReady(false);
        }


        void setSpeed(double speed) {
            PlaybackParameters param = new PlaybackParameters((float) speed);
            exoPlayer.setPlaybackParameters(param);
        }

        void setResolution(int width, int height) {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                            .setMaxVideoSize(width, height));
        }

        void setAudio(String code) {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                            .setPreferredAudioLanguage(code));
        }

        void play() {
            exoPlayer.setPlayWhenReady(true);
        }

        void setLooping(boolean value) {
            exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
        }

        void setVolume(double value) {
            float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
            exoPlayer.setVolume(bracketedValue);
        }

        void seekTo(int location) {
            exoPlayer.seekTo(location);
        }

        long getPosition() {
            return exoPlayer.getCurrentPosition();
        }

        @SuppressWarnings("SuspiciousNameCombination")
        private void sendInitialized(ArrayList<String> audios, ArrayList<String> resolutions, ArrayList<String> subtitles) {
            if (isInitialized) {
                Map<String, Object> event = new HashMap<>();
                event.put("event", "initialized");
                event.put("duration", exoPlayer.getDuration());

                if (exoPlayer.getVideoFormat() != null) {
                    Format videoFormat = exoPlayer.getVideoFormat();
                    int width = videoFormat.width;
                    int height = videoFormat.height;
                    int rotationDegrees = videoFormat.rotationDegrees;
                    // Switch the width/height if video was taken in portrait mode
                    if (rotationDegrees == 90 || rotationDegrees == 270) {
                        width = exoPlayer.getVideoFormat().height;
                        height = exoPlayer.getVideoFormat().width;
                    }
                    event.put("width", width);
                    event.put("height", height);
                }
                setEvent(audios, "audios", event);
                setEvent(resolutions, "resolutions", event);
                setEvent(subtitles, "subtitles", event);
                eventSink.success(event);
            }
        }

        private void setEvent(ArrayList<String> value, String type, Map<String, Object> event) {
            JSONArray array = new JSONArray();
            if (value.size() > 0) {
                for (int i = 0; i < value.size(); i++) {
                    array.put(value.get(i));
                }
            } else {
                array.put("NO_VALUE");
            }
            event.put(type, array.toString());
        }


        void dispose() {
            if (isInitialized) {
                exoPlayer.stop();
            }
            textureEntry.release();
            eventChannel.setStreamHandler(null);
            if (surface != null) {
                surface.release();
            }
            if (exoPlayer != null) {
                exoPlayer.release();
            }
        }
    }

    public static void registerWith(Registrar registrar) {
        final DrmWvFpPlayerPlugin plugin = new DrmWvFpPlayerPlugin(registrar);
        final MethodChannel channel =
                new MethodChannel(registrar.messenger(), "flutter.io/videoPlayer");
        channel.setMethodCallHandler(plugin);
        registrar.addViewDestroyListener(
                new PluginRegistry.ViewDestroyListener() {
                    @Override
                    public boolean onViewDestroy(FlutterNativeView view) {
                        plugin.onDestroy();
                        return false; // We are not interested in assuming ownership of the NativeView.
                    }
                });
    }

    private DrmWvFpPlayerPlugin(Registrar registrar) {
        this.registrar = registrar;
        this.videoPlayers = new LongSparseArray<>();
    }

    private final LongSparseArray<VideoPlayer> videoPlayers;

    private final Registrar registrar;

    private void disposeAllPlayers() {
        for (int i = 0; i < videoPlayers.size(); i++) {
            videoPlayers.valueAt(i).dispose();
        }
        videoPlayers.clear();
    }

    private void onDestroy() {
        // The whole FlutterView is being destroyed. Here we release resources acquired for all instances
        // of VideoPlayer. Once https://github.com/flutter/flutter/issues/19358 is resolved this may
        // be replaced with just asserting that videoPlayers.isEmpty().
        // https://github.com/flutter/flutter/issues/20989 tracks this.
        disposeAllPlayers();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        TextureRegistry textures = registrar.textures();
        if (textures == null) {
            result.error("no_activity", "video_player plugin requires a foreground activity", null);
            return;
        }
        switch (call.method) {
            case "init":
                disposeAllPlayers();
                break;
            case "create": {
                TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                EventChannel eventChannel =
                        new EventChannel(
                                registrar.messenger(), "flutter.io/videoPlayer/videoEvents" + handle.id());

                VideoPlayer player;
                if (call.argument("asset") != null) {
                    String assetLookupKey;
                    if (call.argument("package") != null) {
                        assetLookupKey =
                                registrar.lookupKeyForAsset(call.argument("asset"), call.argument("package"));
                    } else {
                        assetLookupKey = registrar.lookupKeyForAsset(call.argument("asset"));
                    }
                    player =
                            new VideoPlayer(
                                    registrar.context(),
                                    eventChannel,
                                    handle,
                                    "asset:///" + assetLookupKey,
                                    result);
                    videoPlayers.put(handle.id(), player);
                } else {
                    if (call.argument("sourcetype") != null) {

                        MediaContent mediaContent = new MediaContent(call.argument("name"),
                                call.argument("uri"),
                                call.argument("extension"),
                                call.argument("drm_scheme"),
                                call.argument("drm_license_url"),
                                call.argument("ad_tag_uri"),
                                null,
                                call.argument("spherical_stereo_mode"),
                                call.argument("subtitlesLink"));
                        player =
                                new VideoPlayer(
                                        registrar.context(), eventChannel, handle, mediaContent, result);
//                        Log.e("DATA_RETRIVAL", "_____________SOURCETYPE EXOMEDIA____________");
                    } else {
                        player =
                                new VideoPlayer(
                                        registrar.context(), eventChannel, handle,
                                        new MediaContent(null,
                                                call.argument("uri"),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                call.argument("subtitlesLink")), result);
                    }
                    videoPlayers.put(handle.id(), player);
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("name"));
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("drm_scheme"));
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("uri"));
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("sourcetype"));
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("extension"));
//                    Log.e("DATA_RETRIVAL", "onMethodCall: " + call.argument("drm_license_url"));
                }
                break;
            }
            default: {
                long textureId = ((Number) call.argument("textureId")).longValue();
                VideoPlayer player = videoPlayers.get(textureId);
                if (player == null) {
                    result.error(
                            "Unknown textureId",
                            "No video player associated with texture id " + textureId,
                            null);
                    return;
                }
                onMethodCall(call, result, textureId, player);
                break;
            }
        }
    }

    private void onMethodCall(MethodCall call, Result result, long textureId, VideoPlayer player) {
        switch (call.method) {
            case "setLooping":
                player.setLooping(call.argument("looping"));
                result.success(null);
                break;
            case "setVolume":
                player.setVolume(call.argument("volume"));
                result.success(null);
                break;
            case "play":
                player.play();
                result.success(null);
                break;
            case "pause":
                player.pause();
                result.success(null);
                break;
            case "seekTo":
                int location = ((Number) call.argument("location")).intValue();
                player.seekTo(location);
                result.success(null);
                break;
            case "position":
                result.success(player.getPosition());
                player.sendBufferingUpdate();
                break;
            case "dispose":
                player.dispose();
                videoPlayers.remove(textureId);
                result.success(null);
                break;
            case "speed":
                player.setSpeed(call.argument("speed"));
                result.success(null);
                break;
            case "resolution":
                player.setResolution(call.argument("width"), call.argument("height"));
                result.success(null);
                break;
            case "audio":
                player.setAudio(call.argument("code"));
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private static DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
            UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession, Uri uri,Context context)
            throws UnsupportedDrmException, IOException, InterruptedException {


        HttpDataSource.Factory licenseDataSourceFactory =
                new DefaultHttpDataSourceFactory("ExoPlayer");
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1]);
            }
        }
        releaseMediaDrm();
        mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        // Kushal
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = new DefaultDrmSessionManager<>(
                uuid, mediaDrm, drmCallback, null, multiSession);

        /*
        * offline support
        * */
        new Thread() {


            @Override
            public void run() {
                {
                    try {
                        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                                "ExoPlayer");
                         mOfflineLicenseHelper = OfflineLicenseHelper
                                .newWidevineInstance(licenseUrl, httpDataSourceFactory);
                        DataSource dataSource = httpDataSourceFactory.createDataSource();
                        DashManifest dashManifest = DashUtil.loadManifest(dataSource,
                                uri);
                        DrmInitData drmInitData = DashUtil.loadDrmInitData(dataSource, dashManifest.getPeriod(0));
                        byte[] offlineLicenseKeySetId = mOfflineLicenseHelper.downloadLicense(drmInitData);
                        storeKeySetId(offlineLicenseKeySetId,context);
                        Log.d("Sammy","Licence Download Successful: "+ offlineLicenseKeySetId);
                    } catch (Exception e) {
                        Log.e("Sammy", "license download failed", e);
                    }
                }
            }
        }.start();
        byte[] offlineKeySetId = getStoredKeySetId(context);
        if(offlineKeySetId != null || isLicenseValid(offlineKeySetId)) {
            drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineKeySetId);
        }
        return drmSessionManager;
    }

    private static void storeKeySetId(byte[] keySetId,Context context)
    {
        Log.d(TAG, "[LICENSE] Storing key set id value ... " + keySetId);

        if (keySetId != null)
        {
            SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String keySetIdB64 = Base64.encodeToString(keySetId, Base64.DEFAULT);

            // encode in b64 to be able to save byte array
            editor.putString(OFFLINE_KEY_ID, keySetIdB64);
            editor.apply();

            Log.d(TAG, "[LICENSE] Stored keySetId in B64 value :" + keySetIdB64);
        }
    }

    private static byte[] getStoredKeySetId(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String keySetIdB64 = sharedPreferences.getString(OFFLINE_KEY_ID, null);

        if (keySetIdB64 != null)
        {
            byte[] keysetId =  Base64.decode(keySetIdB64, Base64.DEFAULT);
            Log.d(TAG, "[LICENSE] Stored keySetId in B64 value :" + keySetIdB64);

            return keysetId;
        }

        return null;
    }

    /**
     * Check license validity
     * @param keySetId byte[]
     * @return boolean
     * */
    private static boolean isLicenseValid(byte[] keySetId)
    {
        if (mOfflineLicenseHelper != null && keySetId != null)
        {
            try
            {
                // get license duration
                Pair<Long, Long> licenseDurationRemainingSec = mOfflineLicenseHelper.getLicenseDurationRemainingSec(keySetId);
                long licenseDuration = licenseDurationRemainingSec.first;

                Log.d(TAG, "[LICENSE] Time remaining " + licenseDuration + " sec");
                return licenseDuration > 0;
            }
            catch (DrmSession.DrmSessionException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    private static void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }
}