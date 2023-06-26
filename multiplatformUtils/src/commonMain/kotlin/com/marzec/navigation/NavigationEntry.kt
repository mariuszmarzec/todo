package com.marzec.navigation

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

val SECONDARY_ID = "SECONDARY_ID"

data class NavigationEntry(
    val destination: Destination,
    val cacheKey: String,
    val requestKey: RequestKey? = null,
    val screenProvider: @Composable (destination: Destination, cacheKey: String) -> Unit
)

data class RequestKey(
    val requesterKey: String,
    val requestId: Int,
    val options: Map<String, Any> = emptyMap()
)

@Serializable
data class ResultKey(
    val requesterKey: String,
    val requestId: Int
)

val RequestKey.secondaryId: Any?
    get() = options[SECONDARY_ID] as? Any

val RequestKey.secondaryIdValue: Any
    get() = options.getValue(SECONDARY_ID) as Any
