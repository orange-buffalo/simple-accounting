package io.orangebuffalo.simpleaccounting.tests.infra.utils

fun <T> combine(inputs: List<List<T>>, consumer: (List<T>) -> Unit) {
    val nonEmptyLists = inputs.filter { it.isNotEmpty() }
    if (nonEmptyLists.isEmpty()) return

    fun helper(index: Int): List<List<T>> {
        // Base case: if we've processed all lists, return a list with an empty list
        if (index == nonEmptyLists.size) return listOf(emptyList())

        val currentList = nonEmptyLists[index]
        val subResults = helper(index + 1)
        val result = mutableListOf<List<T>>()

        for (item in currentList) {
            for (subResult in subResults) {
                result.add(listOf(item) + subResult)
            }
        }

        return result
    }

    val cartesianProduct = helper(0)
    cartesianProduct.forEach { consumer(it) }
}
