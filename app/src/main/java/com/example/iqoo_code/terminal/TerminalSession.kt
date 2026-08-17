package com.example.iqoo_code.terminal

import com.example.iqoo_code.fs.Workspace
import java.io.File

data class TerminalSession(
    val id: String,
    val name: String,
    val workspace: Workspace,
    val environment: TerminalEnvironment = TerminalEnvironment(workspace),
    val engine: TerminalEngine = TerminalEngine(environment)
) {
    val lines = mutableListOf<TerminalLine>()
}
