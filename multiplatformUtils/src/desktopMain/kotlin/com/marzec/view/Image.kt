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
import kotlin.String
import com.marzec.mvi.State as Result

const val IMAGES_DIR = "images"

@Composable
actual fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderColor: Color,
) {
    ImageInternal(
        url,
        contentDescription,
        modifier,
        contentScale,
        ColorPainter(placeholderColor)
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
) {
    val image by loadNetworkImage(url, imageLoader)

    if (image is Result.Data<ImageBitmap>) {
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

@Composable
fun loadNetworkImage(
    url: String,
    imageLoader: ImageLoader
): State<Result<ImageBitmap>> =
    produceState<Result<ImageBitmap>>(initialValue = Result.Loading(), url, imageLoader) {

        imageLoader.loadPicture(url).collect {
            value = if (it == null) {
                Result.Error(message = "No image")
            } else {
                Result.Data(it)
            }
        }

    }
