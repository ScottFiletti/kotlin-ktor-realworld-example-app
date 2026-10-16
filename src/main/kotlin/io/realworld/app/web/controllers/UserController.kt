package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.response.respond
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.domain.UserUpdateDTO
import io.realworld.app.domain.service.UserService
import io.realworld.app.ext.receiveBody

class UserController(private val userService: UserService) {
    // Never send a password (plain or hashed) back to the client.
    private fun User.toResponse() = UserDTO(this.copy(password = null))

    suspend fun login(ctx: ApplicationCall) {
        ctx.receiveBody<UserDTO>().apply {
            userService.authenticate(this.validLogin()).apply {
                ctx.respond(this.toResponse())
            }
        }
    }

    suspend fun register(ctx: ApplicationCall) {
        ctx.receiveBody<UserDTO>().apply {
            userService.create(this.validRegister()).apply {
                ctx.respond(this.toResponse())
            }
        }
    }

    fun getUserByEmail(email: String?): User {
        return email.let {
            require(!it.isNullOrBlank()) { "User not logged or with invalid email." }
            userService.getByEmail(it)
        }
    }

    suspend fun getCurrent(ctx: ApplicationCall) {
        ctx.respond(ctx.authentication.principal<User>()!!.toResponse())
    }

    suspend fun update(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged." }
        ctx.receiveBody<UserUpdateDTO>().also { userDto ->
            userService.update(email, userDto.validToUpdate())!!.apply {
                ctx.respond(this.toResponse())
            }
        }
    }
}
