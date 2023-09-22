package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilderLegacy
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UsersApiController(
    private val userService: PlatformUserService,
    private val authenticationService: AuthenticationService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilderLegacy
) {

    @GetMapping
    suspend fun getUsers(): ApiPage<UserDto> = filteringApiExecutor.executeFiltering()

    @PostMapping
    suspend fun createUser(@RequestBody @Valid user: CreateUserDto): UserDto = userService
        .save(
            PlatformUser(
                userName = user.userName!!,
                passwordHash = "",
                isAdmin = user.admin!!,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en")
            ).apply {
                authenticationService.setUserPassword(this, user.password!!)
            }
        )
        .mapToUserDto()

    private val filteringApiExecutor = filteringApiExecutorBuilder.executor<PlatformUser, UserDto> {
        query(Tables.PLATFORM_USER) {
            addDefaultSorting { root.id.desc() }
        }
        mapper { mapToUserDto() }
    }
}

data class UserDto(
    var userName: String,
    var id: Long?,
    var version: Int,
    var admin: Boolean
)

data class CreateUserDto(
    @field:NotBlank var userName: String?,
    @field:NotNull var admin: Boolean?,
    @field:NotBlank var password: String?
)

private fun PlatformUser.mapToUserDto() = UserDto(
    userName = userName,
    id = id,
    version = version!!,
    admin = isAdmin
)
