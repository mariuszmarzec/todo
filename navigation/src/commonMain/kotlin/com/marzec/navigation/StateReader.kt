package com.marzec.navigation

/**
 * Interface for reading state values.
 */
interface StateReader {
    suspend fun <T> read(key: String): T?
}