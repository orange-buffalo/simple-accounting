package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
class AuthenticationController {

    @PostMapping("login", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun login(@RequestBody loginRequest: LoginRequest): LoginResponse {
        return LoginResponse(token = "")
    }


}