package io.realworld.app.web.controllers

import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.ErrorResponse
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import io.realworld.app.web.util.HttpUtil.Companion.unique
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// GET /api/articles/search?q=<term>: case-insensitive match on title or body, newest first,
// limit/offset pagination, existing article-list response format.
// All tests share one database, so each test searches for its own unique word.
class ArticleSearchTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    private lateinit var word: String

    @Before
    fun setUp() {
        word = unique("srch")
        appRule.http.createUser()
    }

    private fun search(params: Map<String, Any>, http: HttpUtil = appRule.http) =
        http.get<ArticlesDTO>("/api/articles/search", params)

    private fun searchError(params: Map<String, Any>) =
        appRule.http.get<ErrorResponse>("/api/articles/search", params)

    private fun titles(response: ArticlesDTO) = response.articles.map { it.title }

    @Test
    fun `matches a term in the title`() {
        appRule.http.postArticle(title = "How to train your $word")
        val response = search(mapOf("q" to word))

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(listOf("How to train your $word"), titles(response.body))
        assertEquals(1, response.body.articlesCount)
    }

    @Test
    fun `matches a term that is only in the body`() {
        appRule.http.postArticle(title = "Cooking pasta", body = "Boil water. A $word is not required.")
        val response = search(mapOf("q" to word))

        assertEquals(listOf("Cooking pasta"), titles(response.body))
    }

    @Test
    fun `does not match the description`() {
        appRule.http.postArticle(title = "Unrelated", body = "Unrelated.", description = "About $word")
        val response = search(mapOf("q" to word))

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articles.isEmpty())
    }

    @Test
    fun `is case-insensitive`() {
        appRule.http.postArticle(title = "Shouting ${word.uppercase()}")
        val lower = search(mapOf("q" to word.lowercase()))
        val upper = search(mapOf("q" to word.uppercase()))

        assertEquals(1, lower.body.articlesCount)
        assertEquals(1, upper.body.articlesCount)
    }

    @Test
    fun `matches a multi-word term as a phrase and trims surrounding spaces`() {
        appRule.http.postArticle(title = "train your $word")
        appRule.http.postArticle(title = "your train $word")
        val response = search(mapOf("q" to "  train your $word  "))

        assertEquals(listOf("train your $word"), titles(response.body))
    }

    @Test
    fun `returns an empty list when nothing matches`() {
        val response = search(mapOf("q" to word))

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articles.isEmpty())
        assertEquals(0, response.body.articlesCount)
    }

    @Test
    fun `returns newest articles first`() {
        appRule.http.postArticle(title = "first $word")
        appRule.http.postArticle(title = "second $word")
        appRule.http.postArticle(title = "third $word")
        val response = search(mapOf("q" to word))

        assertEquals(listOf("third $word", "second $word", "first $word"), titles(response.body))
    }

    @Test
    fun `paginates with limit and offset and reports the total count`() {
        (1..5).forEach { appRule.http.postArticle(title = "$word $it") }

        val firstPage = search(mapOf("q" to word, "limit" to 2))
        val secondPage = search(mapOf("q" to word, "limit" to 2, "offset" to 2))
        val pastTheEnd = search(mapOf("q" to word, "offset" to 10))

        assertEquals(listOf("$word 5", "$word 4"), titles(firstPage.body))
        assertEquals(listOf("$word 3", "$word 2"), titles(secondPage.body))
        assertTrue(pastTheEnd.body.articles.isEmpty())
        // articlesCount is the total number of matches, not the page size.
        listOf(firstPage, secondPage, pastTheEnd).forEach { assertEquals(5, it.body.articlesCount) }
    }

    @Test
    fun `defaults to 20 results per page`() {
        (1..21).forEach { appRule.http.postArticle(title = "$word $it") }
        val response = search(mapOf("q" to word))

        assertEquals(20, response.body.articles.size)
        assertEquals(21, response.body.articlesCount)
    }

    @Test
    fun `treats percent and underscore literally, not as wildcards`() {
        appRule.http.postArticle(title = "$word 100% sure")
        appRule.http.postArticle(title = "$word 100 dragons")
        appRule.http.postArticle(title = "$word snake_case")
        appRule.http.postArticle(title = "$word snakeXcase")

        assertEquals(listOf("$word 100% sure"), titles(search(mapOf("q" to "$word 100%")).body))
        assertEquals(listOf("$word snake_case"), titles(search(mapOf("q" to "$word snake_case")).body))
    }

    @Test
    fun `works without logging in and returns full articles`() {
        appRule.http.postArticle(title = "Public $word", tagList = listOf("dragons"))
        val anonymous = HttpUtil(appRule.port)
        val response = search(mapOf("q" to word), anonymous)

        assertEquals(HttpStatus.SC_OK, response.status)
        val article = response.body.articles.single()
        assertEquals(listOf("dragons"), article.tagList)
        assertFalse(article.slug.isNullOrBlank())
        assertFalse(article.author?.username.isNullOrBlank())
    }

    @Test
    fun `missing or blank q returns 422`() {
        listOf(mapOf(), mapOf("q" to ""), mapOf<String, Any>("q" to "   ")).forEach { params ->
            val response = searchError(params)

            assertEquals("params $params", HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
            assertEquals(listOf("Search term q can't be blank."), response.body.errors["body"])
        }
    }

    @Test
    fun `search term longer than 200 characters returns 422, exactly 200 is accepted`() {
        val atLimit = search(mapOf("q" to word + "x".repeat(200 - word.length)))
        val tooLong = searchError(mapOf("q" to "x".repeat(201)))

        assertEquals(HttpStatus.SC_OK, atLimit.status)
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, tooLong.status)
        assertEquals(listOf("Search term q must be at most 200 characters."), tooLong.body.errors["body"])
    }

    @Test
    fun `invalid limit or offset returns 422`() {
        listOf("0", "101", "abc", "2.5").forEach { limit ->
            val response = searchError(mapOf("q" to word, "limit" to limit))
            assertEquals("limit=$limit", HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
            assertEquals(listOf("limit must be a whole number from 1 to 100."), response.body.errors["body"])
        }
        val response = searchError(mapOf("q" to word, "offset" to -1))
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertEquals(listOf("offset must be a whole number of at least 0."), response.body.errors["body"])
    }

    @Test
    fun `limit at the boundaries 1 and 100 is accepted`() {
        appRule.http.postArticle(title = "$word one")
        appRule.http.postArticle(title = "$word two")

        assertEquals(1, search(mapOf("q" to word, "limit" to 1)).body.articles.size)
        assertEquals(2, search(mapOf("q" to word, "limit" to 100)).body.articles.size)
    }
}
