package io.orangebuffalo.simpleaccounting.web.ui

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonResult
import com.github.romankh3.image.comparison.model.ImageComparisonState
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.file.shouldExist
import io.orangebuffalo.simpleaccounting.infra.ui.StorybookEnvironment
import io.orangebuffalo.simpleaccounting.infra.ui.StorybookExtension
import io.orangebuffalo.simpleaccounting.infra.ui.StorybookStory
import io.orangebuffalo.simpleaccounting.infra.utils.StopWatch
import io.orangebuffalo.simpleaccounting.infra.utils.logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.retry.backoff.NoBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.max

@ExtendWith(StorybookExtension::class)
class UiComponentsScreenshotsIT {

    // we need to retry because rendering is flaky, especially when it comes to rendering fonts

    // the outer retry is restarting the whole page, to cover flaky browser startup issues
    private val storyPageRetryTemplate = RetryTemplate().also {
        it.setRetryPolicy(SimpleRetryPolicy(5))
        it.setBackOffPolicy(NoBackOffPolicy())
    }

    @Test
    fun `should have all stories screenshots valid`(env: StorybookEnvironment) {
        val context = ScreenshotsContext(env)

        // limiting the max number of threads - too many of them produce flaky results without significant boost in speed
        val worker = Executors.newFixedThreadPool(max(Runtime.getRuntime().availableProcessors() - 1, 1))

        val futures = env.stories.map { story ->
            CompletableFuture.runAsync({
                executeStoryTest(story, context)
            }, worker)
        }
        CompletableFuture.allOf(*futures.toTypedArray()).get(10, TimeUnit.MINUTES)
        worker.shutdown()

        val deletedStories = context.committedScreenshots.subtract(context.expectedScreenshots)
        if (env.shouldUpdateCommittedScreenshots) {
            logger.info { "Deleting non existing stories $deletedStories" }
            deletedStories.forEach { File(context.committedScreenshotsDir, it).delete() }
        }

        assertSoftly {
            withClue("All screenshots must be committed") {
                context.newStories.shouldBeEmpty()
            }

            withClue("All committed screenshots must have stories in storybook") {
                deletedStories.shouldBeEmpty()
            }

            withClue("All screenshots must be up to date") {
                context.failedScreenshots.shouldBeEmpty()
            }
        }
    }

    private fun executeStoryTest(
        story: StorybookStory,
        context: ScreenshotsContext,
    ) {
        logger.info { "Processing story $story" }
        val stopWatch = StopWatch()

        val committedScreenshotFile = File(context.committedScreenshotsDir, story.screenshotFileName())
        val generatedScreenshotFile = File(context.generatedScreenshotsDirectory, story.screenshotFileName())
        var imageComparison: ImageComparisonResult? = null
        var generatedScreenshot: ByteArray? = null
        var browserConsoleLog = ""
        try {
            context.env.page().use { page ->
                page.onConsoleMessage { consoleMessage ->
                    browserConsoleLog += "${consoleMessage.type()}: ${consoleMessage.text()}\n"
                }
                storyPageRetryTemplate.execute<Unit, ScreenshotRetryException> { storyPageRetry ->
                    if (storyPageRetry.retryCount > 0) {
                        logger.warn { "Retrying page load ${storyPageRetry.retryCount} time" }
                    }

                    page.navigate(story.storybookUrl())

                    page.notifyStorybookAboutScreenshotPreparation()
                    page.waitForCondition {
                        page.isStorybookReadyForTakingScreenshot() ?: false
                    }

                    logger.info { "Story was loaded ${stopWatch.tick()}ms" }

                    generatedScreenshot = page.screenshot(
                        Page.ScreenshotOptions()
                            .setFullPage(true)
                            .setCaret(ScreenshotCaret.HIDE)
                            .setType(ScreenshotType.PNG)
                            .setAnimations(ScreenshotAnimations.DISABLED)
                    )
                    logger.info { "Screenshot taken ${stopWatch.tick()}ms" }

                    if (context.newStories.contains(story.screenshotFileName())) {
                        generatedScreenshotFile.writeBytes(generatedScreenshot!!)
                        logger.info { "Saved new story screenshot ${stopWatch.tick()}ms" }
                        if (context.env.shouldUpdateCommittedScreenshots) {
                            committedScreenshotFile.writeBytes(generatedScreenshot!!)
                            logger.info { "Updated committed file ${stopWatch.tick()}ms" }
                        }
                    } else {
                        val committedScreenshot = ImageIO.read(committedScreenshotFile)
                        val generatedScreenshotImage =
                            ImageIO.read(ByteArrayInputStream(generatedScreenshot))
                        logger.info { "Loaded committed screenshot ${stopWatch.tick()}ms" }

                        imageComparison =
                            ImageComparison(committedScreenshot, generatedScreenshotImage).compareImages()
                        logger.info { "Compared screenshots ${stopWatch.tick()}ms" }

                        if (imageComparison!!.imageComparisonState != ImageComparisonState.MATCH) {
                            logger.info { "Screenshots differ by ${imageComparison!!.differencePercent}%" }
                            throw ScreenshotRetryException()
                        } else {
                            logger.info { "Screenshots match" }
                        }
                    }
                }
            }
        } catch (e: ScreenshotRetryException) {
            logger.info { "Screenshot for story $story eventually does not match" }
            logger.info { "Browser console log:\n$browserConsoleLog" }

            context.failedScreenshots.add(story.screenshotFileName())
            imageComparison!!.result.write(File(context.diffDirectory, "${story.id}-diff.png"))
            logger.info { "Saved diff ${stopWatch.tick()}ms" }

            generatedScreenshotFile.writeBytes(generatedScreenshot!!)
            logger.info { "Saved new screenshot ${stopWatch.tick()}ms" }

            if (context.env.shouldUpdateCommittedScreenshots) {
                committedScreenshotFile.writeBytes(generatedScreenshot!!)
                logger.info { "Updated committed screenshot ${stopWatch.tick()}ms" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process story $story" }
            logger.error { "Browser console log:\n$browserConsoleLog" }
            throw IllegalStateException("Failed to process story $story", e)
        }
    }

    private fun getRelativeFilePathsByBaseDir(baseDir: File) = baseDir.walk()
        .filter { it.isFile }
        .map {
            it.relativeTo(baseDir).path
        }
        .toSet()

    private fun getGeneratedScreenshotsDirectories(): Pair<File, File> {
        val generatedScreenshotsBaseDirectory = File("build/screenshotsTest/")
        generatedScreenshotsBaseDirectory.mkdirs()

        val diffDirectory = File(generatedScreenshotsBaseDirectory, "diff")
        diffDirectory.mkdirs()

        val fullScreenshotsDirectory = File(generatedScreenshotsBaseDirectory, "screenshots")
        fullScreenshotsDirectory.mkdirs()

        return fullScreenshotsDirectory to diffDirectory
    }

    private inner class ScreenshotsContext(val env: StorybookEnvironment) {
        val committedScreenshotsDir: File = File("src/test/resources/__screenshots_test_images")
        val generatedScreenshotsDirectory: File
        val newStories: Set<String>
        val failedScreenshots: MutableList<String>
        val diffDirectory: File
        val committedScreenshots: Set<String>
        val expectedScreenshots: Set<String>

        init {
            committedScreenshotsDir.shouldExist()

            this.committedScreenshots = getRelativeFilePathsByBaseDir(committedScreenshotsDir)
            this.expectedScreenshots = env.stories.asSequence().map { it.screenshotFileName() }.toSet()

            this.newStories = expectedScreenshots.subtract(committedScreenshots)
            this.failedScreenshots = mutableListOf()

            val (generatedScreenshotsDirectory, diffDirectory) = getGeneratedScreenshotsDirectories()
            this.generatedScreenshotsDirectory = generatedScreenshotsDirectory
            this.diffDirectory = diffDirectory
        }
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

private class ScreenshotRetryException : RuntimeException()
