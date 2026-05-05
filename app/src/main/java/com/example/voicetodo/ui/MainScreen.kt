package com.example.voicetodo.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.voicetodo.data.TodoItem
import com.example.voicetodo.update.UpdateManager
import com.example.voicetodo.voice.VoiceRecognizer
import com.example.voicetodo.voice.TextParser
import com.example.voicetodo.voice.ParsedTodo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    todos: List<TodoItem>,
    onAddTodo: (ParsedTodo) -> Unit,
    onToggleComplete: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onSetAlarm: (TodoItem) -> Unit = {}
) {
    val context = LocalContext.current
    val voiceRecognizer = remember { VoiceRecognizer(context) }
    val textParser = remember { TextParser() }
    val updateManager = remember { UpdateManager(context) }
    val recognitionState by voiceRecognizer.state.collectAsState()
    val updateState by updateManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var textInput by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val currentVersion = remember { updateManager.getCurrentVersionName() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) voiceRecognizer.startListening()
    }

    val externalVoiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            voiceRecognizer.handleExternalResult(result.data)
        } else {
            voiceRecognizer.handleExternalCancelled()
        }
    }

    // 处理识别结果
    LaunchedEffect(recognitionState) {
        when (val state = recognitionState) {
            is VoiceRecognizer.RecognitionState.Result -> {
                val parsed = textParser.parse(state.text)
                onAddTodo(parsed)
                voiceRecognizer.resetState()
            }
            is VoiceRecognizer.RecognitionState.NeedExternalRecognition -> {
                try {
                    val intent = voiceRecognizer.getExternalRecognitionIntent()
                    externalVoiceLauncher.launch(intent)
                } catch (e: Exception) {
                    showTextInput = true
                    voiceRecognizer.resetState()
                }
            }
            else -> {}
        }
    }

    // 启动时自动检查更新
    LaunchedEffect(Unit) {
        updateManager.checkUpdate()
    }

    // 检测到新版本弹窗
    LaunchedEffect(updateState) {
        if (updateState is UpdateManager.UpdateState.Available) {
            showUpdateDialog = true
        }
    }

    // 更新弹窗
    if (showUpdateDialog) {
        val available = updateState as? UpdateManager.UpdateState.Available
        if (available != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("发现新版本 v${available.versionName}") },
                text = {
                    Column {
                        Text("当前版本: v$currentVersion")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("更新内容:", fontWeight = FontWeight.Bold)
                        Text(available.changelog, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showUpdateDialog = false
                        updateManager.startDownload(available.apkUrl)
                    }) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }
    }

    // 下载进度 / 状态提示
    when (val state = updateState) {
        is UpdateManager.UpdateState.Downloading -> {
            // 可以加一个 Snackbar 或进度条
        }
        is UpdateManager.UpdateState.Error -> {
            LaunchedEffect(state) {
                // 自动重置
            }
        }
        else -> {}
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "语音待办",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // 检查更新按钮
                    IconButton(onClick = {
                        scope.launch { updateManager.checkUpdate() }
                    }) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "检查更新",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TodoList(
                todos = todos,
                onToggleComplete = onToggleComplete,
                onDelete = onDelete,
                onSetAlarm = onSetAlarm,
                modifier = Modifier.padding(bottom = 160.dp)
            )

            // 更新状态提示条
            when (val state = updateState) {
                is UpdateManager.UpdateState.Checking -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在检查更新...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is UpdateManager.UpdateState.UpToDate -> {
                    // 可选：短暂显示"已是最新"
                }
                is UpdateManager.UpdateState.Downloading -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在下载更新...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is UpdateManager.UpdateState.ReadyToInstall -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "下载完成，正在安装...",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {}
            }

            // 底部输入区域
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showTextInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("输入待办，如：明天下午3点开会") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onAddTodo(textParser.parse(textInput))
                                    textInput = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "添加")
                        }
                    }
                    TextButton(onClick = { showTextInput = false }) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("切换到语音输入")
                    }
                } else {
                    val statusText = when (val state = recognitionState) {
                        is VoiceRecognizer.RecognitionState.Idle -> "点击麦克风开始语音输入"
                        is VoiceRecognizer.RecognitionState.Listening -> "正在聆听..."
                        is VoiceRecognizer.RecognitionState.PartialResult -> state.text
                        is VoiceRecognizer.RecognitionState.Result -> "已识别: ${state.text}"
                        is VoiceRecognizer.RecognitionState.Error -> state.message
                        is VoiceRecognizer.RecognitionState.NeedExternalRecognition -> "正在启动语音识别..."
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (recognitionState) {
                            is VoiceRecognizer.RecognitionState.Error -> MaterialTheme.colorScheme.error
                            is VoiceRecognizer.RecognitionState.Result -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    val isListening = recognitionState is VoiceRecognizer.RecognitionState.Listening ||
                            recognitionState is VoiceRecognizer.RecognitionState.PartialResult

                    Box(contentAlignment = Alignment.Center) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@FloatingActionButton
                                }
                                if (isListening) voiceRecognizer.stopListening()
                                else voiceRecognizer.startListening()
                            },
                            modifier = Modifier.size(64.dp),
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "停止" else "开始语音",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isListening) "点击停止" else "按住说话",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    TextButton(onClick = { showTextInput = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("切换到文字输入")
                    }
                }
            }
        }
    }
}
