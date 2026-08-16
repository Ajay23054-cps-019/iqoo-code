package com.example.iqoo_code.terminal

/**
 * A parsed command: the command name plus its arguments.
 */
data class ParsedCommand(
    val name: String,
    val args: List<String>
) {
    val isBlank: Boolean get() = name.isBlank()
}

/**
 * A small, dependency-free command line tokenizer.
 *
 * Supports:
 *  - collapsing multiple spaces
 *  - double-quoted strings, e.g. echo "Hello iQOO"
 *  - single-quoted strings, e.g. echo 'Hello iQOO'
 *  - basic escaping of quotes within a quoted string using backslash
 *
 * This intentionally does NOT implement a full Bash-like grammar
 * (no pipes, redirection, variable expansion, globbing, etc.) - that is
 * explicitly out of scope for v0.1.
 */
object CommandParser {

    fun parse(input: String): ParsedCommand {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return ParsedCommand("", emptyList())
        return ParsedCommand(tokens.first(), tokens.drop(1))
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuotes = false
        var inDoubleQuotes = false
        var i = 0
        var tokenStarted = false

        fun flush() {
            if (tokenStarted) {
                tokens.add(current.toString())
                current.clear()
                tokenStarted = false
            }
        }

        while (i < input.length) {
            val c = input[i]
            when {
                inSingleQuotes -> {
                    if (c == '\'') {
                        inSingleQuotes = false
                    } else {
                        current.append(c)
                    }
                }
                inDoubleQuotes -> {
                    if (c == '\\' && i + 1 < input.length && input[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else if (c == '"') {
                        inDoubleQuotes = false
                    } else {
                        current.append(c)
                    }
                }
                c == '\'' -> {
                    inSingleQuotes = true
                    tokenStarted = true
                }
                c == '"' -> {
                    inDoubleQuotes = true
                    tokenStarted = true
                }
                c.isWhitespace() -> {
                    flush()
                }
                else -> {
                    tokenStarted = true
                    current.append(c)
                }
            }
            i++
        }
        flush()
        return tokens
    }
}
