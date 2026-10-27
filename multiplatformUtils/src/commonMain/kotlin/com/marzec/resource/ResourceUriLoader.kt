package com.marzec.resource

/**
 * Reads a platform-specific URI, for example one returned by Compose Multiplatform's Res.getUri().
 *
 * Kept separate from ResourceLoader so the original name-based API and its implementations remain
 * unchanged for projects that reuse ResourceLoaderImpl.
 */
interface ResourceUriLoader {
    fun loadResourceUri(uri: String): String?
}
