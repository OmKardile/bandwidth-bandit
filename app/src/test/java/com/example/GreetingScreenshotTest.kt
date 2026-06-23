package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.foundation.layout.fillMaxSize
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { Greeting("Robolectric") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @androidx.compose.runtime.Composable
  fun Greeting(name: String) {
    androidx.compose.material3.Surface(
      color = androidx.compose.material3.MaterialTheme.colorScheme.background,
      modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
      androidx.compose.foundation.layout.Box(
        contentAlignment = androidx.compose.ui.Alignment.Center
      ) {
        androidx.compose.material3.Text(
          text = "Bandwidth Bandit: $name Screenshot Mock",
          style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
          )
        )
      }
    }
  }
}
