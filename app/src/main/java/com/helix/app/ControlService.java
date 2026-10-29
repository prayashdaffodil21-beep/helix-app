package com.helix.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class ControlService extends AccessibilityService {
  public static volatile ControlService instance;

  @Override protected void onServiceConnected() { instance = this; }

  @Override public boolean onUnbind(Intent i) {
    instance = null;
    return super.onUnbind(i);
  }

  @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
  @Override public void onInterrupt() { }

  public void tap(float x, float y) {
    Path p = new Path();
    p.moveTo(x, y);
    dispatchGesture(new GestureDescription.Builder()
        .addStroke(new GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null);
  }

  public void swipe(float x1, float y1, float x2, float y2) {
    Path p = new Path();
    p.moveTo(x1, y1);
    p.lineTo(x2, y2);
    dispatchGesture(new GestureDescription.Builder()
        .addStroke(new GestureDescription.StrokeDescription(p, 0, 250)).build(), null, null);
  }

  public void back() { performGlobalAction(GLOBAL_ACTION_BACK); }
  public void home() { performGlobalAction(GLOBAL_ACTION_HOME); }
}
