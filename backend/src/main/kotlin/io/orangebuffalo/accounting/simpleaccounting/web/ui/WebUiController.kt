package io.orangebuffalo.accounting.simpleaccounting.web.ui

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class WebUiController {

    @RequestMapping("/admin/**", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun getAdminPage(): ClassPathResource {
        return ClassPathResource("META-INF/pages/admin/index.html")
    }

    @RequestMapping("/app/**", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun getAppPage(): ClassPathResource {
        return ClassPathResource("META-INF/pages/app/index.html")
    }
}
