<h1 align="center">Not-Noop</h1>

<p align="center"><b>A community maintenance fork of NOOP, the offline WHOOP companion, focused on Android bug fixes.</b></p>

---

## What this is

Not-Noop is a fork of **NOOP**, an offline, account-free companion app for WHOOP 4.0 / 5.0 straps.
The original project (`github.com/NoopApp/noop`) was deleted and its developer appears to have
stopped working on it. The community kept it going at [`ryanbr/noop`](https://github.com/ryanbr/noop),
and Not-Noop is built on that code (NOOP 12.1.0, build 558), with its full commit history.

**We are here to repair bugs, not to rebuild the app.** The goals are:

- **Fix bugs, mainly in the Android app** (`android/`). Most of the work happens here.
- **Improve the theming** where it's broken or hard to read.
- Keep everything else as it was: offline, on-device, no account, no cloud.

The macOS, iOS and watchOS code is included unchanged so the project stays complete. Fixes there
may happen, but they are not the focus.

## Who it's for

This is mainly a personal project, but anyone is welcome to use it, report bugs or send fixes.
There is no support promise, no release schedule and no roadmap beyond "make it work better."

## How it differs from NOOP

- **Its own app id, `com.not.noop.oop`.** It installs beside the original NOOP app instead of
  replacing it, with its own separate data. On the launcher it is still called "NOOP".
- **Update checks look at this repository's releases**, not upstream's, so Not-Noop never offers to
  "update" you to a different app.
- **Bug fixes**, listed in the commit history. Highlights:
  - Health Connect writeback no longer gets stuck on "Sharing paused — permission was revoked"
    (VO2 max write permission was never declared).
  - Imported Health Connect workouts now get their distance (read permission was never declared).
  - Health Connect days refresh on every sync instead of freezing after the first one.
  - Auto-sync retries after a failure and runs whenever you return to the app.

The original NOOP README (features, protocol notes, platform install tips) is kept for reference at
[`docs/UPSTREAM-README.md`](docs/UPSTREAM-README.md). Links in it point to the upstream project and its
community, not to this fork.

## Building the Android app

You need JDK 17+ and the Android SDK (API 35). See [`android/README.md`](android/README.md) for details.

```bash
cd android
./gradlew assembleFullDebug        # build a debug APK
./gradlew testFullDebugUnitTest    # run unit tests
```

The APK lands in `android/app/build/outputs/apk/`. Sideload it on Android 8.0+.

## Pulling in upstream fixes

Not-Noop shares history with `ryanbr/noop`, so later upstream fixes can be merged in:

```bash
git remote add upstream https://github.com/ryanbr/noop
git fetch upstream
git merge upstream/main
```

## Reporting bugs

Open an issue in this repo with your phone model, Android version, strap model (4.0 / 5.0 / MG),
and what happened versus what you expected. Logs or screenshots help a lot.

## License and credits

Not-Noop is **not** affiliated with the original NOOP developer or the `ryanbr/noop` maintainers, and
**not** affiliated with, endorsed by, or connected to WHOOP, Inc. "WHOOP" is used only to identify the
hardware the app talks to. This is not a medical device. See [`DISCLAIMER.md`](DISCLAIMER.md).

The code is under the **PolyForm Noncommercial License 1.0.0**, inherited from NOOP. See
[`LICENSE`](LICENSE). You may use, modify and share it for **non-commercial** purposes only.

Required Notice: Copyright 2026 NoopApp

Third-party components and prior reverse-engineering work are credited in
[`NOTICE`](NOTICE) and [`ATTRIBUTION.md`](ATTRIBUTION.md).
