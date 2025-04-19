package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonState
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestEnvironment
import io.orangebuffalo.simpleaccounting.tests.infra.utils.StopWatch
import mu.KotlinLogging
import org.junit.jupiter.api.fail
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists

private val logger = KotlinLogging.logger { }

private val committedScreenshotsDir: Path = Path.of("src/test/resources/__rendering__")
private val generatedScreenshotsDir = Path.of("build/rendering-tests/")

fun Locator.assertRendering(name: String) {
    val usingLocalBrowser = TestEnvironment.config.fullStackTestsConfig.useLocalBrowser
    if (usingLocalBrowser) {
        logger.warn { "Currently running with local browser. Screenshot will be processes, but not asserted" }
    }

    val shouldUpdateScreenshots = !usingLocalBrowser && TestEnvironment.config.screenshots.replaceCommittedFiles
    val shouldFailOnDiff = !usingLocalBrowser && TestEnvironment.config.screenshots.failOnDiff

    logger.debug { "Testing rendering of [$name]:" }
    val stopWatch = StopWatch()

    val generatedScreenshot = this.screenshot(
        Locator.ScreenshotOptions()
            .setCaret(ScreenshotCaret.HIDE)
            .setType(ScreenshotType.PNG)
            .setAnimations(ScreenshotAnimations.DISABLED)
    )
    logger.debug { "    - screenshot: ${stopWatch.tick()}ms" }

    val generatedScreenshotFile = generatedScreenshotsDir.resolve("$name.png")
    Files.createDirectories(generatedScreenshotFile.parent)
    Files.write(generatedScreenshotFile, generatedScreenshot)
    logger.debug { "    - saved: ${stopWatch.tick()}ms" }

    val committedScreenshotFile = committedScreenshotsDir.resolve("$name.png")
    if (committedScreenshotFile.exists()) {
        val committedScreenshot = ImageIO.read(committedScreenshotFile.toFile())
        val generatedScreenshotImage = ImageIO.read(ByteArrayInputStream(generatedScreenshot))
        logger.debug { "    - loaded committed screenshot: ${stopWatch.tick()}ms" }

        val imageComparison = ImageComparison(committedScreenshot, generatedScreenshotImage).compareImages()
        logger.debug { "    - compared screenshots ${stopWatch.tick()}ms" }

        if (imageComparison!!.imageComparisonState != ImageComparisonState.MATCH) {
            logger.debug { "    - screenshots differ by ${imageComparison.differencePercent}%" }
            if (shouldUpdateScreenshots) {
                Files.write(committedScreenshotFile, generatedScreenshot)
                logger.debug { "    - updated committed screenshot: ${stopWatch.tick()}ms" }
            }

            val diffFile = generatedScreenshotsDir.resolve("${name}__diff.png")
            ImageIO.write(imageComparison.result, "png", diffFile.toFile())
            logger.debug { "    - saved diff: ${stopWatch.tick()}ms" }

            if (shouldFailOnDiff) {
                fail { "Screenshots differ by ${imageComparison.differencePercent}%" }
            }
        } else {
            logger.debug { "    - screenshots match" }
        }
    } else {
        if (shouldFailOnDiff) {
            fail { "$name committed screenshot not found" }
        } else {
            logger.warn { "Committed screenshot not found" }
        }
    }
}
