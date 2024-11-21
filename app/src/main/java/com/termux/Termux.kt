package com.termux

import android.content.Context
import android.util.Log
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.utils.SELinuxUtils
import com.termux.utils.ShellEnvironmentUtils
import java.io.File
import java.lang.Exception

object Termux {
    fun getTerminalSession(context: Context, client: TerminalSessionClient = DefaultTerminalSessionClient): TerminalSession {
        runCatching {
            val cwd = context.filesDir.absolutePath

            var shell = "/bin/sh"
            if (File("/bin/sh").exists().not()) {
                shell = "/system/bin/sh"
            }

            return TerminalSession(
                shell,
                cwd,
                arrayOf<String>(), // args
                setupEnv(context, shell), // env
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                client
            )
        }.onFailure {
            Log.e("Termux", "getTerminalSession: ${it.stackTraceToString()}")
        }.getOrThrow()
    }

    private fun setupEnv(
        context: Context,
        shell: String
    ): Array<String> {
        val filesDirPath = context.filesDir.absolutePath

        val environment = hashMapOf<String, String>()
        environment["FUQIULUO"] = "1"

        environment["PREFIX"] = "$filesDirPath"
        environment["PATH"] = "$filesDirPath/bin:$filesDirPath/aarch64-linux-android/bin:/bin"
        environment["LD_LIBRARY_PATH"] = "$filesDirPath/lib:$filesDirPath/aarch64-linux-android/lib"
        environment["TMPDIR"] = "$filesDirPath/home"

        environment["HOME"] = context.filesDir.resolve("home").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }.absolutePath
        environment["PUBLIC_HOME"] = context.getExternalFilesDir(null)!!.absolutePath

        environment["COLORTERM"] = "truecolor"
        environment["HISTCONTROL"] = "ignoreboth"
        environment["SHELL_CMD_RUNNER_NAME"] = "terminal-session"
        environment["TERM"] = "xterm-256color"

        environment["TERMUX_APP__PACKAGE_NAME"] = context.packageName
        environment["TERMUX_APP__PID"] = android.os.Process.myPid().toString()
        environment["TERMUX_APP__APK_FILE"] = context.packageCodePath
        environment["TERMUX__USER_ID"] = "0"

        environment["TERMUX__UID"] = android.os.Process.myUid().toString()

        environment["TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE"] = "false"
        environment["TERMUX__SE_FILE_CONTEXT"] = SELinuxUtils.getFileContext(filesDirPath) ?: ""
        environment["TERMUX__SE_PROCESS_CONTEXT"] = SELinuxUtils.getContext() ?: ""
        environment["TERMUX_VERSION"] = "0.118.0"
        environment["TERMUX_APP__APP_VERSION_NAME"] = "0.118.0"
        environment["TERMUX_APP__APP_VERSION_CODE"] = "1019"

        environment["TERMUX_APP__IS_DEBUGGABLE_BUILD"] = "false"


        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_ASSETS")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_DATA")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_ROOT")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_STORAGE")

        // EXTERNAL_STORAGE is needed for /system/bin/am to work on at least
        // Samsung S7 - see https://plus.google.com/110070148244138185604/posts/gp8Lk3aCGp3.
        // https://cs.android.com/android/_/android/platform/system/core/+/fc000489
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "EXTERNAL_STORAGE")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ASEC_MOUNTPOINT")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "LOOP_MOUNTPOINT")

        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_RUNTIME_ROOT")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_ART_ROOT")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_I18N_ROOT")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "ANDROID_TZDATA_ROOT")

        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "BOOTCLASSPATH")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "DEX2OATBOOTCLASSPATH")
        ShellEnvironmentUtils.putToEnvIfInSystemEnv(environment, "SYSTEMSERVERCLASSPATH")

        environment["LANG"] = "en_US.UTF-8"
        environment["LANGUAGE"] = "en_US.UTF-8"

        val bashFile = context.filesDir.resolve("usr/bin/bash")
        environment["SHELL"] = if (bashFile.exists()) bashFile.absolutePath else shell
        environment["SHELL_CMD__SHELL_ID"] = "0"
        environment["SHELL_CMD__PACKAGE_NAME"] = context.packageName
        environment["SHLVL"] = "1"

        return environment.map { "${it.key}=${it.value}" }.toTypedArray()
    }

    object DefaultTerminalSessionClient: TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) {
            Log.d("DefaultTerminalSessionClient", changedSession.emulator.screen.transcriptText)
        }

        override fun onTitleChanged(changedSession: TerminalSession?) {
        }

        override fun onSessionFinished(finishedSession: TerminalSession?) {
        }

        override fun onCopyTextToClipboard(
            session: TerminalSession?,
            text: String?
        ) {
        }

        override fun onPasteTextFromClipboard(session: TerminalSession?) {
        }

        override fun onBell(session: TerminalSession?) {
        }

        override fun onColorsChanged(session: TerminalSession?) {
        }

        override fun onTerminalCursorStateChange(state: Boolean) {
        }

        override fun getTerminalCursorStyle(): Int? {
            return TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE
        }

        override fun logError(tag: String?, message: String?) {
        }

        override fun logWarn(tag: String?, message: String?) {
        }

        override fun logInfo(tag: String?, message: String?) {
        }

        override fun logDebug(tag: String?, message: String?) {
        }

        override fun logVerbose(tag: String?, message: String?) {
        }

        override fun logStackTraceWithMessage(
            tag: String?,
            message: String?,
            e: Exception?
        ) {
        }

        override fun logStackTrace(tag: String?, e: Exception?) {
        }
    }
}