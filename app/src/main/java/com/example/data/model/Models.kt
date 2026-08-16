package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ProfileDto(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("status_message")
    val statusMessage: String = "Hey there! I am using Dapp.",
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean = false
) {
    fun toDomain() = Profile(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        statusMessage = statusMessage,
        lastSeen = lastSeen,
        isOnline = isOnline
    )
}

data class Profile(
    val id: String,
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val statusMessage: String = "Hey there! I am using Dapp.",
    val lastSeen: String? = null,
    val isOnline: Boolean = false
) {
    val initial: String
        get() = (displayName.ifBlank { username }).take(1).uppercase()

    fun toDto() = ProfileDto(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        statusMessage = statusMessage,
        lastSeen = lastSeen,
        isOnline = isOnline
    )
}

@Serializable
data class FriendshipDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("friend_id")
    val friendId: String = "",
    val status: String = "pending", // "pending" | "accepted" | "blocked"
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(friendProfile: Profile? = null) = Friendship(
        userId = userId,
        friendId = friendId,
        status = status,
        friendProfile = friendProfile
    )
}

data class Friendship(
    val userId: String,
    val friendId: String,
    val status: String, // "pending" | "accepted" | "blocked"
    val friendProfile: Profile? = null
)

@Serializable
data class ChatDto(
    val id: String = "",
    val type: String = "private", // "private" | "group"
    val name: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_by")
    val createdBy: String = "",
    @SerialName("last_message_at")
    val lastMessageAt: String = "",
    @SerialName("invite_code")
    val inviteCode: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain() = Chat(
        id = id,
        type = type,
        name = name,
        avatarUrl = avatarUrl,
        createdBy = createdBy,
        lastMessageAt = lastMessageAt,
        inviteCode = inviteCode
    )
}

data class Chat(
    val id: String,
    val type: String = "private", // "private" | "group"
    val name: String = "",
    val avatarUrl: String? = null,
    val createdBy: String = "",
    val lastMessageAt: String = "",
    val inviteCode: String? = null
) {
    fun toDto() = ChatDto(
        id = id,
        type = type,
        name = name,
        avatarUrl = avatarUrl,
        createdBy = createdBy,
        lastMessageAt = lastMessageAt,
        inviteCode = inviteCode
    )
}

@Serializable
data class ChatParticipantDto(
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_muted")
    val isMuted: Boolean = false,
    @SerialName("is_archived")
    val isArchived: Boolean = false,
    @SerialName("is_pinned_chat")
    val isPinnedChat: Boolean = false,
    @SerialName("joined_at")
    val joinedAt: String? = null
) {
    fun toDomain() = ChatParticipant(
        chatId = chatId,
        userId = userId,
        isDeleted = isDeleted,
        isMuted = isMuted,
        isArchived = isArchived,
        isPinnedChat = isPinnedChat
    )
}

data class ChatParticipant(
    val chatId: String,
    val userId: String,
    val isDeleted: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isPinnedChat: Boolean = false
) {
    fun toDto() = ChatParticipantDto(
        chatId = chatId,
        userId = userId,
        isDeleted = isDeleted,
        isMuted = isMuted,
        isArchived = isArchived,
        isPinnedChat = isPinnedChat
    )
}

data class ChatItem(
    val chat: Chat,
    val participant: ChatParticipant,
    val otherUser: Profile? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0
) {
    val displayTitle: String
        get() = if (chat.type == "group") chat.name else (otherUser?.displayName?.ifBlank { otherUser.username } ?: chat.name.ifBlank { "Direct Chat" })

    val displayAvatar: String?
        get() = if (chat.type == "group") chat.avatarUrl else otherUser?.avatarUrl

    val isOtherOnline: Boolean
        get() = if (chat.type == "group") false else (otherUser?.isOnline == true)
}

@Serializable
data class MessageDto(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    val content: String = "",
    @SerialName("message_type")
    val messageType: String = "text", // "text" | "image" | "video" | "voice"
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_forwarded")
    val isForwarded: Boolean = false,
    @SerialName("forwarded_from")
    val forwardedFrom: String? = null,
    @SerialName("media_duration_seconds")
    val mediaDurationSeconds: Int? = null,
    @SerialName("created_at")
    val createdAt: String = ""
) {
    fun toDomain(
        replyToMessage: Message? = null,
        reactions: List<MessageReaction> = emptyList(),
        isPinned: Boolean = false,
        senderProfile: Profile? = null
    ) = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        replyToId = replyToId,
        content = content,
        messageType = messageType,
        mediaUrl = mediaUrl,
        isDeleted = isDeleted,
        isForwarded = isForwarded,
        forwardedFrom = forwardedFrom,
        mediaDurationSeconds = mediaDurationSeconds,
        createdAt = createdAt,
        replyToMessage = replyToMessage,
        reactions = reactions,
        isPinned = isPinned,
        senderProfile = senderProfile
    )
}

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val replyToId: String? = null,
    val content: String,
    val messageType: String = "text", // "text" | "image" | "video" | "voice"
    val mediaUrl: String? = null,
    val isDeleted: Boolean = false,
    val isForwarded: Boolean = false,
    val forwardedFrom: String? = null,
    val mediaDurationSeconds: Int? = null,
    val createdAt: String = "",
    val replyToMessage: Message? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val isPinned: Boolean = false,
    val senderProfile: Profile? = null
) {
    val formattedTime: String
        get() {
            return try {
                if (createdAt.contains("T")) {
                    val parts = createdAt.substringAfter("T").take(5)
                    parts
                } else {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date())
                }
            } catch (e: Exception) {
                "12:00"
            }
        }

    fun toDto() = MessageDto(
        id = id,
        chatId = chatId,
        senderId = senderId,
        replyToId = replyToId,
        content = content,
        messageType = messageType,
        mediaUrl = mediaUrl,
        isDeleted = isDeleted,
        isForwarded = isForwarded,
        forwardedFrom = forwardedFrom,
        mediaDurationSeconds = mediaDurationSeconds,
        createdAt = createdAt
    )
}

@Serializable
data class MessageReactionDto(
    val id: String = "",
    @SerialName("message_id")
    val messageId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val reaction: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
) {
    fun toDomain(userDisplayName: String = "") = MessageReaction(
        id = id,
        messageId = messageId,
        userId = userId,
        reaction = reaction,
        createdAt = createdAt,
        userDisplayName = userDisplayName
    )
}

data class MessageReaction(
    val id: String,
    val messageId: String,
    val userId: String,
    val reaction: String,
    val createdAt: String = "",
    val userDisplayName: String = ""
) {
    fun toDto() = MessageReactionDto(
        id = id,
        messageId = messageId,
        userId = userId,
        reaction = reaction,
        createdAt = createdAt
    )
}

@Serializable
data class PinnedMessageDto(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("message_id")
    val messageId: String = "",
    @SerialName("pinned_by")
    val pinnedBy: String = "",
    @SerialName("pinned_at")
    val pinnedAt: String = ""
) {
    fun toDomain(message: Message? = null) = PinnedMessage(
        id = id,
        chatId = chatId,
        messageId = messageId,
        pinnedBy = pinnedBy,
        pinnedAt = pinnedAt,
        message = message
    )
}

data class PinnedMessage(
    val id: String,
    val chatId: String,
    val messageId: String,
    val pinnedBy: String,
    val pinnedAt: String = "",
    val message: Message? = null
) {
    fun toDto() = PinnedMessageDto(
        id = id,
        chatId = chatId,
        messageId = messageId,
        pinnedBy = pinnedBy,
        pinnedAt = pinnedAt
    )
}

@Serializable
data class StoryDto(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("media_url")
    val mediaUrl: String = "",
    val caption: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("expires_at")
    val expiresAt: String = ""
) {
    fun toDomain(
        user: Profile? = null,
        views: List<StoryView> = emptyList(),
        isViewed: Boolean = false
    ) = Story(
        id = id,
        userId = userId,
        mediaUrl = mediaUrl,
        caption = caption,
        createdAt = createdAt,
        expiresAt = expiresAt,
        user = user,
        views = views,
        isViewed = isViewed
    )
}

data class Story(
    val id: String,
    val userId: String,
    val mediaUrl: String,
    val caption: String = "",
    val createdAt: String = "",
    val expiresAt: String = "",
    val user: Profile? = null,
    val views: List<StoryView> = emptyList(),
    val isViewed: Boolean = false
) {
    fun toDto() = StoryDto(
        id = id,
        userId = userId,
        mediaUrl = mediaUrl,
        caption = caption,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}

@Serializable
data class StoryViewDto(
    val id: String? = null,
    @SerialName("story_id")
    val storyId: String = "",
    @SerialName("viewer_id")
    val viewerId: String = "",
    @SerialName("viewed_at")
    val viewedAt: String = ""
) {
    fun toDomain() = StoryView(
        storyId = storyId,
        viewerId = viewerId,
        viewedAt = viewedAt
    )
}

data class StoryView(
    val storyId: String,
    val viewerId: String,
    val viewedAt: String = ""
) {
    fun toDto() = StoryViewDto(
        storyId = storyId,
        viewerId = viewerId,
        viewedAt = viewedAt
    )
}

@Serializable
data class BlockedUserDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("blocked_id")
    val blockedId: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
) {
    fun toDomain(blockedProfile: Profile? = null) = BlockedUser(
        userId = userId,
        blockedId = blockedId,
        createdAt = createdAt,
        blockedProfile = blockedProfile
    )
}

data class BlockedUser(
    val userId: String,
    val blockedId: String,
    val createdAt: String = "",
    val blockedProfile: Profile? = null
)
