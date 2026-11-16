package de.art.nox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class NoxOverlayService extends Service {
    static final String ACTION_SHOW = "de.art.nox.SHOW";
    static final String ACTION_STOP = "de.art.nox.STOP";
    static final String ACTION_UPDATE = "de.art.nox.UPDATE";
    static final String ACTION_TRICK = "de.art.nox.TRICK";
    static final String ACTION_RESET_POSITION = "de.art.nox.RESET_POSITION";
    private static final String CHANNEL = "nox_overlay";
    private static final int NOTIFICATION_ID = 731;

    private SharedPreferences prefs;
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private WebView web;
    private boolean ready;
    private float pointerX, pointerY;
    private int originalX, originalY;
    private boolean dragging;
    private boolean tricking;
    private long lastInteraction;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable endSpin = () -> tricking = false;
    private final Runnable surprise = new Runnable() {
        @Override public void run() {
            if (web != null && ready && !dragging && !tricking
                    && !prefs.getBoolean(NoxConfig.SLEEP, false)
                    && SystemClock.elapsedRealtime() - lastInteraction > 17000
                    && Math.random() < .45) {
                spin(false);
            }
            handler.postDelayed(this, 16000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = NoxConfig.get(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        lastInteraction = SystemClock.elapsedRealtime();
        handler.postDelayed(surprise, 16000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIFICATION_ID, notification());
            if (web == null) createOverlay();
            if (ACTION_RESET_POSITION.equals(action)) resetPosition();
            if (ACTION_UPDATE.equals(action) || ACTION_SHOW.equals(action)) {
                lastInteraction = SystemClock.elapsedRealtime();
                updateOverlay();
            }
            if (ACTION_TRICK.equals(action)) spin(true);
        } catch (RuntimeException exception) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private Notification notification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "NOX schwebendes Pet", NotificationManager.IMPORTANCE_LOW));
        Intent home = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(this, 0, home, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, NoxOverlayService.class).setAction(ACTION_STOP);
        PendingIntent close = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("NOX ist aktiv")
                .setContentText("Antippen für Einstellungen · jederzeit stoppen")
                .setContentIntent(open)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "NOX stoppen", close)
                .build();
    }

    private void createOverlay() {
        int size = prefs.getInt(NoxConfig.SIZE, 100);
        int width = dp(280) * size / 100;
        int height = dp(236) * size / 100;
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int initialX = Math.max(0, metrics.widthPixels - width - dp(12));
        int initialY = Math.max(0, metrics.heightPixels / 4);

        params = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = prefs.getInt(NoxConfig.X, initialX);
        params.y = prefs.getInt(NoxConfig.Y, initialY);
        clampPosition(metrics);

        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setDomStorageEnabled(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                ready = true;
                updateAnimationState();
            }
        });
        web.setOnTouchListener(this::touch);
        wm.addView(web, params);
        prefs.edit().putBoolean(NoxConfig.ACTIVE, true).apply();
        web.loadUrl("file:///android_asset/nox_overlay.html");
    }

    private boolean touch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastInteraction = SystemClock.elapsedRealtime();
            pointerX = event.getRawX();
            pointerY = event.getRawY();
            originalX = params.x;
            originalY = params.y;
            dragging = false;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = event.getRawX() - pointerX;
            float dy = event.getRawY() - pointerY;
            if (Math.hypot(dx, dy) > dp(10)) dragging = true;
            if (dragging) {
                params.x = originalX + Math.round(dx);
                params.y = originalY + Math.round(dy);
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(metrics);
                clampPosition(metrics);
                wm.updateViewLayout(web, params);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            lastInteraction = SystemClock.elapsedRealtime();
            if (dragging) {
                prefs.edit().putInt(NoxConfig.X, params.x).putInt(NoxConfig.Y, params.y).apply();
            } else {
                spin(true);
            }
            dragging = false;
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            return true;
        }
        return true;
    }

    private void spin(boolean haptic) {
        if (web == null || !ready || tricking) return;
        tricking = true;
        lastInteraction = SystemClock.elapsedRealtime();
        prefs.edit().putBoolean(NoxConfig.SLEEP, false).apply();
        updateAnimationState();
        web.evaluateJavascript("window.noxSpin()", null);
        handler.removeCallbacks(endSpin);
        handler.postDelayed(endSpin, 1900);
        if (haptic) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void updateOverlay() {
        if (web == null || params == null) return;
        int size = prefs.getInt(NoxConfig.SIZE, 100);
        params.width = dp(280) * size / 100;
        params.height = dp(236) * size / 100;
        boolean through = prefs.getBoolean(NoxConfig.PASS_THROUGH, false);
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | (through ? WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE : 0);
        // Android 12+ rejects touches routed through heavily obscuring overlays.
        params.alpha = through ? 0.70f : 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        clampPosition(metrics);
        wm.updateViewLayout(web, params);
        updateAnimationState();
    }

    private void updateAnimationState() {
        if (web == null || !ready) return;
        boolean sleep = prefs.getBoolean(NoxConfig.SLEEP, false);
        boolean turbo = prefs.getBoolean(NoxConfig.TURBO, false);
        web.evaluateJavascript("window.noxSetState(" + sleep + "," + turbo + ")", null);
    }

    private void resetPosition() {
        if (web == null || params == null) return;
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        params.x = Math.max(0, metrics.widthPixels - params.width - dp(12));
        params.y = metrics.heightPixels / 4;
        clampPosition(metrics);
        wm.updateViewLayout(web, params);
        prefs.edit().putInt(NoxConfig.X, params.x).putInt(NoxConfig.Y, params.y).apply();
    }

    private void clampPosition(DisplayMetrics metrics) {
        params.x = Math.max(0, Math.min(params.x, Math.max(0, metrics.widthPixels - params.width)));
        params.y = Math.max(0, Math.min(params.y, Math.max(0, metrics.heightPixels - params.height)));
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(surprise);
        handler.removeCallbacks(endSpin);
        ready = false;
        if (web != null) {
            if (web.getParent() != null) wm.removeView(web);
            web.stopLoading();
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        prefs.edit().putBoolean(NoxConfig.ACTIVE, false).apply();
        super.onDestroy();
    }
}
