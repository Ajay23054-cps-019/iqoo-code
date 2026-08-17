package com.example.iqoo_code.terminal

import java.io.File

sealed interface ParseResult {
    val isBlank: Boolean
}

data class SingleCommand(
    val name: String,
    val args: List<String>,
    val stdin: File? = null
) : ParseResult {
    override val isBlank: Boolean get() = name.isBlank()
}

data class Pipeline(
    val commands: List<SingleCommand>,
    val stdoutRedirect: File? = null,
    val stdoutAppend: Boolean = false
) : ParseResult {
    override val isBlank: Boolean get() = commands.isEmpty() || commands.all { it.isBlank }
}
