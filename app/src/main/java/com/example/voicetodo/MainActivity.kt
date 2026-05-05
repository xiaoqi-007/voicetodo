package com.example.voicetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.voicetodo.ui.MainScreen
import com.example.voicetodo.ui.theme.VoiceTodoTheme
import com.example.voicetodo.viewmodel.TodoViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TodoViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]

        // 请求通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            VoiceTodoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val todos by viewModel.todos.collectAsState()

                    MainScreen(
                        todos = todos,
                        onAddTodo = { parsed -> viewModel.addTodo(parsed) },
                        onToggleComplete = { todo -> viewModel.toggleComplete(todo) },
                        onDelete = { todo -> viewModel.deleteTodo(todo) }
                    )
                }
            }
        }
    }
}
