package de.art.nox;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int CYAN = Color.rgb(23, 230, 255);
    private static final int WHITE = Color.rgb(231, 246, 255);
    private static final int MUTED = Color.rgb(151, 185, 214);

    private SharedPreferences prefs;
    private TextView status;
    private TextView permission;
    private TextView sizeLabel;
    private Switch sleepSwitch;
    private Switch turboSwitch;
    private Switch passSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = NoxConfig.get(this);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(6, 11, 27));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(25), dp(24), dp(30));
        scroll.addView(panel);
        if (Build.VERSION.SDK_INT >= 35) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                panel.setPadding(dp(24), dp(25) + bars.top, dp(24), dp(30) + bars.bottom);
                return insets;
            });
        }
        setContentView(scroll);

        TextView brand = label("DEIN DIGITALER BEGLEITER", 12, MUTED);
        brand.setLetterSpacing(0.19f);
        panel.addView(brand);
        TextView title = label("NOX.", 44, WHITE);
        title.setTypeface(null, 1);
        panel.addView(title);
        panel.addView(label("WAVE RIDER  ·  S24 ULTRA", 13, CYAN));
        addGap(panel, 25);

        status = label("NOX ist ausgeschaltet", 16, WHITE);
        panel.addView(status);
        permission = label("", 12, MUTED);
        panel.addView(permission);
        addGap(panel, 16);

        Button allow = button("1 · Einblendung erlauben");
        allow.setOnClickListener(view -> requestOverlayPermission());
        panel.addView(allow);
        Button show = button("2 · NOX über Apps einblenden");
        show.setOnClickListener(view -> showOverlay());
        panel.addView(show);
        Button hide = button("NOX ausblenden");
        hide.setOnClickListener(view -> {
            send(NoxOverlayService.ACTION_STOP);
            prefs.edit().putBoolean(NoxConfig.ACTIVE, false).apply();
            refresh();
        });
        panel.addView(hide);
        addGap(panel, 20);

        TextView controls = label("INTERAKTIONEN", 14, CYAN);
        controls.setTypeface(null, 1);
        panel.addView(controls);
        addGap(panel, 8);
        Button spin = button("✦ Space-Spin 360°");
        spin.setOnClickListener(view -> {
            prefs.edit().putBoolean(NoxConfig.SLEEP, false).apply();
            if (prefs.getBoolean(NoxConfig.ACTIVE, false)) send(NoxOverlayService.ACTION_TRICK);
            refresh();
        });
        panel.addView(spin);

        sleepSwitch = toggle(panel, "☾ Ruhemodus", NoxConfig.SLEEP);
        turboSwitch = toggle(panel, "ϟ Turbomodus", NoxConfig.TURBO);
        passSwitch = toggle(panel, "Berührungen durchlassen", NoxConfig.PASS_THROUGH);
        TextView note = label("Bei aktivem Durchlassen kannst du Apps hinter NOX bedienen. Zum Verschieben diese Option wieder ausschalten.", 12, MUTED);
        panel.addView(note);
        addGap(panel, 20);

        sizeLabel = label("NOX-Größe: " + prefs.getInt(NoxConfig.SIZE, 100) + "%", 14, WHITE);
        panel.addView(sizeLabel);
        SeekBar size = new SeekBar(this);
        size.setMax(40);
        size.setProgress(prefs.getInt(NoxConfig.SIZE, 100) - 75);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                int value = 75 + progress;
                sizeLabel.setText("NOX-Größe: " + value + "%");
                if (user) {
                    prefs.edit().putInt(NoxConfig.SIZE, value).apply();
                    updateOverlay();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        panel.addView(size);
        Button reset = button("Position zurücksetzen");
        reset.setOnClickListener(view -> {
            prefs.edit().remove(NoxConfig.X).remove(NoxConfig.Y).apply();
            if (prefs.getBoolean(NoxConfig.ACTIVE, false)) send(NoxOverlayService.ACTION_RESET_POSITION);
        });
        panel.addView(reset);
        addGap(panel, 16);
        panel.addView(label("100 % lokal · keine KI-API · keine Internetberechtigung", 12, MUTED));
        panel.addView(label("NOX läuft nur, wenn du ihn ausdrücklich startest. Über die permanente Benachrichtigung kannst du ihn jederzeit stoppen.", 12, MUTED));
    }

    private Switch toggle(LinearLayout panel, String text, String key) {
        Switch toggle = new Switch(this);
        toggle.setText(text);
        toggle.setTextColor(WHITE);
        toggle.setTextSize(15);
        toggle.setPadding(0, dp(10), 0, dp(10));
        toggle.setChecked(prefs.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            updateOverlay();
        });
        panel.addView(toggle);
        return toggle;
    }

    private void refresh() {
        if (status == null) return;
        boolean allowed = Settings.canDrawOverlays(this);
        boolean active = prefs.getBoolean(NoxConfig.ACTIVE, false);
        status.setText(active && allowed ? "● NOX schwebt über deinen Apps" : "○ NOX ist ausgeschaltet");
        status.setTextColor(active && allowed ? CYAN : WHITE);
        permission.setText(allowed ? "Einblendungsberechtigung: erteilt" : "Einblendungsberechtigung: noch nicht erteilt");
        sleepSwitch.setChecked(prefs.getBoolean(NoxConfig.SLEEP, false));
        turboSwitch.setChecked(prefs.getBoolean(NoxConfig.TURBO, false));
        passSwitch.setChecked(prefs.getBoolean(NoxConfig.PASS_THROUGH, false));
    }

    private void requestOverlayPermission() {
        Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        if (Build.VERSION.SDK_INT < 30) settings.setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivity(settings);
        } catch (ActivityNotFoundException exception) {
            status.setText("Die Berechtigungsseite ist auf diesem Gerät nicht verfügbar.");
        }
    }

    private void showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
            return;
        }
        startVisibleService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Notification access is optional; Android still permits a foreground service.
        if (requestCode == 33 && Settings.canDrawOverlays(this)) startVisibleService();
    }

    private void startVisibleService() {
        if (send(NoxOverlayService.ACTION_SHOW)) {
            prefs.edit().putBoolean(NoxConfig.ACTIVE, true).apply();
        }
        refresh();
    }

    private void updateOverlay() {
        if (prefs.getBoolean(NoxConfig.ACTIVE, false)) send(NoxOverlayService.ACTION_UPDATE);
    }

    private boolean send(String action) {
        Intent intent = new Intent(this, NoxOverlayService.class).setAction(action);
        try {
            if (NoxOverlayService.ACTION_SHOW.equals(action)) startForegroundService(intent);
            else startService(intent);
            return true;
        } catch (RuntimeException ex) {
            prefs.edit().putBoolean(NoxConfig.ACTIVE, false).apply();
            if (status != null) status.setText("NOX konnte nicht gestartet werden: " + ex.getClass().getSimpleName());
            return false;
        }
    }

    private TextView label(String value, int sp, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setPadding(0, dp(5), 0, dp(5));
        return text;
    }

    private Button button(String value) {
        Button result = new Button(this);
        result.setAllCaps(false);
        result.setText(value);
        result.setTextColor(WHITE);
        result.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(18, 51, 101)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(53));
        params.bottomMargin = dp(8);
        result.setLayoutParams(params);
        return result;
    }

    private void addGap(LinearLayout panel, int dp) {
        View space = new View(this);
        panel.addView(space, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
