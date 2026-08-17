package com.example.iqoo_code.terminal

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BuiltInCommands {

    val all: List<BuiltInCommand> = listOf(
        Pwd(),
        Echo(),
        Cd(),
        Ls(),
        Cat(),
        Mkdir(),
        Rm(),
        DateCmd(),
        WhoAmI(),
        Clear(),
        Exit(),
        Help(),
        Cp(),
        Mv(),
        Touch(),
        Rmdir(),
        Tree(),
        Grep(),
        Find(),
        Head(),
        Tail(),
        Wc(),
        Sort(),
        Diff(),
        Which(),
        Env(),
        Export(),
        Unset(),
        History()
    )

    val byName: Map<String, BuiltInCommand> = all.associateBy { it.name }

    class Pwd : BuiltInCommand {
        override val name = "pwd"
        override val description = "Print the current working directory"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult.output(env.currentDirectory.absolutePath)
        }
    }

    class Echo : BuiltInCommand {
        override val name = "echo"
        override val description = "Print the supplied arguments"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult.output(args.joinToString(" "))
        }
    }

    class Cd : BuiltInCommand {
        override val name = "cd"
        override val description = "Change the current working directory"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val target: File = when {
                args.isEmpty() -> env.homeDirectory
                args[0] == "-" -> env.previousDirectory
                else -> env.resolvePath(args[0])
            }
            if (env.currentDirectory == target) return CommandResult.empty()
            val error = env.changeDirectory(target)
            return if (error != null) {
                CommandResult.error("cd: $error")
            } else {
                CommandResult.empty()
            }
        }
    }

    class Ls : BuiltInCommand {
        override val name = "ls"
        override val description = "List files and directories"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val target = if (args.isEmpty()) env.currentDirectory else env.resolvePath(args[0])
            if (!target.exists()) {
                return CommandResult.error("ls: no such file or directory: ${args.getOrElse(0) { target.name }}")
            }
            if (!target.isDirectory) {
                return CommandResult.output(target.name)
            }
            val entries = target.listFiles()?.sortedBy { it.name.lowercase() } ?: emptyList()
            if (entries.isEmpty()) {
                return CommandResult.empty()
            }
            val formatted = entries.map { f -> if (f.isDirectory) "${f.name}/" else f.name }
            return CommandResult.output(formatted.joinToString("  "))
        }
    }

    class Cat : BuiltInCommand {
        override val name = "cat"
        override val description = "Display the contents of a text file"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return if (stdin.isNotEmpty()) CommandResult.lines(stdin) else CommandResult.error("cat: missing file operand")
            }
            val target = env.resolvePath(args[0])
            if (!target.exists()) {
                return CommandResult.error("cat: ${args[0]}: no such file")
            }
            if (target.isDirectory) {
                return CommandResult.error("cat: ${args[0]}: is a directory")
            }
            return try {
                val content = target.readText()
                CommandResult.lines(content.lines())
            } catch (e: Exception) {
                CommandResult.error("cat: ${args[0]}: ${e.message ?: "unable to read file"}")
            }
        }
    }

    class Mkdir : BuiltInCommand {
        override val name = "mkdir"
        override val description = "Create a new directory"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("mkdir: missing operand")
            }
            val target = env.resolvePath(args[0])
            if (target.exists()) {
                return CommandResult.error("mkdir: cannot create directory '${args[0]}': already exists")
            }
            return if (target.mkdirs()) {
                CommandResult.empty()
            } else {
                CommandResult.error("mkdir: cannot create directory '${args[0]}'")
            }
        }
    }

    class Rm : BuiltInCommand {
        override val name = "rm"
        override val description = "Remove a file or directory (-r for recursive)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val recursive = args.contains("-r") || args.contains("-rf") || args.contains("-R")
            val targets = args.filterNot { it.startsWith("-") }
            if (targets.isEmpty()) {
                return CommandResult.error("rm: missing operand")
            }
            val outputs = mutableListOf<TerminalLine>()
            for (raw in targets) {
                val target = env.resolvePath(raw)
                if (!target.exists()) {
                    outputs.add(TerminalLine(TerminalLineType.ERROR, "rm: $raw: no such file or directory"))
                    continue
                }
                if (!env.isWithinSandbox(target)) {
                    outputs.add(TerminalLine(TerminalLineType.ERROR, "rm: $raw: permission denied"))
                    continue
                }
                if (target.isDirectory && target.listFiles()?.isNotEmpty() == true && !recursive) {
                    outputs.add(TerminalLine(TerminalLineType.ERROR, "rm: $raw: is a directory (use -r)"))
                    continue
                }
                val deleted = if (recursive) target.deleteRecursively() else target.delete()
                if (!deleted) {
                    outputs.add(TerminalLine(TerminalLineType.ERROR, "rm: $raw: could not remove"))
                }
            }
            return CommandResult(output = outputs)
        }
    }

    class DateCmd : BuiltInCommand {
        override val name = "date"
        override val description = "Print the current date and time"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val fmt = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.getDefault())
            return CommandResult.output(fmt.format(Date()))
        }
    }

    class WhoAmI : BuiltInCommand {
        override val name = "whoami"
        override val description = "Print the current terminal user"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult.output(env.environmentVariables["USER"] ?: "iQOOUser")
        }
    }

    class Clear : BuiltInCommand {
        override val name = "clear"
        override val description = "Clear the terminal screen"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult(didClear = true)
        }
    }

    class Exit : BuiltInCommand {
        override val name = "exit"
        override val description = "Reset the current terminal session"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult(didExit = true)
        }
    }

    class Help : BuiltInCommand {
        override val name = "help"
        override val description = "Show this list of available commands"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val lines = mutableListOf("Available commands:")
            all.sortedBy { it.name }.forEach { cmd ->
                lines.add("  ${cmd.name.padEnd(10)} ${cmd.description}")
            }
            lines.add("")
            lines.add("Anything else is attempted as a system command (e.g. /system/bin/ls).")
            return CommandResult.lines(lines)
        }
    }

    class Cp : BuiltInCommand {
        override val name = "cp"
        override val description = "Copy file or directory (-r for directories)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val recursive = args.contains("-r") || args.contains("-R")
            val targets = args.filterNot { it.startsWith("-") }
            if (targets.size < 2) {
                return CommandResult.error("cp: missing destination file operand")
            }
            val src = env.resolvePath(targets[0])
            if (!src.exists()) {
                return CommandResult.error("cp: ${targets[0]}: no such file or directory")
            }
            if (!env.isWithinSandbox(src)) {
                return CommandResult.error("cp: ${targets[0]}: permission denied")
            }
            val dst = env.resolvePath(targets[1])
            if (dst.exists()) {
                return CommandResult.error("cp: ${targets[1]}: already exists")
            }
            if (!env.isWithinSandbox(dst)) {
                return CommandResult.error("cp: ${targets[1]}: permission denied")
            }
            return try {
                if (src.isDirectory) {
                    if (!recursive) {
                        return CommandResult.error("cp: -r not specified; omitting directory '${targets[0]}'")
                    }
                    src.copyRecursively(dst)
                } else {
                    src.copyTo(dst)
                }
                CommandResult.empty()
            } catch (e: Exception) {
                CommandResult.error("cp: ${e.message ?: "copy failed"}")
            }
        }
    }

    class Mv : BuiltInCommand {
        override val name = "mv"
        override val description = "Move/rename file or directory"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val targets = args.filterNot { it.startsWith("-") }
            if (targets.size < 2) {
                return CommandResult.error("mv: missing destination file operand")
            }
            val src = env.resolvePath(targets[0])
            if (!src.exists()) {
                return CommandResult.error("mv: ${targets[0]}: no such file or directory")
            }
            if (!env.isWithinSandbox(src)) {
                return CommandResult.error("mv: ${targets[0]}: permission denied")
            }
            val dst = env.resolvePath(targets[1])
            if (dst.exists()) {
                return CommandResult.error("mv: ${targets[1]}: already exists")
            }
            if (!env.isWithinSandbox(dst)) {
                return CommandResult.error("mv: ${targets[1]}: permission denied")
            }
            return try {
                src.renameTo(dst)
                if (src.exists()) {
                    CommandResult.error("mv: failed to move '${targets[0]}'")
                } else {
                    CommandResult.empty()
                }
            } catch (e: Exception) {
                CommandResult.error("mv: ${e.message ?: "move failed"}")
            }
        }
    }

    class Touch : BuiltInCommand {
        override val name = "touch"
        override val description = "Create empty file or update mtime"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("touch: missing file operand")
            }
            val target = env.resolvePath(args[0])
            if (!env.isWithinSandbox(target)) {
                return CommandResult.error("touch: ${args[0]}: permission denied")
            }
            return try {
                if (!target.exists()) {
                    target.createNewFile()
                } else {
                    target.setLastModified(System.currentTimeMillis())
                }
                CommandResult.empty()
            } catch (e: Exception) {
                CommandResult.error("touch: ${e.message ?: "failed"}")
            }
        }
    }

    class Rmdir : BuiltInCommand {
        override val name = "rmdir"
        override val description = "Remove empty directory"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("rmdir: missing operand")
            }
            val target = env.resolvePath(args[0])
            if (!target.exists()) {
                return CommandResult.error("rmdir: ${args[0]}: no such file or directory")
            }
            if (!target.isDirectory) {
                return CommandResult.error("rmdir: ${args[0]}: not a directory")
            }
            if (!env.isWithinSandbox(target)) {
                return CommandResult.error("rmdir: ${args[0]}: permission denied")
            }
            val contents = target.listFiles()
            if (contents != null && contents.isNotEmpty()) {
                return CommandResult.error("rmdir: ${args[0]}: Directory not empty")
            }
            return if (target.delete()) {
                CommandResult.empty()
            } else {
                CommandResult.error("rmdir: ${args[0]}: failed to remove")
            }
        }
    }

    class Tree : BuiltInCommand {
        override val name = "tree"
        override val description = "Recursive directory listing"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val target = if (args.isEmpty()) env.currentDirectory else env.resolvePath(args[0])
            if (!target.exists()) {
                return CommandResult.error("tree: ${args.getOrElse(0) { target.name }}: no such file or directory")
            }
            if (!target.isDirectory) {
                return CommandResult.output(target.name)
            }
            val lines = mutableListOf<String>()
            fun walk(dir: File, prefix: String, depth: Int) {
                if (depth > 5 || lines.size >= 1000) return
                val files = dir.listFiles()?.sortedBy { it.name.lowercase() } ?: return
                files.forEachIndexed { index, file ->
                    if (lines.size >= 1000) return
                    val connector = if (index == files.lastIndex) "└── " else "├── "
                    lines.add(prefix + connector + file.name)
                    if (file.isDirectory) {
                        val nextPrefix = prefix + if (index == files.lastIndex) "    " else "│   "
                        walk(file, nextPrefix, depth + 1)
                    }
                }
            }
            lines.add(target.name)
            walk(target, "", 0)
            return CommandResult.output(lines.joinToString("\n"))
        }
    }

    class Grep : BuiltInCommand {
        override val name = "grep"
        override val description = "Search file(s) for pattern (-i case-insensitive, -n line numbers)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val caseInsensitive = args.contains("-i")
            val showLineNumbers = args.contains("-n")
            val patternIdx = args.indexOfFirst { !it.startsWith("-") }
            if (patternIdx == -1) {
                return CommandResult.error("grep: missing pattern")
            }
            val pattern = args[patternIdx]
            val files = args.drop(patternIdx + 1)
            if (files.isEmpty() && stdin.isEmpty()) {
                return CommandResult.error("grep: missing file operand")
            }
            val regex = if (caseInsensitive) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
            val outputs = mutableListOf<String>()
            val sources = if (files.isNotEmpty()) files.map { env.resolvePath(it) } else emptyList()
            if (sources.isNotEmpty()) {
                for (src in sources) {
                    if (!src.exists()) {
                        outputs.add("grep: ${src.name}: no such file")
                        continue
                    }
                    if (!env.isWithinSandbox(src)) {
                        outputs.add("grep: ${src.name}: permission denied")
                        continue
                    }
                    val lines = src.readLines()
                    lines.forEachIndexed { idx, line ->
                        if (regex.containsMatchIn(line)) {
                            val prefix = if (showLineNumbers) "${idx + 1}:" else ""
                            outputs.add("$prefix$line")
                        }
                    }
                }
            } else {
                stdin.forEachIndexed { idx, line ->
                    if (regex.containsMatchIn(line)) {
                        val prefix = if (showLineNumbers) "${idx + 1}:" else ""
                        outputs.add("$prefix$line")
                    }
                }
            }
            return if (outputs.isEmpty()) CommandResult.empty() else CommandResult.output(outputs.joinToString("\n"))
        }
    }

    class Find : BuiltInCommand {
        override val name = "find"
        override val description = "Recursive file search (-name <pattern>)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val start = if (args.isEmpty()) env.currentDirectory else env.resolvePath(args[0])
            val nameIdx = args.indexOf("-name")
            val pattern = if (nameIdx != -1 && nameIdx + 1 < args.size) args[nameIdx + 1] else "*"
            if (!start.exists() || !start.isDirectory) {
                return CommandResult.error("find: ${args.getOrElse(0) { start.name }}: no such directory")
            }
            if (!env.isWithinSandbox(start)) {
                return CommandResult.error("find: ${start.name}: permission denied")
            }
            val regex = Regex(pattern.replace("?", ".").replace("*", ".*"))
            val results = mutableListOf<String>()
            fun walk(dir: File, depth: Int) {
                if (depth > 10 || results.size >= 1000) return
                dir.listFiles()?.forEach { file ->
                    if (results.size >= 1000) return
                    if (regex.matches(file.name)) {
                        results.add(file.absolutePath)
                    }
                    if (file.isDirectory) {
                        walk(file, depth + 1)
                    }
                }
            }
            walk(start, 0)
            return if (results.isEmpty()) CommandResult.empty() else CommandResult.output(results.joinToString("\n"))
        }
    }

    class Head : BuiltInCommand {
        override val name = "head"
        override val description = "Print first N lines (default 10)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val count = args.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull() ?: 10
            val source = args.lastOrNull { !it.startsWith("-") && it.toIntOrNull() == null }
            val lines = if (source != null) {
                val target = env.resolvePath(source)
                if (!target.exists()) return CommandResult.error("head: $source: no such file")
                if (!env.isWithinSandbox(target)) return CommandResult.error("head: $source: permission denied")
                target.readLines()
            } else if (stdin.isNotEmpty()) {
                stdin
            } else {
                return CommandResult.error("head: missing file operand")
            }
            return CommandResult.output(lines.take(count).joinToString("\n"))
        }
    }

    class Tail : BuiltInCommand {
        override val name = "tail"
        override val description = "Print last N lines (default 10)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val count = args.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull() ?: 10
            val source = args.lastOrNull { !it.startsWith("-") && it.toIntOrNull() == null }
            val lines = if (source != null) {
                val target = env.resolvePath(source)
                if (!target.exists()) return CommandResult.error("tail: $source: no such file")
                if (!env.isWithinSandbox(target)) return CommandResult.error("tail: $source: permission denied")
                target.readLines()
            } else if (stdin.isNotEmpty()) {
                stdin
            } else {
                return CommandResult.error("tail: missing file operand")
            }
            return CommandResult.output(lines.takeLast(count).joinToString("\n"))
        }
    }

    class Wc : BuiltInCommand {
        override val name = "wc"
        override val description = "Count lines, words, chars (-l, -w, -c)"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val countLines = args.contains("-l") || args.isEmpty()
            val countWords = args.contains("-w") || args.isEmpty()
            val countChars = args.contains("-c") || args.isEmpty()
            val source = args.lastOrNull { !it.startsWith("-") }
            val lines = if (source != null) {
                val target = env.resolvePath(source)
                if (!target.exists()) return CommandResult.error("wc: $source: no such file")
                if (!env.isWithinSandbox(target)) return CommandResult.error("wc: $source: permission denied")
                target.readLines()
            } else if (stdin.isNotEmpty()) {
                stdin
            } else {
                return CommandResult.error("wc: missing file operand")
            }
            val counts = mutableListOf<String>()
            if (countLines) counts.add(lines.size.toString())
            if (countWords) counts.add(lines.joinToString(" ").split("\\s+".toRegex()).size.toString())
            if (countChars) counts.add(lines.joinToString("\n").length.toString())
            val suffix = if (source != null) " $source" else ""
            return CommandResult.output(counts.joinToString(" ") + suffix)
        }
    }

    class Sort : BuiltInCommand {
        override val name = "sort"
        override val description = "Sort lines of file or stdin"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            val source = args.lastOrNull { !it.startsWith("-") }
            val lines = if (source != null) {
                val target = env.resolvePath(source)
                if (!target.exists()) return CommandResult.error("sort: $source: no such file")
                if (!env.isWithinSandbox(target)) return CommandResult.error("sort: $source: permission denied")
                target.readLines()
            } else if (stdin.isNotEmpty()) {
                stdin
            } else {
                return CommandResult.error("sort: missing file operand")
            }
            return CommandResult.output(lines.sorted().joinToString("\n"))
        }
    }

    class Diff : BuiltInCommand {
        override val name = "diff"
        override val description = "Line-by-line diff of two files"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.size < 2) {
                return CommandResult.error("diff: missing operand")
            }
            val f1 = env.resolvePath(args[0])
            val f2 = env.resolvePath(args[1])
            if (!f1.exists()) return CommandResult.error("diff: ${args[0]}: no such file")
            if (!f2.exists()) return CommandResult.error("diff: ${args[1]}: no such file")
            if (!env.isWithinSandbox(f1)) return CommandResult.error("diff: ${args[0]}: permission denied")
            if (!env.isWithinSandbox(f2)) return CommandResult.error("diff: ${args[1]}: permission denied")
            val lines1 = f1.readLines()
            val lines2 = f2.readLines()
            val max = maxOf(lines1.size, lines2.size)
            val outputs = mutableListOf<String>()
            for (i in 0 until max) {
                val a = lines1.getOrNull(i) ?: ""
                val b = lines2.getOrNull(i) ?: ""
                if (a != b) {
                    outputs.add("< $a")
                    outputs.add("> $b")
                }
            }
            return if (outputs.isEmpty()) CommandResult.empty() else CommandResult.output(outputs.joinToString("\n"))
        }
    }

    class Which : BuiltInCommand {
        override val name = "which"
        override val description = "Search PATH for executable"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("which: missing argument")
            }
            val paths = env.path.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            val outputs = mutableListOf<String>()
            for (arg in args) {
                val found = paths.firstOrNull { dir ->
                    File(dir, arg).let { f -> f.exists() && f.canExecute() }
                }
                if (found != null) {
                    outputs.add(File(found, arg).absolutePath)
                } else {
                    outputs.add("which: $arg not found")
                }
            }
            return CommandResult.output(outputs.joinToString("\n"))
        }
    }

    class Env : BuiltInCommand {
        override val name = "env"
        override val description = "Print environment variables"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            return CommandResult.output(env.environmentVariables.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value}" })
        }
    }

    class Export : BuiltInCommand {
        override val name = "export"
        override val description = "Set environment variable"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("export: missing argument")
            }
            for (arg in args) {
                val idx = arg.indexOf("=")
                if (idx > 0) {
                    val key = arg.substring(0, idx)
                    val value = arg.substring(idx + 1)
                    env.environmentVariables[key] = value
                    if (key == "PATH") {
                        env.environmentVariables["PATH"] = value
                    }
                }
            }
            return CommandResult.empty()
        }
    }

    class Unset : BuiltInCommand {
        override val name = "unset"
        override val description = "Remove environment variable"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("unset: missing argument")
            }
            for (arg in args) {
                env.environmentVariables.remove(arg)
            }
            return CommandResult.empty()
        }
    }

    class History : BuiltInCommand {
        override val name = "history"
        override val description = "Print command history"
        override fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String>): CommandResult {
            if (args.isNotEmpty() && args[0] == "-c") {
                env.clearHistory()
                return CommandResult.empty()
            }
            val lines = env.history.mapIndexed { idx, cmd -> "${idx + 1}  $cmd" }
            return if (lines.isEmpty()) CommandResult.empty() else CommandResult.output(lines.joinToString("\n"))
        }
    }
}
