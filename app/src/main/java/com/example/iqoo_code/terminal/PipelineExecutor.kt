package com.example.iqoo_code.terminal

import java.io.File

object PipelineExecutor {

    suspend fun execute(pipeline: Pipeline, env: TerminalEnvironment): CommandResult {
        var stdinLines: List<String> = emptyList()

        val first = pipeline.commands.first()
        if (first.stdin != null && first.stdin.exists()) {
            try { stdinLines = first.stdin.readLines() } catch (_: Exception) { }
        }

        var currentStdout = mutableListOf<String>()

        for (i in pipeline.commands.indices) {
            val cmd = pipeline.commands[i]
            val isLast = i == pipeline.commands.lastIndex

            val builtIn = BuiltInCommands.byName[cmd.name]
            val result = if (builtIn != null) {
                try {
                    builtIn.execute(cmd.args, env, stdinLines)
                } catch (e: Exception) {
                    CommandResult.error("${cmd.name}: ${e.message ?: "unexpected error"}")
                }
            } else {
                val rawArgs = cmd.args.joinToString(" ")
                val extResult = ExternalCommandExecutor.execute(rawArgs, env, stdinLines)
                val lines = mutableListOf<TerminalLine>()
                lines.addAll(extResult.stdout.map { TerminalLine(TerminalLineType.OUTPUT, it) })
                lines.addAll(extResult.stderr.map { TerminalLine(TerminalLineType.ERROR, it) })
                if (extResult.exitCode != 0 && lines.isEmpty()) {
                    lines.add(TerminalLine(TerminalLineType.ERROR, "${cmd.name}: command not found"))
                }
                CommandResult(output = lines)
            }

            if (isLast) {
                if (pipeline.stdoutRedirect != null) {
                    val target = pipeline.stdoutRedirect
                    if (!env.isWithinSandbox(target)) {
                        return CommandResult.error("pipeline: permission denied: ${target.absolutePath}")
                    }
                    return try {
                        val lines = result.output.map { it.text }
                        if (pipeline.stdoutAppend) {
                            target.appendLines(lines)
                        } else {
                            target.writeLines(lines)
                        }
                        CommandResult.empty()
                    } catch (e: Exception) {
                        CommandResult.error("pipeline: ${e.message ?: "redirect failed"}")
                    }
                }
                return result
            }

            stdinLines = result.output.map { it.text }
        }

        return CommandResult.empty()
    }

    private fun File.writeLines(lines: List<String>) {
        bufferedWriter().use { writer ->
            lines.forEach { writer.write(it); writer.newLine() }
        }
    }

    private fun File.appendLines(lines: List<String>) {
        bufferedWriter().use { writer ->
            lines.forEach { writer.write(it); writer.newLine() }
        }
    }
}
