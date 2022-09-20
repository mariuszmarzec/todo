package com.marzec.navigation

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

internal val SECONDARY_ID = "SECONDARY_ID"

data class NavigationEntry(
    val destination: Destination,
    val cacheKey: String,
    val requestKey: RequestKey? = null,
    val screenProvider: @Composable (destination: Destination, cacheKey: String) -> Unit
)

data class RequestKey(
    val requesterKey: String,
    val requestId: Int,
    val options: Map<String, String> = emptyMap()
)

@Serializable
data class ResultKey(
    val requesterKey: String,
    val requestId: Int
)

val RequestKey.secondaryId: Int?
    get() = options[SECONDARY_ID]?.toInt()

val RequestKey.secondaryIdValue: Int
    get() = options.getValue(SECONDARY_ID).toInt()
