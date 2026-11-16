# NOX Wave Rider – Android-Overlay für Samsung Galaxy S24 Ultra

Eine kleine, vollständig lokale Android-App. Sie zeigt **NOX** als transparentes,
verschiebbares Pet über anderen Apps. Das originale 1470 × 1070-PNG aus
`NOX-Wave-Rider-S24.html` und die vorhandenen Schweb-/Space-Spin-Bewegungen
wurden übernommen.

## Bereits umgesetzt

- Native Android-Steueroberfläche, auf Deutsch.
- Systemweite Pet-Einblendung über `TYPE_APPLICATION_OVERLAY` (nach Freigabe).
- Ziehen am Pet; Tippen startet den 360°-Space-Spin.
- Ruhemodus, Turbo, Größe 75–115 %, Positions-Reset.
- Option „Berührungen durchlassen“ (Pet dann nicht verschiebbar).
- Gelegentliche Überraschungstricks nach Inaktivität.
- Gespeicherte Einstellungen und Position.
- Foreground Service mit dauerhafter Benachrichtigung und Stopp-Schaltfläche.
- Vollständig lokale HTML/CSS/JS-Grafik, keine Internetberechtigung, kein API-Key.
- Android-Backups deaktiviert; Start nur nach ausdrücklichem Nutzerbefehl.

## APK erstellen (Android Studio)

1. Android Studio installieren oder öffnen.
2. Dieses Projektverzeichnis `NOX-Android` über **Open** öffnen.
3. Android SDK Platform 35 und Java 17 bereitstellen, Projekt synchronisieren.
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)** ausführen.
5. Anschließend liegt die Debug-APK unter `app/build/outputs/apk/debug/app-debug.apk`.

Das Repository enthält die Gradle-Wrapper-Einstellungen, jedoch nicht die
Wrapper-JAR-Datei. Android Studio kann Gradle 8.11.1 über die Projekteinrichtung
bereitstellen; alternativ Gradle 8.11.1 lokal installieren und `gradle assembleDebug`
im Projektverzeichnis ausführen. Das Projekt nutzt Android Gradle Plugin 8.9.2.

**Aktueller Stand:** Die Quell- und HTML-Prüfungen wurden hier durchgeführt.
Ein APK-Build und ein Gerätetest waren in der Erstellungsumgebung noch nicht
möglich, weil Android SDK, Gradle und `javac` fehlen. Eine installierbare APK wird
deshalb ausdrücklich nicht als geprüft oder vorhanden ausgegeben.

## Automatisch per GitHub Actions

Die Datei `.github/workflows/build-apk.yml` stellt einen reproduzierbaren
Build bereit. Sobald das Projekt im eigenen GitHub-Repository auf dem
`main`-Branch liegt, wird die Debug-APK bei jedem Push gebaut. Der fertige
Download steht in GitHub unter **Actions → Build NOX debug APK →
letzter erfolgreicher Lauf → Artifacts → NOX-Wave-Rider-debug-APK**.

Der Workflow lässt sich zusätzlich über **Run workflow** manuell starten.
Der Upload erfolgt nur nach erfolgreichem Quelltest und APK-Archivtest.

## Auf dem S24 Ultra testen

1. APK auf das Telefon übertragen und installieren.
2. **NOX Wave Rider** öffnen.
3. **1 · Einblendung erlauben** wählen und die Android-Freigabe für „Über anderen Apps anzeigen“ erteilen.
4. Zurückkehren und **2 · NOX über Apps einblenden** antippen.
5. Zu einer anderen App wechseln. NOX sollte als bewegliches, transparentes Pet erscheinen.
6. NOX ziehen oder kurz antippen; Ruhe, Turbo und Größe in der Steuer-App einstellen.
7. Zum Beenden **NOX ausblenden** oder **NOX stoppen** in der Benachrichtigung wählen.

Die optionale Benachrichtigungsfreigabe wird auf Android 13+ angefragt. Bei
„Berührungen durchlassen“ wird das Fenster nicht anklickbar und seine Deckkraft
verringert, um Androids Schutz gegen ungewollte Berührungen zu berücksichtigen.
Sicherheitsrelevante oder systemeigene Fenster können Overlays unterdrücken.

## Architektur

- `MainActivity.java`: Einstellungen, Android-Berechtigungsdialog, Start/Stopp.
- `NoxOverlayService.java`: Foreground Service, Android-Overlayfenster,
  Dragging, periodische Aktionen, Benachrichtigung.
- `NoxConfig.java`: zentralisierte Schlüssel der lokalen Einstellungen.
- `assets/nox_overlay.html`: transparente WebView-Oberfläche mit Original-Animationen.
- `assets/nox.png`: extrahiertes originales NOX-Bild, ohne neue Bildgenerierung.
- `tests/source_checks.js`: lokale, SDK-unabhängige Basisprüfungen.

## Bewusst nicht enthalten

Keine Sprach-KI, kein Mikrofon, keine Werbung, keine Netzwerkanfragen,
kein Auto-Start beim Systemstart und keine unsichtbaren Hintergrunddienste.
NOX ist noch kein Live-Voice-Assistent und bewegt keine einzelnen Körperteile.

Für eine Veröffentlichung im Play Store müssen Zweck und Zulässigkeit des
Foreground-Service-Typs `specialUse` gesondert geprüft und deklariert werden.
Eine Debug-APK ist für eigene Geräte-Tests gedacht, nicht für einen öffentlichen Release.
