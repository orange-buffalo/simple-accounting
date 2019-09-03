package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/profile")
class ProfileApiController(
    private val extensions: ApiControllersExtensions,
    private val platformUserService: PlatformUserService
) {
    @GetMapping
    fun getProfile(): Mono<ProfileDto> = extensions.toMono {
        val currentUser = platformUserService.getCurrentUser()
        mapToProfileDto(currentUser)
    }
}

private fun mapToProfileDto(currentUser: PlatformUser): ProfileDto = ProfileDto(
    userName = currentUser.userName,
    documentsStorage = currentUser.documentsStorage
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDto(
    val userName: String,
    val documentsStorage: String?
)
