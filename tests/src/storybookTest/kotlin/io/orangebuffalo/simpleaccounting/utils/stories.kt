package io.orangebuffalo.simpleaccounting.utils

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner
import org.openqa.selenium.JavascriptExecutor

fun jsExecutor() = WebDriverRunner.getWebDriver() as JavascriptExecutor

fun waitForStoryToBeVisible() {
    `$x`("//*[@id='root']/*").shouldBe(Condition.visible)
}

fun disableIconsSvgAnimations() {
    //language=JavaScript
    jsExecutor().executeScript(
        """
          const animations = document.getElementsByTagName('animateTransform');
          for (let animation of animations) { 
            animation.parentNode.removeChild(animation); 
          }
        """
    )
}
