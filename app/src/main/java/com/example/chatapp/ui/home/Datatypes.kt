package com.example.chatapp.ui.home

data class Chat(
    val chatId: String = "",
    val lastMessage: Message? = null,
    val user1: UserData? = null,
    val user2: UserData? = null
)

data class Message(
    val msgId: String = "",
    val senderId: String = "",
    val repliedMsg: Message? = null,
    val reaction: Reaction? = null,
    val imgUrl: String? = null,
    val content: String? = null,
    val progress: Double = 0.0, // For file upload/download progress
    val videoUrl: String? = null,
    val msgType: MsgType = MsgType.TEXT,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val timestamp: Long = 0L,
    val isForwarded: Boolean = false
)

data class UserData(
    val userId: String = "",
    val isTyping: Boolean = false,
    val bio: String? = null,
    val username: String = "",
    val profileUrl: String? = null,
    val email: String = "",
    val status: String = "",
    val unreadCount: Int = 0
)

data class Reaction(
    val profileUrl: String? = null,
    val reactionType: String = "", // Example: "😊"
    val username: String = "",
    val userId: String = ""
)

enum class MsgType {
    TEXT, IMAGE, VIDEO, FILE
}
