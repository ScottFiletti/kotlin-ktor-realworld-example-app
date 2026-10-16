package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.Profile
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", Article.SLUG_MAX_LENGTH).uniqueIndex()
    val title: Column<String> = varchar("title", Article.TITLE_MAX_LENGTH)
    val description: Column<String> = varchar("description", Article.DESCRIPTION_MAX_LENGTH)
    val body: Column<String> = text("body")
    val author: Column<EntityID<Long>> = reference("author", Users)
    val createdAt: Column<DateTime> = datetime("created_at")
    val updatedAt: Column<DateTime> = datetime("updated_at")
}

// Many-to-many link between articles and tags.
internal object ArticleTags : Table("article_tags") {
    val article: Column<EntityID<Long>> = reference("article", Articles).primaryKey(0)
    val tag: Column<EntityID<Long>> = reference("tag", Tags).primaryKey(1)
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Users, Tags, Articles, ArticleTags)
        }
    }

    fun slugExists(slug: String): Boolean = transaction {
        Articles.select { Articles.slug eq slug }.count() > 0
    }

    fun create(article: Article, slug: String, authorId: Long) {
        transaction {
            val now = DateTime.now()
            val articleId = Articles.insertAndGetId { row ->
                row[Articles.slug] = slug
                row[title] = article.title!!
                row[description] = article.description!!
                row[body] = article.body!!
                row[author] = EntityID(authorId, Users)
                row[createdAt] = now
                row[updatedAt] = now
            }
            article.tagList.forEach { name ->
                ArticleTags.insert { row ->
                    row[ArticleTags.article] = articleId
                    row[tag] = findOrCreateTag(name)
                }
            }
        }
    }

    fun findBySlug(slug: String): Article? = transaction {
        (Articles innerJoin Users)
            .select { Articles.slug eq slug }
            .firstOrNull()
            ?.let { row -> toDomain(row, tagsFor(listOf(row[Articles.id]))[row[Articles.id]].orEmpty()) }
    }

    // Case-insensitive substring match on title or body, newest first. Returns one page plus the total
    // number of matches (for pagination). id breaks ties so pages are stable when timestamps are equal.
    fun search(term: String, limit: Int, offset: Int): Pair<List<Article>, Int> = transaction {
        val pattern = "%${escapeLike(term.lowercase())}%"
        val matches = (Articles.title.lowerCase() like pattern) or (Articles.body.lowerCase() like pattern)
        val total = Articles.select { matches }.count()
        val rows = (Articles innerJoin Users)
            .select { matches }
            .orderBy(Articles.createdAt to SortOrder.DESC, Articles.id to SortOrder.DESC)
            .limit(limit, offset)
            .toList()
        val tags = tagsFor(rows.map { it[Articles.id] })
        Pair(rows.map { toDomain(it, tags[it[Articles.id]].orEmpty()) }, total)
    }

    // % and _ are LIKE wildcards; escape them (and the escape character, H2's default \) so they match literally.
    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    // Must be called inside a transaction. Tags are shared, so reuse an existing tag with the same name.
    private fun findOrCreateTag(name: String): EntityID<Long> =
        Tags.select { Tags.name eq name }.firstOrNull()?.get(Tags.id)
            ?: Tags.insertAndGetId { it[Tags.name] = name }

    // Must be called inside a transaction. One query for all given articles; tags are sorted by name
    // so the order is stable regardless of insertion order.
    private fun tagsFor(articleIds: List<EntityID<Long>>): Map<EntityID<Long>, List<String>> =
        if (articleIds.isEmpty()) emptyMap() else (ArticleTags innerJoin Tags)
            .select { ArticleTags.article inList articleIds }
            .groupBy({ it[ArticleTags.article] }, { it[Tags.name] })
            .mapValues { (_, names) -> names.sorted() }

    private fun toDomain(row: ResultRow, tags: List<String>) = Article(
        slug = row[Articles.slug],
        title = row[Articles.title],
        description = row[Articles.description],
        body = row[Articles.body],
        tagList = tags,
        createdAt = row[Articles.createdAt].toDate(),
        updatedAt = row[Articles.updatedAt].toDate(),
        // Favorites and follows are not implemented, so these are always false/0 for now.
        favorited = false,
        favoritesCount = 0,
        author = Profile(row[Users.username], row[Users.bio], row[Users.image], following = false)
    )
}
