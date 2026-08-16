package com.example.iqoo_code.terminal

/**
 * Represents a single line rendered in the terminal output.
 *
 * Kept as a plain data class with no Android/Compose dependencies so it can be
 * reused if the backend is later replaced by a native (C++) runtime.
 */
enum class TerminalLineType {
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM
}

data class TerminalLine(
    val type: TerminalLineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
