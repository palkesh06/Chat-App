package com.example.chatapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {
    companion object {

        @Singleton
        @Provides
        fun provideFireStoreInstance(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }

        @Singleton
        @Provides
        fun provideFirebaseAuthInstance() : FirebaseAuth {
            return FirebaseAuth.getInstance()

        }
    }
}