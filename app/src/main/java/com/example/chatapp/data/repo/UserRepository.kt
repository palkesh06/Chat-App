package com.example.chatapp.data.repo

import android.content.ContentValues
import android.util.Log
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
    private val db = firebaseFirestore.collection("users")
    private val auth = firebaseAuth

    suspend fun getCurrentUser(): User? {
        return try {
            val querySnapshot = db.whereEqualTo("userId", auth.currentUser?.uid).get().await()
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
            val querySnapshot = db.whereEqualTo("email", email).get().await()
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
        val userDocument = db.document(userDataMap["userId"].toString())
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

}