package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/profile")
class ProfileApiController(
    private val platformUserService: PlatformUserService
) {
    @GetMapping
    suspend fun getProfile(): ProfileDto {
        val currentUser = platformUserService.getCurrentUser()
        return mapToProfileDto(currentUser)
    }

    @PutMapping
    suspend fun updateProfile(
        @RequestBody @Valid request: UpdateProfileRequestDto
    ): ProfileDto = platformUserService
        .getCurrentUser()
        .apply {
            documentsStorage = request.documentsStorage
            i18nSettings.language = request.i18n.language
            i18nSettings.locale = request.i18n.locale
        }
        .let { platformUserService.save(it) }
        .let { mapToProfileDto(it) }
}

private fun mapToProfileDto(currentUser: PlatformUser): ProfileDto =
    ProfileDto(
        userName = currentUser.userName,
        documentsStorage = currentUser.documentsStorage,
        i18n = I18nSettingsDto(
            locale = currentUser.i18nSettings.locale,
            language = currentUser.i18nSettings.language
        )
    )

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDto(
    val userName: String,
    val documentsStorage: String?,
    val i18n: I18nSettingsDto
)

data class I18nSettingsDto(
    @field:NotBlank val locale: String,
    @field:NotBlank val language: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateProfileRequestDto(
    @field:Size(max = 255) val documentsStorage: String?,
    @field:Valid val i18n: I18nSettingsDto
)
