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
        // Parser includes reference definition with blank line
        assertEquals("reference link\n\n:", result)
    }

    @Test
    fun `extract plain text from short reference link`() {
        val markdown = "[short link][]\n\n[short link]: https://example.com"
        val result = markdown.toPlainText()
        // Parser includes reference definition with blank line
        assertEquals("short link\n\n:", result)
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
}
