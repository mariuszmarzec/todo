package com.marzec.view

import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

object GlideUrlInterceptor {
    var onUrl: (String) -> String = { it }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
actual fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderColor: Color,
) {
    GlideImage(
        model = GlideUrlInterceptor.onUrl(url),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    ) {
        it.placeholder(ColorDrawable(placeholderColor.toArgb()))
    }
}