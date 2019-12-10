package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.QPlatformUser
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/users")
class UsersApiController(
    private val userService: PlatformUserService,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    @PageableApi(UserPageableApiDescriptor::class)
    suspend fun getUsers(pageRequest: ApiPageRequest): Page<PlatformUser> = userService.getUsers(pageRequest.page)

    @PostMapping
    suspend fun createUser(@RequestBody @Valid user: CreateUserDto): UserDto = userService
        .save(
            PlatformUser(
                userName = user.userName!!,
                passwordHash = passwordEncoder.encode(user.password!!),
                isAdmin = user.admin!!
            )
        )
        .let(::mapUserDto)
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

@Component
class UserPageableApiDescriptor : PageableApiDescriptor<PlatformUser, QPlatformUser> {
    override suspend fun mapEntityToDto(entity: PlatformUser) =
        mapUserDto(entity)
}

private fun mapUserDto(entity: PlatformUser): UserDto {
    return UserDto(
        userName = entity.userName,
        id = entity.id,
        version = entity.version,
        admin = entity.isAdmin
    )
}
