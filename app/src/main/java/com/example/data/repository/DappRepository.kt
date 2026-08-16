package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.*
import com.example.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DappRepository(private val context: Context) {

    private val supabase = SupabaseConfig.client
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder().build()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    // Current authenticated user
    private val _currentUser = MutableStateFlow<Profile?>(null)
    val currentUser: StateFlow<Profile?> = _currentUser.asStateFlow()

    // Active profiles directory
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    // Friendships
    private val _friendships = MutableStateFlow<List<Friendship>>(emptyList())
    val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()

    // Blocked users
    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    // Chats and participants
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _participants = MutableStateFlow<List<ChatParticipant>>(emptyList())
    val participants: StateFlow<List<ChatParticipant>> = _participants.asStateFlow()

    // Messages table
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Reactions & Pinned messages table
    private val _reactions = MutableStateFlow<List<MessageReaction>>(emptyList())
    private val _pinnedMessages = MutableStateFlow<List<PinnedMessage>>(emptyList())
    val pinnedMessages: StateFlow<List<PinnedMessage>> = _pinnedMessages.asStateFlow()

    // Stories and Views
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _storyViews = MutableStateFlow<List<StoryView>>(emptyList())
    val storyViews: StateFlow<List<StoryView>> = _storyViews.asStateFlow()

    // App state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        scope.launch {
            checkCurrentSession()
        }
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun checkCurrentSession() {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val profile = fetchProfileById(user.id)
                if (profile != null) {
                    _currentUser.value = profile
                    refreshAllData()
                    startRealtimeSubscriptions()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- AUTHENTICATION VIA SUPABASE AUTH ---

    suspend fun signIn(email: String, pass: String): Result<Profile> {
        _isLoading.value = true
        _errorMessage.value = null
        return withContext(Dispatchers.IO) {
            try {
                if (email.isBlank() || pass.isBlank()) {
                    throw IllegalArgumentException("Email dan password tidak boleh kosong.")
                }
                if (pass.length < 6) {
                    throw IllegalArgumentException("Password minimal 6 karakter.")
                }

                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = pass.trim()
                }

                val authUser = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Gagal mendapatkan data user setelah login")

                var profile = fetchProfileById(authUser.id)
                if (profile == null) {
                    // Create default profile if missing in profiles table
                    val newUsername = email.substringBefore("@").lowercase()
                    val newDisplayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val newProfileDto = ProfileDto(
                        id = authUser.id,
                        username = newUsername,
                        displayName = newDisplayName,
                        avatarUrl = null,
                        statusMessage = "Hey there! I am using Dapp.",
                        lastSeen = nowIso(),
                        isOnline = true
                    )
                    supabase.from("profiles").insert(newProfileDto)
                    profile = newProfileDto.toDomain()
                } else {
                    // Update online status
                    try {
                        supabase.from("profiles").update(mapOf("is_online" to true, "last_seen" to nowIso())) {
                            filter { eq("id", profile.id) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                _currentUser.value = profile
                refreshAllData()
                startRealtimeSubscriptions()

                Result.success(profile)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal masuk"
                Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun signUp(email: String, pass: String, username: String, displayName: String): Result<Profile> {
        _isLoading.value = true
        _errorMessage.value = null
        return withContext(Dispatchers.IO) {
            try {
                if (email.isBlank() || pass.isBlank()) {
                    throw IllegalArgumentException("Email dan password tidak boleh kosong.")
                }
                if (pass.length < 6) {
                    throw IllegalArgumentException("Password minimal 6 karakter.")
                }

                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = pass.trim()
                }

                val authUser = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Pendaftaran berhasil, periksa email konfirmasi jika dibutuhkan")

                val cleanUsername = username.ifBlank { email.substringBefore("@") }.lowercase().trim()
                val cleanDisplayName = displayName.ifBlank { cleanUsername.replaceFirstChar { it.uppercase() } }.trim()

                val newProfileDto = ProfileDto(
                    id = authUser.id,
                    username = cleanUsername,
                    displayName = cleanDisplayName,
                    avatarUrl = null,
                    statusMessage = "Hey there! I am using Dapp.",
                    lastSeen = nowIso(),
                    isOnline = true
                )

                supabase.from("profiles").insert(newProfileDto)
                val profile = newProfileDto.toDomain()

                _currentUser.value = profile
                refreshAllData()
                startRealtimeSubscriptions()

                Result.success(profile)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal mendaftar"
                Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun updateProfile(displayName: String, statusMessage: String, avatarUrl: String?): Result<Profile> {
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("Belum login"))
        return withContext(Dispatchers.IO) {
            try {
                val updatedDto = ProfileDto(
                    id = current.id,
                    username = current.username,
                    displayName = displayName.ifBlank { current.displayName },
                    avatarUrl = avatarUrl ?: current.avatarUrl,
                    statusMessage = statusMessage.ifBlank { current.statusMessage },
                    lastSeen = nowIso(),
                    isOnline = true
                )

                supabase.from("profiles").update(updatedDto) {
                    filter { eq("id", current.id) }
                }

                val updatedDomain = updatedDto.toDomain()
                _currentUser.value = updatedDomain
                _profiles.value = _profiles.value.map { if (it.id == updatedDomain.id) updatedDomain else it }
                Result.success(updatedDomain)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                val myId = _currentUser.value?.id
                if (myId != null) {
                    try {
                        supabase.from("profiles").update(mapOf("is_online" to false, "last_seen" to nowIso())) {
                            filter { eq("id", myId) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                stopRealtimeSubscriptions()
                supabase.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _currentUser.value = null
                _chats.value = emptyList()
                _participants.value = emptyList()
                _messages.value = emptyList()
                _friendships.value = emptyList()
                _blockedUsers.value = emptyList()
                _stories.value = emptyList()
                _storyViews.value = emptyList()
                _reactions.value = emptyList()
                _pinnedMessages.value = emptyList()
            }
        }
    }

    // --- DATA REFRESHING VIA POSTGREST ---

    suspend fun refreshAllData() {
        withContext(Dispatchers.IO) {
            try {
                refreshProfiles()
                refreshFriendships()
                refreshBlockedUsers()
                refreshChats()
                refreshMessages()
                refreshReactions()
                refreshPinnedMessages()
                refreshStories()
                refreshStoryViews()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchProfileById(userId: String): Profile? {
        return try {
            val list = supabase.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeList<ProfileDto>()
            list.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun refreshProfiles() {
        try {
            val dtoList = supabase.from("profiles").select().decodeList<ProfileDto>()
            _profiles.value = dtoList.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshFriendships() {
        try {
            val dtoList = supabase.from("friendships").select().decodeList<FriendshipDto>()
            val profilesMap = _profiles.value.associateBy { it.id }
            _friendships.value = dtoList.map { dto ->
                val myId = _currentUser.value?.id
                val friendId = if (dto.userId == myId) dto.friendId else dto.userId
                dto.toDomain(friendProfile = profilesMap[friendId])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshBlockedUsers() {
        try {
            val dtoList = supabase.from("blocked_users").select().decodeList<BlockedUserDto>()
            val profilesMap = _profiles.value.associateBy { it.id }
            _blockedUsers.value = dtoList.map { dto ->
                dto.toDomain(blockedProfile = profilesMap[dto.blockedId])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshChats() {
        try {
            val chatsList = supabase.from("chats").select().decodeList<ChatDto>()
            _chats.value = chatsList.map { it.toDomain() }

            val participantsList = supabase.from("chat_participants").select().decodeList<ChatParticipantDto>()
            _participants.value = participantsList.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshMessages() {
        try {
            val msgsDto = supabase.from("messages").select().decodeList<MessageDto>()
            val profilesMap = _profiles.value.associateBy { it.id }
            val reactionsMap = _reactions.value.groupBy { it.messageId }
            val pinnedSet = _pinnedMessages.value.map { it.messageId }.toSet()
            val msgsMap = msgsDto.associateBy { it.id }

            _messages.value = msgsDto.map { dto ->
                val replyDto = dto.replyToId?.let { msgsMap[it] }
                val replyMsg = replyDto?.toDomain(
                    senderProfile = profilesMap[replyDto.senderId]
                )
                dto.toDomain(
                    replyToMessage = replyMsg,
                    reactions = reactionsMap[dto.id] ?: emptyList(),
                    isPinned = pinnedSet.contains(dto.id),
                    senderProfile = profilesMap[dto.senderId]
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshReactions() {
        try {
            val reactsDto = supabase.from("message_reactions").select().decodeList<MessageReactionDto>()
            val profilesMap = _profiles.value.associateBy { it.id }
            _reactions.value = reactsDto.map { dto ->
                dto.toDomain(userDisplayName = profilesMap[dto.userId]?.displayName ?: "User")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshPinnedMessages() {
        try {
            val pinsDto = supabase.from("pinned_messages").select().decodeList<PinnedMessageDto>()
            val msgsMap = _messages.value.associateBy { it.id }
            _pinnedMessages.value = pinsDto.map { dto ->
                dto.toDomain(message = msgsMap[dto.messageId])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshStories() {
        try {
            val storiesDto = supabase.from("stories").select().decodeList<StoryDto>()
            val profilesMap = _profiles.value.associateBy { it.id }
            val viewsMap = _storyViews.value.groupBy { it.storyId }
            val myId = _currentUser.value?.id

            _stories.value = storiesDto.map { dto ->
                val storyViews = viewsMap[dto.id] ?: emptyList()
                val isViewed = storyViews.any { it.viewerId == myId }
                dto.toDomain(
                    user = profilesMap[dto.userId],
                    views = storyViews,
                    isViewed = isViewed
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshStoryViews() {
        try {
            val viewsDto = supabase.from("story_views").select().decodeList<StoryViewDto>()
            _storyViews.value = viewsDto.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- REALTIME SUBSCRIPTIONS VIA SUPABASE REALTIME ---

    private fun startRealtimeSubscriptions() {
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                val channel = supabase.channel("dapp_realtime_db")
                val messagesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }
                val chatsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chats"
                }
                val participantsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chat_participants"
                }
                val reactionsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "message_reactions"
                }
                val pinnedFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "pinned_messages"
                }
                val friendshipsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "friendships"
                }
                val storiesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "stories"
                }

                channel.subscribe()
                realtimeChannel = channel

                launch { messagesFlow.collect { refreshMessages() } }
                launch { chatsFlow.collect { refreshChats() } }
                launch { participantsFlow.collect { refreshChats() } }
                launch { reactionsFlow.collect { refreshReactions(); refreshMessages() } }
                launch { pinnedFlow.collect { refreshPinnedMessages(); refreshMessages() } }
                launch { friendshipsFlow.collect { refreshFriendships() } }
                launch { storiesFlow.collect { refreshStories() } }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRealtimeSubscriptions() {
        realtimeJob?.cancel()
        realtimeJob = null
        val channel = realtimeChannel
        realtimeChannel = null
        if (channel != null) {
            scope.launch {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- CHATS & PARTICIPANTS ---

    fun getChatItems(): List<ChatItem> {
        val myId = _currentUser.value?.id ?: return emptyList()
        val myParticipants = _participants.value.filter { it.userId == myId && !it.isDeleted }
        val blockedIds = _blockedUsers.value.filter { it.userId == myId }.map { it.blockedId }

        val items = myParticipants.mapNotNull { p ->
            val chat = _chats.value.find { it.id == p.chatId } ?: return@mapNotNull null
            var otherUser: Profile? = null
            if (chat.type == "private") {
                val otherPart = _participants.value.find { it.chatId == chat.id && it.userId != myId }
                otherUser = if (otherPart != null) {
                    _profiles.value.find { it.id == otherPart.userId }
                } else {
                    _profiles.value.find { it.id != myId }
                }
                if (otherUser != null && blockedIds.contains(otherUser.id)) {
                    return@mapNotNull null
                }
            }

            val chatMessages = _messages.value.filter { it.chatId == chat.id && !it.isDeleted }
            val lastMsg = chatMessages.maxByOrNull { it.createdAt }

            ChatItem(
                chat = chat,
                participant = p,
                otherUser = otherUser,
                lastMessage = lastMsg,
                unreadCount = 0
            )
        }

        return items.sortedWith(
            compareByDescending<ChatItem> { it.participant.isPinnedChat }
                .thenByDescending { it.lastMessage?.createdAt ?: it.chat.lastMessageAt }
        )
    }

    fun getChatById(chatId: String): Chat? = _chats.value.find { it.id == chatId }

    fun getParticipant(chatId: String): ChatParticipant? {
        val myId = _currentUser.value?.id ?: return null
        return _participants.value.find { it.chatId == chatId && it.userId == myId }
    }

    fun getOtherParticipant(chatId: String): Profile? {
        val myId = _currentUser.value?.id ?: return null
        val chat = _chats.value.find { it.id == chatId } ?: return null
        if (chat.type == "group") return null
        val part = _participants.value.find { it.chatId == chatId && it.userId != myId }
        return if (part != null) {
            _profiles.value.find { it.id == part.userId }
        } else {
            _profiles.value.find { it.id != myId }
        }
    }

    suspend fun createOrGetPrivateChat(friendId: String): Chat {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        val friend = _profiles.value.find { it.id == friendId } ?: throw IllegalArgumentException("User tidak ditemukan")

        return withContext(Dispatchers.IO) {
            // Check existing chat in database
            val existingChat = _chats.value.find { chat ->
                if (chat.type != "private") return@find false
                val parts = _participants.value.filter { it.chatId == chat.id }.map { it.userId }
                parts.contains(myId) && parts.contains(friendId)
            }

            if (existingChat != null) {
                try {
                    supabase.from("chat_participants").update(mapOf("is_deleted" to false)) {
                        filter {
                            eq("chat_id", existingChat.id)
                            eq("user_id", myId)
                        }
                    }
                    refreshChats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext existingChat
            }

            val newChatId = UUID.randomUUID().toString()
            val now = nowIso()

            val newChatDto = ChatDto(
                id = newChatId,
                type = "private",
                name = friend.displayName.ifBlank { friend.username },
                avatarUrl = friend.avatarUrl,
                createdBy = myId,
                lastMessageAt = now,
                createdAt = now
            )

            supabase.from("chats").insert(newChatDto)

            val p1 = ChatParticipantDto(chatId = newChatId, userId = myId, joinedAt = now)
            val p2 = ChatParticipantDto(chatId = newChatId, userId = friendId, joinedAt = now)
            supabase.from("chat_participants").insert(listOf(p1, p2))

            refreshChats()
            newChatDto.toDomain()
        }
    }

    suspend fun createGroupChat(name: String, memberIds: List<String>): Chat {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        return withContext(Dispatchers.IO) {
            val newChatId = UUID.randomUUID().toString()
            val now = nowIso()

            val newChatDto = ChatDto(
                id = newChatId,
                type = "group",
                name = name,
                avatarUrl = null,
                createdBy = myId,
                lastMessageAt = now,
                createdAt = now
            )

            supabase.from("chats").insert(newChatDto)

            val allMemberIds = (memberIds + myId).distinct()
            val participantsDto = allMemberIds.map { userId ->
                ChatParticipantDto(chatId = newChatId, userId = userId, joinedAt = now)
            }

            supabase.from("chat_participants").insert(participantsDto)
            refreshChats()
            newChatDto.toDomain()
        }
    }

    suspend fun togglePinChat(chatId: String) {
        val myId = _currentUser.value?.id ?: return
        val currentPart = _participants.value.find { it.chatId == chatId && it.userId == myId } ?: return
        val newPinned = !currentPart.isPinnedChat
        withContext(Dispatchers.IO) {
            try {
                supabase.from("chat_participants").update(mapOf("is_pinned_chat" to newPinned)) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", myId)
                    }
                }
                refreshChats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun toggleMuteChat(chatId: String) {
        val myId = _currentUser.value?.id ?: return
        val currentPart = _participants.value.find { it.chatId == chatId && it.userId == myId } ?: return
        val newMuted = !currentPart.isMuted
        withContext(Dispatchers.IO) {
            try {
                supabase.from("chat_participants").update(mapOf("is_muted" to newMuted)) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", myId)
                    }
                }
                refreshChats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun toggleArchiveChat(chatId: String) {
        val myId = _currentUser.value?.id ?: return
        val currentPart = _participants.value.find { it.chatId == chatId && it.userId == myId } ?: return
        val newArchived = !currentPart.isArchived
        withContext(Dispatchers.IO) {
            try {
                supabase.from("chat_participants").update(mapOf("is_archived" to newArchived)) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", myId)
                    }
                }
                refreshChats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteChatForMe(chatId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                supabase.from("chat_participants").update(mapOf("is_deleted" to true)) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", myId)
                    }
                }
                refreshChats()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- MESSAGES ---

    fun getChatMessages(chatId: String): List<Message> {
        val chatMsgs = _messages.value.filter { it.chatId == chatId }
        val pinnedMsgIds = _pinnedMessages.value.filter { it.chatId == chatId }.map { it.messageId }

        return chatMsgs.map { msg ->
            val replyMsg = msg.replyToId?.let { replyId -> _messages.value.find { it.id == replyId } }
            val sender = _profiles.value.find { it.id == msg.senderId }
            msg.copy(
                replyToMessage = replyMsg,
                isPinned = pinnedMsgIds.contains(msg.id),
                senderProfile = sender
            )
        }.sortedBy { it.createdAt }
    }

    fun getPinnedMessagesForChat(chatId: String): List<PinnedMessage> {
        return _pinnedMessages.value.filter { it.chatId == chatId }.map { pin ->
            val msg = _messages.value.find { it.id == pin.messageId }
            pin.copy(message = msg)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToId: String? = null,
        messageType: String = "text",
        mediaUrl: String? = null
    ): Message {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        return withContext(Dispatchers.IO) {
            val newMsgId = UUID.randomUUID().toString()
            val iso = nowIso()

            val msgDto = MessageDto(
                id = newMsgId,
                chatId = chatId,
                senderId = myId,
                replyToId = replyToId,
                content = content,
                messageType = messageType,
                mediaUrl = mediaUrl,
                isDeleted = false,
                isForwarded = false,
                forwardedFrom = null,
                createdAt = iso
            )

            supabase.from("messages").insert(msgDto)

            try {
                supabase.from("chats").update(mapOf("last_message_at" to iso)) {
                    filter { eq("id", chatId) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            refreshMessages()
            refreshChats()

            val replyMsg = replyToId?.let { id -> _messages.value.find { it.id == id } }
            msgDto.toDomain(
                replyToMessage = replyMsg,
                senderProfile = _currentUser.value
            )
        }
    }

    suspend fun forwardMessage(targetChatId: String, originalMessage: Message): Message {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        return withContext(Dispatchers.IO) {
            val newMsgId = UUID.randomUUID().toString()
            val iso = nowIso()

            val senderName = originalMessage.senderProfile?.displayName
                ?: _profiles.value.find { it.id == originalMessage.senderId }?.displayName
                ?: "User"

            val forwardedDto = MessageDto(
                id = newMsgId,
                chatId = targetChatId,
                senderId = myId,
                content = originalMessage.content,
                messageType = originalMessage.messageType,
                mediaUrl = originalMessage.mediaUrl,
                isForwarded = true,
                forwardedFrom = senderName,
                createdAt = iso
            )

            supabase.from("messages").insert(forwardedDto)

            try {
                supabase.from("chats").update(mapOf("last_message_at" to iso)) {
                    filter { eq("id", targetChatId) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            refreshMessages()
            refreshChats()

            forwardedDto.toDomain(senderProfile = _currentUser.value)
        }
    }

    suspend fun deleteMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("messages").update(mapOf("is_deleted" to true, "content" to "Pesan ini telah dihapus")) {
                    filter { eq("id", messageId) }
                }
                refreshMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addReaction(messageId: String, emoji: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                val existing = _reactions.value.find { it.messageId == messageId && it.userId == myId && it.reaction == emoji }
                if (existing != null) {
                    supabase.from("message_reactions").delete {
                        filter { eq("id", existing.id) }
                    }
                } else {
                    val newReactDto = MessageReactionDto(
                        id = UUID.randomUUID().toString(),
                        messageId = messageId,
                        userId = myId,
                        reaction = emoji,
                        createdAt = nowIso()
                    )
                    supabase.from("message_reactions").insert(newReactDto)
                }
                refreshReactions()
                refreshMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun togglePinMessage(chatId: String, messageId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                val existing = _pinnedMessages.value.find { it.chatId == chatId && it.messageId == messageId }
                if (existing != null) {
                    supabase.from("pinned_messages").delete {
                        filter { eq("id", existing.id) }
                    }
                } else {
                    val newPinDto = PinnedMessageDto(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        messageId = messageId,
                        pinnedBy = myId,
                        pinnedAt = nowIso()
                    )
                    supabase.from("pinned_messages").insert(newPinDto)
                }
                refreshPinnedMessages()
                refreshMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- FRIENDSHIPS & CONTACTS ---

    fun getFriends(): List<Profile> {
        val myId = _currentUser.value?.id ?: return emptyList()
        val acceptedFriendIds = _friendships.value
            .filter { (it.userId == myId || it.friendId == myId) && it.status == "accepted" }
            .map { if (it.userId == myId) it.friendId else it.userId }

        val blockedIds = _blockedUsers.value.filter { it.userId == myId }.map { it.blockedId }

        return _profiles.value.filter { acceptedFriendIds.contains(it.id) && !blockedIds.contains(it.id) && it.id != myId }
    }

    fun getPendingRequests(): List<Profile> {
        val myId = _currentUser.value?.id ?: return emptyList()
        val pendingFriendIds = _friendships.value
            .filter { it.friendId == myId && it.status == "pending" }
            .map { it.userId }

        return _profiles.value.filter { pendingFriendIds.contains(it.id) }
    }

    suspend fun searchUsers(query: String): List<Profile> {
        val myId = _currentUser.value?.id ?: return emptyList()
        if (query.isBlank()) return emptyList()
        val clean = query.trim().lowercase()

        return withContext(Dispatchers.IO) {
            try {
                // Fetch profiles matching username or display_name
                val list = supabase.from("profiles").select().decodeList<ProfileDto>().map { it.toDomain() }
                _profiles.value = list
                list.filter {
                    it.id != myId &&
                            (it.username.lowercase().contains(clean) || it.displayName.lowercase().contains(clean))
                }
            } catch (e: Exception) {
                _profiles.value.filter {
                    it.id != myId &&
                            (it.username.lowercase().contains(clean) || it.displayName.lowercase().contains(clean))
                }
            }
        }
    }

    suspend fun sendFriendRequest(friendId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                val existing = _friendships.value.find {
                    (it.userId == myId && it.friendId == friendId) || (it.userId == friendId && it.friendId == myId)
                }

                if (existing == null) {
                    val friendshipDto = FriendshipDto(
                        id = UUID.randomUUID().toString(),
                        userId = myId,
                        friendId = friendId,
                        status = "pending",
                        createdAt = nowIso()
                    )
                    supabase.from("friendships").insert(friendshipDto)
                    refreshFriendships()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun acceptFriendRequest(friendId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                supabase.from("friendships").update(mapOf("status" to "accepted")) {
                    filter {
                        or {
                            and {
                                eq("user_id", friendId)
                                eq("friend_id", myId)
                            }
                            and {
                                eq("user_id", myId)
                                eq("friend_id", friendId)
                            }
                        }
                    }
                }
                refreshFriendships()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun rejectFriendRequest(friendId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                supabase.from("friendships").delete {
                    filter {
                        or {
                            and {
                                eq("user_id", friendId)
                                eq("friend_id", myId)
                            }
                            and {
                                eq("user_id", myId)
                                eq("friend_id", friendId)
                            }
                        }
                    }
                }
                refreshFriendships()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeFriendship(friendId: String) {
        rejectFriendRequest(friendId)
    }

    suspend fun blockUser(userId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                val blockDto = BlockedUserDto(
                    id = UUID.randomUUID().toString(),
                    userId = myId,
                    blockedId = userId,
                    createdAt = nowIso()
                )
                supabase.from("blocked_users").insert(blockDto)
                removeFriendship(userId)
                refreshBlockedUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun unblockUser(userId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                supabase.from("blocked_users").delete {
                    filter {
                        eq("user_id", myId)
                        eq("blocked_id", userId)
                    }
                }
                refreshBlockedUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- STORIES & VIEWS ---

    fun getActiveStories(): List<Story> {
        val myId = _currentUser.value?.id ?: return emptyList()
        val viewedStoryIds = _storyViews.value.filter { it.viewerId == myId }.map { it.storyId }

        return _stories.value.map { story ->
            val user = _profiles.value.find { it.id == story.userId }
            story.copy(
                user = user,
                isViewed = viewedStoryIds.contains(story.id)
            )
        }.sortedByDescending { it.createdAt }
    }

    suspend fun createStory(mediaUrl: String, caption: String = "") {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                val storyId = UUID.randomUUID().toString()
                val now = nowIso()
                val expires = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(
                    Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                )

                val storyDto = StoryDto(
                    id = storyId,
                    userId = myId,
                    mediaUrl = mediaUrl,
                    caption = caption,
                    createdAt = now,
                    expiresAt = expires
                )

                supabase.from("stories").insert(storyDto)
                refreshStories()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun viewStory(storyId: String) {
        val myId = _currentUser.value?.id ?: return
        withContext(Dispatchers.IO) {
            try {
                if (_storyViews.value.none { it.storyId == storyId && it.viewerId == myId }) {
                    val viewDto = StoryViewDto(
                        id = UUID.randomUUID().toString(),
                        storyId = storyId,
                        viewerId = myId,
                        viewedAt = nowIso()
                    )
                    supabase.from("story_views").insert(viewDto)
                    refreshStoryViews()
                    refreshStories()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- STORAGE & MEDIA VIA SUPABASE STORAGE ---

    suspend fun uploadAvatar(photoUriOrUrl: String): String {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        return withContext(Dispatchers.IO) {
            try {
                val bytes = getBytesFromUriOrUrl(photoUriOrUrl)
                val fileName = "$myId/avatar_${System.currentTimeMillis()}.jpg"

                val finalUrl = if (bytes != null) {
                    val avatarBucket = supabase.storage.from("avatars")
                    avatarBucket.upload(path = fileName, data = bytes) {
                        upsert = true
                    }
                    avatarBucket.publicUrl(fileName)
                } else if (photoUriOrUrl.startsWith("http")) {
                    photoUriOrUrl
                } else {
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&q=80"
                }

                updateProfile(_currentUser.value?.displayName ?: "", _currentUser.value?.statusMessage ?: "", finalUrl)
                finalUrl
            } catch (e: Exception) {
                e.printStackTrace()
                photoUriOrUrl
            }
        }
    }

    suspend fun uploadChatMedia(chatId: String, mediaUrlOrUri: String, messageType: String = "image"): Message {
        val myId = _currentUser.value?.id ?: throw IllegalStateException("Belum login")
        return withContext(Dispatchers.IO) {
            try {
                val bytes = getBytesFromUriOrUrl(mediaUrlOrUri)
                val fileName = "$chatId/${myId}_${System.currentTimeMillis()}.jpg"

                val finalMediaUrl = if (bytes != null) {
                    val mediaBucket = supabase.storage.from("chat-media")
                    mediaBucket.upload(path = fileName, data = bytes) {
                        upsert = true
                    }
                    mediaBucket.publicUrl(fileName)
                } else {
                    mediaUrlOrUri
                }

                sendMessage(chatId = chatId, content = "Media Attachment", replyToId = null, messageType = messageType, mediaUrl = finalMediaUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                sendMessage(chatId = chatId, content = "Media Attachment", replyToId = null, messageType = messageType, mediaUrl = mediaUrlOrUri)
            }
        }
    }

    private suspend fun getBytesFromUriOrUrl(uriOrUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                if (uriOrUrl.startsWith("content://") || uriOrUrl.startsWith("file://")) {
                    val uri = Uri.parse(uriOrUrl)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } else if (uriOrUrl.startsWith("http://") || uriOrUrl.startsWith("https://")) {
                    val req = Request.Builder().url(uriOrUrl).build()
                    httpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) response.body?.bytes() else null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
