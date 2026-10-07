package com.t2e.toolbox.eqbirvin.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException

private const val TAG = "ShizukuShell"

object ShizukuShell {
    fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    suspend fun runShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Shizuku.newProcess method not found", e)
            ShellResult(-1, "", "Shizuku.newProcess method not found: ${e.message.orEmpty()}")
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Access denied to Shizuku.newProcess", e)
            ShellResult(-1, "", "Access denied to Shizuku.newProcess: ${e.message.orEmpty()}")
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "InvocationTargetException while executing Shizuku.newProcess", e)
            val targetMsg = e.targetException?.message ?: e.message.orEmpty()
            ShellResult(-1, "", "Invocation error: $targetMsg")
        } catch (e: IOException) {
            Log.e(TAG, "IOException executing shell command: $command", e)
            ShellResult(-1, "", "IO error: ${e.message.orEmpty()}")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Process interrupted while executing shell command: $command", e)
            ShellResult(-1, "", "Execution interrupted: ${e.message.orEmpty()}")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException executing shell command: $command", e)
            ShellResult(-1, "", "Illegal state: ${e.message.orEmpty()}")
        } finally {
            process?.destroy()
        }
    }
}
