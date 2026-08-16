package com.example.ui.screens.story

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.DappRepository
import com.example.ui.components.DappAvatar
import com.example.ui.components.DappButton
import com.example.ui.components.DappTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun StoryViewScreen(
    storyId: String,
    repository: DappRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by repository.stories.collectAsState()
    val story = remember(stories, storyId) { stories.find { it.id == storyId } }
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(storyId) {
        repository.viewStory(storyId)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        onClose()
    }

    if (story == null) {
        onClose()
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteVoid)
            .clickable { onClose() }
    ) {
        // Story Media Image
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Story Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            GraphiteVoid.copy(alpha = 0.75f),
                            GraphiteVoid.copy(alpha = 0.1f),
                            GraphiteVoid.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Info & Progress Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Linear progress indicator
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PulseTeal,
                trackColor = FogWhite.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DappAvatar(
                    avatarUrl = story.user?.avatarUrl,
                    displayName = story.user?.displayName ?: "User",
                    size = 38.dp,
                    isOnline = story.user?.isOnline == true
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            color = FogWhite,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Baru saja • Supabase Story",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = PulseTeal,
                            fontSize = 10.sp
                        )
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("btn_close_story")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = FogWhite
                    )
                }
            }
        }

        // Bottom Caption
        if (story.caption.isNotBlank()) {
            Surface(
                color = PanelColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = story.caption,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        color = FogWhite
                    ),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun CreateStoryDialog(
    repository: DappRepository,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var selectedMediaUrl by remember {
        mutableStateOf("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80")
    }
    var isPosting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val sampleMedia = listOf(
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelColor,
        titleContentColor = FogWhite,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Buat Story Baru",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Selected preview
                AsyncImage(
                    model = selectedMediaUrl,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // Select image options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleMedia.forEach { url ->
                        val isSelected = url == selectedMediaUrl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) PulseTeal else BorderDark,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedMediaUrl = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                DappTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = "Tambahkan caption story...",
                    testTag = "input_story_caption"
                )
            }
        },
        confirmButton = {
            DappButton(
                text = "Bagikan Story",
                onClick = {
                    isPosting = true
                    coroutineScope.launch {
                        repository.createStory(selectedMediaUrl, caption)
                        isPosting = false
                        onDismiss()
                    }
                },
                icon = Icons.Outlined.Send,
                isLoading = isPosting,
                testTag = "btn_post_story"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = SlateText)
            }
        }
    )
}
