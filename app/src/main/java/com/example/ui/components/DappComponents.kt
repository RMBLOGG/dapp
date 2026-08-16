package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

/**
 * Signature Wordmark: "Dap" in FogWhite + "p" in SignalBlue
 */
@Composable
fun DappWordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 26
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = FogWhite,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = fontSize.sp,
                    letterSpacing = (-0.5).sp
                )
            ) {
                append("Dap")
            }
            withStyle(
                style = SpanStyle(
                    color = SignalBlue,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = fontSize.sp,
                    letterSpacing = (-0.5).sp
                )
            ) {
                append("p")
            }
        },
        modifier = modifier.testTag("dapp_wordmark")
    )
}

/**
 * Signature Avatar with Pulse Ring for online status and optional story ring
 */
@Composable
fun DappAvatar(
    avatarUrl: String?,
    displayName: String,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasActiveStory: Boolean = false,
    isStoryViewed: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val ringModifier = when {
        hasActiveStory -> {
            Modifier.border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        if (isStoryViewed) SlateMuted else PulseTeal,
                        SignalBlue,
                        if (isStoryViewed) SlateMuted else PulseTeal
                    )
                ),
                shape = CircleShape
            ).padding(3.dp)
        }
        isOnline -> {
            Modifier.border(
                width = 1.5.dp,
                color = PulseTeal.copy(alpha = pulseAlpha),
                shape = CircleShape
            ).padding(2.5.dp)
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .clip(CircleShape)
            .background(PanelColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = displayName.take(1).uppercase().ifBlank { "D" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    color = SignalBlue,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Signature Asymmetric Chat Bubble
 */
@Composable
fun DappChatBubble(
    isFromMe: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bubbleShape = if (isFromMe) {
        // Sent bubble: topStart 16, topEnd 16, bottomStart 16, bottomEnd 4
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        // Received bubble: topStart 16, topEnd 16, bottomStart 4, bottomEnd 16
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    val bubbleBg = if (isFromMe) SignalBlue else ElevatedColor

    Surface(
        shape = bubbleShape,
        color = bubbleBg,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Signature Reply Thread Line with Pulse Teal accent
 */
@Composable
fun DappThreadLine(
    senderName: String,
    quotedText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GraphiteVoid.copy(alpha = 0.45f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulse Teal vertical line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PulseTeal)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    color = PulseTeal,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = quotedText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    color = FogWhite.copy(alpha = 0.8f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Reaction Chip with Pulse Teal border
 */
@Composable
fun DappReactionChip(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PulseTeal.copy(alpha = 0.18f) else PanelColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) PulseTeal else BorderDark
        ),
        modifier = modifier
            .testTag("reaction_chip_$emoji")
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 13.sp
            )
            if (count > 1) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = if (isSelected) PulseTeal else SlateText,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Custom Styled Outlined Button
 */
@Composable
fun DappButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "dapp_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) SignalBlue else PanelColor,
            contentColor = if (isPrimary) FogWhite else FogWhite,
            disabledContainerColor = SlateMuted.copy(alpha = 0.4f),
            disabledContentColor = SlateText
        ),
        border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, BorderDark) else null,
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = FogWhite,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = FogWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Custom Input Field
 */
@Composable
fun DappTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    testTag: String = "dapp_text_field"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    color = SlateText
                )
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = SlateText,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PanelColor,
            unfocusedContainerColor = PanelColor,
            focusedBorderColor = SignalBlue,
            unfocusedBorderColor = BorderDark,
            focusedTextColor = FogWhite,
            unfocusedTextColor = FogWhite,
            cursorColor = PulseTeal
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = InterFontFamily,
            color = FogWhite
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
