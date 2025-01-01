package com.example.chatapp.ui.chat

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.repo.ChatRepository
import com.example.chatapp.data.repo.CloudinaryManager
import com.example.chatapp.data.repo.ConnectivityRepository
import com.example.chatapp.data.repo.UserRepository
import com.example.chatapp.ui.signIn.User
import com.example.chatapp.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ChatScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val connectivityRepository: ConnectivityRepository,
    private val cloudinaryManager: CloudinaryManager
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

    fun handleMediaAndSendMessage(
        selectedMediaUris: List<Uri>,
        context: Context,
        result: (List<String>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uploadedUrls = mutableListOf<String>()

            // Upload media and collect URLs
            selectedMediaUris.forEach { uri ->
                val file = uri.toFile(context = context)
                if (file != null) {
                    val uploadedUrl = cloudinaryManager.uploadMedia(file)
                    uploadedUrl?.let { uploadedUrls.add(it) }
                }
            }

            result.invoke(uploadedUrls)
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
}