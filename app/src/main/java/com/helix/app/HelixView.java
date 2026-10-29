package com.helix.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.Random;

public class HelixView extends View {
  volatile float tx = .5f, ty = .5f, topen = .6f, tpinch = 0;
  volatile long lastHand = 0;
  float rot, open = .6f, pinch, yy = .5f, spin;
  static final int N = 700;
  final float[] t = new float[N], j = new float[N], rr = new float[N];
  final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  final float[] hsv = {0, .7f, 1f};

  public HelixView(Context c) {
    super(c);
    Random r = new Random();
    for (int i = 0; i < N; i++) {
      t[i] = r.nextFloat() * 2 - 1;
      j[i] = r.nextFloat() * 6.2832f;
      rr[i] = r.nextFloat();
    }
  }

  public void setHand(float x, float y, float o, float p) {
    tx = x; ty = y; topen = o; tpinch = p;
    lastHand = System.currentTimeMillis();
  }

  @Override protected void onDraw(Canvas c) {
    long now = System.currentTimeMillis();
    boolean live = now - lastHand < 700;
    float k = .12f;
    open += ((live ? topen : .6f) - open) * k;
    pinch += ((live ? tpinch : 0f) - pinch) * k;
    yy += ((live ? ty : .5f) - yy) * k;
    rot += (((live ? tx : .5f) - .5f) * 9.42f - rot) * k;
    spin += .006f + .012f * open;
    double time = now / 1000.0;
    float W = getWidth(), H = getHeight(), minD = Math.min(W, H);
    double R = minD * (.05 + .2 * open), Hh = H * .42;
    double turns = 2.2 + pinch * 4.5, f = minD * 1.4;
    double hue0 = (210 + open * 120 + time * 10) % 360;
    double ca = Math.cos(rot + spin), sa = Math.sin(rot + spin);
    double tilt = .35 + (yy - .5) * .4, ct = Math.cos(tilt), st = Math.sin(tilt);
    float cx = W / 2, cy = H / 2;
    for (int i = 0; i < N; i++) {
      int s = i % 3;
      double ang = t[i] * turns * Math.PI + time * .8, y = t[i] * Hh, x, z;
      if (s == 2) {
        double u = rr[i] * 2 - 1;
        x = Math.cos(ang) * R * u; z = Math.sin(ang) * R * u;
      } else {
        if (s == 1) ang += Math.PI;
        double wob = 1 + Math.sin(j[i] + time * 2) * .06;
        x = Math.cos(ang) * R * wob; z = Math.sin(ang) * R * wob;
      }
      double X = x * ca + z * sa, Z = -x * sa + z * ca;
      double Y = y * ct - Z * st; Z = y * st + Z * ct;
      double sc = f / (f + Z + minD * .5);
      float px = (float) (cx + X * sc), py = (float) (cy + Y * sc);
      double depth = Math.max(0, Math.min(1, (Z / (R + Hh) + 1) / 2));
      hsv[0] = (float) ((hue0 + t[i] * 50 + (s == 1 ? 40 : 0) + 360) % 360);
      int alpha = (int) (255 * (s == 2 ? .35 : .9) * (.4 + .6 * (1 - depth)));
      paint.setColor(Color.HSVToColor(alpha, hsv));
      c.drawCircle(px, py, (float) Math.max(1.2, 2.2 * sc * (1 + open * .5)), paint);
    }
    postInvalidateOnAnimation();
  }
}
