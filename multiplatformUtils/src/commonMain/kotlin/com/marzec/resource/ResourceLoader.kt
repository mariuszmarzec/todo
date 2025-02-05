package com.marzec.resource

interface ResourceLoader {
    fun loadResource(name: String): String?
}