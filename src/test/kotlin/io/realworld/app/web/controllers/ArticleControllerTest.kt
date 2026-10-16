package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.ProfileDTO
import io.realworld.app.domain.TagDTO
import io.realworld.app.web.ErrorResponse
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import io.realworld.app.web.util.HttpUtil.Companion.unique
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ArticleControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles`() {
        appRule.http.createArticle()
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/api/articles")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles with auth`() {
        appRule.http.createArticle()
        val response = appRule.http.get<ArticlesDTO>("/api/articles")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        assertNotNull(response.body.articles.first())
        assertFalse(response.body.articles.first().title.isNullOrBlank())
        assertTrue(response.body.articles.first().tagList.isNotEmpty())
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles by author`() {
        val author = "user_name_test"
        val response = appRule.http.get<ArticlesDTO>("/api/articles?author=$author")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        response.body.articles.forEach {
            assertEquals(it.author?.username, author)
            assertFalse(it.title.isNullOrBlank())
            assertTrue(it.tagList.isNotEmpty())
        }
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles by author with auth`() {
        appRule.http.createArticle()
        val author = "user_name_test"
        val response = appRule.http.get<ArticlesDTO>("/api/articles?author=$author")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        response.body.articles.forEach {
            assertEquals(it.author?.username, author)
            assertFalse(it.title.isNullOrBlank())
            assertTrue(it.tagList.isNotEmpty())
        }
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles favorited by username`() {
        val responseCreate = appRule.http.createArticle()
        appRule.http.post<ArticleDTO>("/api/articles/${responseCreate.body.article?.slug}/favorite")

        val response = appRule.http.get<ArticlesDTO>("/api/articles?favorited=user_name_test")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        assertNotNull(response.body.articles.first())
        assertFalse(response.body.articles.first().title.isNullOrBlank())
        assertTrue(response.body.articles.first().tagList.isNotEmpty())
        assertTrue(response.body.articles.first().favorited)
        assertTrue(response.body.articles.first().favoritesCount > 0)
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles favorited by username with auth`() {
        val responseCreate = appRule.http.createArticle()
        appRule.http.post<ArticleDTO>("/api/articles/${responseCreate.body.article?.slug}/favorite")

        val response = appRule.http.get<ArticlesDTO>("/api/articles?favorited=${responseCreate.body.article?.author?.username}")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        assertNotNull(response.body.articles.first())
        assertFalse(response.body.articles.first().title.isNullOrBlank())
        assertTrue(response.body.articles.first().tagList.isNotEmpty())
    }

    @Test
    @Ignore("Listing/filtering articles (GET /api/articles) is not implemented yet (501).")
    fun `get all articles by tag`() {
        val responseCreate = appRule.http.createArticle()
        val tag = responseCreate.body.article?.tagList?.first()
        val response = appRule.http.get<ArticlesDTO>("/api/articles?tag=${responseCreate.body.article?.tagList?.first()}")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        assertTrue(response.body.articles.first().tagList.contains(tag))
    }

    @Test
    fun `create article`() {
        val article = Article(
            title = "Create How to train your dragon",
            description = "Ever wonder how?",
            body = "Very carefully.",
            tagList = listOf("create_article")
        )
        val response = appRule.http.createArticle(article)
        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.article)
        assertEquals(response.body.article?.title, article.title)
        assertEquals(response.body.article?.description, article.description)
        assertEquals(response.body.article?.body, article.body)
        assertEquals(response.body.article?.tagList, article.tagList)
    }

    @Test
    @Ignore("The article feed and following profiles are not implemented yet (501).")
    fun `get all articles of feed`() {
        appRule.http.createArticle()

        val http = HttpUtil(appRule.port)
        http.createUser("celeb_follow_profile@valid_email.com", "celeb_username")

        http.post<ProfileDTO>("/api/profiles/user_name_test/follow")

        val response = appRule.http.get<ArticlesDTO>("/api/articles/feed")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
        assertNotNull(response.body.articles.first())
        assertFalse(response.body.articles.first().title.isNullOrBlank())
        assertTrue(response.body.articles.first().tagList.isNotEmpty())
    }

    @Test
    fun `get single article by slug`() {
        val responseArticle = appRule.http.createArticle()
        val slug = responseArticle.body.article?.slug
        val response = appRule.http.get<ArticleDTO>("/api/articles/$slug")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.article)
        assertNotNull(response.body.article?.body)
        assertFalse(response.body.article?.title.isNullOrBlank())
        assertNotNull(response.body.article?.description)
        assertTrue(response.body.article?.tagList?.isNotEmpty() ?: false)
    }

    @Test
    @Ignore("Updating articles is not implemented yet (501).")
    fun `update article by slug`() {
        val responseCreated = appRule.http.createArticle()
        val slug = responseCreated.body.article?.slug
        val article = Article(body = "Very carefully.", title = "Teste", description = "Teste Desc")
        val response = appRule.http.put<ArticleDTO>("/api/articles/$slug", ArticleDTO(article))

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.article)
        assertEquals(response.body.article?.body, article.body)
        assertNotNull(response.body.article?.body)
        assertFalse(response.body.article?.title.isNullOrBlank())
        assertNotNull(response.body.article?.description)
        assertTrue(response.body.article?.tagList?.isNotEmpty() ?: false)
    }

    @Test
    @Ignore("Favoriting articles is not implemented yet (501).")
    fun `favorite article by slug`() {
        val article = Article(
            title = "slug test",
            description = "Ever wonder how?",
            body = "Very carefully.",
            tagList = listOf("favorite")
        )
        appRule.http.createArticle(article)
        val slug = "slug-test"
        val response = appRule.http.post<ArticleDTO>("/api/articles/$slug/favorite")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.article)
        assertNotNull(response.body.article?.body)
        assertFalse(response.body.article?.title.isNullOrBlank())
        assertNotNull(response.body.article?.description)
        assertTrue(response.body.article?.tagList?.isNotEmpty() ?: false)
    }

    @Test
    @Ignore("Unfavoriting articles is not implemented yet (501).")
    fun `unfavorite article by slug`() {
        val email = "unfavorite_article@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "user_name_test")
        appRule.http.loginAndSetTokenHeader(email, password)
        val article = Article(
            title = "slug test 2",
            description = "Ever wonder how?",
            body = "Very carefully.",
            tagList = listOf("unfavorite")
        )
        appRule.http.post<ArticleDTO>("/api/articles", ArticleDTO(article))
        val slug = "slug-test-2"
        val response = appRule.http.deleteWithResponseBody<ArticleDTO>("/api/articles/$slug/favorite")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.article)
        assertNotNull(response.body.article?.body)
        assertFalse(response.body.article?.title.isNullOrBlank())
        assertNotNull(response.body.article?.description)
        assertTrue(response.body.article?.tagList?.isNotEmpty() ?: false)
    }

    @Test
    @Ignore("Deleting articles is not implemented yet (501).")
    fun `delete article by slug`() {
        val responseCreate = appRule.http.createArticle()
        val response = appRule.http.delete("/api/articles/${responseCreate.body.article?.slug}")

        assertEquals(response.status, HttpStatus.SC_OK)
    }

    // --- Create / get edge cases ---

    @Test
    fun `create article returns the spec's fields with an ISO date and a profile author`() {
        val user = appRule.http.createUser()
        val response = appRule.http.post<String>(
            "/api/articles", ArticleDTO(Article(title = unique("Spec"), description = "d", body = "b"))
        )
        val isoDate = Regex("\"createdAt\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z\"")

        assertEquals(HttpStatus.SC_OK, response.status)
        listOf("slug", "title", "description", "body", "tagList", "createdAt", "updatedAt", "favorited",
            "favoritesCount", "author").forEach { assertTrue("missing $it", response.body.contains("\"$it\":")) }
        assertTrue("createdAt not ISO-8601: ${response.body}", isoDate.containsMatchIn(response.body))
        assertTrue(response.body.contains("\"username\":\"${user.user?.username}\""))
        // The author is a public profile: no email or token.
        assertFalse(response.body.contains("email"))
        assertFalse(response.body.contains("token"))
    }

    @Test
    fun `create article without login returns 401`() {
        val response = HttpUtil(appRule.port).post<String>(
            "/api/articles", ArticleDTO(Article(title = "t", description = "d", body = "b"))
        )

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `create article with missing or blank required fields returns 422`() {
        appRule.http.createUser()
        val cases = mapOf(
            Article(title = "t", description = "d", body = null) to "Body can't be blank.",
            Article(title = " ", description = "d", body = "b") to "Title can't be blank.",
            Article(title = "t", description = "", body = "b") to "Description can't be blank.",
            Article(title = "t", description = "d", body = "b", tagList = listOf("ok", " ")) to "Tags can't be blank.",
            Article(title = "t".repeat(Article.TITLE_MAX_LENGTH + 1), description = "d", body = "b") to
                "Title must be at most 255 characters.",
            Article(title = "t", description = "d", body = "b".repeat(Article.BODY_MAX_LENGTH + 1)) to
                "Body must be at most 100000 characters."
        )
        cases.forEach { (article, message) ->
            val response = appRule.http.post<ErrorResponse>("/api/articles", ArticleDTO(article))

            assertEquals(message, HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
            assertEquals(listOf(message), response.body.errors["body"])
        }
    }

    @Test
    fun `body exactly at the size limit is accepted`() {
        appRule.http.createUser()
        val body = "b".repeat(Article.BODY_MAX_LENGTH)
        val article = appRule.http.postArticle(title = unique("Long"), body = body)

        assertEquals(body, article.body)
    }

    @Test
    fun `slug is derived from the title and made unique`() {
        appRule.http.createUser()
        val word = unique("slug")
        val first = appRule.http.postArticle(title = "Café, crème! $word")
        val second = appRule.http.postArticle(title = "Café, crème! $word")
        val symbols = appRule.http.postArticle(title = "!!!")

        assertEquals("cafe-creme-$word", first.slug)
        assertEquals("cafe-creme-$word-2", second.slug)
        assertTrue(symbols.slug!!.matches(Regex("article(-\\d+)?")))
    }

    @Test
    fun `tags are trimmed, de-duplicated, sorted, and listed by the tags endpoint`() {
        appRule.http.createUser()
        val tag = unique("tag")
        val article = appRule.http.postArticle(title = unique("Tags"), tagList = listOf("zebra", " $tag ", tag))
        val tags = appRule.http.get<TagDTO>("/api/tags")

        assertEquals(listOf(tag, "zebra"), article.tagList)
        assertTrue(tags.body.tags.containsAll(listOf(tag, "zebra")))
        assertEquals(1, tags.body.tags.count { it == tag })
    }

    @Test
    fun `get article by slug works without login`() {
        appRule.http.createUser()
        val created = appRule.http.postArticle(title = unique("Public"))
        val response = HttpUtil(appRule.port).get<ArticleDTO>("/api/articles/${created.slug}")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(created, response.body.article)
    }

    @Test
    fun `get unknown slug returns 404`() {
        val response = appRule.http.get<ErrorResponse>("/api/articles/${unique("missing")}")

        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
        assertEquals(listOf("Article not found."), response.body.errors["body"])
    }

    // --- Routing ---

    @Test
    fun `feed requires login and is not mistaken for an article slug`() {
        val anonymous = HttpUtil(appRule.port).get<String>("/api/articles/feed")
        appRule.http.createUser()
        val loggedIn = appRule.http.get<ErrorResponse>("/api/articles/feed")

        assertEquals(HttpStatus.SC_UNAUTHORIZED, anonymous.status)
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, loggedIn.status)
        assertEquals(listOf("The article feed is not implemented yet."), loggedIn.body.errors["body"])
    }

    @Test
    fun `endpoints that are not built yet return 501, not 404`() {
        appRule.http.createUser()
        listOf("/api/articles", "/api/articles/some-slug/comments", "/api/profiles/someone").forEach { path ->
            assertEquals(path, HttpStatus.SC_NOT_IMPLEMENTED, appRule.http.get<String>(path).status)
        }
    }
}
