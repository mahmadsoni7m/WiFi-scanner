# WiFi Scanner

A native Android networking utility (Kotlin, Jetpack Compose, Material 3) for scanning Wi-Fi, analysing signal strength, testing internet speed and checking the security of **your own** network. The interface is available in **Tajik (default)** and **English**. A short Tajik quick-start is in [README.tg.md](README.tg.md).

* Application ID: `com.wifiscanner.app`
* Min SDK 26 (Android 8.0), target/compile SDK 34
* Build: Gradle 8.9, Android Gradle Plugin 8.5.2, Kotlin 2.0.20
* No ads, no analytics, no account, no telemetry.

> **Status:** this source was written without access to an Android SDK or the internet, so it has **not been compiled or run on a device by its author**. The GitHub Actions workflow in this repository performs the real build and runs the unit tests. If the first build fails, see [Troubleshooting failed builds](#10-troubleshooting-failed-builds) - fixing a small compile error is expected and normal for a first build.

## 1. What WiFi Scanner does

It shows what Android lets an ordinary app see about nearby Wi-Fi networks and your current connection, measures your internet speed against a documented endpoint, finds responding devices on your own local network, and reports security observations together with recommendations. It never invents data: anything Android hides is shown as **Unavailable** with an explanation.

## 2. Implemented features

| Area | What it does |
|---|---|
| Home | Wi-Fi state, SSID (if permitted), signal in dBm and quality, band/frequency, link speed, local IP, internet status (validated / captive portal / none), quick actions, recent scan, last speed test, security summary, clear states for Wi-Fi off / permission missing / location off |
| Wi-Fi Scanner | Real scan through `WifiManager` (throttle-aware), SSID, BSSID, dBm, MHz, 2.4/5/6 GHz band, channel derived from frequency, reported security capabilities, sort by strength, search, band filter, last-scan time, strength meters, optional auto-refresh (only while the screen is visible) |
| Signal analyzer | Live dBm (every 2 s while open), quality class, history chart, band, link speed, guidance and an honest disclaimer (software cannot raise physical signal strength) |
| Network devices | Safe, limited discovery of your **private** /24 subnet: short reachability check plus a few TCP connection attempts, hostnames via reverse DNS, MAC only if Android allows reading the ARP table, progress, cancel, 45 s timeout |
| Speed test | Latency, jitter, download, upload against a Cloudflare-compatible endpoint (configurable), progress, cancel, real error messages, local history chart (Room) |
| Security | (a) Checker: connection type, reported Wi-Fi security, open/WEP/WPA/TKIP warnings, gateway/DNS/IP, permission state, router recommendations - each finding is labelled *Verified*, *Warning*, *Recommendation* or *Unavailable*. (b) Authorized testing: consent gate, router checklist, gateway reachability, DNS and HTTPS checks, copy/share text report |
| Saved networks | Add/edit/delete/search entries you type yourself; passwords encrypted with an Android Keystore AES-256-GCM key; copy requires biometric/device-credential authentication (explicit fallback dialog if the device has none); delete-all also destroys the key |
| Quick connect | Uses `WifiNetworkSpecifier` (Android 10+) so the **system** shows its own dialog; "connected" is shown only after Android confirms; on older versions or unsupported security types it opens the system Wi-Fi settings |
| Settings | Language (Tajik/English), theme (system/light/dark), scan auto-refresh, speed-test server, clear scan history / speed results / security results / credentials, permission status, privacy, troubleshooting, version, licenses, about |

Deliberately **not** implemented: password cracking, handshake capture, WPS PIN attacks, deauthentication, credential extraction, packet interception, scanning of public IP ranges, or reading passwords Android has stored for other networks.

## 3. Permissions and why

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Android requires it to return Wi-Fi scan results and the connected SSID/BSSID. The app never reads GPS or stores location. |
| `NEARBY_WIFI_DEVICES` (Android 13+) | Wi-Fi scan access on Android 13+. |
| `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | Read Wi-Fi state, request a scan. |
| `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE` | Connection type, gateway/DNS, and the Quick-connect network request. |
| `INTERNET` | Speed test, DNS/HTTPS diagnostics and local-network sockets. |

The app explains the location/nearby permission on screen **before** the system dialog appears, and works in a degraded, clearly labelled mode if it is denied.

**Works offline:** scanner, signal analyzer, device scan, saved networks, settings. **Needs internet:** speed test, DNS test, HTTPS test.

## 4. Known Android platform limitations

* Foreground apps may start only ~4 Wi-Fi scans per 2 minutes (Android 9+); the app shows cached results and says so.
* SSID/BSSID are hidden without permission; on many versions the system **Location** switch must also be on.
* Channel width, vendor info and WPS flags are not shown; channel is derived from frequency.
* MAC addresses of other devices are normally unavailable on Android 10+. Hostnames are often unavailable.
* Device discovery cannot find sleeping devices or networks with client isolation / strict firewalls.
* `WifiNetworkSpecifier` connections are temporary and bound to the app; Android 9 and older use the system settings.
* The app cannot fully audit a router; it can only report what Android exposes.

## 5. Create a GitHub repository (from a phone)

1. Open <https://github.com> in your phone browser and sign in (create an account if needed).
2. Tap **+** > **New repository**. Name it `WiFi-Scanner`, choose **Public** or **Private**, leave "Add a README" **off**, tap **Create repository**.

## 6. Upload the project from an Android phone

Uploading a ZIP does **not** unzip it on GitHub, and the web uploader cannot pick folders or hidden folders such as `.github`. Two reliable ways:

**Option A - Termux (recommended; keeps the folder structure and `.github`)**

```bash
pkg update && pkg install -y git unzip
termux-setup-storage                      # allow storage access once
cd ~ && unzip ~/storage/downloads/WiFi-Scanner-Source.zip
cd WiFi-Scanner
git init -b main
git add -A && git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USER/WiFi-Scanner.git
git push -u origin main                   # username = your GitHub name, password = a Personal Access Token
```

Create the token at GitHub > Settings > Developer settings > Personal access tokens (classic) with the `repo` and `workflow` scopes. Never share it.

**Option B - GitHub web (slower)**: on the empty repository tap **uploading an existing file**, switch the browser to "Desktop site", and upload files folder by folder using your file manager's multi-select. Use **Add file > Create new file** and type `.github/workflows/android-build.yml` as the name to create the workflow file by pasting its text.

## 7. Run GitHub Actions

The **Android Build** workflow starts automatically on every push. To start it manually: repository > **Actions** > **Android Build** > **Run workflow**. A green check means the unit tests ran and the APK was built. A red cross means a failure - open the run and the failed step.

The repository ships `gradle/wrapper/gradle-wrapper.properties`. The binary `gradle-wrapper.jar` could not be produced offline, so the workflow generates the wrapper (`gradle wrapper --gradle-version 8.9`) when it is missing and then builds with `./gradlew`.

## 8. Download the APK (mobile browser)

1. Repository > **Actions** > tap the latest successful run.
2. Scroll to **Artifacts** and tap **WiFi-Scanner-debug-apk**. GitHub downloads a ZIP.
3. Open the ZIP in your Files app, extract `app-debug.apk`. (Artifact download requires being signed in to GitHub.)

## 9. Install the APK

1. Open `app-debug.apk` from Files/Downloads.
2. If asked, allow **Install unknown apps** for the app you opened it with (Settings > Apps > Special access).
3. Tap **Install**. Debug builds are signed with a debug key and are for testing; to update later, install the new APK over the old one (same signature).

## 10. Troubleshooting failed builds

* Open the failed run > the red step > read the first `error:` / `e: file://...` line. Kotlin errors name the file and line.
* `Could not resolve ...`: a dependency version may not exist; check `app/build.gradle.kts` and the root `build.gradle.kts` (plugin versions: AGP 8.5.2, Kotlin 2.0.20, KSP 2.0.20-1.0.25 must match the Kotlin version).
* `Unsupported class file major version`: the workflow uses JDK 17; do not change it.
* `SDK location not found` / licence errors: the `android-actions/setup-android` step provides them; re-run the job.
* Lint reports do not fail the build; they are uploaded as the **build-reports** artifact together with the test report.
* Paste the first error line when asking for help.

## 11. Configure an optional speed-test server

The default is `https://speed.cloudflare.com` (endpoints `GET /__down?bytes=N` and `POST /__up`). Only random test bytes and standard HTTPS headers are sent. To use your own server, expose the same two endpoints over HTTPS (any small HTTP service that returns N random bytes for `/__down?bytes=N` and accepts and discards a POST body on `/__up`, with a `0`-byte response for `bytes=0`), then enter its address in **Settings > Speed-test server**. Cleartext `http://` is rejected.

## 12. Configure release signing securely

Never commit a keystore or passwords. Create a keystore once (for example in Termux: `pkg install openjdk-17`, then `keytool -genkeypair -v -keystore release.jks -alias wifiscanner -keyalg RSA -keysize 2048 -validity 10000`), then in the repository open **Settings > Secrets and variables > Actions** and add:

| Secret | Value |
|---|---|
| `WS_KEYSTORE_BASE64` | `base64 -w0 release.jks` output |
| `WS_KEYSTORE_PASSWORD` | keystore password |
| `WS_KEY_ALIAS` | `wifiscanner` |
| `WS_KEY_PASSWORD` | key password |

Push a tag (`git tag v1.0.0 && git push origin v1.0.0`) to run the **Release APK** workflow. It builds a signed APK when the secrets exist (otherwise the debug APK) and attaches it to a GitHub Release. Keep a backup of the keystore - losing it means you cannot update installed copies.

## 13. Contributing

Fork, create a branch, keep UI text in `res/values` **and** `res/values-en` (Tajik is the default language), add unit tests for pure logic in `app/src/test`, and open a pull request. Do not add features that attack networks, extract credentials or collect data covertly; such changes will not be accepted.

## Project layout

```
app/src/main/java/com/wifiscanner/app/
  MainActivity.kt, WifiScannerApp.kt, AppContainer.kt
  navigation/   bottom navigation + routes
  ui/           theme, components, screens (Compose)
  viewmodel/    ViewModels and UI state
  data/         Room entities/DAOs, DataStore settings, repositories
  domain/       models, pure helpers (WifiMath, Validators, IpUtils)
  network/      WifiScanner, NetworkInfoProvider, DeviceScanner, SpeedTester, QuickConnector, diagnostics
  security/     Keystore cipher, biometric helper, security analyzer, report builder
  utils/        permissions and system-intent helpers
```

License: MIT (see `LICENSE`).
