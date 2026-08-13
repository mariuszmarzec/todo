package com.marzec.navigation

interface NavigationCache {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}