package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QPlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
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
    private val passwordEncoder: PasswordEncoder,
    private val extensions: ApiControllersExtensions
) {

    @GetMapping
    @PageableApi(UserPageableApiDescriptor::class)
    fun getUsers(pageRequest: ApiPageRequest): Mono<Page<PlatformUser>> = extensions.toMono {
        userService.getUsers(pageRequest.page)
    }

    @PostMapping
    fun createUser(@RequestBody @Valid user: CreateUserDto): Mono<UserDto> = extensions.toMono {
        userService.save(
            PlatformUser(
                userName = user.userName!!,
                passwordHash = passwordEncoder.encode(user.password!!),
                isAdmin = user.admin!!
            )
        ).let(::mapUserDto)
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
