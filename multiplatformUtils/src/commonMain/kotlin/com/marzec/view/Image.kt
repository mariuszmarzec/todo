package com.marzec.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.marzec.cache.Cache
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.logger.Logger
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import com.marzec.time.currentTime
import io.ktor.client.*
import io.ktor.client.call.receive
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.jetbrains.skia.Image as ImageUtil

const val IMAGES_DIR = "images"

@Composable
fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter = ColorPainter(Color.Gray),
    store: ImageStore = ImageStore(url, ImageLoader.imageMemoryCache, rememberCoroutineScope()),
    ) {
    val image by store.collectState {
        store.loadPicture()
    }

    if (image is State.Data) {
        Image(
            modifier = modifier,
            bitmap = image.data!!,
            contentDescription = contentDescription,
            contentScale = contentScale
        )
    } else {
        Image(
            modifier = modifier,
            painter = placeholder,
            contentDescription = contentDescription
        )
    }
}

class ImageStore(private val url: String, imageCache: Cache, scope: CoroutineScope) :
    Store3<State<ImageBitmap>>(
        scope,
        runBlocking<State<ImageBitmap>?> {
            imageCache.get<ByteArray>(url)?.let { State.Data(it.toImageBitmap()) }
        }
            ?: State.Loading(null)) {

    override val identifier: Any
        get() = currentTime()

    fun loadPicture() = intent<Content<ImageBitmap>>("image") {
        onTrigger {
            ImageLoader.loadPicture(url).mapNotNull {
                it?.let {
                    asContent { it }
                } ?: Content.Loading(null)
            }
        }
        reducer {
            val resultNonNull = resultNonNull()
            if (resultNonNull is Content.Data) {
                State.Data(resultNonNull.data)
            } else if (state is State.Data) {
                state
            } else {
                State.Loading(null)
            }
        }
    }
}

interface ImageLoader {
    suspend fun loadPicture(url: String): Flow<ImageBitmap?>

    companion object : ImageLoader {

        lateinit var imageMemoryCache: Cache
        lateinit var ioDispatcher: CoroutineDispatcher
        lateinit var clientProvider: () -> HttpClient

        private val imageLoader: ImageLoader by lazy {
            ImageLoaderDesktop(clientProvider, imageMemoryCache, ioDispatcher)
        }

        override suspend fun loadPicture(url: String): Flow<ImageBitmap?> =
            imageLoader.loadPicture(url)
    }
}

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
        memoryCache.observe<ByteArray>(url).map {
            it?.toImageBitmap()
        }
    }


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


fun ByteArray.toImageBitmap() = ImageUtil.makeFromEncoded(this).asImageBitmap()