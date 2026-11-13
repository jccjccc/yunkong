package com.dashabi.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dashabi.control.ui.screens.DesignPreview
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Render the actual components with offline fixtures; never needs a production key. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun devices_dark() = render("devices-dark")
    @Test fun devices_light() = render("devices-light", dark = false)
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun devices_large_font() = render("devices-font130-320", fontScale = 1.3f)
    @Test fun accounts_dark() = render("accounts-dark", accounts = true)
    @Test fun accounts_light() = render("accounts-light", accounts = true, dark = false)
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun compact_narrow() = render("accounts-compact-320", accounts = true, compact = true)
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun accounts_large_font() = render("accounts-font130-320", accounts = true, fontScale = 1.3f)

    private fun render(name: String, accounts: Boolean = false, dark: Boolean = true, compact: Boolean = false, fontScale: Float = 1f) {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density, fontScale)
            ) { DesignPreview(accounts, dark, compact) }
        }
        compose.waitForIdle()
        compose.onNodeWithText(if (accounts) "哈夫币合计" else "运行总览").assertExists()
        val target = File("build/design-previews/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
