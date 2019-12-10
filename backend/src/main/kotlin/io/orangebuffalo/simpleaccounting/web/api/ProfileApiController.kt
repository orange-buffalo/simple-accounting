package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
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
        }
        .let { platformUserService.save(it) }
        .let { mapToProfileDto(it) }
}

private fun mapToProfileDto(currentUser: PlatformUser): ProfileDto =
    ProfileDto(
        userName = currentUser.userName,
        documentsStorage = currentUser.documentsStorage
    )

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDto(
    val userName: String,
    val documentsStorage: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateProfileRequestDto(
    @field:Size(max = 255) val documentsStorage: String?
)
