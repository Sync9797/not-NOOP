package com.noop.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.noop.ui.HowNoopWorksScreen
import com.noop.ui.LazyScreenScaffold
import com.noop.ui.NoopCard
import com.noop.ui.NoopPrefs
import com.noop.ui.NoopTheme
import com.noop.ui.ScoringGuideScreen
import com.noop.ui.WhoopModelComparisonScreen
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Not-Noop screenshot tour: renders real screens to PNG so layout and theme changes can be looked at
 * without a device. Skipped unless Gradle runs with `-PnoopScreenshots`, which also merges the app's
 * resources into the test classpath (docs/SCREENSHOTS.md):
 *
 *   ./gradlew testFullDebugUnitTest -PnoopScreenshots --tests "com.noop.screenshots.*"
 *
 * PNGs land in app/build/screenshots, one per screen and size: phone portrait, tablet landscape and,
 * via the qualifier on each test, light and night. Each shot uses the day-cycle sky as the wallpaper.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = com.noop.NoopApplication::class)
class ScreenshotTour {

    private val outDir: File? = System.getProperty("noop.screenshots.dir")?.let(::File)

    @Before fun onlyWhenAsked() {
        assumeTrue("screenshot tour runs only with -PnoopScreenshots", outDir != null)
        NoopPrefs.setShowDayCycleBackground(RuntimeEnvironment.getApplication(), true)
    }

    @Test @Config(qualifiers = PHONE) fun phoneScaffold() = shoot("phone-scaffold") { SampleScaffold() }
    @Test @Config(qualifiers = PHONE_NIGHT) fun phoneScaffoldNight() = shoot("phone-scaffold-night") { SampleScaffold() }
    @Test @Config(qualifiers = TABLET) fun tabletScaffold() = shoot("tablet-scaffold") { SampleScaffold() }

    @Test @Config(qualifiers = PHONE) fun phoneHowNoopWorks() = shoot("phone-how-noop-works") { HowNoopWorksScreen(onClose = {}) }
    @Test @Config(qualifiers = PHONE_NIGHT) fun phoneHowNoopWorksNight() = shoot("phone-how-noop-works-night") { HowNoopWorksScreen(onClose = {}) }
    @Test @Config(qualifiers = PHONE) fun phoneScoringGuide() = shoot("phone-scoring-guide") { ScoringGuideScreen(onClose = {}) }
    @Test @Config(qualifiers = PHONE_NIGHT) fun phoneModelComparisonNight() = shoot("phone-model-comparison-night") { WhoopModelComparisonScreen(onClose = {}) }
    @Test @Config(qualifiers = TABLET) fun tabletScoringGuide() = shoot("tablet-scoring-guide") { ScoringGuideScreen(onClose = {}) }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent { NoopTheme { content() } }
        // Let composition, layout and the one-shot sky render settle.
        repeat(30) { ShadowLooper.idleMainLooper(); Thread.sleep(50) }
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = checkNotNull(outDir).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Tear the activity down. Shots share one process, and a light activity left composing beside a
        // night one would make the two themes overwrite the shared Palette in turn, forever.
        controller.pause().stop().destroy()
    }

    @Composable private fun SampleScaffold() {
        LazyScreenScaffold(title = "Backup & Sync", subtitle = "A standard screen over the wallpaper") {
            item { NoopCard { Text("Backup folder") } }
            item { NoopCard { Text("Daily auto-backup") } }
        }
    }

    private companion object {
        const val PHONE = "w411dp-h891dp-port-xhdpi"
        const val PHONE_NIGHT = "w411dp-h891dp-port-night-xhdpi"
        const val TABLET = "w1280dp-h800dp-land-mdpi"
    }
}
