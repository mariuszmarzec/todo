package com.marzec.featuretoggle

import android.content.Context
import com.marzec.cache.MemoryCache
import com.marzec.common.readFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

class FeatureTogglesManagerInitializerImpl(
    private val context: Context,
    private val json: Json,
    private val featureToggleRepository: FeatureToggleRepository,
    private val memoryCache: MemoryCache,
    private val updaterCoroutineScope: CoroutineScope,
    private val featureToggleConfFile: String
) : FeatureTogglesManagerInitializer {

    override fun create(): FeatureTogglesManager {
        val featureTogglesDefaults: Map<String, String> = context.readFile(featureToggleConfFile)
            .let {
                json.decodeFromString<Map<String, String>>(it)
            }
        return FeatureTogglesManagerImpl(
            featureTogglesDefaults = featureTogglesDefaults,
            featureToggleRepository = featureToggleRepository,
            memoryCache = memoryCache,
            updaterCoroutineScope = updaterCoroutineScope
        ).apply { init() }
    }
}
