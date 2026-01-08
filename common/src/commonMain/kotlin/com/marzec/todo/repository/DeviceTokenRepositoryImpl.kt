package com.marzec.todo.repository

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import com.marzec.logger.Logger
import com.marzec.todo.Api
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

class DeviceTokenRepositoryImpl(
    private val fileCache: FileCache,
    private val client: HttpClient,
    private val logger: Logger,
) : DeviceTokenRepository {
    override suspend fun saveToken(token: String) {
        try {
            val oldToken = fileCache.getTyped<String>(DEVICE_TOKEN)
            if (oldToken != token) {
                if (oldToken != null) {
                    client.delete(deleteToken(oldToken))
                }
                fileCache.putTyped(DEVICE_TOKEN, token)
                client.post(FCM_API_URL) {
                    setBody(CreateFcmTokenDto(token, "android"))
                }
            }
        } catch (e: Exception) {
            logger.log("DeviceTokenRepositoryImpl", "saveToken", e)
        }

    }

    override suspend fun removeCurrentToken() {
        try {
            val oldToken = fileCache.getTyped<String>(DEVICE_TOKEN)
            oldToken?.let {
                client.delete(deleteToken(it))
            }
        } catch (e: Exception) {
            logger.log("DeviceTokenRepositoryImpl", "removeCurrentToken", e)
        }
    }

    private companion object {
        const val DEVICE_TOKEN = "fcm_device_token"

        val FCM_API_URL = "${Api.HOST}/fcm-token"

        fun deleteToken(token: String) = "$FCM_API_URL/$token"
    }
}

@Serializable
data class CreateFcmTokenDto(
    val fcmToken: String,
    val platform: String? = null
)
