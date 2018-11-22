package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtoMapperAdapter
import org.springframework.data.domain.Page
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import kotlin.reflect.KClass

@RestController
@RequestMapping("/api/v1/admin/users")
class UsersApiController(
    private val userService: PlatformUserService,
    private val userDtoMapper: UserDtoMapper,
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
            .map { userDtoMapper.map(it) }
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

class UserPageableApiDescriptor : PageableApiDescriptor {
    override val dtoClass: KClass<*>
        get() = UserDto::class
}

@Component
class UserDtoMapper
    : ApiDtoMapperAdapter<PlatformUser, UserDto>(PlatformUser::class.java, UserDto::class.java) {

    override fun map(source: PlatformUser): UserDto = UserDto(
        userName = source.userName,
        id = source.id,
        version = source.version,
        admin = source.isAdmin
    )
}