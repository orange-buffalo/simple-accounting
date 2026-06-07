package io.orangebuffalo.simpleaccounting.business.common.pesistence

import io.orangebuffalo.simpleaccounting.business.common.exceptions.SubmittedOutdatedStateException
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import java.time.Instant

abstract class AbstractEntity {

    @get:Id
    abstract val id: String?

    @get:Version
    abstract val version: Int?

    abstract val createdAt: Instant?

    fun validateVersion(submittedVersion: Int) {
        if (version != submittedVersion) {
            throw SubmittedOutdatedStateException(
                "Submitted version $submittedVersion does not match current version $version of ${javaClass.simpleName} $id"
            )
        }
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AbstractEntity
        return id != null && id == other.id
    }

    final override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }

    final override fun toString(): String {
        return "[${this.javaClass.simpleName}#${this.id}/v${this.version}]"
    }
}
