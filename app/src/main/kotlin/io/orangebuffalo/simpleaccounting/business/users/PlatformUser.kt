package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
data class PlatformUser(
    val userName: String,
    val passwordHash: String,
    val isAdmin: Boolean,
    val activated: Boolean,
    val documentsStorage: String? = null,

    @field:Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val loginStatistics: LoginStatistics = LoginStatistics(0, null),

    @field:Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val i18nSettings: I18nSettings = I18nSettings(
        locale = "en_AU",
        language = "en"
    ),
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

data class LoginStatistics(
    val failedAttemptsCount: Int,
    val temporaryLockExpirationTime: Instant?
)

data class I18nSettings(
    val locale: String,
    val language: String
)
