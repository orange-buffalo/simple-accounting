package io.orangebuffalo.accounting.simpleaccounting.web.ui

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class WebUiController {

    @RequestMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun getRootPage(): ClassPathResource = getIndexPage()

    @RequestMapping("/index.html", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun getIndexPage(): ClassPathResource = ClassPathResource("META-INF/resources/index.html")
}
