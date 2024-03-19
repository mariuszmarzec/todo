package com.marzec.example.navigation

import com.marzec.navigation.Destination

internal sealed class NavigationExampleDestination : Destination {
    data object HomeScreen : NavigationExampleDestination()

    data object A : NavigationExampleDestination()

    data object B : NavigationExampleDestination()
}