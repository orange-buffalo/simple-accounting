package io.orangebuffalo.simpleaccounting.business.common.pesistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

private val stickyHashRegistry: AtomicLong = AtomicLong()

abstract class AbstractEntity {

    @Id
    var id: Long? = null

    @Version
    var version: Int? = null

    var createdAt: Instant? = null

    @delegate:Transient
    private val stickyHash: Long by lazy(LazyThreadSafetyMode.NONE) { id ?: stickyHashRegistry.incrementAndGet() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AbstractEntity
        return stickyHash == other.stickyHash
    }

    override fun hashCode(): Int {
        return stickyHash.hashCode()
    }

    override fun toString(): String {
        return "[${this.javaClass.simpleName}#${this.id}/v${this.version}]"
    }
}
