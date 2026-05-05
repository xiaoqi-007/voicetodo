package com.example.voicetodo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]

        requestPermissions()

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

    private fun requestPermissions() {
        // Android 13+: 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 忽略电池优化（国产手机必须，否则闹钟被杀）
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (_: Exception) {
            // 部分设备不支持，忽略
        }

        // Android 12+: 精确闹钟权限（静默检查，不弹窗）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!AlarmScheduler.canScheduleExact(this)) {
                try {
                    startActivity(AlarmScheduler.getExactAlarmSettingsIntent(this))
                } catch (_: Exception) { }
            }
        }
    }
}
