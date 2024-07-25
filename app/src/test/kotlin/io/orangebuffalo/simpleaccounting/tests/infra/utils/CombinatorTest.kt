package io.orangebuffalo.simpleaccounting.tests.infra.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CombinatorTest {

    private val consumer = CombinationConsumer()

    @Test
    fun `should not fail on empty input`() {
        combine(emptyList(), consumer::consume)

        assertThat(consumer.combinations).isEmpty()
    }

    @Test
    fun `should not fail on empty groups`() {
        combine(listOf(emptyList()), consumer::consume)

        assertThat(consumer.combinations).isEmpty()
    }

    @Test
    fun `should support single group single element`() {
        combine(listOf(listOf("1")), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder("1")
    }

    @Test
    fun `should ignore empty groups`() {
        combine(listOf(listOf("1"), emptyList()), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder("1")
    }

    @Test
    fun `should support single group of multiple elements`() {
        combine(listOf(listOf("1", "2")), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    fun `should support multiple groups with single elements`() {
        combine(listOf(listOf("a"), listOf("1")), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder("1, a")
    }

    @Test
    fun `should support multiple groups, where one has single element`() {
        combine(listOf(listOf("a", "b"), listOf("1")), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder("1, a", "1, b")
    }

    @Test
    fun `should support multiple groups`() {
        combine(listOf(listOf("a", "b", "c"), listOf("1", "2"), listOf("@")), consumer::consume)

        assertThat(consumer.combinations).containsExactlyInAnyOrder(
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
