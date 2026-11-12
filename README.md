<h1 align="center">Not-Noop</h1>

<p align="center"><b>A community maintenance fork of NOOP, the offline WHOOP companion, focused on Android bug fixes.</b></p>

---

## What this is

Not-Noop is a fork of **NOOP**, an offline, account-free companion app for WHOOP 4.0 / 5.0 straps.
The original project (`github.com/NoopApp/noop`) was deleted and its developer appears to have
stopped working on it. This fork starts from the recovered source preserved at
[`ParthJadhav/noop`](https://github.com/ParthJadhav/noop) (commit `63f3ed0`).

**We are here to repair bugs, not to rebuild the app.** The goals are:

- **Fix bugs, mainly in the Android app** (`android/`). Most of the work happens here.
- **Improve the theming** where it's broken or hard to read.
- Keep everything else as it was: offline, on-device, no account, no cloud.

The macOS, iOS and watchOS code is included unchanged so the project stays complete. Fixes there
may happen, but they are not the focus.

## Who it's for

This is mainly a personal project, but anyone is welcome to use it, report bugs or send fixes.
There is no support promise, no release schedule and no roadmap beyond "make it work better."

## Status

- Android: being worked on. See [`android/README.md`](android/README.md) for build instructions
  (JDK 17, Android SDK 34, a real phone with Bluetooth, and a strap).
- Apple platforms: inherited as-is from upstream and not actively maintained.

The original NOOP README (features, protocol notes, platform install tips) is kept for
reference at [`docs/UPSTREAM-README.md`](docs/UPSTREAM-README.md). Links in it point to the
upstream project and its community, not to this fork.

## Building the Android app

```bash
cd android
./gradlew assembleFullDebug        # build a debug APK
./gradlew testFullDebugUnitTest    # run unit tests
```

The APK lands in `android/app/build/outputs/apk/`. Sideload it on Android 8.0+.

## Reporting bugs

Open an issue in this repo with your phone model, Android version, strap model (4.0 / 5.0 / MG),
and what happened versus what you expected. Logs or screenshots help a lot.

## License and credits

Not-Noop is **not** affiliated with the original NOOP developer, and **not** affiliated with,
endorsed by, or connected to WHOOP, Inc. "WHOOP" is used only to identify the hardware the app
talks to. This is not a medical device. See [`DISCLAIMER.md`](DISCLAIMER.md).

The code is under the **PolyForm Noncommercial License 1.0.0**, inherited from NOOP. See
[`LICENSE`](LICENSE). You may use, modify and share it for **non-commercial** purposes only.

Required Notice: Copyright 2026 NoopApp

Third-party components and prior reverse-engineering work are credited in
[`NOTICE`](NOTICE) and [`ATTRIBUTION.md`](ATTRIBUTION.md).
