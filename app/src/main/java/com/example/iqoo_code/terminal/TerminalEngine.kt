package com.example.iqoo_code.terminal

import java.io.File

class TerminalEngine(private val environment: TerminalEnvironment) {

    val env: TerminalEnvironment get() = environment

    suspend fun run(rawInput: String): CommandResult {
        val parsed = CommandParser.parse(rawInput)
        if (parsed.isBlank) {
            return CommandResult.empty()
        }

        environment.addToHistory(rawInput)

        return when (parsed) {
            is SingleCommand -> executeSingle(parsed)
            is Pipeline -> PipelineExecutor.execute(parsed, environment)
        }
    }

    private suspend fun executeSingle(command: SingleCommand): CommandResult {
        val stdinFile = command.stdin
        val stdinLines = if (stdinFile != null && stdinFile.exists()) {
            try { stdinFile.readLines() } catch (_: Exception) { emptyList() }
        } else emptyList()

        val builtIn = BuiltInCommands.byName[command.name]
        if (builtIn != null) {
            return try {
                builtIn.execute(command.args, environment, stdinLines)
            } catch (e: Exception) {
                CommandResult.error("${command.name}: ${e.message ?: "unexpected error"}")
            }
        }

        return runExternal(command.name, command.args.joinToString(" "), stdinLines)
    }

    private suspend fun runExternal(commandName: String, rawArgs: String, stdinLines: List<String> = emptyList()): CommandResult {
        val result = ExternalCommandExecutor.execute(rawArgs, environment, stdinLines)

        if (result.exitCode != 0 && result.stdout.isEmpty() && result.stderr.isEmpty()) {
            return CommandResult.error("$commandName: command not found")
        }

        val lines = mutableListOf<TerminalLine>()
        lines.addAll(result.stdout.map { TerminalLine(TerminalLineType.OUTPUT, it) })
        lines.addAll(result.stderr.map { TerminalLine(TerminalLineType.ERROR, it) })

        if (lines.isEmpty() && result.exitCode != 0) {
            lines.add(TerminalLine(TerminalLineType.ERROR, "$commandName: command not found"))
        }

        return CommandResult(output = lines)
    }
}
