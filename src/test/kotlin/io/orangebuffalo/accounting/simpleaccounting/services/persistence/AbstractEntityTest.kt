package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AbstractEntityTest {

    @Test
    fun `New objects without IDs should not be equal`() {
        assertThat(TestEntity())
            .isNotEqualTo(TestEntity())
    }

    @Test
    fun `Hashcode should not be changed after ID assignment`() {
        val testEntity = TestEntity()
        val initialHashcode = testEntity.hashCode()

        testEntity.id = 42

        assertThat(testEntity.hashCode())
            .isEqualTo(initialHashcode)
    }

    @Test
    fun `Two objects with the same ID should be equal`() {
        val firstEntity = TestEntity()
        firstEntity.id = 42

        val secondEntity = TestEntity()
        secondEntity.id = 42

        assertThat(firstEntity)
            .isEqualTo(secondEntity)
    }

    @Test
    fun `Two entities with the same ID should have the same hashcode`() {
        val firstEntity = TestEntity()
        firstEntity.id = 42

        val secondEntity = TestEntity()
        secondEntity.id = 42

        assertThat(firstEntity.hashCode())
            .isEqualTo(secondEntity.hashCode())
    }
}

internal class TestEntity : AbstractEntity()