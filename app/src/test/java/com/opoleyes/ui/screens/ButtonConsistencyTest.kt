package com.opoleyes.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that GameOverScreen and ExamResultScreen use consistent button styling.
 * Since we can't run Compose UI tests in unit tests, we verify by source inspection
 * that both screens use GameButton with the same color scheme.
 */
class ButtonConsistencyTest {

    private fun findProjectRoot(): java.io.File {
        var dir = java.io.File(".").canonicalFile
        while (dir != null && !java.io.File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir ?: java.io.File(".")
    }

    private fun sourceFile(path: String): String {
        val root = findProjectRoot()
        val file = java.io.File(root, path)
        assertTrue("Source file should exist: ${file.absolutePath}", file.exists())
        return file.readText()
    }

    @Test
    fun gameOverScreen_usesGameButtonWithSuccessAndPrimaryColors() {
        val source = sourceFile("app/src/main/java/com/opoleyes/ui/screens/GameOverScreen.kt")

        assertTrue("GameOverScreen should use GameButton for retry",
            source.contains("GameButton("))
        assertTrue("GameOverScreen retry button should use Success color",
            source.contains("color1 = Success"))
        assertTrue("GameOverScreen retry button should use SuccessDark color",
            source.contains("color2 = SuccessDark"))
        assertTrue("GameOverScreen menu button should use Primary color",
            source.contains("color1 = Primary"))
        assertTrue("GameOverScreen menu button should use PurpleDark color",
            source.contains("color2 = PurpleDark"))
    }

    @Test
    fun examResultScreen_usesGameButtonWithSuccessAndPrimaryColors() {
        val source = sourceFile("app/src/main/java/com/opoleyes/ui/screens/ExamResultScreen.kt")

        assertTrue("ExamResultScreen should use GameButton for retry",
            source.contains("GameButton("))
        assertTrue("ExamResultScreen retry button should use Success color",
            source.contains("color1 = Success"))
        assertTrue("ExamResultScreen retry button should use SuccessDark color",
            source.contains("color2 = SuccessDark"))
        assertTrue("ExamResultScreen menu button should use Primary color",
            source.contains("color1 = Primary"))
        assertTrue("ExamResultScreen menu button should use PurpleDark color",
            source.contains("color2 = PurpleDark"))
    }

    @Test
    fun examResultScreen_doesNotUseOutlinedButtonOrAccentForRetry() {
        val source = sourceFile("app/src/main/java/com/opoleyes/ui/screens/ExamResultScreen.kt")

        // The old SimulacroActions used OutlinedButton and Accent - verify they're gone
        org.junit.Assert.assertFalse(
            "ExamResultScreen should not use OutlinedButton for navigation buttons",
            source.contains("OutlinedButton(")
        )
        org.junit.Assert.assertFalse(
            "ExamResultScreen should not use Accent color for retry button",
            source.contains("containerColor = Accent")
        )
    }

    @Test
    fun bothScreens_useSameButtonHeight() {
        val gameOverSource = sourceFile("app/src/main/java/com/opoleyes/ui/screens/GameOverScreen.kt")
        val examResultSource = sourceFile("app/src/main/java/com/opoleyes/ui/screens/ExamResultScreen.kt")

        assertTrue("GameOverScreen should use 50.dp height",
            gameOverSource.contains("height(50.dp)"))
        assertTrue("ExamResultScreen should use 50.dp height",
            examResultSource.contains("height(50.dp)"))
    }
}
