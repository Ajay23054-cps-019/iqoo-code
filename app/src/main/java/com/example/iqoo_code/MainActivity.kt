package com.example.iqoo_code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iqoo_code.fs.Workspace
import com.example.iqoo_code.project.ProjectManagerScreen
import com.example.iqoo_code.project.ProjectManagerViewModel
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TerminalApp(filesDir: File) {
    val workspace = remember(filesDir) { Workspace(filesDir) }
    var currentScreen by remember { mutableStateOf("terminal") }

    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text("iQOO Code") },
            actions = {
                TextButton(onClick = { currentScreen = "terminal" }) {
                    Text("Terminal", color = if (currentScreen == "terminal") Color(0xFF4AF626) else Color.White)
                }
                TextButton(onClick = { currentScreen = "projects" }) {
                    Text("Projects", color = if (currentScreen == "projects") Color(0xFF4AF626) else Color.White)
                }
            }
        )

        when (currentScreen) {
            "terminal" -> {
                val context = LocalContext.current
                val factory = remember(workspace) { TerminalViewModel.Factory(context, filesDir) }
                val viewModel: TerminalViewModel = viewModel(factory = factory)
                TerminalScreen(viewModel = viewModel)
            }
            "projects" -> {
                val factory = remember(workspace) { ProjectManagerViewModel.Factory(workspace) }
                val viewModel: ProjectManagerViewModel = viewModel(factory = factory)
                ProjectManagerScreen(viewModel = viewModel)
            }
        }
    }
}
