import 'package:drm_wv_fp_player_example/models/style/subtitle_border_style.dart';
import 'package:drm_wv_fp_player_example/models/style/subtitle_position.dart';
import 'package:flutter/material.dart';

class SubtitleStyle {
  final bool hasBorder;
  final SubtitleBorderStyle borderStyle;
  final double fontSize;
  final Color textColor;
  final SubtitlePosition position;

  const SubtitleStyle(
      {this.hasBorder = false,
      this.borderStyle = const SubtitleBorderStyle(),
      this.fontSize = 16,
      this.textColor = Colors.black,
      this.position = const SubtitlePosition()});
}
