package com.marzec.resource

import java.net.URI

class ResourceUriLoaderImpl : ResourceUriLoader {
    override fun loadResourceUri(uri: String): String? {
        val resourceUrl = runCatching { URI(uri).toURL() }.getOrNull() ?: return null
        return resourceUrl.openStream().bufferedReader().use { it.readText() }
    }
}
