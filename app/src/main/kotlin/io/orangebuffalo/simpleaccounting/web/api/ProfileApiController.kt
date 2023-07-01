package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping("/api/profile")
class ProfileApiController(
    private val platformUserService: PlatformUserService,
    private val documentsService: DocumentsService
) {
    @GetMapping
    suspend fun getProfile(): ProfileDto = platformUserService
        .getCurrentUser()
        .mapToProfileDto()

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
        .mapToProfileDto()

    @GetMapping("/documents-storage")
    suspend fun getDocumentsStorageStatus() = documentsService.getCurrentUserStorageStatus()
}

private fun PlatformUser.mapToProfileDto() = ProfileDto(
    userName = userName,
    documentsStorage = documentsStorage,
    i18n = I18nSettingsDto(
        locale = i18nSettings.locale,
        language = i18nSettings.language
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
