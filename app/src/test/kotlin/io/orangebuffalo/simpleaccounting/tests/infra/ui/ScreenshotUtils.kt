package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.orangebuffalo.simpleaccounting.tests.infra.utils.StopWatch
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

private val generatedScreenshotsDir = Path.of("build/rendering-report/")

/**
 * Reports rendering of the given [name] by taking a screenshot and saving to the output directory.
 *
 * We decided against screenshot tests, as it is too costly to make them right - animations and similar
 * features are asynchronous, and there is no guarantee that disabling/removing them from DOM will
 * re-render the component in exactly the same way on different runs.
 *
 * Thus, we only collect screenshots and then assess them manually before the release.
 */
fun Locator.reportRendering(name: String) {
    if (name.contains("/")) {
        throw IllegalArgumentException("Name [$name] must not contain slashes, as it is used to create a file path")
    }

    val stopWatch = StopWatch()

    val generatedScreenshot = this.screenshot(
        Locator.ScreenshotOptions()
            .setCaret(ScreenshotCaret.HIDE)
            .setType(ScreenshotType.PNG)
            .setAnimations(ScreenshotAnimations.DISABLED)
    )
    stopWatch.tick("screenshot")

    val generatedScreenshotFile = generatedScreenshotsDir.resolve("$name.png")
    Files.createDirectories(generatedScreenshotFile.parent)
    Files.deleteIfExists(generatedScreenshotFile)
    Files.write(generatedScreenshotFile, generatedScreenshot)
    stopWatch.tick("saving")

    logger.debug { "Recorded rendering of [$name]: ${stopWatch.log()}" }
}
