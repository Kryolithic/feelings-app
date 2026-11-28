package com.example.feelingsapp

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feelingsapp.data.DefaultDataRepository
import com.example.feelingsapp.theme.FeelingsAppTheme
import com.example.feelingsapp.ui.main.MainScreen
import com.example.feelingsapp.ui.main.MainScreenViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.setFlags(
      WindowManager.LayoutParams.FLAG_SECURE,
      WindowManager.LayoutParams.FLAG_SECURE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      setRecentsScreenshotEnabled(false)
    }

    enableEdgeToEdge()
    setContent {
      val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultDataRepository(applicationContext))
      }
      val isDarkTheme by viewModel.isDarkTheme.collectAsState()

      FeelingsAppTheme(darkTheme = isDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
