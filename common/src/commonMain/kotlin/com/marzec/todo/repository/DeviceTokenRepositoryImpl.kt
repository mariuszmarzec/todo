package com.marzec.todo.repository

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import com.marzec.logger.Logger
import com.marzec.todo.Api
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

class DeviceTokenRepositoryImpl(
    private val fileCache: FileCache,
    private val client: HttpClient,
    private val logger: Logger,
) : DeviceTokenRepository {

    override suspend fun updateToken(token: String, isLogged: Boolean) {
        try {
            val oldToken = fileCache.getTyped<String>(DEVICE_TOKEN)
            if (oldToken != token) {
                if (oldToken != null && isLogged) {
                    client.delete(deleteToken(oldToken))
                }
                fileCache.putTyped(DEVICE_TOKEN, token)
                if (isLogged) {
                    client.post(FCM_API_URL) {
                        setBody(CreateFcmTokenDto(token, "android"))
                    }.body<Unit>()
                }
            }
        } catch (e: Exception) {
            logger.log("DeviceTokenRepositoryImpl", "updateToken", e)
        }
    }

    override suspend fun sendCurrentToken() {
        try {
            val fcmToken = fileCache.getTyped<String>(DEVICE_TOKEN)
            fcmToken ?: return
            val res = client.post(FCM_API_URL) {
                setBody(CreateFcmTokenDto(fcmToken, "android"))
            }.body<FcmTokenDto>()
            logger.log("DeviceTokenRepositoryImpl", res.toString())
        } catch (e: Exception) {
            logger.log("DeviceTokenRepositoryImpl", "saveToken", e)
        }
    }

    override suspend fun removeCurrentToken() {
        try {
            val oldToken = fileCache.getTyped<String>(DEVICE_TOKEN)
            oldToken?.let {
                client.delete(deleteToken(it)).body<Unit>()
            }
        } catch (e: Exception) {
            logger.log("DeviceTokenRepositoryImpl", "removeCurrentToken", e)
        }
    }

    private companion object {
        const val DEVICE_TOKEN = "fcm_device_token"

        val FCM_API_URL = "${Api.Login.BASE}/fcm-token"

        fun deleteToken(token: String) = "$FCM_API_URL/$token"
    }
}

@Serializable
data class CreateFcmTokenDto(
    val fcmToken: String,
    val platform: String? = null
)

@Serializable
data class FcmTokenDto(
    val id: Int,
    val userId: Int,
    val fcmToken: String,
    val platform: String?,
    val updatedAt: String
)
