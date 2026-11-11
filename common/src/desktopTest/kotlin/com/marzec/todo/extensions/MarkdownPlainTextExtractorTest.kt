package com.marzec.todo.extensions

import com.marzec.todo.model.Task
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownPlainTextExtractorTest {

    @Test
    fun `empty input returns empty string`() {
        assertEquals("", "".toPlainText())
    }

    @Test
    fun `heading markdown is stripped`() {
        assertEquals("Heading", "# Heading".toPlainText())
        assertEquals("Heading", "## Heading".toPlainText())
        assertEquals("Heading", "### Heading".toPlainText())
    }

    @Test
    fun `bold markdown is stripped`() {
        assertEquals("bold text", "**bold text**".toPlainText())
        assertEquals("bold text", "__bold text__".toPlainText())
    }

    @Test
    fun `italic markdown is stripped`() {
        assertEquals("italic text", "*italic text*".toPlainText())
        assertEquals("italic text", "_italic text_".toPlainText())
    }

    @Test
    fun `strikethrough markdown is stripped`() {
        assertEquals("strikethrough", "~~strikethrough~~".toPlainText())
    }

    @Test
    fun `inline code markdown is stripped`() {
        assertEquals("code", "`code`".toPlainText())
    }

    @Test
    fun `link markdown preserves link text`() {
        assertEquals("link text", "[link text](https://example.com)".toPlainText())
        assertEquals("Google", "[Google](https://google.com)".toPlainText())
    }

    @Test
    fun `image markdown preserves alt text`() {
        assertEquals("alt text", "![alt text](https://example.com/image.png)".toPlainText())
    }

    @Test
    fun `autolink markdown is stripped`() {
        assertEquals("https://example.com", "<https://example.com>".toPlainText())
    }

    @Test
    fun `unordered list markers are stripped`() {
        assertEquals("item 1\nitem 2", "- item 1\n- item 2".toPlainText())
        assertEquals("item 1\nitem 2", "* item 1\n* item 2".toPlainText())
    }

    @Test
    fun `ordered list markers are stripped`() {
        assertEquals("first\nsecond", "1. first\n2. second".toPlainText())
    }

    @Test
    fun `blockquote markers are stripped`() {
        assertEquals("quoted text", "> quoted text".toPlainText())
    }

    @Test
    fun `multiline markdown preserves line breaks`() {
        val input = """# Title
        |Some **bold** text
        |- item 1
        |- item 2
        |[link](https://example.com)""".trimMargin()
        val expected = "Title\nSome bold text\nitem 1\nitem 2\nlink"
        assertEquals(expected, input.toPlainText())
    }

    @Test
    fun `complex markdown with mixed elements`() {
        val input = """# Main Title
        |Some **bold** and *italic* text with `code`.
        |[Link](https://example.com) and ![Image](image.png)
        |> Quote
        |- Item 1
        |- Item 2
        |~~Strikethrough~~""".trimMargin()
        val expected = "Main Title\nSome bold and italic text with code.\n" +
            "Link and Image\nQuote\nItem 1\nItem 2\nStrikethrough"
        assertEquals(expected, input.toPlainText())
    }

    @Test
    fun `task descriptionWithProgress uses plain text`() {
        val task = Task(
            id = 1,
            description = "# Task with **bold** and [link](https://example.com)",
            addedTime = LocalDateTime(2026, 1, 1, 0, 0),
            modifiedTime = LocalDateTime(2026, 1, 1, 0, 0),
            parentTaskId = null,
            subTasks = listOf(
                Task(
                    id = 2,
                    description = "Subtask 1",
                    addedTime = LocalDateTime(2026, 1, 1, 0, 0),
                    modifiedTime = LocalDateTime(2026, 1, 1, 0, 0),
                    parentTaskId = 1,
                    subTasks = emptyList(),
                    isToDo = false,
                    priority = 0,
                    scheduler = null,
                    expirationDate = null,
                    shares = emptyList(),
                    ownerId = null
                ),
                Task(
                    id = 3,
                    description = "Subtask 2",
                    addedTime = LocalDateTime(2026, 1, 1, 0, 0),
                    modifiedTime = LocalDateTime(2026, 1, 1, 0, 0),
                    parentTaskId = 1,
                    subTasks = emptyList(),
                    isToDo = true,
                    priority = 0,
                    scheduler = null,
                    expirationDate = null,
                    shares = emptyList(),
                    ownerId = null
                )
            ),
            isToDo = true,
            priority = 0,
            scheduler = null,
            expirationDate = null,
            shares = emptyList(),
            ownerId = null
        )

        val result = task.descriptionWithProgress
        assertEquals("Task with bold and link - 1/2", result)
    }
}
