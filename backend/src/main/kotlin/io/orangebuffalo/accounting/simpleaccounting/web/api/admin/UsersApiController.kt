package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QPlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/v1/admin/users")
class UsersApiController(
    private val userService: PlatformUserService,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    @PageableApi(UserPageableApiDescriptor::class)
    fun getUsers(pageRequest: ApiPageRequest): Mono<Page<PlatformUser>> {
        return userService.getUsers(pageRequest.page)
    }

    @PostMapping
    fun createUser(@RequestBody @Valid user: Mono<CreateUserDto>): Mono<UserDto> {
        return user
            .map {
                PlatformUser(
                    userName = it.userName!!,
                    passwordHash = passwordEncoder.encode(it.password!!),
                    isAdmin = it.admin!!
                )
            }
            .flatMap { userService.save(it) }
            .map { mapUserDto(it) }
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

class UserPageableApiDescriptor : PageableApiDescriptor<PlatformUser, QPlatformUser> {
    override fun mapEntityToDto(entity: PlatformUser) = mapUserDto(entity)
}

private fun mapUserDto(entity: PlatformUser): UserDto {
    return UserDto(
        userName = entity.userName,
        id = entity.id,
        version = entity.version,
        admin = entity.isAdmin
    )
}
