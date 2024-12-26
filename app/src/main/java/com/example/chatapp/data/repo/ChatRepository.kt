package com.example.chatapp.data.repo

import android.util.Log
import com.example.chatapp.ui.chat.MessageCollection
import com.example.chatapp.ui.chat.Messages
import com.example.chatapp.ui.home.Chat
import com.example.chatapp.ui.signIn.User
import com.example.chatapp.util.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    firebaseFirestore: FirebaseFirestore,
) {
    private val chatCollectionRef = firebaseFirestore.collection("chats")
    private val chatDetailsCollectionRef = firebaseFirestore.collection("chat_details")


    fun setChatsUpdateListener(userId: String, onChatsUpdated: (List<Chat>) -> Unit) {
        chatCollectionRef.where(
            Filter.or(
                Filter.equalTo("user1.userId", userId),
                Filter.equalTo("user2.userId", userId)
            )
        ).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error listening to chat updates: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                try {
                    val chats = snapshot.documents.mapNotNull { document ->
                        document.toObject(Chat::class.java)
                    }
                    onChatsUpdated(chats)
                } catch (e: Exception) {
                    Log.e("Firestore", "Error deserializing chat documents: ${e.message}", e)
                }
            } else {
                onChatsUpdated(emptyList()) // No documents match the query
            }
        }
    }

    suspend fun createChat(chat: Chat): Result<Unit> {
        return try {
            val id = chatCollectionRef.document().id
            chatCollectionRef.document(id).set(chat.copy(chatId = id)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        sender: User,
        message: Messages
    ): Result<Unit> {
        return try {
            val documentRef = chatDetailsCollectionRef.document(chatId)

            // Check if the chat document exists
            val documentSnapshot = documentRef.get().await()

            if (documentSnapshot.exists()) {
                // Chat exists, update the messages field
                documentRef.update("messages", FieldValue.arrayUnion(message)).await()
                Log.d("Firestore", "Message sent successfully")
                Result.Success(Unit)
            } else {
                // Chat does not exist, create a new chat document
                val messageCollection = MessageCollection(
                    chatId = chatId,
                    sender = sender,
                    messages = listOf(message)
                )
                documentRef.set(messageCollection).await()
                Log.d("Firestore", "New chat created and message sent successfully")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            // Handle error
            Log.e("Firestore", "Error sending message: ${e.message}", e)
            Result.Failure(e)
        }
    }

    suspend fun getChatDetailWithChatId(chatId: String): Result<Chat> {
        return try {
            val documentSnapshot = chatCollectionRef.document(chatId).get().await()

            if (documentSnapshot.exists()) {
                Log.d("Firestore", "Chat document exists: $chatId")
                val chat = documentSnapshot.toObject(Chat::class.java)
                if (chat != null) {
                    Result.Success(chat)
                } else {
                    Log.e("Firestore", "Failed to map document to Chat object: $chatId")
                    Result.Failure(Exception("Chat document is null"))
                }
            } else {
                Log.d("Firestore", "Chat document does not exist: $chatId")
                Result.Failure(Exception("Chat document not found"))
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching chat details for chatId: $chatId", e)
            Result.Failure(e)
        }
    }

    fun setListenerForNewMessagesInChat(chatId: String, onNewMessage: (MessageCollection) -> Unit) {
        chatDetailsCollectionRef.document(chatId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error listening to chat updates: ${error.message}", error)
                return@addSnapshotListener
            } else {
                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(MessageCollection::class.java)?.let { messageCollection ->
                        onNewMessage(messageCollection)
                    }
                }

            }
        }
    }

}