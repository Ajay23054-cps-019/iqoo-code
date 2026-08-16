package com.example.iqoo_code.terminal

/**
 * Result of executing any command (built-in or external).
 */
data class CommandResult(
    val output: List<TerminalLine> = emptyList(),
    /** True if the "exit" command was invoked and the session should reset. */
    val didExit: Boolean = false,
    /** True if "clear" was invoked and prior output should be wiped. */
    val didClear: Boolean = false
) {
    companion object {
        fun output(text: String) = CommandResult(output = listOf(TerminalLine(TerminalLineType.OUTPUT, text)))
        fun error(text: String) = CommandResult(output = listOf(TerminalLine(TerminalLineType.ERROR, text)))
        fun lines(lines: List<String>, type: TerminalLineType = TerminalLineType.OUTPUT) =
            CommandResult(output = lines.map { TerminalLine(type, it) })
        fun empty() = CommandResult()
    }
}

/**
 * Contract implemented by every built-in terminal command.
 *
 * Built-ins operate purely on [TerminalEnvironment] and their arguments -
 * they never touch Compose or Android UI state directly.
 */
interface BuiltInCommand {
    /** The command's invocation name, e.g. "pwd". */
    val name: String

    /** Short one-line description shown by `help`. */
    val description: String

    fun execute(args: List<String>, env: TerminalEnvironment): CommandResult
}
