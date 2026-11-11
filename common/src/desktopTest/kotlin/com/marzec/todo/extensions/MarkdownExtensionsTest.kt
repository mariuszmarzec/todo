package com.marzec.todo.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focused unit tests for [toMarkdownPlainTextFirstLine].
 *
 * Covers the acceptance-criteria examples plus empty/multiline descriptions.
 */
class MarkdownExtensionsTest {

    @Test
    fun `plain text is returned unchanged`() {
        assertEquals("Buy milk", "Buy milk".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `heading with bold is stripped`() {
        assertEquals("Fix login", "# **Fix login**".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `emphasis markers are removed`() {
        assertEquals("This is important", "This is *important*".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `bold and italic markers are removed`() {
        assertEquals("Do the thing", "**Do the *thing***".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `inline link label is kept`() {
        assertEquals("Open docs", "[Open docs](https://example.com)".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `image alt text is kept`() {
        assertEquals("screenshot", "![screenshot](https://example.com/image.png)".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `inline code markers are removed`() {
        assertEquals("Run cmd now", "Run `cmd` now".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `strikethrough markers are removed`() {
        assertEquals("Old text", "~~Old text~~".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `unordered list bullet is removed`() {
        assertEquals("First item", "- First item".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `ordered list number is removed`() {
        assertEquals("First item", "1. First item".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `blockquote prefix is removed`() {
        assertEquals("Quoted text", "> Quoted text".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `only first physical line is returned`() {
        val multiline = "# Title\nSecond line\nThird line"
        assertEquals("Title", multiline.toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `multiline plain text returns first line`() {
        val multiline = "First line\nSecond line"
        assertEquals("First line", multiline.toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `empty string returns empty`() {
        assertEquals("", "".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `whitespace-only line is trimmed`() {
        assertEquals("", "   ".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `autolink URL is kept`() {
        assertEquals("https://example.com", "<https://example.com>".toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `mixed markdown first line`() {
        val input = "# **Title** with *emphasis* and [link](https://example.com)"
        assertEquals("Title with emphasis and link", input.toMarkdownPlainTextFirstLine())
    }

    @Test
    fun `result never contains markdown marker characters`() {
        val inputs = listOf(
            "# **Fix login**",
            "This is *important*",
            "[Open docs](https://example.com)",
            "![screenshot](https://example.com/image.png)",
            "Run `cmd` now",
            "~~Strikethrough~~",
            "- bullet",
            "1. ordered",
            "> quote"
        )
        val markers = setOf('#', '*', '_', '[', ']', '(', ')', '!', '`', '~', '>', '-')
        inputs.forEach { input ->
            val result = input.toMarkdownPlainTextFirstLine()
            assertTrue(result.none { it in markers }, "Marker leaked into preview: '$result' from '$input'")
        }
    }
}