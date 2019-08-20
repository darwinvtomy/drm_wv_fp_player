import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'video_player.dart';
import 'model/media.dart';

Future<List<Media>> loadMediaFiles() async {
  String jsonString = await rootBundle.loadString('assets/media.exolist.json');
  print(Media.parseMediaLists(jsonString).toString());
  return Media.parseMediaLists(jsonString);
}

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final appTitle = 'Flutter Exoplayer (Beta)';

    return MaterialApp(
      title: appTitle,
      home: MyHomePage(title: appTitle),
    );
  }
}

class MyHomePage extends StatelessWidget {
  final String title;

  MyHomePage({Key key, this.title}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
      ),
      body: FutureBuilder<List<Media>>(
        future: loadMediaFiles(),
        builder: (context, snapshot) {
          if (snapshot.hasError) print(snapshot.error);
          print('SNAP SHOT DATA  ${snapshot.data.toString()}');
          return snapshot.hasData
              ? PhotosList(medias: snapshot.data)
              : Center(child: CircularProgressIndicator());
        },
      ),
    );
  }
}

class PhotosList extends StatelessWidget {
  final List<Media> medias;

  PhotosList({Key key, this.medias}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: medias.length,
      itemBuilder: (context, index) {
        return StuffInTiles(medias[index]);
      },
    );
  }
}

class StuffInTiles extends StatelessWidget {
  final Media myTile;
  BuildContext _context;

  StuffInTiles(this.myTile);

  @override
  Widget build(BuildContext context) {
    _context = context;
    return _buildTiles(myTile);
  }

  Widget _buildTiles(Media t) {
    return ExpansionTile(
      key: PageStorageKey<int>(3),
      title: Text(t.name),
      children: t.samples.map(_buildSubTiles).toList(),
    );
  }

  Widget _buildSubTiles(Sample t) {
    return ListTile(
        dense: true,
        enabled: true,
        isThreeLine: false,
        onLongPress: () => print("long press"),
        onTap: () async {
          await Navigator.push(
              _context,
              MaterialPageRoute(
                  builder: (_) => VideoApp(
                        sampleVideo: t,
                      )));
          SystemChrome.setEnabledSystemUIOverlays(SystemUiOverlay.values);
          SystemChrome.setPreferredOrientations([
            DeviceOrientation.portraitUp,
            DeviceOrientation.portraitDown,
          ]);
        },
        subtitle: Text(
          "Subtitle Description",
        ),
        selected: true,
        title: Text(t.name,
            style: TextStyle(fontSize: 18.0, color: Colors.black54)));
  }
}
//ChewieDemo(sampleVideo: t,)
