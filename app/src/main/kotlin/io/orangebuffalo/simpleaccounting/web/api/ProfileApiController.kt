package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUserService
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.services.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.services.security.authentication.PasswordChangeException
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.DefaultErrorHandler
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.HandleApiErrorsWith
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.SaApiErrorDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/profile")
class ProfileApiController(
    private val platformUserService: PlatformUserService,
    private val documentsService: DocumentsService,
    private val authenticationService: AuthenticationService,
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

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @HandleApiErrorsWith(ProfileApiBadRequestErrorHandler::class)
    suspend fun changePassword(
        @RequestBody @Valid request: ChangePasswordRequestDto
    ) {
        authenticationService.changeCurrentUserPassword(request.currentPassword, request.newPassword)
    }
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

data class ChangePasswordRequestDto(
    @field:NotNull @field:NotEmpty val currentPassword: String,
    @field:NotNull @field:NotEmpty val newPassword: String,
)

class ProfileApiBadRequestErrorHandler : DefaultErrorHandler<ProfileApiErrors, ProfileApiBadRequestErrors>(
    responseType = ProfileApiBadRequestErrors::class,
    exceptionMappings = mapOf(
        PasswordChangeException.InvalidCurrentPasswordException::class to ProfileApiErrors.CurrentPasswordMismatch,
        PasswordChangeException.TransientUserException::class to ProfileApiErrors.TransientUser,
        PasswordChangeException.UserNotAuthenticatedException::class to ProfileApiErrors.NotAuthenticated,
    )
)

class ProfileApiBadRequestErrors(error: ProfileApiErrors, message: String?) :
    SaApiErrorDto<ProfileApiErrors>(error, message)

enum class ProfileApiErrors {
    CurrentPasswordMismatch,
    TransientUser,
    NotAuthenticated,
}
