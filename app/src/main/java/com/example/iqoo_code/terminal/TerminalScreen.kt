package com.example.iqoo_code.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


private val TerminalBackground = Color(0xFF0B0F0C)
private val TerminalGreen = Color(0xFF4AF626)
private val TerminalWhite = Color(0xFFE6E6E6)
private val TerminalError = Color(0xFFFF6B6B)
private val TerminalSystem = Color(0xFF6C7A89)

/**
 * Full-screen terminal UI. Talks only to [TerminalViewModel] - no direct
 * filesystem or process access happens here.
 */
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.lines) { line ->
                Text(
                    text = line.text,
                    color = colorForLine(line.type),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }
        }

        InputBar(
            input = state.input,
            promptPath = state.promptPath,
            isRunning = state.isRunning,
            focusRequester = focusRequester,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::onSubmit,
            onHistoryUp = viewModel::browsePrevious,
            onHistoryDown = viewModel::browseNext
        )
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
    onHistoryDown: () -> Unit
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
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        )

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(
                color = TerminalWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
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
    }
}

private fun colorForLine(type: TerminalLineType): Color = when (type) {
    TerminalLineType.INPUT -> TerminalWhite
    TerminalLineType.OUTPUT -> TerminalGreen
    TerminalLineType.ERROR -> TerminalError
    TerminalLineType.SYSTEM -> TerminalSystem
}
