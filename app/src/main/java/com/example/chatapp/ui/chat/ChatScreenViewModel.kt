package com.example.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.repo.ChatRepository
import com.example.chatapp.data.repo.ConnectivityRepository
import com.example.chatapp.data.repo.UserRepository
import com.example.chatapp.ui.signIn.User
import com.example.chatapp.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatScreenState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                loading = true
            )
        }
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()
            _state.update {
                it.copy(
                    currentLoggedInUser = currentUser,
                    loading = false
                )
            }
        }
    }

    private fun observeNetworkChanges(chatId: String, userId: String) {
        viewModelScope.launch {
            connectivityRepository.isConnected.collect { connected ->
                updateUserStatus(chatId, userId, connected)
            }
        }
    }

    fun updateUserStatus(chatId: String, userId: String, isOnline: Boolean) {
        try {
            chatRepository.updateUserStatus(chatId, userId, isOnline)
        } catch (e: Exception) {
            // Handle any exceptions (e.g., logging or retry mechanisms)
            e.printStackTrace()
        }
    }

    fun updateUserIsTyping(partnerUserId: String, chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.updateUserIsTyping(
                partnerUserId,
                chatId,
                isTyping
            )
        }
    }

    fun observePartnerUser(chatId: String, userId: String) {
        chatRepository.observePartnerUser(
            chatId,
            userId
        ) { isTyping, status ->
            _state.update {
                it.copy(
                    isTyping = isTyping,
                    status = status
                )
            }
        }
        observeNetworkChanges(chatId, state.value.currentLoggedInUser?.userId.toString())
    }


    fun getChatDetailWithChatId(chatId: String) {
        _state.update {
            it.copy(
                loading = true
            )
        }
        viewModelScope.launch {
            try {
                val result = chatRepository.getChatDetailWithChatId(chatId)
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                chat = result.data,
                                error = null,
                                loading = false
                            )
                        }
                    }

                    is Result.Failure -> {
                        _state.update {
                            it.copy(
                                chat = null,
                                error = result.exception.message,
                                loading = false
                            )
                        }
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        chat = null,
                        error = e.message,
                        loading = false
                    )
                }
            }
        }
        startListeningForNewMessagesInChat(chatId)
    }


    fun startListeningForNewMessagesInChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.setListenerForNewMessagesInChat(chatId) { messageCollection ->
                _state.update {
                    it.copy(
                        lastMessage = messageCollection.messages.last(),
                        messages = messageCollection
                    )
                }
            }
        }
    }


    fun sendMessage(
        chatId: String,
        sender: User,
        message: Messages
    ) {
        viewModelScope.launch {
            chatRepository.sendMessage(chatId, sender, message)
            chatRepository.updateLastMessage(chatId, message)
        }
    }
}