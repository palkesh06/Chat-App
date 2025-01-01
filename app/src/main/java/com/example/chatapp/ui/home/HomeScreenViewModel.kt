package com.example.chatapp.ui.home

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.repo.ChatRepository
import com.example.chatapp.data.repo.CloudinaryManager
import com.example.chatapp.data.repo.UserRepository
import com.example.chatapp.ui.home.Story
import com.example.chatapp.ui.signIn.User
import com.example.chatapp.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlin.collections.List
import kotlin.collections.mutableListOf

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val cloudinaryManager: CloudinaryManager
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

    fun addImageToStory(selectedUri: Uri, context: Context) {
        _state.update {
            it.copy(isAddingToStory = true)
        }
        viewModelScope.launch {
            try {
                val file = selectedUri.toFile(context)
                if (file != null) {
                    val uploadedUrl = cloudinaryManager.uploadMedia(file)
                    if (uploadedUrl != null) {
                        userRepository.addToStory(
                            state.value.currentLoggedInUser?.userId.toString(),
                            uploadedUrl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error occurred while adding image to story: ${e.message}", e)
            } finally {
                _state.update {
                    it.copy(isAddingToStory = false)
                }
            }
        }
    }

    fun Uri.toFile(context: Context): File? {
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = getFileName(context, this) ?: return null
        val tempFile = File(context.cacheDir, fileName)

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(this)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return tempFile
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex("_display_name")
                    fileName = it.getString(columnIndex)
                }
            }
        }
        return fileName
    }


    fun observeFriendsStory(userId: String) {
        viewModelScope.launch {
            try {
                // Fetch the list of friends' user IDs
                val friendsUserIds = chatRepository.getAllFriends(userId)

                if (friendsUserIds.isEmpty()) {
                    Log.d("ViewModel", "No friends found for userId: $userId")
                    return@launch
                }
                // Observe each friend's story
                userRepository.observeFriendsStory(friendsUserIds + userId) { storyUpdates ->
                    // Get the current list of stories and ensure the "Add Story" item is at the top
                    val updatedStories: MutableList<Story> = mutableListOf(Story("Add Story", ""))
                    storyUpdates?.let { latestStories ->
                        updatedStories.addAll(latestStories)
                    }

                    val yourStoryIndex = updatedStories.indexOfFirst { it.name == state.value.currentLoggedInUser?.username }
                    if (yourStoryIndex != -1 && updatedStories.size > 2) {
                        val temp = updatedStories[yourStoryIndex]
                        updatedStories[yourStoryIndex] = updatedStories[1]
                        updatedStories[1] = temp
                    }
                    // Update the state with the new list of stories
                    _state.update {
                        it.copy(stories = updatedStories)
                    }
                    Log.d("ViewModel", "Story updates received: $storyUpdates")
                }
            } catch (e: Exception) {
                // Log the error
                Log.e("ViewModel", "Error observing friends' stories", e)
            }
        }
    }

}