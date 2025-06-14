package io.orangebuffalo.simpleaccounting.tests.ui

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ScreenshotAnimations
import com.microsoft.playwright.options.ScreenshotCaret
import com.microsoft.playwright.options.ScreenshotType
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.tests.infra.ui.StorybookEnvironment
import io.orangebuffalo.simpleaccounting.tests.infra.ui.StorybookStory
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Select
import io.orangebuffalo.simpleaccounting.tests.infra.utils.StopWatch
import io.orangebuffalo.simpleaccounting.tests.infra.utils.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File

class UiComponentsScreenshotsTest {

    companion object {
        @AfterAll
        fun cleanup() {
            StorybookEnvironment.cleanup()
        }
    }

    @Test
    fun `all preparations must link to existing stories`() {
        storiesPreparations.keys.forEach { storyId ->
            if (StorybookEnvironment.stories.firstOrNull { story -> story.id == storyId } == null) {
                throw IllegalStateException("Story $storyId does not exist, but preparation is configured")
            }
        }
    }

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    fun `should have all stories screenshots valid`(): List<DynamicTest> {
        return StorybookEnvironment.stories.map { story ->
            DynamicTest.dynamicTest("${story.title}/${story.name} (id=${story.id})") {
                renderStory(story)
            }
        }
    }

    private fun renderStory(
        story: StorybookStory
    ) {
        logger.info { "Processing story $story" }
        val stopWatch = StopWatch()

        val generatedScreenshotsBaseDirectory = File("build/rendering-report/")
        generatedScreenshotsBaseDirectory.mkdirs()
        val generatedScreenshotFile = File(generatedScreenshotsBaseDirectory, story.screenshotFileName())
        val page = StorybookEnvironment.page

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

        val generatedScreenshot = page.screenshot(
            Page.ScreenshotOptions()
                .setFullPage(true)
                .setCaret(ScreenshotCaret.HIDE)
                .setType(ScreenshotType.PNG)
                .setAnimations(ScreenshotAnimations.DISABLED)
        )
        logger.info { "Screenshot taken ${stopWatch.tick()}ms" }

        generatedScreenshotFile.writeBytes(generatedScreenshot)
        logger.info { "Saved new story screenshot ${stopWatch.tick()}ms" }
    }
}

private fun StorybookStory.screenshotFileName() = "$id.png"
private fun StorybookStory.storybookUrl() = "iframe.html?id=${id}&viewMode=story"

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
