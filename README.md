# Scan to Go

A small, portable QR reader for Android. It reads the standard QR stack and acts
on each type. It is built for devices whose stock camera app cannot scan QR codes,
including offline devices without Google Play Services.

## What it does

Scan a QR code. The app classifies it and shows one Material 3 card with the right
actions:

| Type | Actions |
| --- | --- |
| Link (URL) | Open (with confirm), Copy, Share |
| Wi-Fi | Connect or guide, Copy password, Copy |
| Phone | Call, Add contact, Copy |
| SMS | Send message, Copy |
| Email | Send email, Copy |
| Location (geo) | Open in maps, Copy |
| Calendar event | Add to calendar, Copy |
| Contact (vCard/MeCard) | Add contact, Copy, Share |
| Text | Copy, Share, Search the web |

The only runtime permission is CAMERA. Every action is a standard Android Intent.

## Design decisions

- **Language:** Kotlin.
- **QR engine:** ZXing (`zxing-android-embedded` + `zxing:core`), the same engine
  the K2Go / CoGo apps already ship. Pure Java, no Google Play Services, works on
  Android 7. No native code, so the APK is architecture-agnostic: one universal APK
  covers 32-bit and 64-bit, with no ABI splits to maintain.
- **Camera UI:** `DecoratedBarcodeView` embedded in an app-owned Material 3 screen.
- **Architecture:** light layering. `scan/domain` is pure Kotlin (parser and the
  type-to-action matrix) and is unit-tested off-device. `scan/presentation` holds
  the camera UI and a thin `ViewModel`. There is no data layer: no network, no
  persistence, no scan history.
- **Scan flow:** single-shot. The camera pauses while a result is shown, so nothing
  decodes behind the result card. One state owner (the `ViewModel`), rotation-safe.
- **Wi-Fi:** one `connectOrGuide` function, tiered by OS version. Android 11+ (API
  30) uses the one-tap system dialog (`ACTION_WIFI_ADD_NETWORKS`). Android 7 to 10
  degrades to: show credentials, copy the password, open Wi-Fi settings. It avoids
  the deprecated `WifiConfiguration` path on purpose.
- **Security:** validate before acting. A scanned link is never auto-opened: the app
  shows the destination and the user confirms.
- **Telemetry:** none. Firebase would pull in Google Play Services and break the
  no-GMS goal. If analytics is added later (for example PostHog, which is HTTP-only
  and does not need GMS), it goes behind a single optional port. Nothing is added now.

## Project layout

```
app/src/main/java/org/appdevforall/scan2go/
  domain/        ScannedContent, ContentParser, ScanAction, ScanActions   (pure Kotlin)
  presentation/  ScanActivity, ScanViewModel, ActionRunner, WifiConnector
  util/          Clipboard, SnackbarDuration
  Scan2GoApp.kt  Application (Material 3 dynamic color)
app/src/test/java/.../domain/   ContentParserTest, ScanActionsTest
```

## Build and run

Open the project in Android Studio and let it sync. It builds a debug APK and runs
on an Android 7 (API 24) or newer device or emulator.

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

## Localization

The UI strings live in `res/values/strings.xml` (English, source of truth) and
`res/values-es/strings.xml` (Spanish). The target is the full K2Go set of 33
languages. Adding the remaining 31 locales is a pre-merge step: machine-translate,
then mark for human review. The locale set is:

```
ar az bg bn cs de el es fa fr gu hi hu in it ja ko lt nl no
pl pt ro ru-rRU sk sr sw ta tr uk vi yo zh-rCN
```

`app_name` ("Scan to Go") stays untranslated: it is a proper noun.

## Scaffold notes

- **Gradle wrapper:** the binary `gradle-wrapper.jar` is not included. Android Studio
  sets up the wrapper on import; from a shell with a system Gradle, run `gradle wrapper`
  once.
- **Launcher icon:** a single vector icon (`drawable/ic_launcher`) is used. For a
  release, generate density PNG fallbacks and an adaptive icon with the Android Studio
  Image Asset tool.
- **No repo home and no Jira ticket yet.** This is a local scaffold. It links to no
  remote. A K2GO ticket, if wanted, is created only on explicit approval.
