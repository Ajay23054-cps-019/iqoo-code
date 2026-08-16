package com.example.iqoo_code.terminal

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registry of all built-in terminal commands. TerminalEngine consults this
 * before falling back to [ExternalCommandExecutor].
 */
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
        Help()
    )

    val byName: Map<String, BuiltInCommand> = all.associateBy { it.name }

    // ---------------------------------------------------------------------

    class Pwd : BuiltInCommand {
        override val name = "pwd"
        override val description = "Print the current working directory"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            return CommandResult.output(env.currentDirectory.absolutePath)
        }
    }

    class Echo : BuiltInCommand {
        override val name = "echo"
        override val description = "Print the supplied arguments"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            return CommandResult.output(args.joinToString(" "))
        }
    }

    class Cd : BuiltInCommand {
        override val name = "cd"
        override val description = "Change the current working directory"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            val target: File = when {
                args.isEmpty() -> env.homeDirectory
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
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
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
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            if (args.isEmpty()) {
                return CommandResult.error("cat: missing file operand")
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
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
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
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
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
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            val fmt = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.getDefault())
            return CommandResult.output(fmt.format(Date()))
        }
    }

    class WhoAmI : BuiltInCommand {
        override val name = "whoami"
        override val description = "Print the current terminal user"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            return CommandResult.output(env.environmentVariables["USER"] ?: "iQOOUser")
        }
    }

    class Clear : BuiltInCommand {
        override val name = "clear"
        override val description = "Clear the terminal screen"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            return CommandResult(didClear = true)
        }
    }

    class Exit : BuiltInCommand {
        override val name = "exit"
        override val description = "Reset the current terminal session"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            return CommandResult(didExit = true)
        }
    }

    class Help : BuiltInCommand {
        override val name = "help"
        override val description = "Show this list of available commands"
        override fun execute(args: List<String>, env: TerminalEnvironment): CommandResult {
            val lines = mutableListOf("Available commands:")
            all.sortedBy { it.name }.forEach { cmd ->
                lines.add("  ${cmd.name.padEnd(10)} ${cmd.description}")
            }
            lines.add("")
            lines.add("Anything else is attempted as a system command (e.g. /system/bin/ls).")
            return CommandResult.lines(lines)
        }
    }
}
