package com.xiaofan.androidadbshell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.xiaofan.androidadbshell.utils.AdbBinaryExecutor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adbExecutor: AdbBinaryExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adbExecutor = AdbBinaryExecutor(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TerminalTheme {
                TerminalScreen()
            }
        }
    }
}

@Composable
fun TerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            surface = Color.Black,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val terminalContent = remember { mutableStateOf("$ ") }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val inputBuffer = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val terminalSession = remember {
        TerminalSession(context).apply {
            start(
                onOutput = { output ->
                    // 确保在主线程更新UI
                    coroutineScope.launch {
                        terminalContent.value += output
                        scrollState.scrollTo(scrollState.maxValue)
                    }
                },
                onError = { error ->
                    coroutineScope.launch {
                        terminalContent.value += "\u001B[31m$error\u001B[0m"
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            terminalSession.stop()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 终端输出
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = terminalContent.value,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        // 输入区域
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color.Green, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = inputBuffer.value,
                onValueChange = { inputBuffer.value = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        terminalSession.execute(inputBuffer.value)
                        inputBuffer.value = ""
                    }
                ),
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}