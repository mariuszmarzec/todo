package com.marzec.resource

import java.net.URI

class ResourceLoaderImpl : ResourceLoader {
    override fun loadResource(uri: String): String? {
        val resourceUrl = runCatching { URI(uri).toURL() }.getOrNull() ?: return null
        return resourceUrl.openStream().bufferedReader().use { it.readText() }
    }
}
