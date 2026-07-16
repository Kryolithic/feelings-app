package com.example.feelingsapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {
    
    val user: StateFlow<User?> = repository.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val logs: StateFlow<List<LogEntry>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun login(newUser: User) {
        viewModelScope.launch {
            repository.login(newUser)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun saveLog(log: LogEntry) {
        viewModelScope.launch {
            repository.saveLog(log)
        }
    }

    fun resetLogs() {
        viewModelScope.launch {
            repository.resetLogs()
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(enabled)
        }
    }
}
