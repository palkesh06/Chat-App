package com.example.chatapp.di

import android.content.Context
import com.example.chatapp.data.repo.ChatRepository
import com.example.chatapp.data.repo.ConnectivityRepository
import com.example.chatapp.data.repo.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    companion object {
        @Singleton
        @Provides
        fun provideConnectivityRepository(
            @ApplicationContext context: Context
        ): ConnectivityRepository{
            return ConnectivityRepository(context)

        }

        @Singleton
        @Provides
        fun provideUserRepository(
            db: FirebaseFirestore,
            auth: FirebaseAuth
        ): UserRepository {
            return UserRepository(db, auth)
        }

        @Singleton
        @Provides
        fun providesChatRepository(
            db: FirebaseFirestore
        ): ChatRepository{
            return ChatRepository(db)
        }
    }

}