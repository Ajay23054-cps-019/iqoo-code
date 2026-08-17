package com.example.iqoo_code.terminal

import java.io.File

object CompletionEngine {

    private val cachedBinDirs by lazy {
        "/system/bin:/system/xbin".split(":").map { File(it) }.filter { it.exists() && it.isDirectory }
    }

    fun complete(input: String, cwd: File, envPath: String): List<String> {
        if (!input.contains(" ") && !input.contains("/")) {
            val cmdMatches = BuiltInCommands.byName.keys.filter { it.startsWith(input) }.sorted()
            val binMatches = cachedBinDirs.flatMap { dir ->
                dir.listFiles()?.filter { it.name.startsWith(input) && it.canExecute() }?.map { it.name } ?: emptyList()
            }.distinct().sorted()
            return (cmdMatches + binMatches).distinct().sorted()
        }

        val lastSlash = input.lastIndexOf("/")
        val prefix = if (lastSlash >= 0) input.substring(lastSlash + 1) else input.substringAfterLast(" ")
        val dir = if (lastSlash >= 0) File(input.substring(0, lastSlash + 1)) else cwd
        val resolved = if (dir.isAbsolute) dir else File(cwd, dir.path)

        if (!resolved.exists() || !resolved.isDirectory) return emptyList()

        return resolved.listFiles()
            ?.filter { it.name.startsWith(prefix) }
            ?.map { if (it.isDirectory) it.name + "/" else it.name }
            ?.sorted() ?: emptyList()
    }
}
