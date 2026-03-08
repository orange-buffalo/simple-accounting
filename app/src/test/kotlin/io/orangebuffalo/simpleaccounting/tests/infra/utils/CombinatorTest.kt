package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

internal class CombinatorTest {

    private val consumer = CombinationConsumer()

    @Test
    fun `should not fail on empty input`() {
        combine(emptyList(), consumer::consume)

        consumer.combinations.shouldBeEmpty()
    }

    @Test
    fun `should not fail on empty groups`() {
        combine(listOf(emptyList()), consumer::consume)

        consumer.combinations.shouldBeEmpty()
    }

    @Test
    fun `should support single group single element`() {
        combine(listOf(listOf("1")), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder("1")
    }

    @Test
    fun `should ignore empty groups`() {
        combine(listOf(listOf("1"), emptyList()), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder("1")
    }

    @Test
    fun `should support single group of multiple elements`() {
        combine(listOf(listOf("1", "2")), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder("1", "2")
    }

    @Test
    fun `should support multiple groups with single elements`() {
        combine(listOf(listOf("a"), listOf("1")), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder("1, a")
    }

    @Test
    fun `should support multiple groups, where one has single element`() {
        combine(listOf(listOf("a", "b"), listOf("1")), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder("1, a", "1, b")
    }

    @Test
    fun `should support multiple groups`() {
        combine(listOf(listOf("a", "b", "c"), listOf("1", "2"), listOf("@")), consumer::consume)

        consumer.combinations.shouldContainExactlyInAnyOrder(
            "1, @, a",
            "1, @, b",
            "1, @, c",

            "2, @, a",
            "2, @, b",
            "2, @, c"
        )
    }

    class CombinationConsumer {
        val combinations: MutableList<String> = mutableListOf()

        fun consume(combinationResult: List<String>) {
            combinations.add(combinationResult.sorted().joinToString(", "))
        }
    }
}
