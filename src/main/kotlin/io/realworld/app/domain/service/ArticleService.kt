package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.UserRepository
import java.text.Normalizer

class ArticleService(private val articleRepository: ArticleRepository, private val userRepository: UserRepository) {

    fun create(email: String, article: Article): Article {
        val author = userRepository.findByEmail(email) ?: throw NotFoundException("User not found to create article.")
        val slug = uniqueSlug(article.title!!)
        articleRepository.create(article, slug, author.id!!)
        return findBySlug(slug)
    }

    fun findBySlug(slug: String): Article =
        articleRepository.findBySlug(slug) ?: throw NotFoundException("Article not found.")

    fun search(term: String, limit: Int, offset: Int): ArticlesDTO =
        articleRepository.search(term, limit, offset).let { (articles, total) ->
            ArticlesDTO(articles, total)
        }

    // "How to Train Your Dragon!" -> "how-to-train-your-dragon"; if taken, "-2", "-3", ... is appended.
    // Accents are stripped first ("Café crème" -> "cafe-creme"). Titles with no ASCII letters/digits
    // (e.g. "!!!" or non-Latin scripts) fall back to "article".
    private fun uniqueSlug(title: String): String {
        val base = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(Article.TITLE_MAX_LENGTH)
            .trimEnd('-')
            .ifEmpty { "article" }
        return generateSequence(1) { it + 1 }
            .map { n -> if (n == 1) base else "$base-$n" }
            .first { !articleRepository.slugExists(it) }
    }
}
