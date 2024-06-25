package com.danp.beacon_position_scanner.di

import android.content.Context
import com.danp.artexploreapp.services.utilsIBeacon.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providerConext(@ApplicationContext context: Context): Context{
        return  context
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        // Suponiendo que PermissionManager tiene un método para obtener una instancia desde el contexto
        return PermissionManager.from(context)
    }
}