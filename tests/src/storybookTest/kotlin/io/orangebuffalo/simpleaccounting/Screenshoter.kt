package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.WebDriverRunner
import org.intellij.lang.annotations.Language
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.min

class Screenshoter {
    private val takesScreenshot = WebDriverRunner.getWebDriver() as TakesScreenshot
    private val jsExecutor = WebDriverRunner.getWebDriver() as JavascriptExecutor

    fun makeScreenshot(): BufferedImage {
        val (pageHeight, viewportHeight, viewportWidth) = getPageInfo()

        val finalImage = BufferedImage(viewportWidth, pageHeight, BufferedImage.TYPE_3BYTE_BGR)
        val graphics = finalImage.createGraphics()

        val scrollTimes = ceil(pageHeight / viewportHeight.toDouble()).toInt()
        repeat(scrollTimes) { currentScrollIteration ->
            val heightRecorded = viewportHeight * currentScrollIteration
            val heightToRecord = min(viewportHeight, pageHeight - heightRecorded)

            val newScrollPos = min(heightRecorded, pageHeight - viewportHeight)
            scrollVertically(newScrollPos)
            Selenide.Wait().until {
                getCurrentScrollY() == newScrollPos
            }

            val part = ImageIO.read(takesScreenshot.getScreenshotAs(OutputType.BYTES).inputStream())
            graphics.drawImage(
                part,
                0, heightRecorded,
                viewportWidth, heightRecorded + heightToRecord,
                0, viewportHeight - heightToRecord,
                viewportWidth, viewportHeight,
                null
            )
        }
        graphics.dispose()

        return finalImage
    }

    private fun getCurrentScrollY(): Int {
        @Language("JavaScript") val currentScrollY = jsExecutor
            .executeScript("return window.pageYOffset") as Number
        return currentScrollY.toInt()
    }

    private fun scrollVertically(scrollTo: Int) {
        jsExecutor.executeScript("scrollTo(0, arguments[0]); return [];", scrollTo)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPageInfo(): PageInfo {
        //language=JavaScript
        val values = jsExecutor.executeScript(
            """
            var body = document.body;
            var documentElement = document.documentElement;
            var pageHeight = Math.max(body.scrollHeight, body.offsetHeight, documentElement.clientHeight,
                documentElement.scrollHeight, documentElement.offsetHeight);
            var viewportHeight = window.innerHeight || documentElement.clientHeight|| body.clientHeight;
            return {
                "pageHeight": pageHeight, 
                "viewportHeight": viewportHeight,
                "viewportWidth": document.body.clientWidth
            }    
        """
        ) as Map<String, Number>

        return PageInfo(
            pageHeight = values["pageHeight"]?.toInt() ?: throw IllegalStateException(),
            viewportWidth = values["viewportWidth"]?.toInt() ?: throw IllegalStateException(),
            viewportHeight = values["viewportHeight"]?.toInt() ?: throw IllegalStateException(),
        )
    }

    private data class PageInfo(
        val pageHeight: Int,
        val viewportHeight: Int,
        val viewportWidth: Int,
    )
}
