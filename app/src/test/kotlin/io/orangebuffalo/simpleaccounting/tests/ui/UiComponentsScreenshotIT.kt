package io.orangebuffalo.simpleaccounting.tests.ui

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonResult
import com.github.romankh3.image.comparison.model.ImageComparisonState
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.StorybookEnvironment
import io.orangebuffalo.simpleaccounting.tests.infra.ui.StorybookExtension
import io.orangebuffalo.simpleaccounting.tests.infra.ui.StorybookStory
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Select
import io.orangebuffalo.simpleaccounting.tests.infra.utils.StopWatch
import io.orangebuffalo.simpleaccounting.tests.infra.utils.logger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.retry.backoff.NoBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

@ExtendWith(StorybookExtension::class)
class UiComponentsScreenshotsTest {

    @Test
    fun `all committed screenshots must have stories in storybook`(env: StorybookEnvironment) {
        val context = ScreenshotsContext(env)
        val deletedStories = context.committedScreenshots.subtract(context.expectedScreenshots)
        if (env.shouldUpdateCommittedScreenshots) {
            logger.info { "Deleting non existing stories $deletedStories" }
            deletedStories.forEach { File(context.committedScreenshotsDir, it).delete() }
        }
        deletedStories.shouldBeEmpty()
    }

    @Test
    fun `all stories in storybook must have committed screenshots`(env: StorybookEnvironment) {
        val context = ScreenshotsContext(env)
        val newStories = context.expectedScreenshots.subtract(context.committedScreenshots)
        newStories.shouldBeEmpty()
    }

    @Test
    fun `all preparations must link to existing stories`(env: StorybookEnvironment) {
        storiesPreparations.keys.forEach { storyId ->
            if (env.stories.firstOrNull { story -> story.id == storyId } == null) {
                throw IllegalStateException("Story $storyId does not exist, but preparation is configured")
            }
        }
    }

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    fun `should have all stories screenshots valid`(env: StorybookEnvironment): List<DynamicTest> {
        val context = ScreenshotsContext(env)
        return env.stories.map { story ->
            DynamicTest.dynamicTest("${story.title}/${story.name} (id=${story.id})") {
                executeStoryTest(story, context)
            }
        }
    }

    @Test
    @Disabled("Use this test to debug a particular story")
    fun `debug particular story`(env: StorybookEnvironment) {
        val storyId = "components-basic-sacurrencyinput--default"
        val context = ScreenshotsContext(env)
        val story = env.stories.find { it.id == storyId } ?: error("Story $storyId not found")
        env.forceLocalPlaywright()
        executeStoryTest(story, context)
    }

    private fun executeStoryTest(
        story: StorybookStory,
        context: ScreenshotsContext,
    ) {
        logger.info { "Processing story $story" }
        val stopWatch = StopWatch()

        // we need to retry because rendering is flaky, especially when it comes to rendering fonts
        val storyPageRetryTemplate = RetryTemplate().also {
            it.setRetryPolicy(SimpleRetryPolicy(5))
            it.setBackOffPolicy(NoBackOffPolicy())
        }

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
                // the outer retry is restarting the whole page, to cover flaky browser startup issues
                storyPageRetryTemplate.execute<Unit, ScreenshotRetryException> { storyPageRetry ->
                    if (storyPageRetry.retryCount > 0) {
                        logger.warn { "Retrying page load ${storyPageRetry.retryCount} time" }
                    }

                    page.navigate(story.storybookUrl())

                    val preparations = storiesPreparations[story.id]
                    if (preparations == null) {
                        logger.info { "Using legacy TS preparations" }
                        page.notifyStorybookAboutScreenshotPreparation()
                        page.waitForCondition {
                            page.isStorybookReadyForTakingScreenshot() ?: false
                        }
                    } else {
                        logger.info { "Using Playwright preparations" }
                        preparations(page)
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

            imageComparison!!.result.write(File(context.diffDirectory, "${story.id}-diff.png"))
            logger.info { "Saved diff ${stopWatch.tick()}ms" }

            generatedScreenshotFile.writeBytes(generatedScreenshot!!)
            logger.info { "Saved new screenshot ${stopWatch.tick()}ms" }

            if (context.env.shouldUpdateCommittedScreenshots) {
                committedScreenshotFile.writeBytes(generatedScreenshot!!)
                logger.info { "Updated committed screenshot ${stopWatch.tick()}ms" }
            }

            fail("${story.name} has failed screenshot comparison")
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
        val diffDirectory: File
        val committedScreenshots: Set<String>
        val expectedScreenshots: Set<String>

        init {
            committedScreenshotsDir.shouldExist()

            this.committedScreenshots = getRelativeFilePathsByBaseDir(committedScreenshotsDir)
            this.expectedScreenshots = env.stories.asSequence().map { it.screenshotFileName() }.toSet()

            this.newStories = expectedScreenshots.subtract(committedScreenshots)

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

private val storiesPreparations = mapOf<String, (Page) -> Unit>(
    "components-basic-saentityselect--default" to {
        Select.byContainer(it.locator("#screenshotTarget")).shouldHaveOptions { actualOptions ->
            actualOptions.shouldContain("Entity 0")
        }
    },

    "components-domain-category-sacategoryinput--default" to {
        Select.byContainer(it.locator("#preset-select")).shouldBeVisible()
        Select.byContainer(it.locator("#initially-empty-select")).shouldHaveOptions { actualOptions ->
            actualOptions.shouldContain("Slurm")
        }
    },

    "components-domain-customer-sacustomerinput--default" to {
        Select.byContainer(it.locator("#preset-select")).shouldBeVisible()
        Select.byContainer(it.locator("#initially-empty-select")).shouldHaveOptions { actualOptions ->
            actualOptions.shouldContain("Democratic Order of Planets")
        }
    },

    "components-domain-generaltax-sageneraltaxinput--default" to {
        Select.byContainer(it.locator("#preset-select")).shouldBeVisible()
        Select.byContainer(it.locator("#initially-empty-select")).shouldHaveOptions { actualOptions ->
            actualOptions.shouldContain("Planet Express Tax")
        }
    },

    "components-domain-sainvoiceselect--default" to {
        Select.byContainer(it.locator("#screenshotTarget")).shouldHaveOptions { actualOptions ->
            actualOptions.firstOrNull { option ->
                option.contains("Invoice #0000")
            }.shouldNotBeNull()
        }
    },

    "components-basic-sacurrencyinput--default" to {
        Select.byContainer(it.locator("//h4[text()='Default']/following-sibling::*[contains(@class, 'el-select')]"))
            .shouldHaveGroupedOptions { actualOptions ->
                actualOptions.map { it.name }.shouldContain("Recently Used Currencies")
            }
    }
)
