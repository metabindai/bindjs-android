package ai.metabind.bindjs.preview

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.ThresholdValidator
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.security.MessageDigest

/**
 * One screenshot per component fixture, painted from its committed tree by the real
 * renderer on a fake runtime, and compared against `src/test/screenshots/<fixture>.png`.
 *
 * Chart fixtures are deliberately left out: Vico builds its chart model in a coroutine
 * after Compose has reported idle, so a chart may or may not have drawn by the time the
 * screenshot is taken. They stay in the preview app for eyeballing.
 *
 *     ./gradlew :preview:recordRoborazziDebug   # (re)write the goldens
 *     ./gradlew :preview:verifyRoborazziDebug   # fail on any difference
 *
 * A failed verify leaves `<fixture>_compare.png` under `build/outputs/roborazzi` with the
 * golden, the new image and their diff side by side.
 *
 * The device is pinned along with the SDK because the goldens are specific to both:
 * a Pixel 6a in portrait, the emulator the fixtures are eyeballed on.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class FixtureScreenshotTest(private val fixtureName: String) {

    @get:Rule
    val compose = createComposeRule()

    private val fixture = componentFixtures.first { it.name == fixtureName }

    @Before
    fun seedRemoteFonts() {
        RemoteFonts.seed(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun screenshot() {
        val tree = GsonProvider.get().fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)

        compose.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // The same white pane the preview app draws a fixture into.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        BindJSView(
                            jsRuntime = FakeJsRuntime(),
                            component = tree,
                            version = 1,
                            onUiEvent = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        compose.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$fixtureName.png",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    outputDirectoryPath = "build/outputs/roborazzi",
                    resultValidator = ThresholdValidator(0.001f),
                )
            )
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun fixtures(): List<Array<Any>> = componentFixtures.map { arrayOf<Any>(it.name) }
    }
}

/**
 * Puts the fonts the fixtures fetch by URL into the renderer's font cache before it
 * looks, so the CustomFont fixture paints its remote face offline and identically on
 * every run instead of showing the system face while a download that never happens is
 * awaited.
 *
 * The cache is `<cacheDir>/bindjs-fonts/<sha256 of url>.<ext>`, the naming
 * `RemoteFontCache` uses; it is internal to the library, so the rule is repeated here.
 */
private object RemoteFonts {
    private val fonts = mapOf(
        "https://raw.githubusercontent.com/google/fonts/main/ofl/pacifico/Pacifico-Regular.ttf"
            to "remote-fonts/Pacifico-Regular.ttf",
    )

    fun seed(context: Context) {
        val dir = File(context.cacheDir, "bindjs-fonts").apply { mkdirs() }
        fonts.forEach { (url, resource) ->
            val bytes = checkNotNull(javaClass.classLoader?.getResourceAsStream(resource)) {
                "missing test resource $resource"
            }.use { it.readBytes() }
            File(dir, cacheFileName(url)).writeBytes(bytes)
        }
    }

    private fun cacheFileName(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        val extension = url.substringBefore('?').substringBefore('#').substringAfterLast('.', "")
        return if (extension.isEmpty()) hash else "$hash.$extension"
    }
}
