package com.example.iqoo_code.fs

import java.io.File

class Workspace(private val root: File) {
    val projectsDir: File = File(root, "projects").apply { mkdirs() }
    val runtimeDir: File = File(root, "runtime").apply { mkdirs() }

    fun resolve(path: String): File {
        val target = if (path.startsWith("/")) File(path) else File(root, path)
        return try {
            File(target.path).canonicalFile
        } catch (_: Exception) {
            File(target.path).absoluteFile
        }
    }

    fun resolveSafe(path: String): File {
        val resolved = resolve(path)
        if (!isWithinRoot(resolved)) {
            throw SecurityException("Path escapes workspace root: $path")
        }
        return resolved
    }

    fun isWithinRoot(file: File): Boolean {
        val rootPath = root.canonicalPath
        val targetPath = try {
            file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }
}
