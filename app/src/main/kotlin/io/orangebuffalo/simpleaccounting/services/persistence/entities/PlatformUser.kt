package io.orangebuffalo.simpleaccounting.services.persistence.entities

import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class PlatformUser(
    var userName: String,
    var passwordHash: String,
    var isAdmin: Boolean,
    var documentsStorage: String? = null,

    @field:Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val loginStatistics: LoginStatistics = LoginStatistics(0, null),

    @field:Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val i18nSettings: I18nSettings = I18nSettings(
        locale = "en_AU",
        language = "en"
    )
) : AbstractEntity()

data class LoginStatistics(
    var failedAttemptsCount: Int,
    var temporaryLockExpirationTime: Instant?
) {

    fun reset() {
        failedAttemptsCount = 0
        temporaryLockExpirationTime = null
    }
}

data class I18nSettings(
    var locale: String,
    var language: String
)
