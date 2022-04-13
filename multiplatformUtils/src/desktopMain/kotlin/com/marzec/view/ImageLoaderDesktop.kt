package com.marzec.view

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.marzec.cache.Cache
import com.marzec.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.File
import kotlin.collections.contains
import kotlin.collections.firstOrNull
import kotlin.collections.listOf
import kotlin.collections.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as ImageUtil

class ImageLoaderDesktop(
    private val clientProvider: () -> HttpClient,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher
) : ImageLoader {

    private val job = Job()
    private val scope = CoroutineScope(job)
    private val commands = MutableSharedFlow<suspend () -> Unit>(extraBufferCapacity = 200)

    init {
        scope.launch {
            commands.collect {
                it()
            }
        }
    }

    override suspend fun loadPicture(url: String): Flow<ImageBitmap?> = withContext(dispatcher) {
        scope.launch {
            if (memoryCache.get<ByteArray>(url) == null) {
                commands.emit { loadImage(url) }
            }
        }
        memoryCache.observe<ByteArray>(url).map { bytes ->
            bytes?.let { toImageBitmap(it) }
        }
    }

    override fun toImageBitmap(bytes: ByteArray): ImageBitmap = bytes.toImageBitmap()


    private suspend fun loadImage(url: String) = withContext(dispatcher) {
        try {
            val supportedContentTypes =
                listOf(ContentType.Image.GIF, ContentType.Image.JPEG, ContentType.Image.PNG)
            val supportedImagesExtensions = supportedContentTypes.map { it.contentSubtype }
            val cachedImage = supportedImagesExtensions.map { urlToFile(url, it) }.firstOrNull {
                it.exists()
            }
            if (cachedImage != null) {
                memoryCache.put(url, cachedImage.readBytes())
            } else {
                clientProvider().use { client ->
                    val httpResponse: HttpResponse = client.get(url)
                    val loadedImage = httpResponse.receive<ByteArray>()
                    memoryCache.put(url, loadedImage)
                    when (val contentType = httpResponse.contentType()) {
                        in supportedContentTypes -> {
                            saveImageToFile(loadedImage, url, contentType?.contentSubtype.orEmpty())
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Logger.log("ImageLoader", exception.message.orEmpty(), exception)
            null
        }
    }
}


private fun saveImageToFile(
    image: ByteArray,
    url: String,
    extension: String
) {
    val imagesDir = File(IMAGES_DIR)
    if (!imagesDir.exists()) {
        imagesDir.mkdir()
    }

    val imageFile = urlToFile(url, extension)
    imageFile.writeBytes(image)
}

private fun urlToFile(url: String, extension: String): File {
    val imagesDir = File(IMAGES_DIR)
    val fileName = url.replace(Regex("[:/]"), "_")
    return File(imagesDir.absolutePath + File.separator + fileName + "." + extension)
}

private fun ByteArray.toImageBitmap() = ImageUtil.makeFromEncoded(this).asImageBitmap()