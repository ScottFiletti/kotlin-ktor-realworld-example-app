package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall

// Not implemented yet: every endpoint answers 501 (see ErrorExceptionMapping). UserService already has
// getProfileByUsername/follow/unfollow; the commented code is the original author's sketch.
class ProfileController {
    //class ProfileController(private val userService: UserService) {
    fun get(ctx: ApplicationCall) {
        TODO("Profiles are not implemented yet.")
//            userService.getProfileByUsername(ctx.attribute("email")!!, usernameFollowing).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }

    fun follow(ctx: ApplicationCall) {
        TODO("Following users is not implemented yet.")
//            userService.follow(ctx.attribute("email")!!, usernameToFollow).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }

    fun unfollow(ctx: ApplicationCall) {
        TODO("Unfollowing users is not implemented yet.")
//            userService.unfollow(ctx.attribute("email")!!, usernameToUnfollow).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }
}
