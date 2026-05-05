package com.example.voicetodo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.voicetodo.reminder.AlarmScheduler
import com.example.voicetodo.ui.MainScreen
import com.example.voicetodo.ui.theme.VoiceTodoTheme
import com.example.voicetodo.viewmodel.TodoViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TodoViewModel

    // 通知权限
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // 精确闹钟权限
    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]

        // Android 13+: 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+: 请求精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!AlarmScheduler.canScheduleExact(this)) {
                try {
                    exactAlarmPermissionLauncher.launch(
                        AlarmScheduler.getExactAlarmSettingsIntent(this)
                    )
                } catch (_: Exception) { }
            }
        }

        // Android 14+: 请求全屏通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!pm.canUseFullScreenIntent()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) { }
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
