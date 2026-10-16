package io.realworld.app.domain

import java.util.*

data class ArticleDTO(val article: Article?) {
    // RealWorld spec: title, description and body are required; tagList is optional.
    fun validCreate(): Article {
        requireNotNull(article) { "Article is required." }
        require(!article.title.isNullOrBlank()) { "Title can't be blank." }
        require(!article.description.isNullOrBlank()) { "Description can't be blank." }
        require(!article.body.isNullOrBlank()) { "Body can't be blank." }
        require(article.title.length <= Article.TITLE_MAX_LENGTH) {
            "Title must be at most ${Article.TITLE_MAX_LENGTH} characters."
        }
        require(article.description.length <= Article.DESCRIPTION_MAX_LENGTH) {
            "Description must be at most ${Article.DESCRIPTION_MAX_LENGTH} characters."
        }
        require(article.body.length <= Article.BODY_MAX_LENGTH) {
            "Body must be at most ${Article.BODY_MAX_LENGTH} characters."
        }
        // Trim tags and drop duplicates, so " dragons" and "dragons" are one tag.
        val tags = article.tagList.map { it.trim() }.distinct()
        require(tags.none { it.isEmpty() }) { "Tags can't be blank." }
        require(tags.all { it.length <= Article.TAG_MAX_LENGTH }) {
            "Tags must be at most ${Article.TAG_MAX_LENGTH} characters."
        }
        return article.copy(tagList = tags)
    }
}

// articlesCount is the total number of matching articles (for pagination), not the size of this page.
data class ArticlesDTO(val articles: List<Article>, val articlesCount: Int)

data class Article(val slug: String? = null,
                   val title: String?,
                   val description: String?,
                   // Nullable so a missing body gets a clear validation message instead of a JSON parse error.
                   val body: String?,
                   val tagList: List<String> = listOf(),
                   val createdAt: Date? = null,
                   val updatedAt: Date? = null,
                   val favorited: Boolean = false,
                   val favoritesCount: Long = 0,
                   // A public profile, per the RealWorld spec. A User here would leak the author's email and token.
                   val author: Profile? = null) {
    companion object {
        // Column sizes, shared with the Articles/Tags tables so validation and schema can't drift apart.
        const val TITLE_MAX_LENGTH = 255
        const val DESCRIPTION_MAX_LENGTH = 255
        const val TAG_MAX_LENGTH = 100
        // Guardrail, not a column size (body is TEXT): without it a single request could store
        // arbitrarily large bodies (a 20 MB body was accepted). 100,000 characters is a long article.
        const val BODY_MAX_LENGTH = 100_000
        // Room for a title-derived slug plus a "-<n>" suffix that keeps it unique.
        const val SLUG_MAX_LENGTH = 300
    }
}
