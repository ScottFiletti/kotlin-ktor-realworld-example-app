package io.realworld.app.web.controllers

import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// All routes live under /api (RealWorld spec). The old unprefixed paths were removed on purpose.
class ApiPrefixTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `routes are served under the api prefix`() {
        val response = appRule.http.get<String>("/api/tags")

        assertEquals(HttpStatus.SC_OK, response.status)
    }

    @Test
    fun `unprefixed paths are no longer served`() {
        val tags = appRule.http.get<String>("/tags")
        val register = appRule.http.post<String>(
            "/users", UserDTO(User(email = "no_prefix@valid_email.com", password = "Test", username = "no_prefix"))
        )

        assertEquals(HttpStatus.SC_NOT_FOUND, tags.status)
        assertEquals(HttpStatus.SC_NOT_FOUND, register.status)
    }
}
