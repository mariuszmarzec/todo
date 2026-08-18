package com.marzec.navigation

/**
 * Interface for editing state values.
 */
interface StateEditor {
    suspend fun <T> write(key: String, value: T?)
    suspend fun remove(key: String)
}