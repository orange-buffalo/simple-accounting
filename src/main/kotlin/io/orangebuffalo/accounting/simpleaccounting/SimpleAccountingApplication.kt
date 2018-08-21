package io.orangebuffalo.accounting.simpleaccounting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimpleAccountingApplication

fun main(args: Array<String>) {
    runApplication<SimpleAccountingApplication>(*args)
}
