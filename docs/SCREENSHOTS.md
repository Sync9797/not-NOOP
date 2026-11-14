# Screenshot tour (Android, Not-Noop)

The screenshot tour draws real Compose screens to PNG files on a desktop JVM, using Robolectric. No
device or emulator is needed. Use it to look at layout and theme changes (phone and tablet sizes, light
and night) before installing a build.

```bash
cd android
./gradlew testFullDebugUnitTest -PnoopScreenshots --tests "com.noop.screenshots.*"
# PNGs: android/app/build/screenshots/
```

- **Off by default.** Without `-PnoopScreenshots` the tour skips itself, and the app's resources are not
  merged into unit tests. Ordinary test runs and CI behave exactly as before. Merging those resources
  slows down every test run, so only the tour turns it on.
- **What it renders:** `android/app/src/test/java/com/noop/screenshots/ScreenshotTour.kt`. Each test
  sets a size and theme through a Robolectric qualifier, for example `w411dp-h891dp-port-night-xhdpi`.
  Qualifiers must follow Android's order, so `night` goes before the density. Each shot uses the
  day-cycle sky as the wallpaper.
- **Adding a screen:** add a `shoot("name") { YourScreen(...) }` test. Screens that need a view model or
  a database are harder to render this way. Pass them sample state, or render the stateless part.
- **Limits:**
  - Robolectric draws with the desktop graphics stack, so fonts and blur can differ slightly from a phone.
  - Folds, wallpaper-based Material You colours and Bluetooth state are not simulated. Check those on a
    device.
- The tour is a way to look at screens, not a pixel-diff gate. Nothing is compared against stored images.
