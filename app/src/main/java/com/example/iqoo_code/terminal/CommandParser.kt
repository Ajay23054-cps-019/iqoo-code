package com.example.iqoo_code.terminal

import java.io.File

object CommandParser {

    fun parse(input: String): ParseResult {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return SingleCommand("", emptyList())

        val segments = splitByPipe(tokens)
        if (segments.size == 1) {
            val seg = segments[0]
            val stdin = extractStdinRedirect(seg)
            val cleaned = seg.filterNot { it == "<" }
            val (name, args) = extractCommand(cleaned)
            return SingleCommand(name, args, stdin)
        }

        val commands = segments.map { seg ->
            val (name, args) = extractCommand(seg)
            SingleCommand(name, args)
        }

        val last = segments.last()
        val stdoutRedirect = extractRedirect(last, ">", ">>")
        val append = last.contains(">>")

        return Pipeline(commands = commands, stdoutRedirect = stdoutRedirect, stdoutAppend = append)
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
                    flush()
                    inSingleQuotes = true
                    tokenStarted = true
                }
                c == '"' -> {
                    flush()
                    inDoubleQuotes = true
                    tokenStarted = true
                }
                c == '>' || c == '<' || c == '|' -> {
                    if (c == '>' && i + 1 < input.length && input[i + 1] == '>') {
                        flush()
                        tokens.add(">>")
                        i++
                    } else {
                        flush()
                        tokens.add(c.toString())
                    }
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

    private fun splitByPipe(tokens: List<String>): List<List<String>> {
        val segments = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        for (token in tokens) {
            if (token == "|") {
                segments.add(current)
                current = mutableListOf()
            } else {
                current.add(token)
            }
        }
        segments.add(current)
        return segments
    }

    private fun extractStdinRedirect(tokens: List<String>): File? {
        val idx = tokens.indexOf("<")
        if (idx == -1 || idx + 1 >= tokens.size) return null
        return File(tokens[idx + 1])
    }

    private fun extractRedirect(tokens: List<String>, vararg ops: String): File? {
        for (op in ops) {
            val idx = tokens.indexOf(op)
            if (idx != -1 && idx + 1 < tokens.size) {
                return File(tokens[idx + 1])
            }
        }
        return null
    }

    private fun extractCommand(tokens: List<String>): Pair<String, List<String>> {
        if (tokens.isEmpty()) return Pair("", emptyList())
        val name = tokens.first()
        val args = tokens.drop(1).filterNot { it == ">" || it == ">>" || it == "<" }
        return Pair(name, args)
    }
}
