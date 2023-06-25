package io.orangebuffalo.simpleaccounting

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonState
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.file.shouldExist
import io.orangebuffalo.simpleaccounting.utils.StopWatch
import io.orangebuffalo.simpleaccounting.utils.logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

@ExtendWith(StorybookExtension::class)
class StorybookTests {

    @Test
    fun `should have all stories screenshots valid`(env: StorybookEnvironment) {
        val committedScreenshotsDir = File("src/storybookTest/resources/__images")
        committedScreenshotsDir.shouldExist()

        val committedScreenshots = getRelativeFilePathsByBaseDir(committedScreenshotsDir)
        val expectedScreenshots = env.stories.asSequence().map { it.screenshotFileName() }.toSet()

        val newStories = expectedScreenshots.subtract(committedScreenshots)
        val failedScreenshots = mutableListOf<String>()

        val (generatedScreenshotsDirectory, diffDirectory) = getGeneratedScreenshotsDirectories()

        // limiting the max number of threads - too many of them produce flaky results without significant boost in speed
        val worker = Executors.newFixedThreadPool(min(4, max(Runtime.getRuntime().availableProcessors() - 1, 1)))

        val futures = env.stories.map { story ->
            CompletableFuture.runAsync({
                logger.info { "Processing story $story" }
                val stopWatch = StopWatch()

                val page = env.page()
                page.navigate(story.storybookUrl())

                page.notifyStorybookAboutScreenshotPreparation()
                page.waitForCondition {
                    page.isStorybookReadyForTakingScreenshot() ?: false
                }

                logger.info { "Story was loaded ${stopWatch.tick()}ms" }

                val generatedScreenshot = page.screenshot(
                    Page.ScreenshotOptions()
                        .setFullPage(true)
                        .setCaret(ScreenshotCaret.HIDE)
                        .setType(ScreenshotType.PNG)
                        .setAnimations(ScreenshotAnimations.DISABLED)
                )

                logger.info { "Screenshot taken ${stopWatch.tick()}ms" }

                val committedScreenshotFile = File(committedScreenshotsDir, story.screenshotFileName())
                val generatedScreenshotFile = File(generatedScreenshotsDirectory, story.screenshotFileName())
                if (newStories.contains(story.screenshotFileName())) {
                    generatedScreenshotFile.writeBytes(generatedScreenshot)
                    logger.info { "Saved new story screenshot ${stopWatch.tick()}ms" }
                    if (env.shouldUpdateCommittedScreenshots) {
                        committedScreenshotFile.writeBytes(generatedScreenshot)
                        logger.info { "Updated committed file ${stopWatch.tick()}ms" }
                    }
                } else {
                    val committedScreenshot = ImageIO.read(committedScreenshotFile)
                    val generatedScreenshotImage = ImageIO.read(ByteArrayInputStream(generatedScreenshot))
                    logger.info { "Loaded committed screenshot ${stopWatch.tick()}ms" }

                    val imageComparison = ImageComparison(committedScreenshot, generatedScreenshotImage).compareImages()
                    logger.info { "Compared screenshots ${stopWatch.tick()}ms" }

                    if (imageComparison.imageComparisonState != ImageComparisonState.MATCH) {
                        logger.info { "Screenshots differ by ${imageComparison.differencePercent}%" }

                        failedScreenshots.add(story.screenshotFileName())
                        imageComparison.result.write(File(diffDirectory, "${story.id}-diff.png"))
                        logger.info { "Saved diff ${stopWatch.tick()}ms" }

                        generatedScreenshotFile.writeBytes(generatedScreenshot)
                        logger.info { "Saved new screenshot ${stopWatch.tick()}ms" }

                        if (env.shouldUpdateCommittedScreenshots) {
                            committedScreenshotFile.writeBytes(generatedScreenshot)
                            logger.info { "Updated committed screenshot ${stopWatch.tick()}ms" }
                        }
                    }
                }
            }, worker)
        }
        CompletableFuture.allOf(*futures.toTypedArray()).get(10, TimeUnit.MINUTES)
        worker.shutdown()

        val deletedStories = committedScreenshots.subtract(expectedScreenshots)
        if (env.shouldUpdateCommittedScreenshots) {
            logger.info { "Deleting non existing stories $deletedStories" }
            deletedStories.forEach { File(committedScreenshotsDir, it).delete() }
        }

        assertSoftly {
            withClue("All screenshots must be committed") {
                newStories.shouldBeEmpty()
            }

            withClue("All committed screenshots must have stories in storybook") {
                deletedStories.shouldBeEmpty()
            }

            withClue("All screenshots must be up to date") {
                failedScreenshots.shouldBeEmpty()
            }
        }
    }

    private fun getRelativeFilePathsByBaseDir(baseDir: File) = baseDir.walk()
        .filter { it.isFile }
        .map {
            it.relativeTo(baseDir).path
        }
        .toSet()

    private fun getGeneratedScreenshotsDirectories(): Pair<File, File> {
        val generatedScreenshotsBaseDirectory = File("build/storybookTest/")
        generatedScreenshotsBaseDirectory.mkdirs()

        val diffDirectory = File(generatedScreenshotsBaseDirectory, "diff")
        diffDirectory.mkdirs()

        val fullScreenshotsDirectory = File(generatedScreenshotsBaseDirectory, "screenshots")
        fullScreenshotsDirectory.mkdirs()

        return fullScreenshotsDirectory to diffDirectory
    }
}

private fun StorybookStory.screenshotFileName() = "$id.png"
private fun StorybookStory.storybookUrl() = "iframe.html?id=${id}&viewMode=story"
private fun BufferedImage.write(file: File) = ImageIO.write(this, "png", file)

private fun Page.notifyStorybookAboutScreenshotPreparation() {
    //language=JavaScript
    evaluate(
        """
          window.saScreenshotRequired = true;
        """
    )
}

private fun Page.isStorybookReadyForTakingScreenshot(): Boolean? {
    //language=JavaScript
    return evaluate(
        """
          window.saReadyForScreenshot
        """
    ) as Boolean?
}
