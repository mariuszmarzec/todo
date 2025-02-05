package com.marzec.featuretoggle

import com.marzec.cache.MemoryCache
import com.marzec.content.ifDataSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FeatureTogglesManagerImpl(
    private val featureTogglesDefaults: Map<String, String>,
    private val featureToggleRepository: FeatureToggleRepository,
    private val memoryCache: MemoryCache,
    private val updaterCoroutineScope: CoroutineScope
) : FeatureTogglesManager {

    fun init() {
        runBlocking {
            featureTogglesDefaults.forEach {
                memoryCache.put(it.key, it.value)
            }
        }
        update()
    }

    private fun update() {
        updaterCoroutineScope.launch {
            featureToggleRepository.observeAll().collect { featureToggles ->
                featureToggles.ifDataSuspend {
                    data.forEach {
                        memoryCache.put(it.name, it.value)
                    }
                }
            }
        }
    }

    override fun observe(featureToggle: String): Flow<Boolean> = runBlocking {
        memoryCache.observe<String>(featureToggle)
            .map { it.toBoolean() }
            .also { triggerUpdate() }
    }

    private fun triggerUpdate() {
        updaterCoroutineScope.launch {
            featureToggleRepository.refreshCache()
        }
    }

    override fun get(featureToggle: String): Boolean = runBlocking {
        memoryCache.get<String>(featureToggle).toBoolean()
    }
}
