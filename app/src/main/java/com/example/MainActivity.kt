package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.TaskFlowNavGraph
import com.example.ui.theme.TaskFlowTheme
import com.example.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val taskViewModel: TaskViewModel = viewModel()
      val themeMode by taskViewModel.themeMode.collectAsStateWithLifecycle()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
          ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      TaskFlowTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          TaskFlowNavGraph(viewModel = taskViewModel)
        }
      }
    }
  }
}
