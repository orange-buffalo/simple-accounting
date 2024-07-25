package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.google.common.collect.Lists

fun <T> combine(inputs: List<List<T>>, consumer: (List<T>) -> Unit) {
    val nonEmptyGroups = inputs.filter { it.isNotEmpty() }

    Lists.cartesianProduct(nonEmptyGroups).asSequence()
        .filter { it.isNotEmpty() }
        .forEach { consumer(it) }
}
