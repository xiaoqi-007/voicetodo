package com.example.voicetodo.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import com.example.voicetodo.BuildConfig
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    sealed class UpdateState {
        data object Idle : UpdateState()
        data object Checking : UpdateState()
        data class Available(
            val versionName: String,
            val versionCode: Int,
            val changelog: String,
            val apkUrl: String
        ) : UpdateState()
        data object UpToDate : UpdateState()
        data object Downloading : UpdateState()
        data class DownloadProgress(val progress: Int) : UpdateState()
        data object ReadyToInstall : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var downloadId: Long = -1

    fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt()
                else @Suppress("DEPRECATION") it.versionCode
            }
        } catch (e: Exception) { 0 }
    }

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (e: Exception) { "未知" }
    }

    suspend fun checkUpdate() {
        _state.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val mirrors = listOf(
                    "https://ghfast.top/https://raw.githubusercontent.com/xiaoqi-007/voicetodo/main/version.json",
                    "https://mirror.ghproxy.com/https://raw.githubusercontent.com/xiaoqi-007/voicetodo/main/version.json",
                    "https://raw.githubusercontent.com/xiaoqi-007/voicetodo/main/version.json"
                )

                var jsonStr: String? = null
                for (url in mirrors) {
                    try {
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            jsonStr = response.body?.string()
                            if (jsonStr != null) break
                        }
                    } catch (_: Exception) { }
                }

                if (jsonStr == null) {
                    _state.value = UpdateState.Error("无法连接更新服务器")
                    return@withContext
                }

                val json = JSONObject(jsonStr!!)
                val remoteCode = json.getInt("versionCode")
                val remoteName = json.getString("versionName")
                val apkUrl = json.getString("apkUrl")
                val changelog = json.optString("changelog", "")

                val currentCode = getCurrentVersionCode()

                if (remoteCode > currentCode) {
                    _state.value = UpdateState.Available(
                        versionName = remoteName,
                        versionCode = remoteCode,
                        changelog = changelog,
                        apkUrl = apkUrl
                    )
                } else {
                    _state.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _state.value = UpdateState.Error("检查更新失败: ${e.message}")
            }
        }
    }

    fun startDownload(apkUrl: String) {
        _state.value = UpdateState.Downloading

        val mirrors = listOf(
            "https://ghfast.top/",
            "https://mirror.ghproxy.com/",
            "https://ghproxy.com/"
        )

        val fullUrl = if (apkUrl.startsWith("http")) apkUrl
        else "https://ghfast.top/https://github.com/xiaoqi-007/voicetodo/releases/download/v${BuildConfig.VERSION_NAME}/$apkUrl"

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "VoiceTodo.apk")
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(fullUrl))
            .setTitle("语音待办 更新")
            .setDescription("正在下载新版本...")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // 监听下载完成
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    _state.value = UpdateState.ReadyToInstall
                    installApk(file)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            setDataAndType(uri, "application/vnd.android.package-archive")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.value = UpdateState.Error("安装失败: ${e.message}")
        }
    }
}
