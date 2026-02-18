package com.elmendezz.qsre

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellHelper {
    data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

    fun runAsRoot(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val isReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val stdout = isReader.readLines().joinToString("\n")
            val stderr = errReader.readLines().joinToString("\n")
            val exitCode = process.waitFor()

            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }
}
