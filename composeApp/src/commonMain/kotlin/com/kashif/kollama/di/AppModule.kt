package com.kashif.kollama.di

import com.kashif.kollama.data.local.database.DatabaseWrapper
import com.kashif.kollama.data.local.service.OllamaService
import com.kashif.kollama.data.repository.ChatRepositoryImpl
import com.kashif.kollama.domain.interfaces.ChatRepository
import com.kashif.kollama.presentation.state.ChatViewModel
import org.koin.dsl.module


fun getAppModule(context: Any)= module {
    single<DatabaseWrapper> { DatabaseWrapper(context) }
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    single { OllamaService() }
    single { ChatViewModel(get(), get()) }
}