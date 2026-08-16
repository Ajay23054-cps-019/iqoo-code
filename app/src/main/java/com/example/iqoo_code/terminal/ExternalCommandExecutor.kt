package com.example.iqoo_code.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Executes commands that are not recognized as built-ins by shelling out to
 * the Android system shell via [ProcessBuilder].
 *
 * This is intentionally isolated behind a small interface-like object so it
 * can later be swapped for a native (C++/JNI) process/runtime implementation
 * without touching [TerminalEngine] call sites beyond this class.
 *
 * No root, no privilege escalation, no sandbox escape - this simply runs
 * `/system/bin/sh -c "<command>"` with the app's own process permissions,
 * exactly like any other unprivileged Android app could.
 */
object ExternalCommandExecutor {

    data class ExternalResult(
        val stdout: List<String>,
        val stderr: List<String>,
        val exitCode: Int
    )

    private const val TIMEOUT_SECONDS = 15L

    suspend fun execute(rawCommand: String, env: TerminalEnvironment): ExternalResult =
        withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder("/system/bin/sh", "-c", rawCommand)
                    .directory(env.currentDirectory)
                    .redirectErrorStream(false)

                processBuilder.environment().apply {
                    putAll(env.environmentVariables)
                }

                val process = processBuilder.start()

                val stdout = mutableListOf<String>()
                val stderr = mutableListOf<String>()

                BufferedReader(InputStreamReader(process.inputStream)).useLines { seq ->
                    seq.forEach { stdout.add(it) }
                }
                BufferedReader(InputStreamReader(process.errorStream)).useLines { seq ->
                    seq.forEach { stderr.add(it) }
                }

                val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                val exitCode = if (finished) process.exitValue() else {
                    process.destroyForcibly()
                    -1
                }

                if (!finished) {
                    stderr.add("command timed out after ${TIMEOUT_SECONDS}s")
                }

                ExternalResult(stdout, stderr, exitCode)
            } catch (e: Exception) {
                ExternalResult(
                    stdout = emptyList(),
                    stderr = listOf(e.message ?: "failed to execute command"),
                    exitCode = -1
                )
            }
        }
}
