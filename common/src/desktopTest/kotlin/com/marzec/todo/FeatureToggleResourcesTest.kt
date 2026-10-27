package com.marzec.todo

import com.marzec.resource.ResourceLoaderImpl
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeatureToggleResourcesTest {

    @Test
    fun loadsDefaultsForEachEnvironmentFromComposeResources() {
        val expectedByEnvironment = mapOf(
            "m" to mapOf(
                "todo.doneButtonOnTaskList" to "true",
                "todo.taskSharing" to "false"
            ),
            "p" to mapOf(
                "todo.doneButtonOnTaskList" to "false",
                "todo.taskSharing" to "true"
            ),
            "t" to mapOf(
                "todo.doneButtonOnTaskList" to "false",
                "todo.taskSharing" to "true"
            )
        )
        val loader = ResourceLoaderImpl()

        expectedByEnvironment.forEach { (environment, expected) ->
            val content = assertNotNull(loader.loadComonResourceFile("$environment/feature_toggle_conf.json"), "Missing feature-toggle config for $environment")
            val actual = Json.decodeFromString<Map<String, String>>(content)

            assertEquals(expected, actual, "Unexpected feature-toggle defaults for $environment")
        }
    }
}
