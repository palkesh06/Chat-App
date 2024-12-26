package com.example.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.repo.ChatRepository
import com.example.chatapp.data.repo.UserRepository
import com.example.chatapp.ui.signIn.User
import com.example.chatapp.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
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
        }
    }
}