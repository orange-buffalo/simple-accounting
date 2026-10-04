package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.microsoft.playwright.Page
import com.microsoft.playwright.Route
import java.util.function.Consumer

fun Page.withFailedGqlApiResponse(operationName: String, spec: () -> Unit) {
    val handler = Consumer<Route> { route ->
        if (route.request().postData()?.contains("\"operationName\":\"$operationName\"") == true) {
            route.fulfill(Route.FulfillOptions().setStatus(200).setContentType("application/json")
                .setBody("""{"errors":[{"message":"Good news, everyone! The service is unavailable."}],"data":null}"""))
        } else {
            route.resume()
        }
    }
    context().route("/api/graphql", handler)
    try {
        spec()
    } finally {
        context().unroute("/api/graphql", handler)
    }
}
