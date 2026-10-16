package io.realworld.app.web.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.ObjectMapper
import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import java.util.concurrent.atomic.AtomicInteger

class HttpUtil(port: Int) {
    private val json = "application/json"
    val headers = mutableMapOf("Accept" to json, "Content-Type" to json)

    init {
        Unirest.setObjectMapper(object : ObjectMapper {
            override fun <T> readValue(value: String, valueType: Class<T>): T {
                return jacksonObjectMapper().readValue(value, valueType)
            }

            override fun writeValue(value: Any): String {
                return jacksonObjectMapper().writeValueAsString(value)
            }
        })
    }

    @JvmField
    val origin: String = "http://localhost:$port"

    inline fun <reified T> post(path: String) =
        Unirest.post(origin + path).headers(headers).asObject(T::class.java)

    inline fun <reified T> post(path: String, body: Any) =
        Unirest.post(origin + path).headers(headers).body(body).asObject(T::class.java)

    inline fun <reified T> get(path: String, params: Map<String, Any>? = null) =
        Unirest.get(origin + path).headers(headers).queryString(params).asObject(T::class.java)

    inline fun <reified T> put(path: String, body: Any) =
        Unirest.put(origin + path).headers(headers).body(body).asObject(T::class.java)

    inline fun <reified T> deleteWithResponseBody(path: String) =
        Unirest.delete(origin + path).headers(headers).asObject(T::class.java)

    fun delete(path: String) =
        Unirest.delete(origin + path).headers(headers).asString()

    fun loginAndSetTokenHeader(email: String, password: String) {
        val userDTO = UserDTO(User(email = email, password = password))
        val response = post<UserDTO>("/api/users/login", userDTO)
        headers["Authorization"] = "Token ${response.body.user?.token}"
    }

    fun registerUser(email: String, password: String, username: String): UserDTO {
        val userDTO = UserDTO(User(email = email, password = password, username = username))
        val response = post<UserDTO>("/api/users", userDTO)
        return response.body
    }

    // Defaults are unique per call: all tests share one in-memory database, so fixed emails/usernames
    // would collide ("Email already registered!") as soon as two tests create a user.
    fun createUser(userEmail: String = "${unique("user")}@valid_email.com", username: String = unique("user")): UserDTO {
        val password = "password"
        val user = registerUser(userEmail, password, username)
        loginAndSetTokenHeader(userEmail, password)
        return user
    }

    fun createArticle(article: Article): HttpResponse<ArticleDTO> {
        createUser()
        return post<ArticleDTO>("/api/articles", ArticleDTO(article))
    }

    // Creates an article as the currently logged-in user (call createUser() first).
    fun postArticle(
        title: String,
        body: String = "Some body.",
        description: String = "Some description.",
        tagList: List<String> = listOf()
    ): Article = post<ArticleDTO>(
        "/api/articles", ArticleDTO(Article(title = title, description = description, body = body, tagList = tagList))
    ).body.article!!

    companion object {
        private val counter = AtomicInteger()

        // A name no other test uses in this run, e.g. unique("srch") -> "srch17x". Letters and digits only,
        // so it is safe in emails, usernames, slugs, and search terms.
        fun unique(prefix: String) = "$prefix${counter.incrementAndGet()}x"
    }

    fun createArticle(): HttpResponse<ArticleDTO> {
        return createArticle(
            Article(
                title = "How to train your dragon",
                description = "Ever wonder how?",
                body = "Very carefully.",
                tagList = listOf("dragons", "training")
            )
        )
    }
}
