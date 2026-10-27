package com.marzec.resource

import com.marzec.todo.common.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads files from this module's Compose Multiplatform common resources.
 *
 * The caller provides a path relative to composeResources/files; the prefix and generated Res URI
 * stay encapsulated here, rather than leaking into DI or changing ResourceLoaderImpl's old API.
 */
class ComposeResourceLoader(
    private val resourceUriLoader: ResourceUriLoader
) : ResourceLoader {

    override fun loadResource(name: String): String? = loadResourceFile(name)

    @OptIn(ExperimentalResourceApi::class)
    fun loadResourceFile(path: String): String? =
        resourceUriLoader.loadResourceUri(Res.getUri("files/$path"))
}
