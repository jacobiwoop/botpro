package com.example.botpro.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class SkeletonItem(
    val isOutgoing: Boolean,
    val widthFraction: Float,
    val height: Dp
)

private val SKELETON_ITEMS = listOf(
    SkeletonItem(isOutgoing = false, widthFraction = 0.60f, height = 44.dp),
    SkeletonItem(isOutgoing = true, widthFraction = 0.45f, height = 40.dp),
    SkeletonItem(isOutgoing = false, widthFraction = 0.75f, height = 60.dp),
    SkeletonItem(isOutgoing = true, widthFraction = 0.35f, height = 40.dp),
    SkeletonItem(isOutgoing = false, widthFraction = 0.50f, height = 44.dp),
    SkeletonItem(isOutgoing = true, widthFraction = 0.65f, height = 54.dp)
)

@Composable
fun ConversationSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        SKELETON_ITEMS.forEach { item ->
            val alignment = if (item.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
            val bubbleBg = if (item.isOutgoing) Color(0xFF2B5278) else Color(0xFF202B36)
            val bubbleShape = if (item.isOutgoing) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                contentAlignment = alignment
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.widthFraction)
                        .height(item.height)
                        .alpha(pulseAlpha)
                        .clip(bubbleShape)
                        .background(bubbleBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        // Ligne de texte simulée 1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        if (item.height > 46.dp) {
                            Spacer(modifier = Modifier.height(6.dp))
                            // Ligne de texte simulée 2
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            )
                        }
                    }
                }
            }
        }
    }
}
