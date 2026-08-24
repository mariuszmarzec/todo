package com.marzec.navigation

import com.marzec.core.StoreTest
import com.marzec.core.runStoreTest
import com.marzec.navigation.NavigationStateCache
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationStoreTest {

    val requestId = 1

    val stateWithCurrentFlow = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "2"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "3",
                            requestKey = RequestKey(
                                requesterKey = "1",
                                requestId = requestId
                            ),
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "4",
                                        requestKey = RequestKey(
                                            requesterKey = "1",
                                            requestId = requestId
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    val stateWithSubFlow = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "2",
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        destination = TestDestination.B,
                                        cacheKey = "3"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            NavigationEntry(destination = TestDestination.A, "4"),
        )
    )

    val stateWithSubFlowsAndEntryIds = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, "0"),
            NavigationEntry(
                destination = SubFlow(TestDestination.B, "subflow"),
                cacheKey = "1",
                id = "entry",
                subFlow = NavigationFlow(
                    id = "subflow",
                    backStack = listOf(
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow2"),
                            cacheKey = "2",
                            id = "entry2",
                            subFlow = NavigationFlow(
                                id = "subflow2",
                                backStack = listOf(
                                    NavigationEntry(
                                        id = "entry3",
                                        destination = TestDestination.B,
                                        cacheKey = "3"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            NavigationEntry(
                destination = TestDestination.A,
                cacheKey = "4",
                id = "entry4",
            ),
        )
    )

    var keyProviderIncrement = 0

    val stateCache: NavigationStateCache = mockk(relaxed = true)
    val resultCache: ResultCache = mockk(relaxed = true)
    val cacheKey: String = "navigation_cache_key"
    val cacheKeyProvider: () -> String = { keyProviderIncrement++.toString() }
    val defaultState: NavigationState = navigationState(
        backStack = listOf(
            NavigationEntry(destination = TestDestination.A, cacheKeyProvider())
        )
    )
    val overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null

    @Before
    fun setUp() {
        coEvery { stateCache.read<NavigationState>(any()) } returns null
    }

    @Test
    fun initialization() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                )
            )

            coVerify {
                stateCache.read("navigation_cache_key")
                resultCache wasNot called
            }
        }
    }

    @Test
    fun next_Destination() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(TestDestination.B)

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1")
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }

        }
    }

    @Test
    fun next_Destination_WithRequest() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)
        val requestId = 1

        store.test {

            next(
                NavigationAction(destination = TestDestination.B),
                requestId = requestId
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = 1
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_Destination_WithOptions() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)
        val requestId = 1

        store.test {

            nextWithOptionRequest(
                action = NavigationAction(destination = TestDestination.B),
                requestId = requestId,
                options = mapOf("Options" to 1)
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = 1,
                                options = mapOf("Options" to 1)
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_Destination_WithRequestAndSecondaryId() = runTest {

        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(
                NavigationAction(destination = TestDestination.B),
                requestId = requestId,
                secondaryId = 2
            )

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            "1",
                            requestKey = RequestKey(
                                requesterKey = "0",
                                requestId = requestId,
                                options = mapOf("SECONDARY_ID" to 2)
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }
        }
    }

    @Test
    fun next_Destination_Then_Back() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(TestDestination.B)

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1")
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }

            goBack()

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                ),
                defaultState
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("1")
            }
        }
    }

    @Test
    fun next_Destination_Then_CloseFlow() = runTest {
        keyProviderIncrement = 2
        store = navigationStore(stateWithSubFlow)

        store.test {

            next(TestDestination.B)

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.B, "2"),
                                    NavigationEntry(destination = TestDestination.B, "3")
                                )
                            )
                        ),
                        NavigationEntry(destination = TestDestination.A, "4")
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("1")
            }

            closeFlow()

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.A, "4")
                    )
                ),
                stateWithSubFlow
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("2")
                stateCache.remove("2")
                resultCache.remove("3")
                stateCache.remove("3")
            }
        }
    }

    @Test
    fun next_Destination_Then_GoBack_Then_Next_Then_GoBack() = runTest {
        store = navigationStore(overrideLastClose = overrideLastClose)

        store.test {

            next(TestDestination.B)

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1")
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }

            goBack()

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                ),
                defaultState
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("1")
            }

            next(TestDestination.B)

            values.isEqualTo(
                defaultState,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(destination = TestDestination.B, "1")
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("0")
            }

            goBack()

            values.isEqualTo(
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0")
                    )
                ),
                defaultState
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())
                resultCache.remove("1")
            }
        }
    }

    @Test
    fun next_Destination_WithSubFlow_Then_Back() = runTest {
        keyProviderIncrement = 2
        store = navigationStore(stateWithSubFlow)

        store.test {

            next(
                SubFlow(TestDestination.B, "subflow"),
            )

            values.isEqualTo(
                stateWithSubFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = TestDestination.B,
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.B, "2")
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")
            }
        }
    }

    @Test
    fun closeFlow_withResult() = runTest {
        keyProviderIncrement = 5
        store = navigationStore(stateWithCurrentFlow)

        store.test {

            closeFlow("result")

            values.isEqualTo(
                stateWithCurrentFlow,
                navigationState(
                    backStack = listOf(
                        NavigationEntry(destination = TestDestination.A, "0"),
                        NavigationEntry(
                            destination = SubFlow(TestDestination.B, "subflow"),
                            cacheKey = "1",
                            subFlow = NavigationFlow(
                                id = "subflow",
                                backStack = listOf(
                                    NavigationEntry(destination = TestDestination.A, "2")
                                )
                            )
                        )
                    )
                )
            )

            coVerify {
                stateCache.write("navigation_cache_key", any())

                resultCache.save(RequestKey(
                    requesterKey = "1",
                    requestId = requestId
                ), "result")

                resultCache.remove("4")
                stateCache.remove("4")

                resultCache.remove("3")
                stateCache.remove("3")
            }
        }
    }

    @Test
    fun closeFlow_overrideIfRootFlow() {
        val initialState = navigationState(
            backStack = listOf(
                NavigationEntry(
                    TestDestination.A,
                    "0"
                )
            )
        )
        var calledLastClose = false
        val overrideLastClose: NavigationState.() -> NavigationUpdate =
            {
                calledLastClose = true
                NavigationUpdate(this, emptyList())
            }
        runTest {
            store = navigationStore(initialState, overrideLastClose)

            store.test {

                closeFlow()

                values.isEqualTo(initialState)

                assertTrue(calledLastClose)
            }
        }
    }

    private fun StoreTest<NavigationState, NavigationStore>.navigationStore(
        initialState: NavigationState = defaultState,
        overrideLastClose: (NavigationState.() -> NavigationUpdate)? = this@NavigationStoreTest.overrideLastClose
    ) =
        NavigationStore(
            scope = scope,
            navigationStateCache = stateCache,
            resultCache = resultCache,
            cacheKey = cacheKey,
            cacheKeyProvider = cacheKeyProvider,
            initialState = initialState,
            overrideLastClose = overrideLastClose,
        )

    private fun runTest(block: suspend StoreTest<NavigationState, NavigationStore>.() -> Unit) {
        runStoreTest(block)
    }
}

sealed class TestDestination : Destination {

    object A : Destination

    object B : Destination

    object C : Destination
}
