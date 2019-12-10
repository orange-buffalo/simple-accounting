package io.orangebuffalo.simpleaccounting.services.persistence

import com.querydsl.core.types.dsl.PathBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class QueryDslExtensionsTest {

    @Test
    fun `should convert query dsl order specifier to spring data sort`() {
        val pathBuilder = PathBuilder(QueryDslExtensionsTest::class.java, "testEntity")
        val entityPath = pathBuilder.getString("testField")

        val sort = entityPath.desc().toSort()

        assertThat(sort)
            .hasSize(1)
            .allSatisfy {
                assertThat(it.property).isEqualTo("testField")
                assertThat(it.isDescending).isTrue()
            }
    }

}
