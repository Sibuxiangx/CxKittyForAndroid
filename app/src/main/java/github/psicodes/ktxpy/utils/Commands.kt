package github.psicodes.ktxpy.utils

import android.content.Context

object Commands {
    const val NEED_CLEAR = true

    fun getBasicCommand(context: Context): String {
        return "clear"
    }

    fun getInterpreterCommand(context: Context, filePath: String): String {
        return "python3 $filePath && " +
                "echo \'[Enter to Exit]\' && " +
                "read junk && exit"
    }

    fun getPythonShellCommand(context: Context): String {
        return "python3 && " +
                "echo \'[Enter to Exit]\' && read junk && exit"
    }
}