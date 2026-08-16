package com.example.iqoo_code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iqoo_code.terminal.TerminalScreen
import com.example.iqoo_code.terminal.TerminalViewModel
import com.example.iqoo_code.ui.theme.IqoocodeTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IqoocodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TerminalApp(filesDir = filesDir)
                }
            }
        }
    }
}

@Composable
fun TerminalApp(filesDir: File) {
    val factory = remember(filesDir) { TerminalViewModel.Factory(filesDir) }
    val viewModel: TerminalViewModel = viewModel(factory = factory)
    TerminalScreen(viewModel = viewModel)
}
