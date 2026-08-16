package com.example.iqoo_code.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File


/**
 * UI-facing state for the terminal screen. Immutable snapshot consumed by
 * Compose.
 */
data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val input: String = "",
    val promptPath: String = "~",
    val isRunning: Boolean = false
)

/**
 * Bridges [TerminalScreen] (Compose UI) and [TerminalEngine] (business
 * logic). Owns no filesystem/process logic itself - it only forwards user
 * intents to the engine and republishes results as UI state.
 */
class TerminalViewModel(homeDirectory: File) : ViewModel() {

    private val environment = TerminalEnvironment(homeDirectory)
    private val engine = TerminalEngine(environment)

    private val _uiState = MutableStateFlow(
        TerminalUiState(
            lines = listOf(
                TerminalLine(TerminalLineType.SYSTEM, "iQOO Code Terminal v0.1"),
                TerminalLine(TerminalLineType.SYSTEM, "Type 'help' for a list of commands.")
            ),
            promptPath = environment.displayPath()
        )
    )
    val uiState: StateFlow<TerminalUiState> = _uiState

    /** Local browsing index separate from environment history cursor, for UI "prev/next" buttons. */
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
        val promptLine = TerminalLine(
            type = TerminalLineType.INPUT,
            text = "${environment.displayPath()} $ $command"
        )

        _uiState.update {
            it.copy(
                lines = it.lines + promptLine,
                input = "",
                isRunning = true
            )
        }
        browseIndex = -1

        viewModelScope.launch {
            val result = engine.run(command)

            _uiState.update { current ->
                val baseLines = if (result.didClear) emptyList() else current.lines
                current.copy(
                    lines = baseLines + result.output,
                    promptPath = environment.displayPath(),
                    isRunning = false
                )
            }

            if (result.didExit) {
                resetSession()
            }
        }
    }

    private fun resetSession() {
        environment.let {
            // Return to home directory; keep environment vars, clear visible history.
            it.changeDirectory(it.homeDirectory)
        }
        _uiState.update {
            it.copy(
                lines = listOf(
                    TerminalLine(TerminalLineType.SYSTEM, "Session reset."),
                    TerminalLine(TerminalLineType.SYSTEM, "Type 'help' for a list of commands.")
                ),
                input = "",
                promptPath = environment.displayPath()
            )
        }
    }

    /** Browses to the previous command in history (e.g. bound to an "up" UI affordance). */
    fun browsePrevious() {
        val history = environment.history
        if (history.isEmpty()) return
        if (browseIndex == -1) browseIndex = history.size
        if (browseIndex > 0) browseIndex--
        _uiState.update { it.copy(input = history.getOrElse(browseIndex) { "" }) }
    }

    /** Browses to the next command in history (e.g. bound to a "down" UI affordance). */
    fun browseNext() {
        val history = environment.history
        if (history.isEmpty() || browseIndex == -1) return
        if (browseIndex < history.size - 1) {
            browseIndex++
            _uiState.update { it.copy(input = history.getOrElse(browseIndex) { "" }) }
        } else {
            browseIndex = -1
            _uiState.update { it.copy(input = "") }
        }
    }

    class Factory(private val homeDirectory: File) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TerminalViewModel(homeDirectory) as T
        }
    }
}

