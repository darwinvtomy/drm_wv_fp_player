import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:drm_wv_fp_player_example/PlayerPage.dart';
import 'model/media.dart';

Future<List<Media>> loadMediaFiles() async {
  String jsonString = await rootBundle.loadString('assets/media.exolist.json');
  return Media.parseMediaLists(jsonString);
}

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final appTitle = 'Flutter Exoplayer (Beta)';

    return MaterialApp(
      title: appTitle,
      debugShowCheckedModeBanner: false,
      home: MyHomePage(title: appTitle),
    );
  }
}

class MyHomePage extends StatefulWidget {
  final String title;

  MyHomePage({Key key, this.title}) : super(key: key);

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  Widget build(BuildContext context) {
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
    ]);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: FutureBuilder<List<Media>>(
        future: loadMediaFiles(),
        builder: (context, snapshot) {
          if (snapshot.hasError) print(snapshot.error);
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

class StuffInTiles extends StatefulWidget {
  final Media myTile;
  BuildContext _context;

  StuffInTiles(this.myTile);
  @override
  _StuffInTilesState createState() => _StuffInTilesState();
}

class _StuffInTilesState extends State<StuffInTiles> {
  @override
  Widget build(BuildContext context) {
    widget._context = context;
    return _buildTiles(widget.myTile);
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
          print("url ${t.uri ?? ""}");
          print("url ${t.drm_license_url ?? ""}");
          final result = await Navigator.push(
              widget._context,
              MaterialPageRoute(
                  builder: (_) => Player(
                    sampleVideo: t,
                  )));
          if(result != null) {
//            SystemChrome.setEnabledSystemUIOverlays(SystemUiOverlay.values);
            print("back from player");
            setState(() {
              SystemChrome.setPreferredOrientations([
                DeviceOrientation.portraitUp,
              ]);
            });
          }
        },
        subtitle: Text(
          t.extension ?? "",
        ),
        selected: true,
        title: Text(t.name,
            style: TextStyle(fontSize: 18.0, color: Colors.black54)));
  }
}

//ChewieDemo(sampleVideo: t,)
