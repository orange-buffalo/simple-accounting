package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner
import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonState
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.file.shouldExist
import io.orangebuffalo.simpleaccounting.utils.StopWatch
import io.orangebuffalo.simpleaccounting.utils.logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.JavascriptExecutor
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO

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

        val allScreenshotsTaken = AtomicBoolean(false)
        val screenshotsTaken = LinkedBlockingQueue<Pair<StorybookStory, BufferedImage>>()

        // offload comparison to the background thread to improve test performance
        // concurrent execution of screenshots by Selenium is extremely unstable, so we only do it in a single thread
        val comparisonWorker = Executors.newSingleThreadExecutor()
        comparisonWorker.submit {
            while (true) {
                val storyAndScreenshot = screenshotsTaken.poll(100, TimeUnit.MILLISECONDS)
                    ?: if (allScreenshotsTaken.get()) return@submit else continue

                val stopWatch = StopWatch()
                val (story, generatedScreenshot) = storyAndScreenshot

                val committedScreenshotFile = File(committedScreenshotsDir, story.screenshotFileName())
                val generatedScreenshotFile = File(generatedScreenshotsDirectory, story.screenshotFileName())
                if (newStories.contains(story.screenshotFileName())) {
                    generatedScreenshot.write(generatedScreenshotFile)
                    logger.info { "Saved new story screenshot ${stopWatch.tick()}ms" }
                    if (env.shouldUpdateCommittedScreenshots) {
                        generatedScreenshot.write(committedScreenshotFile)
                        logger.info { "Updated committed file ${stopWatch.tick()}ms" }
                    }
                } else {
                    val committedScreenshot = ImageIO.read(committedScreenshotFile)
                    logger.info { "Loaded committed screenshot ${stopWatch.tick()}ms" }

                    val imageComparison = ImageComparison(committedScreenshot, generatedScreenshot).compareImages()
                    logger.info { "Compared screenshots ${stopWatch.tick()}ms" }

                    if (imageComparison.imageComparisonState != ImageComparisonState.MATCH) {
                        logger.info { "Screenshots differ by ${imageComparison.differencePercent}%" }

                        failedScreenshots.add(story.screenshotFileName())
                        imageComparison.result.write(File(diffDirectory, "${story.id}-diff.png"))
                        logger.info { "Saved diff ${stopWatch.tick()}ms" }

                        generatedScreenshot.write(generatedScreenshotFile)
                        logger.info { "Saved new screenshot ${stopWatch.tick()}ms" }

                        if (env.shouldUpdateCommittedScreenshots) {
                            generatedScreenshot.write(committedScreenshotFile)
                            logger.info { "Updated committed screenshot ${stopWatch.tick()}ms" }
                        }
                    }
                }
            }
        }

        env.stories.forEach { story ->
            logger.info { "Processing story $story" }
            val stopWatch = StopWatch()

            Selenide.open(story.storybookUrl())

            notifyStorybookAboutScreenshotPreparation()
            Selenide.Wait().until {
                isStorybookReadyForTakingScreenshot() ?: false
            }

            logger.info { "Story was loaded ${stopWatch.tick()}ms" }

            val generatedScreenshot = Screenshoter().makeScreenshot()

            logger.info { "Screenshot taken ${stopWatch.tick()}ms" }

            screenshotsTaken.offer(story to generatedScreenshot)
        }
        allScreenshotsTaken.set(true)
        comparisonWorker.shutdown()
        comparisonWorker.awaitTermination(10, TimeUnit.MINUTES)

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

private fun notifyStorybookAboutScreenshotPreparation() {
    //language=JavaScript
    jsExecutor().executeScript(
        """
          window.saScreenshotRequired = true;
        """
    )
}

private fun isStorybookReadyForTakingScreenshot(): Boolean? {
    //language=JavaScript
    return jsExecutor().executeScript(
        """
          return window.saReadyForScreenshot
        """
    ) as Boolean?
}

private fun jsExecutor() = WebDriverRunner.getWebDriver() as JavascriptExecutor
