package github.psicodes.ktxpy.utils
import android.os.Build
import android.os.FileObserver
import androidx.compose.runtime.mutableStateOf
import com.termux.shared.termux.shell.TermuxShellManager
import com.termux.terminal.TerminalSession

object SessionManager: TermuxShellManager() {
    var sessionList = mutableStateOf(listOf<TerminalSession>())
    var filesDir: String = ""

    fun init() {

    }
}