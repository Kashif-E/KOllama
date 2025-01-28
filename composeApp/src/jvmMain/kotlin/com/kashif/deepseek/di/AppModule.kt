package com.kashif.deepseek.di

import com.kashif.deepseek.data.local.dao.ChatDao
import com.kashif.deepseek.data.local.database.ChatDatabase
import com.kashif.deepseek.data.repository.ChatRepositoryImpl
import com.kashif.deepseek.domain.repository.ChatRepository
import com.kashif.deepseek.presentation.state.ChatViewModel
import com.kashif.deepseek.data.local.service.OllamaService
import org.koin.dsl.module


val appModule = module {
    single<ChatDatabase> { ChatDatabase.create() }
    single<ChatDao> { get<ChatDatabase>().chatDao() }
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    single { OllamaService() }
    single { ChatViewModel(get(), get()) }
}