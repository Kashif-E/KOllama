package com.kashif.kollama.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.core.Dialog
import com.composables.core.DialogProperties
import com.composables.core.Scrim
import com.composables.core.rememberDialogState
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@Composable
fun RightSideSheet(
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    isVisible: Boolean,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val dialogState = rememberDialogState(initiallyVisible = isVisible)
    if (isVisible) {
        val offsetX = remember { Animatable(400f) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(isVisible) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }

        Dialog(
            state = dialogState,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            ),
            onDismiss = onDismiss
        ) {
            Scrim()
            Box(
                modifier = Modifier
                    .fillMaxSize().clickable(interactionSource = null, indication = null) {
                        onDismiss()
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    modifier = modifier
                        .fillMaxHeight()
                        .clip(shape)
                        .width(400.dp)
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .pointerInput(Unit) {

                            detectHorizontalDragGestures(
                                onDragStart = { offset ->

                                    if (offset.x > 20.dp.toPx()) {
                                        throw CancellationException("Drag not started in drag zone")
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value > 100) {
                                            offsetX.animateTo(
                                                targetValue = 400f,
                                                animationSpec = tween(
                                                    durationMillis = 300,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                            onDismiss()
                                        } else {
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                                    }
                                    change.consume()
                                }
                            )
                        }.background(containerColor, shape).clip(shape),
                ) {
                    content()
                }

            }
        }
    }
}

@Composable
fun AnimatedBorderBox(
    borderColors: List<Color>,
    backgroundColor: Color,
    shape: Shape = RectangleShape,
    borderWidth: Dp = 1.dp,
    animationDurationInMillis: Int = 3000,
    easing: Easing = LinearEasing,
    content: @Composable BoxScope.() -> Unit
) {

    val brush = Brush.sweepGradient(borderColors)
    val infiniteTransition = rememberInfiniteTransition(label = "animatedBorder")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationInMillis, easing = easing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleAnimation"
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .padding(borderWidth)
            .drawWithContent {
                val diagonal = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
                rotate(angle) {
                    drawCircle(
                        brush = brush,
                        radius = diagonal / 2,
                        blendMode = BlendMode.SrcIn,
                        center = center
                    )
                }
                drawContent()
            }
            .background(color = backgroundColor, shape = shape)
    ) {
        content()
    }
}