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

    suspend fun updateUserIsTyping(userId: String, chatId: String, isTyping: Boolean) {
        try {
            // Fetch the chat document by chatId
            val chatDocument = chatCollectionRef.document(chatId).get().await()

            if (chatDocument.exists()) {
                val chatData = chatDocument.data

                if (chatData != null) {
                    // Determine which user object to update
                    val user1 = chatData["user1"] as? Map<*, *>
                    val user2 = chatData["user2"] as? Map<*, *>

                    val updateField = when (userId) {
                        user1?.get("userId") -> "user1.isTyping"
                        user2?.get("userId") -> "user2.isTyping"
                        else -> null
                    }

                    if (updateField != null) {
                        // Update the correct user's isTyping field
                        chatCollectionRef.document(chatId).update(updateField, isTyping).await()
                        println("Updated $updateField to $isTyping for user $userId in chat $chatId")
                    } else {
                        println("User with userId $userId is not part of chat $chatId.")
                    }
                }
            } else {
                println("Chat document with id $chatId does not exist.")
            }
        } catch (e: Exception) {
            println("Error updating typing status: ${e.message}")
        }
    }

     fun observePartnerUser(chatId: String, userId: String, onResult: (Boolean, String) -> Unit) {
        chatCollectionRef.document(chatId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error listening to chat updates: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val chatData = snapshot.data

                if (chatData != null) {
                    // Access user data for user1 and user2
                    val user1 = chatData["user1"] as? Map<*, *>
                    val user2 = chatData["user2"] as? Map<*, *>

                    // Determine which user is the partner based on userId
                    val partnerUserData = when (userId) {
                        user1?.get("userId") -> user1
                        user2?.get("userId") -> user2
                        else -> null
                    }

                    // If partner user data exists, extract isTyping and status fields
                    partnerUserData?.let {
                        val isTyping = it["isTyping"] as? Boolean ?: false
                        val status = it["status"] as? String ?: ""

                        // Return the observed values
                        onResult(isTyping, status)
                    }
                }
            }
        }
    }


    fun updateUserStatus(chatId: String, userId: String, isOnline: Boolean) {
        chatCollectionRef.document(chatId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val chatData = documentSnapshot.data
                val user1 = chatData?.get("user1") as? Map<*, *>
                val user2 = chatData?.get("user2") as? Map<*, *>
                val updateField = when (userId) {
                    user1?.get("userId") -> "user1.status"
                    user2?.get("userId") -> "user2.status"
                    else -> null
                }
                if (updateField != null) {
                    val isOnline = if (isOnline) "In Chat" else "offline"
                    chatCollectionRef.document(chatId).update(updateField, isOnline).addOnSuccessListener {
                        println("Updated $updateField to $isOnline for user $userId in chat $chatId")
                        }.addOnFailureListener { e ->
                        println("Error updating user status: ${e.message}")
                    }
                } else {
                    println("User with userId $userId is not part of chat $chatId.")
                }
            }
        }.addOnFailureListener {
            println("Error fetching chat document: ${it.message}")
        }
    }

    suspend fun updateLastMessage (
        chatId: String,
        message: Messages
    ): Result<Unit> {
        return try {
            val documentRef = chatCollectionRef.document(chatId)

            // Check if the chat document exists
            val documentSnapshot = documentRef.get().await()

            if (documentSnapshot.exists()) {
                // Chat exists, update the messages field
                documentRef.update("lastMessage", message).await()
                Log.d("Firestore", "Message sent successfully")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            // Handle error
            Log.e("Firestore", "Error sending message: ${e.message}", e)
            Result.Failure(e)
        }
    }

    suspend fun getAllFriends(userId: String): List<String> {
        return try {
            val querySnapshot = chatCollectionRef
                .where(
                    Filter.or(
                        Filter.equalTo("user1.userId", userId),
                        Filter.equalTo("user2.userId", userId)
                    )
                )
                .get()
                .await()

            // Extracting userIds from the query result
            val friendsUserIds = mutableListOf<String>()
            for (document in querySnapshot.documents) {
                val user1Id = document.getString("user1.userId")
                val user2Id = document.getString("user2.userId")

                // Add the friend userIds, skipping the current userId
                if (user1Id != userId) user1Id?.let { friendsUserIds.add(it) }
                if (user2Id != userId) user2Id?.let { friendsUserIds.add(it) }
            }

            // Return the list of friends' user IDs
            friendsUserIds

        }catch (e: Exception){
            Log.e("Firestore", "Error fetching friends: ${e.message}", e)
            emptyList()
        }
    }
}