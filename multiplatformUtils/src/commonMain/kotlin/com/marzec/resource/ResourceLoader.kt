package com.marzec.resource

interface ResourceLoader {
    /**
     * Loads a resource from the platform-specific URI returned by Compose Multiplatform's Res.getUri().
     */
    fun loadResource(uri: String): String?
}