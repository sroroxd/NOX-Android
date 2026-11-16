const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const vm = require('node:vm');

const root = path.resolve(__dirname, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const png = fs.readFileSync(path.join(root, 'app/src/main/assets/nox.png'));
const html = read('app/src/main/assets/nox_overlay.html');
const manifest = read('app/src/main/AndroidManifest.xml');
const service = read('app/src/main/java/de/art/nox/NoxOverlayService.java');
const activity = read('app/src/main/java/de/art/nox/MainActivity.java');

assert.equal(png.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', 'PNG signature');
assert.equal(png.readUInt32BE(16), 1470, 'Original width');
assert.equal(png.readUInt32BE(20), 1070, 'Original height');

assert.match(manifest, /android\.permission\.SYSTEM_ALERT_WINDOW/);
assert.match(manifest, /android\.permission\.FOREGROUND_SERVICE_SPECIAL_USE/);
assert.match(manifest, /android\.permission\.VIBRATE/);
assert.match(manifest, /android:foregroundServiceType="specialUse"/);
assert.match(manifest, /android:exported="false"/);
assert.match(manifest, /android:allowBackup="false"/);
assert.doesNotMatch(manifest, /android\.permission\.INTERNET/);
assert.match(service, /TYPE_APPLICATION_OVERLAY/);
assert.match(service, /FLAG_NOT_TOUCHABLE/);
assert.match(service, /ACTION_STOP/);
assert.match(service, /START_NOT_STICKY/);
assert.match(activity, /Settings\.canDrawOverlays/);
assert.match(html, /@keyframes stardrive/);
assert.match(html, /@keyframes surf/);
assert.match(html, /img src="nox.png"/);

function classList() {
  const values = new Set();
  return {
    values,
    add: value => values.add(value),
    remove: value => values.delete(value),
    toggle: (value, state) => state ? values.add(value) : values.delete(value),
    contains: value => values.has(value),
  };
}

const rootElement = { classList: classList() };
let animationEnd;
const pet = {
  classList: classList(),
  offsetWidth: 280,
  addEventListener: (event, callback) => {
    if (event === 'animationend') animationEnd = callback;
  },
};
const vars = new Map();
const sandbox = {
  window: { matchMedia: () => ({ matches: false }) },
  document: {
    getElementById: id => id === 'root' ? rootElement : pet,
    documentElement: { style: { setProperty: (name, value) => vars.set(name, value) } },
  },
};
const script = html.match(/<script>([\s\S]*?)<\/script>/);
assert.ok(script, 'Embedded JavaScript');
new vm.Script(script[1]).runInNewContext(sandbox);
sandbox.window.noxSetState(true, true);
assert.ok(rootElement.classList.contains('sleeping'));
assert.equal(vars.get('--speed'), '2.2s');
sandbox.window.noxSpin();
assert.ok(pet.classList.contains('trick'));
assert.ok(rootElement.classList.contains('tricking'));
assert.ok(!rootElement.classList.contains('sleeping'));
animationEnd({ animationName: 'stardrive' });
assert.ok(!pet.classList.contains('trick'));
assert.ok(!rootElement.classList.contains('tricking'));
sandbox.window.noxSetState(false, false);
assert.equal(vars.get('--speed'), '4.8s');

process.stdout.write('OK: PNG, Manifest, Overlay-Quellcode und Animationssteuerung.\n');
