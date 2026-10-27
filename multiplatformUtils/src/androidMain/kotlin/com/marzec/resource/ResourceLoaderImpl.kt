package com.marzec.resource

import android.content.Context
import android.net.Uri

class ResourceLoaderImpl(private val context: Context) : ResourceLoader {

    override fun loadResource(uri: String): String? {
        val path = Uri.parse(uri).path ?: return null
        val assetsPrefix = "/android_asset/"
        if (!path.startsWith(assetsPrefix)) return null

        val assetPath = path.removePrefix(assetsPrefix)
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
}
