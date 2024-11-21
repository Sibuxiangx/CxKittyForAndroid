/*
Copyright (C) 2022-2023  PsiCodes

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
package github.psicodes.ktxpy.activities

import android.os.Bundle
import android.system.Os
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.termux.app.TermuxInstaller
import github.psicodes.ktxpy.dataStore.SettingsDataStore
import github.psicodes.ktxpy.ui.theme.KtxPyTheme
import github.psicodes.ktxpy.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import android.util.Pair
import com.termux.app.TermuxInstaller.ensureDirectoryExists
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class HomeActivity: ComponentActivity() {
    private lateinit var dataStore: SettingsDataStore
    private val isFileExtracting = mutableStateOf(false)
    private val stagingText = mutableStateOf("少女祈祷中...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = SettingsDataStore(applicationContext)
        // create a directory for python files if not exists
        val pythonFilesDir = File(filesDir.absolutePath + "/pythonFiles")
        if (!pythonFilesDir.exists()) {
            pythonFilesDir.mkdir()
        }
        SessionManager.filesDir = pythonFilesDir.absolutePath
        SessionManager.init()

        setContent {
            KtxPyTheme{
                if (isFileExtracting.value){
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(text = stagingText.value)
                    }
                } else {
                    val navHostEngine = rememberAnimatedNavHostEngine(
                        navHostContentAlignment = Alignment.TopCenter,
                        rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING
                    )

                    DestinationsNavHost(
                        navGraph = github.psicodes.ktxpy.ui.screens.NavGraphs.root,
                        dependenciesContainerBuilder = { dependency(this@HomeActivity) },
                        engine = navHostEngine
                    )
                }
            }
        }
        extractFiles()
    }

    private fun extractFiles() {
        isFileExtracting.value = true
        CoroutineScope(Dispatchers.IO).launch {
            if (dataStore.areFilesExtracted.first() == true && filesDir.resolve("ok").exists()) {
                setupSuccess()
            } else {
                dataStore.updateFileStatus(false)
                CoroutineScope(Dispatchers.IO).launch {
                    if (filesDir.resolve("bin").exists()) {
                        filesDir.deleteRecursively()
                    }

                    when(val result = TermuxInstaller.setupBootstrapIfNeeded(this@HomeActivity)) {
                        is Exception -> {
                            stagingText.value = "无法处理链接: ${result.stackTraceToString()}"
                            return@launch
                        }
                        else -> {
                            stagingText.value = "处理链接成功"
                        }
                    }

                    stagingText.value = "释放Python环境...不要退出耐心等待！"

                    var extraStatus = false
                    val job = lifecycleScope.launch {
                        val tips = arrayOf(
                            "释放Python环境...不要退出耐心等待！",
                            "释放需要时间是因为这个环境真的很大...",
                            "其实还是要自己看看学习视频的！",
                            "觉得不好用的话，不妨PR点功能呗？",
                            "其实我也不知道当前解压到什么阶段了~~",
                            "这个环境真的很大，不要退出哦！",
                            "Python真的在安卓环境真的一坨狗屎...",
                            "伏秋洛是个大傻逼，不要听他的！",
                        )
                        while (!extraStatus) {
                            stagingText.value = tips.random()
                            delay(1000)
                        }
                    }

                    fun extraFromAssets(name: String) {
                        val temp7zStream = assets.open(name)
                        val file = File("${filesDir.absolutePath}/$name")
                        if (file.exists()) {
                            file.delete()
                        }
                        file.createNewFile()
                        temp7zStream.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        temp7zStream.close()
                    }

                    fun extraBoot(name: String, outPath: String) {
                        val file = File("${filesDir.absolutePath}/$name")
                        val outDir = File(outPath)
                        outDir.mkdirs()

                        if (name.endsWith(".7z")) {
                            throw Exception("7z is not supported")
                        }

                        val inputStream = FileInputStream(file)
                        val buffer = ByteArray(8096)

                        ZipInputStream(inputStream).use { zipInput ->
                            var zipEntry: ZipEntry? = null
                            while (true) {
                                zipEntry = zipInput.nextEntry
                                if (zipEntry == null) {
                                    break
                                }
                                val zipEntryName = zipEntry.name
                                val targetFile = File(outPath, zipEntryName)
                                val isDirectory = zipEntry.isDirectory
                                if (!targetFile.exists()) {
                                    if (isDirectory)
                                        targetFile.mkdirs()
                                    else
                                        targetFile.createNewFile()
                                }

                                Log.d("Pydroid", "Extracting ${if (isDirectory) "directory" else "file"} \"$zipEntryName\" to \"$targetFile\".")
                                if (!isDirectory) {
                                    FileOutputStream(targetFile).use { outStream ->
                                        var readBytes: Int
                                        while (zipInput.read(buffer).also { readBytes = it } != -1) {
                                            outStream.write(buffer, 0, readBytes)
                                        }
                                    }
                                }
                            }
                        }

                        inputStream.close()
                        file.delete()
                    }

                    extraFromAssets("boot.zip")
                    extraBoot("boot.zip", filesDir.absoluteFile.resolve("aarch64-linux-android").also {
                        it.mkdirs()
                    }.absolutePath)

                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.updateFileStatus(true)
                    }
                    extraStatus = true
                    job.cancel()

                    if (FIX_PACKAGE || filesDir.resolve("usr/x64").exists()) {
                        val targetBytes = "com.termux".toByteArray()
                        val replacementBytes = "moe.fuqiuluo.xxt".toByteArray()

                        val count = Files.walk(Paths.get(filesDir.absolutePath)).count()
                        var handledCount = 0
                        Files.walk(Paths.get(filesDir.absolutePath)).forEach { path ->
                            if (Files.isRegularFile(path)) {
                                val file = path.toFile()
                                replaceBytesInFile(file, targetBytes, replacementBytes)
                                if (handledCount % 10 == 0) {
                                    stagingText.value = "Handle file: $handledCount/$count"
                                }
                            }
                            handledCount++
                        }
                    }

                    stagingText.value = "设置可执行权限..."
                    Files.walk(Paths.get(filesDir.absolutePath)).forEach { path ->
                        runCatching {
                            Os.chmod(path.toString(), 448)
                        }.onFailure {
                            Log.e(TAG, "file: ${path}, extractFiles: ${it.stackTraceToString()}")
                        }
                    }
                    Files.createFile(Paths.get(filesDir.absolutePath, "ok"))

                    setupSuccess()
                }
            }
        }
    }

    private fun setupSuccess() {
        isFileExtracting.value = false
    }

    companion object {
        const val TAG = "WelcomeActivity"
        const val FIX_PACKAGE = false

        fun replaceBytesInFile(file: File, target: ByteArray, replacement: ByteArray) {
            val content = file.readBytes()
            val updatedContent = replaceBytes(content, target, replacement)
            if (updatedContent != null) {
                file.writeBytes(updatedContent)
            }
        }

        fun replaceBytes(content: ByteArray, target: ByteArray, replacement: ByteArray): ByteArray? {
            assert(target.size == replacement.size)
            val result = content.toMutableList()
            var i = 0
            var replaced = false
            while (i <= result.size - target.size) {
                var j = 0
                while (j < target.size) {
                    if (result[i + j] != target[j]) {
                        break
                    }
                    j++
                }
                if (j == target.size) {
                    for (k in target.indices) {
                        result[i + k] = replacement[k]
                    }
                    i += target.size
                    replaced = true
                } else {
                    i++
                }
            }
            return if (replaced) result.toByteArray() else null
        }
    }
}