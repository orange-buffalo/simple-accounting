package io.orangebuffalo.simpleaccounting.business.users

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/profile")
class ProfileApi(
    private val platformUsersService: PlatformUsersService,
    private val documentsService: DocumentsService,
) {
    @GetMapping
    suspend fun getProfile(): ProfileDto = platformUsersService
        .getCurrentUser()
        .mapToProfileDto()

    @PutMapping
    suspend fun updateProfile(
        @RequestBody @Valid request: UpdateProfileRequestDto
    ): ProfileDto = platformUsersService
        .getCurrentUser()
        .apply {
            documentsStorage = request.documentsStorage
            i18nSettings.language = request.i18n.language
            i18nSettings.locale = request.i18n.locale
        }
        .let { platformUsersService.save(it) }
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
    @field:NotBlank @field:Size(max = 36) val locale: String,
    @field:NotBlank @field:Size(max = 36) val language: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateProfileRequestDto(
    @field:Size(max = 255) val documentsStorage: String?,
    @field:Valid val i18n: I18nSettingsDto
)
