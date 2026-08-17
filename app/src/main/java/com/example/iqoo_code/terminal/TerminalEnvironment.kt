package com.example.iqoo_code.terminal

import com.example.iqoo_code.fs.Workspace
import java.io.File

/**
 * Owns the terminal's "operating environment": home/working directory,
 * environment variables, PATH, and command history.
 *
 * This class has no Compose/Android UI dependency. It is constructed once
 * with the app's private files directory as the initial home, but nothing
 * outside this class should ever reference `context.filesDir` directly -
 * everything goes through [homeDirectory] / [currentDirectory].
 *
 * In the future this can be swapped for a richer environment backed by a
 * real userspace layout (/home, /bin, /usr/bin, /lib, /tmp, /projects)
 * without changing any calling code, as long as the same public surface
 * is preserved.
 */
class TerminalEnvironment(private val workspace: Workspace) {

    /** The terminal's home directory. Delegates to workspace root. */
    val homeDirectory: File = workspace.projectsDir.parentFile!!.also { it.mkdirs() }

    /** Current working directory. Starts at [homeDirectory]. */
    var currentDirectory: File = homeDirectory
        private set

    /** Previous working directory, used by `cd -` to toggle back. */
    var previousDirectory: File = homeDirectory
        private set

    /** Simple environment variable map, similar in spirit to a shell's env. */
    val environmentVariables: MutableMap<String, String> = mutableMapOf(
        "HOME" to homeDirectory.absolutePath,
        "USER" to "iQOOUser",
        "SHELL" to "iqoo-terminal",
        "PATH" to "/system/bin:/system/xbin"
    )

    val path: String
        get() = environmentVariables["PATH"] ?: ""

    /** Full command history, oldest first. */
    private val _history = mutableListOf<String>()
    val history: List<String> get() = _history

    /** Cursor used while a UI browses previous commands (e.g. up/down). */
    var historyIndex: Int = -1

    fun addToHistory(command: String) {
        if (command.isNotBlank()) {
            _history.add(command)
        }
        historyIndex = _history.size
    }

    fun clearHistory() {
        _history.clear()
        historyIndex = _history.size
    }

    fun previousHistory(): String? {
        if (_history.isEmpty()) return null
        if (historyIndex > 0) historyIndex--
        return _history.getOrNull(historyIndex)
    }

    fun nextHistory(): String? {
        if (_history.isEmpty()) return null
        if (historyIndex < _history.size - 1) {
            historyIndex++
            return _history.getOrNull(historyIndex)
        }
        historyIndex = _history.size
        return null
    }

    /**
     * Resolves a raw path argument (relative or absolute) against the
     * current directory, without resolving symlinks or doing any
     * validation (existence checks are the caller's responsibility).
     */
    fun resolvePath(rawPath: String): File {
        return if (rawPath.startsWith("/")) {
            workspace.resolve(rawPath)
        } else {
            workspace.resolve(File(currentDirectory, rawPath).path)
        }
    }

    /**
     * Returns the current directory as a "~"-relative display path when
     * inside the home directory tree, otherwise the absolute path.
     */
    fun displayPath(): String {
        val homePath = homeDirectory.absolutePath
        val curPath = currentDirectory.absolutePath
        return when {
            curPath == homePath -> "~"
            curPath.startsWith("$homePath/") -> "~" + curPath.removePrefix(homePath)
            else -> curPath
        }
    }

    /**
     * Attempts to change the current directory. Returns null on success or
     * an error message describing why the change failed.
     */
    fun changeDirectory(target: File): String? {
        if (!target.exists()) {
            return "no such directory: ${target.absolutePath}"
        }
        if (!target.isDirectory) {
            return "not a directory: ${target.absolutePath}"
        }
        if (!isWithinSandbox(target)) {
            return "permission denied: ${target.absolutePath}"
        }
        previousDirectory = currentDirectory
        currentDirectory = target
        return null
    }

    /**
     * Toggles between the current directory and the previous directory.
     * Returns null on success or an error message.
     */
    fun togglePreviousDirectory(): String? {
        val target = previousDirectory
        if (!target.exists()) {
            return "no such directory: ${target.absolutePath}"
        }
        if (!target.isDirectory) {
            return "not a directory: ${target.absolutePath}"
        }
        if (!isWithinSandbox(target)) {
            return "permission denied: ${target.absolutePath}"
        }
        val oldCurrent = currentDirectory
        currentDirectory = target
        previousDirectory = oldCurrent
        return null
    }

    /**
     * Returns true if [file] lives inside the sandboxed home directory
     * tree. Used by destructive commands (e.g. rm) to avoid touching
     * anything outside the app's permitted storage.
     */
    fun isWithinSandbox(file: File): Boolean {
        return workspace.isWithinRoot(file)
    }
}

