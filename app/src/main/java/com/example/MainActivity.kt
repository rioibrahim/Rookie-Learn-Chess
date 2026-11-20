package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.audio.SoundEngine
import com.example.data.ProgressRepository
import com.example.data.RookieDatabase
import com.example.data.SettingsDataStore
import com.example.ui.RookieApp
import com.example.ui.RookieViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: RookieViewModel by viewModels {
    val db = RookieDatabase.getInstance(applicationContext)
    val settings = SettingsDataStore(applicationContext)
    val repository = ProgressRepository(db.rookieDao(), settings)
    val soundEngine = SoundEngine(applicationContext.cacheDir)
    RookieViewModel.Factory(repository, soundEngine)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      RookieApp(viewModel = viewModel)
    }
  }
}
