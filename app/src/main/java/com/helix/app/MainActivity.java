package com.helix.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setGravity(Gravity.CENTER);
    l.setPadding(48, 48, 48, 48);
    TextView t = new TextView(this);
    t.setTextSize(18);
    t.setText("Hand Helix\n\nTap Start and allow camera, notifications and 'display over other apps'. Tap Start again after each permission. Then open any app.");
    Button s = new Button(this); s.setText("Start");
    Button x = new Button(this); x.setText("Stop");
    s.setOnClickListener(v -> start());
    x.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
    l.addView(t); l.addView(s); l.addView(x);
    setContentView(l);
  }

  void start() {
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.CAMERA, "android.permission.POST_NOTIFICATIONS"}, 1);
      return;
    }
    if (!Settings.canDrawOverlays(this)) {
      startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:" + getPackageName())));
      return;
    }
    startForegroundService(new Intent(this, OverlayService.class));
  }
}
