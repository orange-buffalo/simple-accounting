package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.util.concurrent.atomic.AtomicLong
import javax.persistence.*

private val stickyHashRegistry: AtomicLong = AtomicLong()

@MappedSuperclass
abstract class AbstractEntity {

    @Id
    @GeneratedValue
    var id: Long? = null

    @Version
    var version: Int = -1

    @delegate:Transient
    private val stickyHash: Long by lazy(LazyThreadSafetyMode.NONE) { id ?: stickyHashRegistry.incrementAndGet() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractEntity

        if (stickyHash != other.stickyHash) return false

        return true
    }

    override fun hashCode(): Int {
        return stickyHash.hashCode()
    }

    override fun toString(): String {
        return "[${this.javaClass.simpleName}#${this.id}/v${this.version}]"
    }
}
