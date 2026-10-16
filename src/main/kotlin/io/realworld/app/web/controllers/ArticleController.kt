package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService
import io.realworld.app.ext.receiveBody

// Endpoints not implemented yet answer 501 (see ErrorExceptionMapping). The commented code is the
// original author's sketch from a previous (Javalin) version of this app.
class ArticleController(private val articleService: ArticleService) {
    companion object {
        const val SEARCH_TERM_MAX_LENGTH = 200
    }

    fun findBy(ctx: ApplicationCall) {
        TODO("Listing and filtering articles is not implemented yet; use /api/articles/search.")
//        articleService.findBy(tag, author, favorited, limit.toInt(), offset.toInt()).also { articles ->
//            ctx.json(ArticlesDTO(articles, articles.size))
//        }
    }

    // GET /api/articles/search?q=<term>&limit=&offset= : matches title or body, newest first.
    suspend fun search(ctx: ApplicationCall) {
        val term = ctx.parameters["q"]?.trim()
        require(!term.isNullOrEmpty()) { "Search term q can't be blank." }
        // Guardrail: every search scans all articles (LIKE '%term%'), so keep the pattern bounded.
        require(term.length <= SEARCH_TERM_MAX_LENGTH) { "Search term q must be at most $SEARCH_TERM_MAX_LENGTH characters." }
        val limit = ctx.intParameter("limit", default = 20, range = 1..100)
        val offset = ctx.intParameter("offset", default = 0, range = 0..Int.MAX_VALUE)
        ctx.respond(articleService.search(term, limit, offset))
    }

    // Pagination parameters as in the RealWorld spec's article list (limit default 20, offset default 0).
    private fun ApplicationCall.intParameter(name: String, default: Int, range: IntRange): Int {
        val raw = parameters[name] ?: return default
        val value = raw.toIntOrNull()
        require(value != null && value in range) {
            if (range.last == Int.MAX_VALUE) "$name must be a whole number of at least ${range.first}."
            else "$name must be a whole number from ${range.first} to ${range.last}."
        }
        return value
    }

    fun feed(ctx: ApplicationCall) {
        TODO("The article feed is not implemented yet.")
//        articleService.findFeed(ctx.attribute("email"), limit.toInt(), offset.toInt()).also { articles ->
//            ctx.json(ArticlesDTO(articles, articles.size))
//        }
    }

    suspend fun get(ctx: ApplicationCall) {
        articleService.findBySlug(ctx.parameters["slug"]!!).apply {
            ctx.respond(ArticleDTO(this))
        }
    }

    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()!!.email
        ctx.receiveBody<ArticleDTO>().apply {
            articleService.create(email, this.validCreate()).apply {
                ctx.respond(ArticleDTO(this))
            }
        }
    }

    fun update(ctx: ApplicationCall) {
        TODO("Updating articles is not implemented yet.")
//            articleService.update(slug, article).apply {
//                ctx.json(ArticleDTO(this))
//            }
    }

    fun delete(ctx: ApplicationCall) {
        TODO("Deleting articles is not implemented yet.")
//            articleService.delete(slug)
    }

    fun favorite(ctx: ApplicationCall) {
        TODO("Favoriting articles is not implemented yet.")
//            articleService.favorite(ctx.attribute("email"), slug).apply {
//                ctx.json(ArticleDTO(this))
//            }
    }

    fun unfavorite(ctx: ApplicationCall) {
        TODO("Unfavoriting articles is not implemented yet.")
//            articleService.unfavorite(ctx.attribute("email"), slug).apply {
//                ctx.json(ArticleDTO(this))
//            }
    }
}
