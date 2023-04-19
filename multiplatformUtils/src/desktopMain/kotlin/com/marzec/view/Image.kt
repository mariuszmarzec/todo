package com.marzec.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.animatedimage.animate
import kotlin.String

const val IMAGES_DIR = "images"

@Composable
actual fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderColor: Color,
    animationEnabled: Boolean
) {
    ImageInternal(
        url,
        contentDescription,
        modifier,
        contentScale,
        ColorPainter(placeholderColor),
        animationEnabled = animationEnabled
    )
}

@Composable
private fun ImageInternal(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter = ColorPainter(Color.Gray),
    imageLoader: ImageLoader = ImageLoader.imageLoader,
    animationEnabled: Boolean = true
) {
    val image = loadImage(url, imageLoader, animationEnabled)

    if (image != null) {
        Image(
            modifier = modifier,
            bitmap = image,
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

@Composable
fun loadImage(
    url: String,
    imageLoader: ImageLoader,
    animationEnabled: Boolean
): ImageBitmap? {
    val image by loadImageState(url, imageLoader)

    return image?.let {
        runCatching {
            if (animationEnabled && it.isAnimated()) {
                it.toAnimatedImage().animate()
            }
            else {
                it.bytes.toImageBitmap()
            }
        }.getOrNull()
    }
}

@Composable
fun loadImageState(
    url: String,
    imageLoader: ImageLoader
): State<Image?> =
    produceState(
        initialValue = runBlocking { ImageLoader.imageMemoryCache.get(url) },
        key1 = url,
        key2 = imageLoader
    ) {
        imageLoader.loadPicture(url).collect { image ->
            value = image
        }
    }
