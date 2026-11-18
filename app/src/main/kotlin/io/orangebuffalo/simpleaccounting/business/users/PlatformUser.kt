package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.ZoneId

@Table
data class PlatformUser(
    val userName: String,
    val passwordHash: String,
    val isAdmin: Boolean,
    val activated: Boolean,
    val timeZone: String = "Australia/Melbourne",
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
) : AbstractEntity() {
    init {
        require(isSupportedTimeZone(timeZone)) { "Unsupported time zone" }
    }
}

data class LoginStatistics(
    val failedAttemptsCount: Int,
    val temporaryLockExpirationTime: Instant?
)

data class I18nSettings(
    val locale: String,
    val language: String,
)

// JVM-only aliases are not portable to browsers and PostgreSQL.
private val supportedTimeZones = ZoneId.getAvailableZoneIds().filterTo(destination = mutableSetOf("UTC")) {
    '/' in it && !it.startsWith("SystemV/")
}

fun isSupportedTimeZone(timeZone: String): Boolean = timeZone in supportedTimeZones
