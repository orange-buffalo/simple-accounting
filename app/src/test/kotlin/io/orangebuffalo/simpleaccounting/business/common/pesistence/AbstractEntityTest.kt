package io.orangebuffalo.simpleaccounting.business.common.pesistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.Instant

internal class AbstractEntityTest {

    @Test
    fun `new objects without IDs should not be equal`() {
        TestEntity().shouldNotBe(TestEntity())
    }

    @Test
    fun `copy with assigned ID should be equal to persisted entity`() {
        val testEntity = TestEntity()

        val persistedEntity = testEntity.copy(id = "42")

        persistedEntity.shouldBe(TestEntity(id = "42"))
    }

    @Test
    fun `two objects with the same ID should be equal`() {
        val firstEntity = TestEntity(id = "42")
        val secondEntity = TestEntity(id = "42")

        firstEntity.shouldBe(secondEntity)
    }

    @Test
    fun `two entities with the same ID should have the same hashcode`() {
        val firstEntity = TestEntity(id = "42")
        val secondEntity = TestEntity(id = "42")

        firstEntity.hashCode().shouldBe(secondEntity.hashCode())
    }
}

internal data class TestEntity(
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
