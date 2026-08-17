package com.example.iqoo_code.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val TerminalBackground = Color(0xFF0B0F0C)
private val TerminalGreen = Color(0xFF4AF626)
private val TerminalWhite = Color(0xFFE6E6E6)
private val TerminalError = Color(0xFFFF6B6B)
private val TerminalSystem = Color(0xFF6C7A89)
private val TabActive = Color(0xFF2A3B32)
private val TabInactive = Color(0xFF1A2520)

@Composable
fun TerminalScreen(viewModel: TerminalViewModel) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showCompletion by remember { mutableStateOf(false) }
    var completionMatches by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.size - 1)
        }
    }

    LaunchedEffect(state.isRunning) {
        if (!state.isRunning) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SessionTabs(
            sessions = state.sessions,
            activeIndex = state.activeSessionIndex,
            onSwitch = viewModel::switchSession,
            onAdd = viewModel::newSession,
            onClose = viewModel::closeSession
        )

        val fontSize = state.settings.fontSize.sp
    val lineHeight = (fontSize.value * 1.3).sp

    LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.lines) { line ->
                TerminalLineItem(
                    line = line,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    onCopy = { text ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("terminal", text))
                    }
                )
            }
        }

        if (showCompletion && completionMatches.isNotEmpty()) {
            CompletionDropdown(
                matches = completionMatches,
                onSelect = {
                    viewModel.onInputChange(it)
                    showCompletion = false
                },
                onDismiss = { showCompletion = false }
            )
        }

        InputBar(
            input = state.input,
            promptPath = state.promptPath,
            isRunning = state.isRunning,
            focusRequester = focusRequester,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::onSubmit,
            onHistoryUp = viewModel::browsePrevious,
            onHistoryDown = viewModel::browseNext,
            onTab = { text ->
                val matches = viewModel.complete(text)
                completionMatches = matches
                showCompletion = matches.isNotEmpty()
            },
            onPaste = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item = cm.primaryClip?.getItemAt(0)
                val pasted = item?.text?.toString() ?: ""
                if (pasted.isNotBlank()) {
                    viewModel.onInputChange(state.input + pasted.replace("\n", " ").replace("\r", ""))
                }
            },
            fontSize = fontSize
        )
    }
}

@Composable
private fun SessionTabs(
    sessions: List<String>,
    activeIndex: Int,
    onSwitch: (Int) -> Unit,
    onAdd: () -> Unit,
    onClose: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TabInactive)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessions.forEachIndexed { index, name ->
            val isActive = index == activeIndex
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(32.dp)
                    .background(if (isActive) TabActive else Color.Transparent)
                    .clickable { onSwitch(index) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = if (isActive) TerminalWhite else TerminalSystem,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    if (sessions.size > 1) {
                        IconButton(
                            onClick = { onClose(index) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close tab",
                                tint = TerminalSystem,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New session",
                tint = TerminalSystem
            )
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine, fontSize: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit, onCopy: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(line.text) }
    ) {
        Text(
            text = line.text,
            color = colorForLine(line.type),
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            lineHeight = lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompletionDropdown(
    matches: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBackground)
        ) {
            matches.take(8).forEach { match ->
                DropdownMenuItem(
                    text = { Text(text = match, fontFamily = FontFamily.Monospace, color = TerminalWhite) },
                    onClick = { onSelect(match) }
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    promptPath: String,
    isRunning: Boolean,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onTab: (String) -> Unit,
    onPaste: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHistoryUp, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Previous command",
                tint = TerminalSystem
            )
        }
        IconButton(onClick = onHistoryDown, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Next command",
                tint = TerminalSystem
            )
        }

        Text(
            text = "$promptPath $",
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        )

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Tab) {
                        onTab(input)
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            textStyle = TextStyle(
                color = TerminalWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TerminalBackground,
                unfocusedContainerColor = TerminalBackground,
                disabledContainerColor = TerminalBackground,
                focusedTextColor = TerminalWhite,
                unfocusedTextColor = TerminalWhite,
                cursorColor = TerminalGreen,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() })
        )

        IconButton(onClick = onPaste) {
            Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = "Paste",
                tint = TerminalSystem
            )
        }
    }
}

private fun colorForLine(type: TerminalLineType): Color = when (type) {
    TerminalLineType.INPUT -> TerminalWhite
    TerminalLineType.OUTPUT -> TerminalGreen
    TerminalLineType.ERROR -> TerminalError
    TerminalLineType.SYSTEM -> TerminalSystem
}
