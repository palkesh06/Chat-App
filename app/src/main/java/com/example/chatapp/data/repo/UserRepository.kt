package com.example.chatapp.data.repo

import android.content.ContentValues
import android.util.Log
import com.example.chatapp.ui.home.Story
import com.example.chatapp.ui.signIn.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    firebaseFirestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth
) {
    private val usersCollectionRef = firebaseFirestore.collection("users")
    private val auth = firebaseAuth

    suspend fun getCurrentUser(): User? {
        return try {
            val querySnapshot = usersCollectionRef.whereEqualTo("userId", auth.currentUser?.uid).get().await()
            if (querySnapshot.isEmpty) {
                null // No user found
            } else {
                querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching user by email: ${e.message}", e)
            throw e // Propagate the exception to the caller
        }
    }


    suspend fun findUserWithEmail(email: String): User? {
        return try {
            val querySnapshot = usersCollectionRef.whereEqualTo("email", email).get().await()
            if (querySnapshot.isEmpty) {
                null // No user found
            } else {
                querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching user by email: ${e.message}", e)
            throw e // Propagate the exception to the caller
        }
    }

    fun addUserToFireStore(userDataMap: Map<String, String?>) {
        val userDocument = usersCollectionRef.document(userDataMap["userId"].toString())
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                userDocument.update(userDataMap).addOnSuccessListener {
                    Log.d(ContentValues.TAG, "User Updated to Firestore")
                }.addOnFailureListener {
                    Log.e(ContentValues.TAG, "Error updating user to Firestore", it)
                }
            } else {
                userDocument.set(userDataMap).addOnSuccessListener {
                    Log.d(ContentValues.TAG, "User added to Firestore")
                }.addOnFailureListener {
                    Log.e(ContentValues.TAG, "Error adding user to Firestore", it)
                }
            }
        }
    }

    suspend fun addToStory(userId: String, url: String) {
        val documentRef = usersCollectionRef.document(userId)
        val snapshot = documentRef.get().await()

        if (snapshot.exists()) {
            // Update the existing document
            documentRef.update("storyUrl", url).await()
            Log.d("Firestore", "Story URL updated successfully for userId: $userId")
        } else {
            Log.d("Firestore", "No matching documents found for userId: $userId")
        }
    }


    fun observeFriendsStory(userIds: List<String>, onResult: (List<Story>?) -> Unit) {
        // Validate if userIds is not empty
        if (userIds.isEmpty()) {
            Log.w("Firestore", "No userIds provided to observe stories")
            onResult(null)
            return
        }

        // Split the userIds list into chunks of 10 to adhere to Firestore's limit
        val chunkedUserIds = userIds.chunked(10)

        // Observe stories for each chunk separately
        chunkedUserIds.forEach { chunk ->
            // Create a query that observes the stories of the provided user IDs in the chunk
            val query = usersCollectionRef.whereIn("userId", chunk)

            // Start observing for updates
            query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    // Log the error if something goes wrong during the query
                    Log.e("Firestore", "Error observing stories: ${exception.message}", exception)
                    onResult(null)  // Return null in case of an error
                } else {
                    snapshot?.documents?.let { documents ->
                        // Find the first document that matches the userId and storyUrl
                        val storyUpdate = documents.mapNotNull { doc ->

                            val username = doc.getString("username")
                            val storyUrl = doc.getString("storyUrl")

                            // Ensure the data is valid before creating a Story object
                            if (!username.isNullOrBlank() && !storyUrl.isNullOrBlank()) {
                                Story(username, storyUrl)
                            } else {
                                null
                            }
                        }

                        // Return the story update or null if no valid story is found
                        onResult(storyUpdate)
                    } ?: run {
                        // Handle case where the snapshot is empty or null
                        Log.d("Firestore", "No story documents found for the provided userIds.")
                        onResult(null)
                    }
                }
            }
        }
    }

}