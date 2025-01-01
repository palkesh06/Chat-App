package com.example.chatapp.ui.chat

import com.example.chatapp.ui.home.Chat
import com.example.chatapp.ui.signIn.User
import com.google.firebase.Timestamp

data class ChatScreenState(
    val chatId: String = "",
    val chat: Chat? = null,
    val currentLoggedInUser: User? = null,
    val lastMessage: Messages? = null,
    val messages: MessageCollection? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val isTyping: Boolean = false,
    val status: String = ""
)

data class MessageCollection(
    val chatId: String = "",
    val sender: User = User(),
    val messages: List<Messages> = emptyList()
)

data class Messages(
    val text: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val sender: User = User(),
    val mediaUrl: List<String> = emptyList()
)