package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall

// Not implemented yet: every endpoint answers 501 (see ErrorExceptionMapping). The commented code is the
// original author's sketch from a previous (Javalin) version of this app.
class CommentController {
    //class CommentController(private val commentService: CommentService) {
    fun add(ctx: ApplicationCall) {
        TODO("Adding comments is not implemented yet.")
//                commentService.add(slug, ctx.attribute("email")!!, this.comment!!).also {
//                    ctx.json(CommentDTO(it))
//                }
    }

    fun findBySlug(ctx: ApplicationCall) {
        TODO("Listing comments is not implemented yet.")
//            commentService.findBySlug(this).also { comments ->
//                ctx.json(CommentsDTO(comments))
//            }
    }

    fun delete(ctx: ApplicationCall) {
        TODO("Deleting comments is not implemented yet.")
//        commentService.delete(id, slug)
    }
}
