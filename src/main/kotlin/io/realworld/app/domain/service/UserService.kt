package io.realworld.app.domain.service

import io.realworld.app.domain.Profile
import io.realworld.app.domain.User
import io.realworld.app.domain.UserUpdate
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.exceptions.UnauthorizedException
import io.realworld.app.domain.repository.UserRepository
import io.realworld.app.utils.Cipher
import io.realworld.app.utils.JwtProvider
import java.util.Base64

class UserService(private val jwtProvider: JwtProvider, private val userRepository: UserRepository) {
    private val base64Encoder = Base64.getEncoder()

    fun create(user: User): User {
        val newUser = user.copy(email = normalizeEmail(user.email))
        userRepository.findByEmail(newUser.email).apply {
            require(this == null) { "Email already registered!" }
        }
        userRepository.findByUsername(newUser.username!!).apply {
            require(this == null) { "Username already taken!" }
        }
        userRepository.create(newUser.copy(password = hashPassword(newUser.password)))
        return newUser.copy(token = generateJwtToken(newUser))
    }

    fun authenticate(user: User): User {
        val userFound = userRepository.findByEmail(normalizeEmail(user.email))
        if (userFound?.password == hashPassword(user.password)) {
            return userFound.copy(token = generateJwtToken(userFound))
        }
        throw UnauthorizedException("email or password invalid!")
    }

    fun getByEmail(email: String): User {
        val user = userRepository.findByEmail(normalizeEmail(email))
        user ?: throw NotFoundException("User not found to get.")
        return user.copy(token = generateJwtToken(user))
    }

    fun getProfileByUsername(email: String, usernameFollowing: String): Profile {
        return userRepository.findByUsername(usernameFollowing).let { user ->
            user ?: throw NotFoundException("User not found to find.")
            Profile(user.username, user.bio, user.image, userRepository.findIsFollowUser(email, user.id!!))
        }
    }

    fun update(email: String, changes: UserUpdate): User? {
        val current = userRepository.findByEmail(email) ?: throw NotFoundException("User not found to update.")
        val newEmail = changes.email?.let { normalizeEmail(it) } ?: current.email
        if (newEmail != current.email) {
            userRepository.findByEmail(newEmail).apply {
                require(this == null) { "Email already registered!" }
            }
        }
        if (changes.username != null && changes.username != current.username) {
            userRepository.findByUsername(changes.username).apply {
                require(this == null) { "Username already taken!" }
            }
        }
        // Only fields that were sent change; everything else keeps its current value.
        val merged = current.copy(
            email = newEmail,
            username = changes.username ?: current.username,
            // Hash a new password the same way as on register, otherwise login with it fails.
            password = changes.password?.let { hashPassword(it) } ?: current.password,
            bio = changes.bio ?: current.bio,
            image = changes.image ?: current.image
        )
        val updated = userRepository.update(email, merged)
        // The token carries the email claim, so issue a fresh one in case the email changed.
        return updated?.copy(token = generateJwtToken(updated))
    }

    private fun hashPassword(password: String?): String =
        String(base64Encoder.encode(Cipher.encrypt(password)))

    // Emails are case-insensitive: store and look them up in lowercase.
    private fun normalizeEmail(email: String): String = email.lowercase()

    private fun generateJwtToken(user: User): String? {
        return jwtProvider.createJWT(user)
    }

    fun follow(email: String, usernameToFollow: String): Profile {
        return userRepository.follow(email, usernameToFollow).let { user ->
            Profile(user.username, user.bio, user.image, true)
        }
    }

    fun unfollow(email: String, usernameToUnfollow: String): Profile {
        return userRepository.unfollow(email, usernameToUnfollow).let { user ->
            Profile(user.username, user.bio, user.image, false)
        }
    }
}
