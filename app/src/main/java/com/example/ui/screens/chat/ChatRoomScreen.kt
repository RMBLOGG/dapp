package com.example.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatItem
import com.example.data.model.Message
import com.example.data.repository.DappRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    repository: DappRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val chats by repository.chats.collectAsState()
    val allMessages by repository.messages.collectAsState()
    val pinnedList by repository.pinnedMessages.collectAsState()
    val participants by repository.participants.collectAsState()

    val chat = remember(chats, chatId) { repository.getChatById(chatId) }
    val otherUser = remember(chatId, currentUser) { repository.getOtherParticipant(chatId) }
    val participant = remember(chatId, participants, currentUser) { repository.getParticipant(chatId) }

    val messages = remember(allMessages, pinnedList, chatId) {
        repository.getChatMessages(chatId)
    }

    val pinnedMessages = remember(pinnedList, chatId) {
        repository.getPinnedMessagesForChat(chatId)
    }

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var messageToForward by remember { mutableStateOf<Message?>(null) }
    var showChatSettingsMenu by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to latest message on receive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val title = if (chat?.type == "group") chat.name else (otherUser?.displayName?.ifBlank { otherUser.username } ?: chat?.name ?: "Chat")
    val avatarUrl = if (chat?.type == "group") chat.avatarUrl else otherUser?.avatarUrl
    val isOtherOnline = if (chat?.type == "group") false else (otherUser?.isOnline == true)

    Scaffold(
        containerColor = GraphiteVoid,
        topBar = {
            Surface(
                color = PanelColor,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_chat_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                tint = FogWhite
                            )
                        }

                        // Avatar with Pulse Ring
                        DappAvatar(
                            avatarUrl = avatarUrl,
                            displayName = title,
                            size = 42.dp,
                            isOnline = isOtherOnline
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = FogWhite
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (chat?.type == "group") "Grup Dapp" else if (isOtherOnline) "online" else "terakhir dilihat baru saja",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = if (isOtherOnline) PulseTeal else SlateText
                                )
                            )
                        }

                        // Options Menu
                        Box {
                            IconButton(onClick = { showChatSettingsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Menu Chat",
                                    tint = FogWhite
                                )
                            }

                            DropdownMenu(
                                expanded = showChatSettingsMenu,
                                onDismissRequest = { showChatSettingsMenu = false },
                                modifier = Modifier.background(PanelColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (participant?.isPinnedChat == true) "Lepas Pin Chat" else "Pin Chat Ini", color = FogWhite) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.PushPin, contentDescription = null, tint = PulseTeal)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.togglePinChat(chatId)
                                            showChatSettingsMenu = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (participant?.isMuted == true) "Nyalakan Suara" else "Mute Chat", color = FogWhite) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.NotificationsOff, contentDescription = null, tint = SlateText)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.toggleMuteChat(chatId)
                                            showChatSettingsMenu = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (participant?.isArchived == true) "Buka Arsip" else "Arsipkan Chat", color = FogWhite) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.Archive, contentDescription = null, tint = SlateText)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.toggleArchiveChat(chatId)
                                            showChatSettingsMenu = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus Chat untuk Saya", color = ErrorRed) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null, tint = ErrorRed)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteChatForMe(chatId)
                                            showChatSettingsMenu = false
                                            onBack()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Pinned Message Banner at top of chat room
                    if (pinnedMessages.isNotEmpty()) {
                        val latestPin = pinnedMessages.lastOrNull()
                        Surface(
                            color = ElevatedColor,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, PulseTeal.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    latestPin?.message?.let { target ->
                                        val idx = messages.indexOfFirst { it.id == target.id }
                                        if (idx != -1) {
                                            coroutineScope.launch { listState.animateScrollToItem(idx) }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = "Pesan Disematkan",
                                    tint = PulseTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pesan Tersemat",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = SpaceGroteskFontFamily,
                                            color = PulseTeal,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = latestPin?.message?.content ?: "Pesan",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = InterFontFamily,
                                            color = FogWhite
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        latestPin?.let {
                                            coroutineScope.launch { repository.togglePinMessage(chatId, it.messageId) }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Lepas Pin",
                                        tint = SlateText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = PanelColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quoted Reply Preview Bar
                    AnimatedVisibility(
                        visible = replyingToMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingToMessage?.let { replyTarget ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ElevatedColor)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DappThreadLine(
                                    senderName = replyTarget.senderProfile?.displayName ?: "User",
                                    quotedText = replyTarget.content,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Batal Balas",
                                        tint = SlateText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment Button (Upload foto/video ke chat-media bucket)
                        IconButton(
                            onClick = { showAttachmentDialog = true },
                            modifier = Modifier.testTag("btn_attach_media")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Lampirkan Foto/File",
                                tint = PulseTeal
                            )
                        }

                        // Message Text Field
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "Tulis pesan...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        color = SlateText
                                    )
                                )
                            },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedColor,
                                unfocusedContainerColor = ElevatedColor,
                                focusedBorderColor = BorderDark,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = FogWhite,
                                unfocusedTextColor = FogWhite,
                                cursorColor = PulseTeal
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = FogWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button (Signal Blue)
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    val textToSend = textInput.trim()
                                    val replyId = replyingToMessage?.id
                                    textInput = ""
                                    replyingToMessage = null
                                    coroutineScope.launch {
                                        repository.sendMessage(
                                            chatId = chatId,
                                            content = textToSend,
                                            replyToId = replyId
                                        )
                                    }
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (textInput.isNotBlank()) SignalBlue else PanelColor)
                                .testTag("btn_send_message")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Kirim",
                                tint = if (textInput.isNotBlank()) FogWhite else SlateMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        DappAvatar(
                            avatarUrl = avatarUrl,
                            displayName = title,
                            size = 64.dp,
                            isOnline = isOtherOnline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Mulai percakapan dengan $title",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                color = FogWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pesan terenkripsi end-to-end melalui Supabase Realtime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = PulseTeal
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isFromMe = message.senderId == currentUser?.id
                        MessageBubbleItem(
                            message = message,
                            isFromMe = isFromMe,
                            onReply = { replyingToMessage = message },
                            onReact = { emoji ->
                                coroutineScope.launch { repository.addReaction(message.id, emoji) }
                            },
                            onPin = {
                                coroutineScope.launch { repository.togglePinMessage(chatId, message.id) }
                            },
                            onForward = { messageToForward = message },
                            onDelete = {
                                coroutineScope.launch { repository.deleteMessage(message.id) }
                            },
                            onLongPress = { selectedMessageForMenu = message }
                        )
                    }
                }
            }
        }

        // Message Action Popup Menu (Quick Reactions + Reply + Pin + Forward + Delete)
        selectedMessageForMenu?.let { targetMsg ->
            AlertDialog(
                onDismissRequest = { selectedMessageForMenu = null },
                containerColor = PanelColor,
                titleContentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val quickEmojis = listOf("❤️", "👍", "🔥", "😂", "😮", "🙏")
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.addReaction(targetMsg.id, emoji)
                                            selectedMessageForMenu = null
                                        }
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Reply
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    replyingToMessage = targetMsg
                                    selectedMessageForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.Reply, contentDescription = null, tint = PulseTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Balas Pesan", color = FogWhite)
                        }

                        // Forward
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    messageToForward = targetMsg
                                    selectedMessageForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Outlined.Forward, contentDescription = null, tint = SignalBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Teruskan Pesan", color = FogWhite)
                        }

                        // Pin Message
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.togglePinMessage(chatId, targetMsg.id)
                                        selectedMessageForMenu = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Outlined.PushPin, contentDescription = null, tint = PulseTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (targetMsg.isPinned) "Lepas Pin Pesan" else "Sematkan Pesan (Pin)", color = FogWhite)
                        }

                        // Copy Text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(targetMsg.content))
                                    selectedMessageForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, tint = SlateText)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Salin Teks", color = FogWhite)
                        }

                        // Delete (Soft delete)
                        if (targetMsg.senderId == currentUser?.id && !targetMsg.isDeleted) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.deleteMessage(targetMsg.id)
                                            selectedMessageForMenu = null
                                        }
                                    }
                                    .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null, tint = ErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Hapus Pesan", color = ErrorRed)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedMessageForMenu = null }) {
                        Text("Tutup", color = SlateText)
                    }
                }
            )
        }

        // Forward Message Destination Picker Dialog
        messageToForward?.let { forwardMsg ->
            val myChatItems = remember(chats, participants) { repository.getChatItems() }
            AlertDialog(
                onDismissRequest = { messageToForward = null },
                containerColor = PanelColor,
                titleContentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Teruskan Pesan ke...",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily)
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(myChatItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.forwardMessage(item.chat.id, forwardMsg)
                                            messageToForward = null
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DappAvatar(
                                    avatarUrl = item.displayAvatar,
                                    displayName = item.displayTitle,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.displayTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = FogWhite),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { messageToForward = null }) {
                        Text("Batal", color = SlateText)
                    }
                }
            )
        }

        // Attachment / Media Picker Dialog
        if (showAttachmentDialog) {
            val sampleMedia = listOf(
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&q=80" to "Cyber Architecture",
                "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&q=80" to "Retro Workstation",
                "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80" to "Aurora Void",
                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80" to "Hardware Chip"
            )

            AlertDialog(
                onDismissRequest = { showAttachmentDialog = false },
                containerColor = PanelColor,
                titleContentColor = FogWhite,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Unggah Media (Bucket: chat-media)",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pilih aset gambar untuk dikirim ke chat:",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateText)
                        )

                        sampleMedia.forEach { (url, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.sendMessage(
                                                chatId = chatId,
                                                content = label,
                                                messageType = "image",
                                                mediaUrl = url
                                            )
                                            showAttachmentDialog = false
                                        }
                                    }
                                    .background(ElevatedColor)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = label, color = FogWhite, style = MaterialTheme.typography.titleSmall)
                                    Text(text = "Kirim gambar ke chat", color = SlateText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachmentDialog = false }) {
                        Text("Batal", color = SlateText)
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubbleItem(
    message: Message,
    isFromMe: Boolean,
    onReply: () -> Unit,
    onReact: (emoji: String) -> Unit,
    onPin: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val currentUserDisplayName = message.senderProfile?.displayName ?: "User"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongPress() },
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Forwarded Badge
        if (message.isForwarded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Forward,
                    contentDescription = null,
                    tint = SlateMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Diteruskan dari ${message.forwardedFrom ?: "sumber lain"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = SlateMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Pinned badge indicator on bubble
        if (message.isPinned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = PulseTeal,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pesan Disematkan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = PulseTeal,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Asymmetric Chat Bubble
        DappChatBubble(
            isFromMe = isFromMe,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Sender name if not from me
                if (!isFromMe) {
                    Text(
                        text = currentUserDisplayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            color = PulseTeal,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Quoted reply in bubble with Pulse Teal thread line
                if (message.replyToMessage != null) {
                    DappThreadLine(
                        senderName = message.replyToMessage.senderProfile?.displayName ?: "User",
                        quotedText = message.replyToMessage.content,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Media Attachment if any
                if (!message.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 6.dp)
                    )
                }

                // Message Text Content
                if (message.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            tint = SlateMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pesan ini telah dihapus",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = if (isFromMe) FogWhite.copy(alpha = 0.7f) else SlateMuted
                            )
                        )
                    }
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            color = FogWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Time in JetBrains Mono
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = if (isFromMe) FogWhite.copy(alpha = 0.75f) else SlateText,
                            fontSize = 10.sp
                        )
                    )

                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = "Terkirim",
                            tint = PulseTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Reaction chips under message bubble with Pulse Teal border
        if (message.reactions.isNotEmpty()) {
            val groupedReactions = message.reactions.groupBy { it.reaction }
            Row(
                modifier = Modifier
                    .padding(top = 3.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedReactions.forEach { (emoji, list) ->
                    val isMyReaction = list.any { it.userId == message.senderId }
                    DappReactionChip(
                        emoji = emoji,
                        count = list.size,
                        isSelected = isMyReaction,
                        onClick = { onReact(emoji) }
                    )
                }
            }
        }
    }
}
