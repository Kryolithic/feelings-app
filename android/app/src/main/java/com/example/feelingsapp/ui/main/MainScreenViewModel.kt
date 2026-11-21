package com.example.feelingsapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    val user: StateFlow<User?> = repository.user
    val logs: StateFlow<List<LogEntry>> = repository.logs
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
    val isFirebaseConfigured: Boolean = repository.isFirebaseConfigured
    val syncError: StateFlow<String?> = repository.syncError

    fun login(newUser: User) {
        viewModelScope.launch {
            repository.login(newUser)
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<User> {
        return repository.signInWithGoogleIdToken(idToken)
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

    override fun onCleared() {
        super.onCleared()
        repository.clear()
    }
}
