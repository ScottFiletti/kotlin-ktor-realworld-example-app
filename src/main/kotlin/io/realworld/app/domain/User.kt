package io.realworld.app.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.auth.Principal
import io.realworld.app.ext.isEmailValid

data class UserDTO(val user: User? = null) {
    fun validRegister(): User {
        requireNotNull(user) { "User is required." }
        require(user.email.isEmailValid()) { "Email is invalid." }
        require(!user.password.isNullOrBlank()) { "Password can't be blank." }
        require(!user.username.isNullOrBlank()) { "Username can't be blank." }
        user.requireValidLengths()
        return user
    }

    fun validLogin(): User {
        require(
            user != null &&
                user.email.isEmailValid() &&
                !user.password.isNullOrBlank()
        ) { "Email or password is invalid." }
        return user
    }

}

data class UserUpdateDTO(val user: UserUpdate? = null) {
    fun validToUpdate(): UserUpdate {
        // Every field is optional, but a field that is sent must be valid
        // (e.g. a blank password would lock the user out).
        requireNotNull(user) { "User is required." }
        require(user.email == null || user.email.isEmailValid()) { "Email is invalid." }
        require(user.password == null || user.password.isNotBlank()) { "Password can't be blank." }
        require(user.username == null || user.username.isNotBlank()) { "Username can't be blank." }
        User.requireValidLengths(user.email, user.username, user.bio, user.image)
        return user
    }
}

// Body of PUT /user. Per the RealWorld spec all fields are optional and only the ones sent are changed.
// id/token are ignored because clients commonly send back the user object they received.
@JsonIgnoreProperties("id", "token")
data class UserUpdate(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
)

data class User(
    val id: Long? = null,
    val email: String,
    val token: String? = null,
    val username: String? = null,
    // Accepted in requests; omitted from responses when null (controllers null it out before responding).
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
) : Principal {
    companion object {
        // Column sizes, shared with the Users table so validation and schema can't drift apart.
        const val EMAIL_MAX_LENGTH = 200
        const val USERNAME_MAX_LENGTH = 100
        const val BIO_MAX_LENGTH = 1000
        const val IMAGE_MAX_LENGTH = 255

        // Values longer than their column would otherwise fail in the database with a 500.
        fun requireValidLengths(email: String?, username: String?, bio: String?, image: String?) {
            require((email?.length ?: 0) <= EMAIL_MAX_LENGTH) { "Email must be at most $EMAIL_MAX_LENGTH characters." }
            require((username?.length ?: 0) <= USERNAME_MAX_LENGTH) { "Username must be at most $USERNAME_MAX_LENGTH characters." }
            require((bio?.length ?: 0) <= BIO_MAX_LENGTH) { "Bio must be at most $BIO_MAX_LENGTH characters." }
            require((image?.length ?: 0) <= IMAGE_MAX_LENGTH) { "Image must be at most $IMAGE_MAX_LENGTH characters." }
        }
    }

    fun requireValidLengths() = requireValidLengths(email, username, bio, image)
}
