package com.example.iqoo_code.terminal

import android.content.Context
import com.example.iqoo_code.fs.Workspace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val input: String = "",
    val promptPath: String = "~",
    val isRunning: Boolean = false,
    val sessions: List<String> = emptyList(),
    val activeSessionIndex: Int = 0,
    val settings: TerminalSettings = TerminalSettings()
)

class TerminalViewModel(
    private val context: Context,
    private val homeDirectory: File
) : ViewModel() {

    private val workspace = Workspace(homeDirectory)
    private val sessionManager = TerminalSessionManager(workspace)
    private val settingsManager = TerminalSettingsManager(context)

    private val _uiState = MutableStateFlow(
        TerminalUiState(
            sessions = sessionManager.sessions.map { it.name },
            activeSessionIndex = sessionManager.activeIndex
        )
    )
    val uiState: StateFlow<TerminalUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionManager.activeSession().lines.addAll(
                listOf(
                    TerminalLine(TerminalLineType.SYSTEM, "iQOO Code Terminal v0.2"),
                    TerminalLine(TerminalLineType.SYSTEM, "Type 'help' for a list of commands.")
                )
            )
            refreshLines()
            settingsManager.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun refreshLines() {
        _uiState.update { current ->
            val session = sessionManager.activeSession()
            current.copy(
                lines = session.lines.toList(),
                promptPath = session.environment.displayPath(),
                sessions = sessionManager.sessions.map { it.name },
                activeSessionIndex = sessionManager.activeIndex
            )
        }
    }

    private fun formatPrompt(path: String, command: String): String =
        "$path $ $command"

    private var browseIndex: Int = -1

    fun onInputChange(newValue: String) {
        val filtered = newValue.replace("\n", "").replace("\r", "")
        if (filtered != newValue) {
            submitCommand(filtered)
            return
        }
        _uiState.update { it.copy(input = newValue) }
    }

    fun onSubmit() {
        val command = _uiState.value.input
        if (_uiState.value.isRunning) return
        submitCommand(command)
    }

    private fun submitCommand(command: String) {
        val session = sessionManager.activeSession()
        val promptLine = TerminalLine(
            type = TerminalLineType.INPUT,
            text = formatPrompt(session.environment.displayPath(), command)
        )

        session.lines.add(promptLine)
        _uiState.update {
            it.copy(
                lines = it.lines + promptLine,
                input = "",
                isRunning = true
            )
        }
        browseIndex = -1

        viewModelScope.launch {
            val result = session.engine.run(command)
            session.lines.addAll(result.output)
            refreshLines()
            _uiState.update { current ->
                current.copy(isRunning = false)
            }

            if (result.didExit) {
                resetSession()
            }
        }
    }

    private fun resetSession() {
        val session = sessionManager.activeSession()
        session.environment.let {
            it.changeDirectory(it.homeDirectory)
        }
        session.lines.clear()
        session.lines.add(TerminalLine(TerminalLineType.SYSTEM, "Session reset."))
        session.lines.add(TerminalLine(TerminalLineType.SYSTEM, "Type 'help' for a list of commands."))
        refreshLines()
    }

    fun browsePrevious() {
        val session = sessionManager.activeSession()
        val history = session.environment.history
        if (history.isEmpty()) return
        if (browseIndex == -1) browseIndex = history.size
        if (browseIndex > 0) browseIndex--
        _uiState.update { it.copy(input = history.getOrElse(browseIndex) { "" }) }
    }

    fun browseNext() {
        val session = sessionManager.activeSession()
        val history = session.environment.history
        if (history.isEmpty() || browseIndex == -1) return
        if (browseIndex < history.size - 1) {
            browseIndex++
            _uiState.update { it.copy(input = history.getOrElse(browseIndex) { "" }) }
        } else {
            browseIndex = -1
            _uiState.update { it.copy(input = "") }
        }
    }

    fun switchSession(index: Int): Boolean {
        val switched = sessionManager.switchTo(index)
        if (switched) {
            browseIndex = -1
            refreshLines()
        }
        return switched
    }

    fun newSession(): TerminalSession {
        val session = sessionManager.addSession()
        browseIndex = -1
        refreshLines()
        return session
    }

    fun closeSession(index: Int): Boolean {
        val removed = sessionManager.removeSession(index)
        if (removed) {
            browseIndex = -1
            refreshLines()
        }
        return removed
    }

    fun complete(input: String): List<String> {
        val session = sessionManager.activeSession()
        return CompletionEngine.complete(input, session.environment.currentDirectory, session.environment.path)
    }

    class Factory(private val context: Context, private val homeDirectory: File) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TerminalViewModel(context, homeDirectory) as T
        }
    }
}
