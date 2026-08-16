package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatItem
import com.example.data.model.Story
import com.example.data.repository.DappRepository
import com.example.ui.components.DappAvatar
import com.example.ui.components.DappTextField
import com.example.ui.components.DappWordmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: DappRepository,
    onOpenChat: (chatId: String) -> Unit,
    onOpenFriends: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStory: (storyId: String) -> Unit,
    onAddStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val chats by repository.chats.collectAsState()
    val participants by repository.participants.collectAsState()
    val messages by repository.messages.collectAsState()
    val stories by repository.stories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "pinned", "group", "archived"
    var chatToManage by remember { mutableStateOf<ChatItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val chatItems = remember(chats, participants, messages, currentUser) {
        repository.getChatItems()
    }

    val activeStories = remember(stories, currentUser) {
        repository.getActiveStories()
    }

    val filteredChats = remember(chatItems, searchQuery, selectedFilter) {
        chatItems.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.displayTitle.contains(searchQuery, ignoreCase = true) ||
                        (item.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true)
            }

            val matchesFilter = when (selectedFilter) {
                "pinned" -> item.participant.isPinnedChat && !item.participant.isArchived
                "group" -> item.chat.type == "group" && !item.participant.isArchived
                "archived" -> item.participant.isArchived
                else -> !item.participant.isArchived
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = GraphiteVoid,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraphiteVoid)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DappWordmark(fontSize = 28)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.testTag("btn_search_toggle")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = "Cari Pesan",
                                tint = if (isSearchActive) PulseTeal else FogWhite
                            )
                        }

                        IconButton(
                            onClick = onOpenFriends,
                            modifier = Modifier.testTag("btn_friends_screen")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PeopleOutline,
                                contentDescription = "Teman & Kontak",
                                tint = FogWhite
                            )
                        }

                        // User profile avatar thumbnail
                        DappAvatar(
                            avatarUrl = currentUser?.avatarUrl,
                            displayName = currentUser?.displayName ?: "User",
                            size = 38.dp,
                            isOnline = true,
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("btn_my_profile")
                        )
                    }
                }

                if (isSearchActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DappTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Cari nama kontak, grup, atau pesan...",
                        leadingIcon = Icons.Outlined.Search,
                        testTag = "home_search_input"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stories Row
                StoriesRow(
                    stories = activeStories,
                    currentUserAvatar = currentUser?.avatarUrl,
                    onAddStory = onAddStory,
                    onStoryClick = onOpenStory
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Tabs (Semua, Tersemat, Grup, Arsip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("Semua", fontFamily = SpaceGroteskFontFamily, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SignalBlue,
                            selectedLabelColor = FogWhite,
                            containerColor = PanelColor,
                            labelColor = SlateText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "all",
                            borderColor = BorderDark,
                            selectedBorderColor = SignalBlue
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == "pinned",
                        onClick = { selectedFilter = "pinned" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedFilter == "pinned") FogWhite else PulseTeal
                            )
                        },
                        label = { Text("Pin", fontFamily = SpaceGroteskFontFamily, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PanelColor,
                            selectedLabelColor = PulseTeal,
                            containerColor = PanelColor,
                            labelColor = SlateText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "pinned",
                            borderColor = BorderDark,
                            selectedBorderColor = PulseTeal
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == "group",
                        onClick = { selectedFilter = "group" },
                        label = { Text("Grup", fontFamily = SpaceGroteskFontFamily, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PanelColor,
                            selectedLabelColor = SignalBlue,
                            containerColor = PanelColor,
                            labelColor = SlateText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "group",
                            borderColor = BorderDark,
                            selectedBorderColor = SignalBlue
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == "archived",
                        onClick = { selectedFilter = "archived" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedFilter == "archived") FogWhite else SlateText
                            )
                        },
                        label = { Text("Arsip", fontFamily = SpaceGroteskFontFamily, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PanelColor,
                            selectedLabelColor = FogWhite,
                            containerColor = PanelColor,
                            labelColor = SlateText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "archived",
                            borderColor = BorderDark,
                            selectedBorderColor = SlateText
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenFriends,
                containerColor = SignalBlue,
                contentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_new_chat")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = "Mulai Percakapan Baru",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkChatUnread,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Tidak ada chat yang sesuai" else "Belum ada obrolan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            color = SlateText
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ketuk ikon orang atau tombol di bawah untuk memulai chat baru dengan teman",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            color = SlateMuted
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredChats, key = { it.chat.id }) { item ->
                    ChatItemRow(
                        item = item,
                        onClick = { onOpenChat(item.chat.id) },
                        onLongClick = { chatToManage = item }
                    )
                }
            }
        }

        // Chat Action Management Dialog (Pin, Mute, Archive, Delete for me)
        chatToManage?.let { item ->
            AlertDialog(
                onDismissRequest = { chatToManage = null },
                containerColor = PanelColor,
                titleContentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            color = FogWhite
                        )
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Pin Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.togglePinChat(item.chat.id)
                                        chatToManage = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = PulseTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (item.participant.isPinnedChat) "Lepas Pin Chat" else "Sematkan Chat ke Atas",
                                style = MaterialTheme.typography.bodyMedium.copy(color = FogWhite)
                            )
                        }

                        // Mute Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.toggleMuteChat(item.chat.id)
                                        chatToManage = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.participant.isMuted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = SlateText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (item.participant.isMuted) "Nyalakan Notifikasi" else "Bisukan Notifikasi (Mute)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = FogWhite)
                            )
                        }

                        // Archive Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.toggleArchiveChat(item.chat.id)
                                        chatToManage = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.participant.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                contentDescription = null,
                                tint = SlateText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (item.participant.isArchived) "Keluarkan dari Arsip" else "Arsipkan Chat",
                                style = MaterialTheme.typography.bodyMedium.copy(color = FogWhite)
                            )
                        }

                        // Delete Chat for Me
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.deleteChatForMe(item.chat.id)
                                        chatToManage = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Hapus Chat (Hanya untuk Saya)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = ErrorRed)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { chatToManage = null }) {
                        Text("Tutup", color = SlateText)
                    }
                }
            )
        }
    }
}

@Composable
fun StoriesRow(
    stories: List<Story>,
    currentUserAvatar: String?,
    onAddStory: () -> Unit,
    onStoryClick: (storyId: String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Add My Story item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onAddStory() }
                    .testTag("btn_add_story")
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, PulseTeal, CircleShape)
                        .background(PanelColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Tambah Story",
                        tint = PulseTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Story Anda",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        color = SlateText
                    )
                )
            }
        }

        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStoryClick(story.id) }
                    .testTag("story_item_${story.id}")
            ) {
                DappAvatar(
                    avatarUrl = story.user?.avatarUrl ?: story.mediaUrl,
                    displayName = story.user?.displayName ?: "User",
                    size = 56.dp,
                    hasActiveStory = true,
                    isStoryViewed = story.isViewed,
                    isOnline = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.user?.displayName?.take(8) ?: "Story",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        color = if (story.isViewed) SlateMuted else FogWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    item: ChatItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (item.participant.isPinnedChat) ElevatedColor.copy(alpha = 0.45f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("chat_item_${item.chat.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Pulse Ring
            DappAvatar(
                avatarUrl = item.displayAvatar,
                displayName = item.displayTitle,
                size = 52.dp,
                isOnline = item.isOtherOnline
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = FogWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Time in JetBrains Mono
                    Text(
                        text = item.lastMessage?.formattedTime ?: "12:00",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = SlateText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message text preview
                    val previewText = when {
                        item.lastMessage?.isDeleted == true -> "Pesan ini telah dihapus"
                        item.lastMessage?.messageType == "image" -> "📷 Foto"
                        item.lastMessage?.messageType == "video" -> "📹 Video"
                        else -> item.lastMessage?.content ?: "Mulai percakapan baru"
                    }

                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = if (item.lastMessage?.isDeleted == true) SlateMuted else SlateText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Status Icons: Muted, Pinned
                    if (item.participant.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = "Muted",
                            tint = SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (item.participant.isPinnedChat) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned",
                            tint = PulseTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
