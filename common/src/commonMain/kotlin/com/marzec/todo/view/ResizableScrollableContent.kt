package com.marzec.todo.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal const val RESIZABLE_CONTENT_VIEWPORT_TAG = "resizableContentViewport"
internal const val RESIZABLE_CONTENT_HANDLE_TAG = "resizableContentHandle"

private val minimumResizeHeight = 72.dp
private const val maximumResizeHeightFraction = 2f / 3f

/**
 * Displays arbitrary composable content in a vertically scrollable viewport.
 *
 * With subtasks, the viewport starts at at most half of the available height. If
 * the content exceeds that initial limit, a drag handle lets the user resize it.
 * The handle remains visible while the content is taller than the initial limit,
 * even after the viewport has been expanded far enough to remove the scroll range.
 *
 * Without subtasks, the viewport uses all available height and is never resizable.
 * The parent must provide a bounded height so the scroll viewport remains finite.
 */
@Composable
fun ResizableScrollableContent(
    modifier: Modifier = Modifier,
    hasSubtasks: Boolean,
    content: @Composable () -> Unit
) {
    val scrollState = remember(hasSubtasks) { ScrollState(0) }
    var contentHeightPx by remember(hasSubtasks) { mutableIntStateOf(0) }
    var resizedHeightPx by remember(hasSubtasks) { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier) {
        check(maxHeight != Dp.Infinity) {
            "ResizableScrollableContent requires bounded height constraints."
        }

        val density = LocalDensity.current
        val initialMaxHeight = maxHeight / 2f
        val initialMaxHeightPx = with(density) { initialMaxHeight.roundToPx() }
        val minResizeHeightPx = with(density) {
            minOf(minimumResizeHeight, initialMaxHeight).roundToPx()
        }
        // Reserve at least one third of the available height for the subtask list
        // and the summary/spacing that follows the description row.
        val maxResizeHeightPx = with(density) {
            (maxHeight * maximumResizeHeightFraction).roundToPx()
        }.coerceAtLeast(minResizeHeightPx)

        val latestInitialHeightPx = rememberUpdatedState(initialMaxHeightPx)
        val latestMinHeightPx = rememberUpdatedState(minResizeHeightPx)
        val latestMaxHeightPx = rememberUpdatedState(maxResizeHeightPx)

        val viewportHeightPx = (resizedHeightPx.takeIf { it > 0 } ?: initialMaxHeightPx)
            .coerceIn(minResizeHeightPx, maxResizeHeightPx)
        val viewportHeight = with(density) { viewportHeightPx.toDp() }
        val shouldShowResizeHandle = hasSubtasks && contentHeightPx > initialMaxHeightPx

        Column(
            modifier = if (hasSubtasks) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().fillMaxHeight()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when {
                            !hasSubtasks -> Modifier.fillMaxHeight()
                            resizedHeightPx > 0 -> Modifier.height(viewportHeight)
                            else -> Modifier.heightIn(max = initialMaxHeight)
                        }
                    )
                    .verticalScroll(scrollState)
                    .testTag(RESIZABLE_CONTENT_VIEWPORT_TAG)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (contentHeightPx != size.height) {
                                contentHeightPx = size.height
                            }
                        }
                ) {
                    content()
                }
            }

            if (shouldShowResizeHandle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { dragAmount ->
                                val currentHeightPx = resizedHeightPx
                                    .takeIf { it > 0 }
                                    ?: latestInitialHeightPx.value
                                resizedHeightPx = (currentHeightPx + dragAmount.roundToInt())
                                    .coerceIn(latestMinHeightPx.value, latestMaxHeightPx.value)
                            }
                        )
                        .semantics {
                            contentDescription = "Resize description"
                            stateDescription = "Description height: ${viewportHeight.value.toInt()} dp"
                        }
                        .testTag(RESIZABLE_CONTENT_HANDLE_TAG),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
            }
        }
    }
}
