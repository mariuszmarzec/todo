package com.marzec.resource

class ResourceLoaderImpl : ResourceLoader {
    override fun loadResource(name: String): String? {
        return this::class.java.classLoader?.getResource(name)?.readText()
    }
}