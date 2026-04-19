package io.orangebuffalo.simpleaccounting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    // JOOQ brings dependencies that activate this autoconfig,
    // there is no way to disable it via properties
    exclude = [R2dbcAutoConfiguration::class]
)
class SimpleAccountingApplication

fun main(args: Array<String>) {
    runApplication<SimpleAccountingApplication>(*args)
}
