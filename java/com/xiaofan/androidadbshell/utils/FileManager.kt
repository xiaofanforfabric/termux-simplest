package com.xiaofan.androidadbshell.utils

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AdbBinaryExecutor(private val context: Context) {
    private val adbFile = File(context.filesDir, "adb").apply {
        Log.d("ADB_PATH", "ADB目标路径: $absolutePath")
    }
    private val prootFile = File(context.filesDir, "proot").apply {
        Log.d("PROOT_PATH", "proot目标路径: $absolutePath")
    }
    private val bashFile = File(context.filesDir, "bash").apply {
        Log.d("BASH_PATH", "bash目标路径: $absolutePath")
    }

    init {
        ensureAdbReady()
    }

    private fun ensureAdbReady() {
        try {
            // 检查两个文件是否都已存在且有效
            if (adbFile.exists() && adbFile.length() > 1_000_000 &&
                prootFile.exists() && prootFile.length() > 1_000_000 &&
                bashFile.exists() && bashFile.length() > 1_000_000) {
                Log.d("ADB_INIT", "使用现有文件")
                return
            }

            // 从assets释放文件
            extractFromAssets()

            // 设置权限
            setFullPermissions()

        } catch (e: Exception) {
            Log.e("ADB_INIT", "初始化失败", e)
            throw RuntimeException("ADB初始化失败: ${e.message}")
        }
    }

    private fun extractFromAssets() {
        Log.d("ADB_INIT", "开始从assets释放文件...")

        // 释放adb文件
        extractSingleFile("adb", adbFile)

        // 释放proot文件
        extractSingleFile("proot", prootFile)

        //释放bash文件
        extractSingleFile("bash", bashFile)
    }

    private fun extractSingleFile(assetName: String, targetFile: File) {
        val tempFile = File(context.filesDir, "$assetName.tmp").apply {
            deleteOnExit()
        }

        try {
            // 从assets复制到临时文件
            context.assets.open(assetName).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    Log.d("ADB_INIT", "$assetName 临时文件大小: ${tempFile.length()}字节")
                }
            }

            // 重命名为目标文件
            if (!tempFile.renameTo(targetFile)) {
                throw IllegalStateException("$assetName 文件重命名失败")
            }
            Log.d("ADB_INIT", "$assetName 释放成功，最终大小: ${targetFile.length()}字节")

        } finally {
            tempFile.delete() // 确保清理临时文件
        }
    }

    private fun setFullPermissions() {
        try {
            // 设置adb文件权限
            setFilePermissions(adbFile)

            // 设置proot文件权限
            setFilePermissions(prootFile)

            //设置bash文件权限
            setFilePermissions(bashFile)

        } catch (e: Exception) {
            Log.e("ADB_PERM", "权限设置失败", e)
            throw e
        }
    }

    private fun setFilePermissions(file: File) {
        // 方法1：Java API
        file.setExecutable(true, false)
        file.setReadable(true, false)

        // 方法2：Linux命令
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.chmod(file.absolutePath, 0b111_101_101) // rwxr-xr-x
        } else {
            Runtime.getRuntime()
                .exec(arrayOf("chmod", "755", file.absolutePath))
                .waitFor()
        }
        Log.d("ADB_PERM", "${file.name} 权限设置结果: 可执行=${file.canExecute()}")
    }

    fun executeCommand(command: String): String {
        return try {
            Log.d("ADB_CMD", "Executing: $command")

            // 使用proot运行adb命令
            val fullCommand = arrayOf(
                prootFile.absolutePath,
                "-b", "${context.filesDir.absolutePath}/tmp:/tmp",
                adbFile.absolutePath
            ) + command.split(" ")

            val process = ProcessBuilder()
                .command(*fullCommand)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            output.trim()
        } catch (e: Exception) {
            Log.e("ADB_CMD", "Command failed", e)
            "ERROR: ${e.message}"
        }
    }
}