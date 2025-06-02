package com.xiaofan.androidadbshell

import android.content.Context
import android.system.Os
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.Executors

class TerminalSession(private val context: Context) {
    private var process: Process? = null
    private lateinit var inputWriter: BufferedWriter
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("TerminalSession"))

    fun start(
        onOutput: (String) -> Unit,
        onError: (String) -> Unit = { Log.e("Terminal", it) }
    ) {
        try {
            process = ProcessBuilder("/system/bin/sh").apply {
                environment().apply {
                    put("TERM", "xterm-256color")
                    put("HOME", context.filesDir.absolutePath)
                }
                redirectErrorStream(true)
            }.start()

            inputWriter = process!!.outputStream.bufferedWriter()
            isRunning = true

            // 使用协程处理输出流
            scope.launch {
                val reader = process!!.inputStream.bufferedReader()
                val buffer = CharArray(8192)

                while (isRunning) {
                    try {
                        val read = reader.read(buffer)
                        when {
                            read > 0 -> {
                                val output = String(buffer, 0, read)
                                withContext(Dispatchers.Main.immediate) {
                                    onOutput(output)
                                }
                            }
                            read == -1 -> break
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            withContext(Dispatchers.Main.immediate) {
                                onError("Read error: ${e.message}")
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            onError("Start failed: ${e.message}")
        }
    }

    fun execute(command: String) {
        if (!isRunning) return
        scope.launch {
            try {
                inputWriter.apply {
                    write("$command\n")
                    flush()
                }
            } catch (e: Exception) {
                Log.e("Terminal", "Execute failed", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.launch {
            try {
                inputWriter.apply {
                    write("exit\n")
                    flush()
                    close()
                }
                process?.destroy()
            } catch (e: Exception) {
                Log.e("Terminal", "Stop failed", e)
            }
        }
        scope.cancel()
    }
}