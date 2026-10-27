package com.marzec.resource

import android.content.Context
import com.marzec.common.readFile

class ResourceLoaderImpl(private val context: Context) : ResourceLoader {

    override fun loadResource(name: String): String? = context.readFile(name)
}