package com.marzec.navigation

import kotlinx.coroutines.CoroutineScope

fun initialState(
    defaultDestination: Destination,
    cacheKeyProvider: () -> String
) = navigationState(
    backStack = listOf(
        NavigationEntry(
            destination = defaultDestination,
            cacheKey = cacheKeyProvider()
        )
    )
)
