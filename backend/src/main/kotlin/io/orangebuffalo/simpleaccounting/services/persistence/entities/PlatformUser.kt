package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity

@Entity
class PlatformUser(
    @field:Column(nullable = false) var userName: String,
    @field:Column(nullable = false) var passwordHash: String,
    @field:Column(nullable = false) var isAdmin: Boolean,
    @field:Column var documentsStorage: String? = null,
    @field:Embedded val loginStatistics: LoginStatistics = LoginStatistics(0, null),
    @field:Embedded val i18nSettings: I18nSettings
) : LegacyAbstractEntity()

@Embeddable
data class LoginStatistics(
    @field:Column(nullable = false) var failedAttemptsCount: Int,
    @field:Column var temporaryLockExpirationTime: Instant?
) {

    fun reset() {
        failedAttemptsCount = 0
        temporaryLockExpirationTime = null
    }
}

@Embeddable
data class I18nSettings(
    @field:Column(nullable = false, length = 36) var locale: String,
    @field:Column(nullable = false, length = 36) var language: String
)
