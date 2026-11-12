package com.marzec.todo.extensions

import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.TaskShare
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownPlainTextExtractorTest {

    private val fixedTime = LocalDateTime.parse("2026-01-01T00:00:00")

    private fun task(
        id: Int,
        description: String,
        isToDo: Boolean = true,
        subTasks: List<Task> = emptyList(),
        priority: Int = 0,
        scheduler: Scheduler? = null,
        expirationDate: LocalDateTime? = null,
        shares: List<TaskShare> = emptyList(),
        ownerId: Int? = 1
    ): Task = Task(
        id = id,
        description = description,
        addedTime = fixedTime,
        modifiedTime = fixedTime,
        parentTaskId = null,
        subTasks = subTasks,
        isToDo = isToDo,
        priority = priority,
        scheduler = scheduler,
        expirationDate = expirationDate,
        shares = shares,
        ownerId = ownerId
    )

    @Test
    fun `extract plain text from heading`() {
        val markdown = "# Heading 1\n## Heading 2\n### Heading 3"
        val result = markdown.toPlainText()
        // Parser preserves space after # marker
        assertEquals("Heading 1\n Heading 2\n Heading 3", result)
    }

    @Test
    fun `extract plain text from bold`() {
        val markdown = "**bold text** and __also bold__"
        val result = markdown.toPlainText()
        assertEquals("bold text and also bold", result)
    }

    @Test
    fun `extract plain text from italic`() {
        val markdown = "*italic text* and _also italic_"
        val result = markdown.toPlainText()
        assertEquals("italic text and also italic", result)
    }

    @Test
    fun `extract plain text from strikethrough`() {
        val markdown = "~~strikethrough~~"
        val result = markdown.toPlainText()
        assertEquals("strikethrough", result)
    }

    @Test
    fun `extract plain text from inline code`() {
        val markdown = "`inline code` and ``double backticks``"
        val result = markdown.toPlainText()
        assertEquals("inline code and double backticks", result)
    }

    @Test
    fun `extract plain text from link`() {
        val markdown = "[link text](https://example.com)"
        val result = markdown.toPlainText()
        assertEquals("link text", result)
    }

    @Test
    fun `extract plain text from reference link`() {
        val markdown = "[reference link][ref]\n\n[ref]: https://example.com"
        val result = markdown.toPlainText()
        // Reference definition syntax should be stripped from visible preview
        assertEquals("reference link", result)
    }

    @Test
    fun `extract plain text from short reference link`() {
        val markdown = "[short link][]\n\n[short link]: https://example.com"
        val result = markdown.toPlainText()
        // Reference definition syntax should be stripped from visible preview
        assertEquals("short link", result)
    }

    @Test
    fun `extract plain text from image`() {
        val markdown = "![alt text](https://example.com/image.png)"
        val result = markdown.toPlainText()
        assertEquals("alt text", result)
    }

    @Test
    fun `extract plain text from autolink`() {
        val markdown = "<https://example.com> and <email@example.com>"
        val result = markdown.toPlainText()
        assertEquals("https://example.com and email@example.com", result)
    }

    @Test
    fun `extract plain text from unordered list`() {
        val markdown = "- Item 1\n- Item 2\n  - Nested item"
        val result = markdown.toPlainText()
        // Parser preserves indentation for nested items
        assertEquals("Item 1\nItem 2\n  Nested item", result)
    }

    @Test
    fun `extract plain text from ordered list`() {
        val markdown = "1. First\n2. Second\n3. Third"
        val result = markdown.toPlainText()
        assertEquals("First\nSecond\nThird", result)
    }

    @Test
    fun `extract plain text from blockquote`() {
        val markdown = "> Quote line 1\n> Quote line 2"
        val result = markdown.toPlainText()
        // Parser preserves space after > marker
        assertEquals("Quote line 1\n Quote line 2", result)
    }

    @Test
    fun `extract plain text from multiline content`() {
        val markdown = "Line 1\nLine 2\n\nLine 4"
        val result = markdown.toPlainText()
        assertEquals("Line 1\nLine 2\n\nLine 4", result)
    }

    @Test
    fun `extract plain text from complex markdown`() {
        val markdown = """
            # Title
            
            Some **bold** and *italic* text with a [link](https://example.com).
            
            - List item 1
            - List item 2
            
            > Blockquote
            
            `code` and ~~strikethrough~~
            """.trimIndent()
        val result = markdown.toPlainText()
        assertEquals(
            "Title\n\nSome bold and italic text with a link.\n\n" +
                "List item 1\nList item 2\n\nBlockquote\n\ncode and strikethrough",
            result
        )
    }

    @Test
    fun `extract plain text from empty string`() {
        val markdown = ""
        val result = markdown.toPlainText()
        assertEquals("", result)
    }

    @Test
    fun `extract plain text from task description with markdown`() {
        val t = task(
            id = 1,
            description = "# Task Title\n\n**Bold** description with [link](https://example.com)",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("Task Title", result)
    }

    @Test
    fun `extract plain text from task description with progress`() {
        val subTask1 = task(
            id = 2,
            description = "Subtask 1",
            isToDo = false
        )
        val subTask2 = task(
            id = 3,
            description = "Subtask 2",
            isToDo = true
        )
        val t = task(
            id = 1,
            description = "# Main Task\n\nDetails here",
            isToDo = true,
            subTasks = listOf(subTask1, subTask2)
        )
        val result = t.descriptionWithProgress
        assertEquals("Main Task - 1/2", result)
    }

    @Test
    fun `descriptionWithProgress shows link text for reference link on first line`() {
        // First line is a full reference link, definition is on later line
        val t = task(
            id = 1,
            description = "[reference link][ref]\n\n[ref]: https://example.com\n\nMore details",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        // Only first physical line processed, reference definition stripped
        assertEquals("reference link", result)
    }

    @Test
    fun `descriptionWithProgress shows link text for short reference link on first line`() {
        // First line is a short reference link, definition is on later line
        val t = task(
            id = 1,
            description = "[short link][]\n\n[short link]: https://example.com\n\nMore details",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        // Only first physical line processed, reference definition stripped
        assertEquals("short link", result)
    }

    @Test
    fun `descriptionWithProgress shows link text for inline link on first line`() {
        val t = task(
            id = 1,
            description = "[inline link](https://example.com)\n\nMore details",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("inline link", result)
    }

    @Test
    fun `descriptionWithProgress shows image alt text on first line`() {
        val t = task(
            id = 1,
            description = "![screenshot](https://example.com/image.png)\n\nMore details",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("screenshot", result)
    }

    @Test
    fun `descriptionWithProgress shows heading text without markers`() {
        val t = task(
            id = 1,
            description = "# **Fix login**\n\nDetails here",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("Fix login", result)
    }

    @Test
    fun `descriptionWithProgress shows emphasized text without markers`() {
        val t = task(
            id = 1,
            description = "This is *important* and **bold** text",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("This is important and bold text", result)
    }

    @Test
    fun `descriptionWithProgress shows strikethrough text without markers`() {
        val t = task(
            id = 1,
            description = "~~strikethrough~~ text",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("strikethrough text", result)
    }

    @Test
    fun `descriptionWithProgress shows inline code without markers`() {
        val t = task(
            id = 1,
            description = "Use `code` here",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("Use code here", result)
    }

    @Test
    fun `descriptionWithProgress shows autolink URL as plain text`() {
        val t = task(
            id = 1,
            description = "Visit <https://example.com> for info",
            isToDo = true
        )
        val result = t.descriptionWithProgress
        assertEquals("Visit https://example.com for info", result)
    }

    @Test
    fun `descriptionWithProgress includes subtask progress suffix`() {
        val subTask1 = task(
            id = 2,
            description = "Subtask 1",
            isToDo = false
        )
        val subTask2 = task(
            id = 3,
            description = "Subtask 2",
            isToDo = true
        )
        val t = task(
            id = 1,
            description = "[Task with link][link]\n\n[link]: https://example.com",
            isToDo = true,
            subTasks = listOf(subTask1, subTask2)
        )
        val result = t.descriptionWithProgress
        assertEquals("Task with link - 1/2", result)
    }
}
