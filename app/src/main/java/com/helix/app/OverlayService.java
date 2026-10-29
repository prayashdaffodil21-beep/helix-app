package com.helix.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverlayService extends LifecycleService {
  WindowManager wm;
  HelixView view;
  HandLandmarker lm;
  final ExecutorService exec = Executors.newSingleThreadExecutor();

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel("helix", "Helix", NotificationManager.IMPORTANCE_LOW));
    Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
    PendingIntent pi = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE);
    Notification n = new Notification.Builder(this, "helix")
        .setContentTitle("Hand Helix is running")
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .addAction(new Notification.Action.Builder(android.R.drawable.ic_delete, "Stop", pi).build())
        .build();
    startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);

    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    view = new HelixView(this);
    int size = (int) (getResources().getDisplayMetrics().density * 220);
    WindowManager.LayoutParams p = new WindowManager.LayoutParams(size, size,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT);
    p.gravity = Gravity.TOP | Gravity.END;
    p.y = 200;
    wm.addView(view, p);

    try {
      BaseOptions base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build();
      HandLandmarker.HandLandmarkerOptions o = HandLandmarker.HandLandmarkerOptions.builder()
          .setBaseOptions(base).setNumHands(1).setRunningMode(RunningMode.IMAGE).build();
      lm = HandLandmarker.createFromOptions(this, o);
    } catch (Exception e) { lm = null; }

    ListenableFuture<ProcessCameraProvider> fut = ProcessCameraProvider.getInstance(this);
    fut.addListener(() -> {
      try {
        ProcessCameraProvider cp = fut.get();
        ImageAnalysis ia = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build();
        ia.setAnalyzer(exec, this::analyze);
        cp.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, ia);
      } catch (Exception e) { }
    }, getMainExecutor());
  }

  void analyze(ImageProxy img) {
    try {
      if (lm == null) return;
      Bitmap bmp = img.toBitmap();
      Matrix m = new Matrix();
      m.postRotate(img.getImageInfo().getRotationDegrees());
      Bitmap r = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
      HandLandmarkerResult res = lm.detect(new BitmapImageBuilder(r).build());
      if (!res.landmarks().isEmpty()) {
        List<NormalizedLandmark> l = res.landmarks().get(0);
        NormalizedLandmark w = l.get(0), m9 = l.get(9);
        double palm = d(w, m9);
        if (palm < 1e-4) palm = .1;
        double avg = (d(w, l.get(8)) + d(w, l.get(12)) + d(w, l.get(16)) + d(w, l.get(20))) / 4 / palm;
        float open = (float) Math.max(0, Math.min(1, (avg - 1.0) / 0.9));
        float pinch = d(l.get(4), l.get(8)) / palm < .45 ? 1f : 0f;
        view.setHand(1f - m9.x(), m9.y(), open, pinch);
      }
    } catch (Exception e) {
    } finally {
      img.close();
    }
  }

  static double d(NormalizedLandmark a, NormalizedLandmark b) {
    return Math.hypot(a.x() - b.x(), a.y() - b.y());
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    if (intent != null && "STOP".equals(intent.getAction())) stopSelf();
    return START_NOT_STICKY;
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (view != null) wm.removeView(view);
    if (lm != null) lm.close();
    exec.shutdown();
  }
                    }
