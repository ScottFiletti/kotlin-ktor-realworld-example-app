package io.realworld.app.web.controllers

import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.web.ErrorResponse
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class UserControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

//    @Test
//    fun `invalid login without pass valid body`() {
//        val response = appRule.http.post<ErrorResponse>(
//            "/api/users/login",
//            UserDTO()
//        )
//
//        assertEquals(response.status, HttpStatus.SC_UNPROCESSABLE_ENTITY)
//        assertTrue(response.body.errors["body"]!!.contains("can't be empty or invalid"))
//    }

    @Test
    fun `success login with email and password`() {
        val email = "success_login@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "success_login")
        val userDTO = UserDTO(User(email = email, password = password))
        val response = appRule.http.post<UserDTO>("/api/users/login", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.email, userDTO.user?.email)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `success register user`() {
        val userDTO = UserDTO(
            User(
                email = "success_register@valid_email.com", password = "Test", username =
                "success_register"
            )
        )
        val response = appRule.http.post<UserDTO>("/api/users", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.username, userDTO.user?.username)
        assertNull("password must not be returned", response.body.user?.password)
    }

    @Test
    fun `register with taken username returns 422`() {
        appRule.http.registerUser("taken_username_1@valid_email.com", "Test", "taken_username")
        val userDTO = UserDTO(User(email = "taken_username_2@valid_email.com", password = "Test", username = "taken_username"))
        val response = appRule.http.post<ErrorResponse>("/api/users", userDTO)

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertEquals(listOf("Username already taken!"), response.body.errors["body"])
    }

    @Test
    fun `register with username over the column limit returns 422`() {
        val tooLong = "u".repeat(User.USERNAME_MAX_LENGTH + 1)
        val response = appRule.http.post<ErrorResponse>(
            "/api/users", UserDTO(User(email = "too_long@valid_email.com", password = "Test", username = tooLong))
        )

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertEquals(listOf("Username must be at most 100 characters."), response.body.errors["body"])
    }

    @Test
    fun `register with username exactly at the column limit succeeds`() {
        val maxLength = "u".repeat(User.USERNAME_MAX_LENGTH)
        val response = appRule.http.post<UserDTO>(
            "/api/users", UserDTO(User(email = "max_length@valid_email.com", password = "Test", username = maxLength))
        )

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(maxLength, response.body.user?.username)
    }

    @Test
    fun `register without a body returns 422`() {
        val response = appRule.http.post<ErrorResponse>("/api/users")

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
    }

    @Test
    fun `get current user with an invalid token returns 401`() {
        appRule.http.headers["Authorization"] = "Token not-a-real-token"
        val response = appRule.http.get<String>("/api/user")

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `update password to blank returns 422 and keeps the old password`() {
        val email = "blank_password@valid_email.com"
        appRule.http.registerUser(email, "Test", "blank_password")
        appRule.http.loginAndSetTokenHeader(email, "Test")
        val response = appRule.http.put<ErrorResponse>("/api/user", UserDTO(User(email = email, password = "")))
        val login = appRule.http.post<UserDTO>("/api/users/login", UserDTO(User(email = email, password = "Test")))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertEquals(listOf("Password can't be blank."), response.body.errors["body"])
        assertEquals(HttpStatus.SC_OK, login.status)
    }

    @Test
    fun `update username to blank returns 422`() {
        val email = "blank_username@valid_email.com"
        appRule.http.registerUser(email, "Test", "blank_username")
        appRule.http.loginAndSetTokenHeader(email, "Test")
        val response = appRule.http.put<ErrorResponse>("/api/user", UserDTO(User(email = email, username = " ")))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertEquals(listOf("Username can't be blank."), response.body.errors["body"])
    }

    @Test
    fun `update only bio without email keeps other fields`() {
        val email = "partial_update@valid_email.com"
        appRule.http.registerUser(email, "Test", "partial_update")
        appRule.http.loginAndSetTokenHeader(email, "Test")
        val response = appRule.http.put<UserDTO>("/api/user", mapOf("user" to mapOf("bio" to "I like dragons")))

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals("I like dragons", response.body.user?.bio)
        assertEquals(email, response.body.user?.email)
        assertEquals("partial_update", response.body.user?.username)
    }

    @Test
    fun `email is case-insensitive for register and login`() {
        val registered = appRule.http.registerUser("Mixed_Case@Valid_Email.com", "Test", "mixed_case")
        val duplicate = appRule.http.post<ErrorResponse>(
            "/api/users", UserDTO(User(email = "mixed_case@valid_email.com", password = "Test", username = "mixed_case_2"))
        )
        val login = appRule.http.post<UserDTO>(
            "/api/users/login", UserDTO(User(email = "MIXED_CASE@VALID_EMAIL.COM", password = "Test"))
        )

        assertEquals("mixed_case@valid_email.com", registered.user?.email)
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, duplicate.status)
        assertEquals(listOf("Email already registered!"), duplicate.body.errors["body"])
        assertEquals(HttpStatus.SC_OK, login.status)
    }

    @Test
    fun `login with wrong password returns 401`() {
        appRule.http.registerUser("wrong_password@valid_email.com", "Test", "wrong_password")
        val userDTO = UserDTO(User(email = "wrong_password@valid_email.com", password = "WRONG"))
        val response = appRule.http.post<ErrorResponse>("/api/users/login", userDTO)

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `update password then login with new password`() {
        val email = "update_password@valid_email.com"
        appRule.http.registerUser(email, "OldPass", "update_password")
        appRule.http.loginAndSetTokenHeader(email, "OldPass")
        appRule.http.put<UserDTO>("/api/user", UserDTO(User(email = email, password = "NewPass")))

        val newLogin = appRule.http.post<UserDTO>("/api/users/login", UserDTO(User(email = email, password = "NewPass")))
        val oldLogin = appRule.http.post<ErrorResponse>("/api/users/login", UserDTO(User(email = email, password = "OldPass")))

        assertEquals(HttpStatus.SC_OK, newLogin.status)
        assertEquals(HttpStatus.SC_UNAUTHORIZED, oldLogin.status)
    }

    @Test
    fun `update email returns a new token that works`() {
        appRule.http.registerUser("change_email_old@valid_email.com", "Test", "change_email")
        appRule.http.loginAndSetTokenHeader("change_email_old@valid_email.com", "Test")
        val updated = appRule.http.put<UserDTO>("/api/user", UserDTO(User(email = "change_email_new@valid_email.com")))
        assertNotNull(updated.body.user?.token)

        appRule.http.headers["Authorization"] = "Token ${updated.body.user?.token}"
        val current = appRule.http.get<UserDTO>("/api/user")

        assertEquals(HttpStatus.SC_OK, current.status)
        assertEquals("change_email_new@valid_email.com", current.body.user?.email)
    }

//    @Test
//    fun `invalid get current user without token`() {
//        val response = appRule.http.get<ErrorResponse>("/api/user")
//
//        assertEquals(response.status, HttpStatus.SC_FORBIDDEN)
//    }

    @Test
    fun `get current user by token`() {
        val email = "get_current@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "get_current")
        appRule.http.loginAndSetTokenHeader(email, password)
        val response = appRule.http.get<UserDTO>("/api/user")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.user?.username)
        assertNull("password must not be returned", response.body.user?.password)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `update user data`() {
        val email = "update_email@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "update_email")

        appRule.http.loginAndSetTokenHeader("update_email@valid_email.com", "Test")
        val userDTO = UserDTO(User(email = "update_user@update_test.com", password = "Test"))
        val response = appRule.http.put<UserDTO>("/api/user", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.email, userDTO.user?.email)
    }
}
