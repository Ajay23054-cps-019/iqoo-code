package com.example.iqoo_code.terminal

import com.example.iqoo_code.fs.Workspace
import java.util.UUID

class TerminalSessionManager(private val workspace: Workspace) {

    private val _sessions = mutableListOf<TerminalSession>()
    val sessions: List<TerminalSession> get() = _sessions

    var activeIndex: Int = 0
        private set

    init {
        addSession("Terminal 1")
    }

    fun activeSession(): TerminalSession {
        return _sessions[activeIndex]
    }

    fun addSession(name: String? = null): TerminalSession {
        val session = TerminalSession(
            id = UUID.randomUUID().toString(),
            name = name ?: "Terminal ${_sessions.size + 1}",
            workspace = workspace
        )
        session.lines.add(TerminalLine(TerminalLineType.SYSTEM, "iQOO Code Terminal v0.2"))
        session.lines.add(TerminalLine(TerminalLineType.SYSTEM, "Type 'help' for a list of commands."))
        _sessions.add(session)
        activeIndex = _sessions.size - 1
        return session
    }

    fun removeSession(index: Int): Boolean {
        if (_sessions.size <= 1) return false
        _sessions.removeAt(index)
        if (activeIndex >= _sessions.size) {
            activeIndex = _sessions.size - 1
        }
        return true
    }

    fun switchTo(index: Int): Boolean {
        if (index in _sessions.indices) {
            activeIndex = index
            return true
        }
        return false
    }
}
