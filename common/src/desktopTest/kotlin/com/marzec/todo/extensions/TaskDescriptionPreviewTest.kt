package com.marzec.todo.extensions

import com.marzec.todo.model.Task
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskDescriptionPreviewTest {

    private val fixedTime = LocalDateTime.parse("2026-01-01T00:00:00")

    private fun task(description: String) = Task(
        id = 1,
        description = description,
        addedTime = fixedTime,
        modifiedTime = fixedTime,
        parentTaskId = null,
        subTasks = emptyList(),
        isToDo = true,
        priority = 0,
        scheduler = null,
        expirationDate = null,
        shares = emptyList(),
        ownerId = 1
    )

    @Test
    fun `extract plain text from a bare URL`() {
        assertEquals("https://example.com", "https://example.com".toPlainText())
    }

    @Test
    fun `task list preview displays a bare URL`() {
        assertEquals("https://example.com", task("https://example.com").descriptionWithProgress)
    }
}
