package com.example.iqoo_code.terminal

/**
 * Central dispatcher for terminal command execution.
 *
 * Flow: raw input string -> [CommandParser] -> built-in lookup
 * ([BuiltInCommands]) -> else [ExternalCommandExecutor].
 *
 * This class owns no Compose/Android state. [TerminalViewModel] is the only
 * caller, keeping UI concerns fully separated from command execution and
 * making it straightforward to later replace the internals with a native
 * (C++/JNI) runtime while keeping this same public surface.
 */
class TerminalEngine(private val environment: TerminalEnvironment) {

    val env: TerminalEnvironment get() = environment

    /**
     * Executes [rawInput] and returns the resulting [CommandResult].
     * Suspends because external command execution requires IO.
     */
    suspend fun run(rawInput: String): CommandResult {
        val parsed = CommandParser.parse(rawInput)
        if (parsed.isBlank) {
            return CommandResult.empty()
        }

        environment.addToHistory(rawInput)

        val builtIn = BuiltInCommands.byName[parsed.name]
        if (builtIn != null) {
            return try {
                builtIn.execute(parsed.args, environment)
            } catch (e: Exception) {
                CommandResult.error("${parsed.name}: ${e.message ?: "unexpected error"}")
            }
        }

        return runExternal(rawInput, parsed.name)
    }

    private suspend fun runExternal(rawInput: String, commandName: String): CommandResult {
        val result = ExternalCommandExecutor.execute(rawInput, environment)

        // A negative exit code combined with no stderr output typically means
        // the shell itself couldn't find/run the command.
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
