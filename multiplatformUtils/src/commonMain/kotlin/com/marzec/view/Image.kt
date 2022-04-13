package com.marzec.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.marzec.cache.Cache
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import com.marzec.time.currentTime
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

const val IMAGES_DIR = "images"

@Composable
fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter = ColorPainter(Color.Gray),
    store: ImageStore = ImageStore(
        url = url,
        imageCache = ImageLoader.imageMemoryCache,
        imageLoader = ImageLoader.imageLoader,
        scope = rememberCoroutineScope()
    ),
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

class ImageStore(
    private val url: String,
    imageCache: Cache,
    imageLoader: ImageLoader,
    scope: CoroutineScope
) :
    Store3<State<ImageBitmap>>(
        scope,
        runBlocking<State<ImageBitmap>?> {
            imageCache.get<ByteArray>(url)?.let { State.Data(imageLoader.toImageBitmap(it)) }
        } ?: State.Loading(null)) {

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

    fun toImageBitmap(bytes: ByteArray): ImageBitmap

    companion object : ImageLoader {

        lateinit var imageMemoryCache: Cache
        lateinit var ioDispatcher: CoroutineDispatcher
        lateinit var clientProvider: () -> HttpClient

        lateinit var imageLoader: ImageLoader

        override suspend fun loadPicture(url: String): Flow<ImageBitmap?> =
            imageLoader.loadPicture(url)

        override fun toImageBitmap(bytes: ByteArray): ImageBitmap = imageLoader.toImageBitmap(bytes)
    }
}
