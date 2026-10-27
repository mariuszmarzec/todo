package com.marzec.todo

import com.marzec.resource.ResourceLoaderImpl
import com.marzec.todo.common.generated.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeatureToggleResourcesTest {

    @OptIn(ExperimentalResourceApi::class)
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
            val uri = Res.getUri("files/$environment/feature_toggle_conf.json")
            val content = assertNotNull(loader.loadResource(uri), "Missing feature-toggle config for $environment")
            val actual = Json.decodeFromString<Map<String, String>>(content)

            assertEquals(expected, actual, "Unexpected feature-toggle defaults for $environment")
        }
    }
}
