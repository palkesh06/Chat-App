package com.example.chatapp.ui.home

import android.util.Log
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
class HomeScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()
            chatRepository.setChatsUpdateListener(currentUser?.userId.toString()) { chatList ->
                _state.update {
                    it.copy(
                        chatList = chatList
                    )
                }
            }
            _state.update {
                it.copy(
                    currentLoggedInUser = currentUser
                )
            }
        }
    }

    fun inviteUserToChat(email: String) {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                val invitedUser = findInvitedUser(email)

                if (currentUser == null) {
                    //handle this
                    return@launch
                }
                if (invitedUser == null) {
                    Log.d("ViewModel", "User not found")
                    // invalid invited user
                    return@launch
                }

                val chat = Chat(
                    chatId = "",
                    lastMessage = null,
                    user1 = UserData(
                        userId = currentUser.userId.toString(),
                        isTyping = false,
                        email = currentUser.email,
                        bio = currentUser.bio,
                        username = currentUser.username.toString(),
                        profileUrl = currentUser.profilePictureUrl,
                    ),
                    user2 = UserData(
                        userId = invitedUser.userId.toString(),
                        isTyping = false,
                        email = invitedUser.email,
                        bio = invitedUser.bio,
                        username = invitedUser.username.toString(),
                        profileUrl = invitedUser.profilePictureUrl,
                    )
                )
                // Create the chat in the repository
                val result = chatRepository.createChat(chat)

                // Handle the result from createChat
                when (result) {
                    is Result.Success -> {
                        Log.d("ViewModel", "Chat created successfully")
                        // Optionally, update the UI or notify the user that the chat was created
                    }

                    is Result.Failure -> {
                        Log.e("ViewModel", "Error creating chat: ${result.exception.message}")
                        // Handle the error, e.g., show an error message to the user
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                // Handle any unexpected exceptions (e.g., network issues, invalid user data)
                Log.e("ViewModel", "Error occurred while inviting user to chat: ${e.message}", e)
            }
        }
    }

    private suspend fun findInvitedUser(email: String): User? {
        return try {
            userRepository.findUserWithEmail(email)
        } catch (e: Exception) {
            Log.e("ViewModel", "Error occurred while fetching user: ${e.message}", e)
            throw e
        }
    }


}