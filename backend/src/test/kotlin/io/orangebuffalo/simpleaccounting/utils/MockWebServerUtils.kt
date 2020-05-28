package io.orangebuffalo.simpleaccounting.utils

import okhttp3.mockwebserver.RecordedRequest
import org.springframework.util.StringUtils
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Parses request body assuming it is url-encoded-form-data and returns a list of pairs for the parameters in the body.
 */
fun getFormParameters(request: RecordedRequest): List<Pair<String, String?>> {
    val body = request.body.readUtf8()
    val charset = StandardCharsets.UTF_8.name()
    return StringUtils.tokenizeToStringArray(body, "&").map { pair ->
        val idx = pair.indexOf('=')
        if (idx == -1) {
            URLDecoder.decode(pair, charset) to null
        } else {
            val name = URLDecoder.decode(pair.substring(0, idx), charset)
            val value = URLDecoder.decode(pair.substring(idx + 1), charset)
            name to value
        }
    }
}
