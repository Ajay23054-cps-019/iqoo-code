package com.example.iqoo_code.terminal

import java.io.File

data class CommandResult(
    val output: List<TerminalLine> = emptyList(),
    val didExit: Boolean = false,
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

interface BuiltInCommand {
    val name: String
    val description: String
    fun execute(args: List<String>, env: TerminalEnvironment, stdin: List<String> = emptyList()): CommandResult
}
