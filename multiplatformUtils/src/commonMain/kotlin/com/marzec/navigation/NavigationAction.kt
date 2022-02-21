package com.marzec.navigation

data class NavigationAction(
    val destination: Destination,
    val options: NavigationOptions? = null
)

data class NavigationOptions(
    val popTo: Destination,
    val popToInclusive: Boolean
)

interface Destination
