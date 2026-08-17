package com.example.iqoo_code.project

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

private val PmBackground = Color(0xFF1E1E1E)
private val PmSurface = Color(0xFF2D2D2D)
private val PmText = Color(0xFFE0E0E0)
private val PmAccent = Color(0xFF4AF626)

@Composable
fun ProjectManagerScreen(viewModel: ProjectManagerViewModel) {
    val state by viewModel.uiState.collectAsState()
    var newProjectName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var editingFileName by remember { mutableStateOf("") }
    var editingFileContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PmBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Project Manager", color = PmAccent, fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.weight(1f))
        }

        if (state.currentPath == null) {
            ProjectsList(
                projects = state.projects,
                onOpen = viewModel::openProject,
                onCreate = {
                    viewModel.createProject(newProjectName)
                    newProjectName = ""
                },
                newProjectName = newProjectName,
                onNameChange = { newProjectName = it }
            )
        } else {
            FileBrowser(
                currentPath = state.currentPath!!,
                selectedFile = state.selectedFile,
                fileContent = state.fileContent,
                onBack = { if (state.currentPath!!.parentFile != null) viewModel.navigateUp() else viewModel.refreshProjects() },
                onSelect = viewModel::selectFile,
                onCreateFile = {
                    viewModel.createFile(newFileName)
                    newFileName = ""
                },
                onCreateFolder = {
                    viewModel.createFolder(newFolderName)
                    newFolderName = ""
                },
                onRename = {
                    viewModel.renameFile(editingFileName)
                    editingFileName = ""
                },
                onSave = {
                    viewModel.saveFile(editingFileContent)
                },
                onDelete = viewModel::deleteSelected,
                newFileName = newFileName,
                newFolderName = newFolderName,
                onNewFileNameChange = { newFileName = it },
                onNewFolderNameChange = { newFolderName = it },
                editingFileName = if (state.selectedFile != null) state.selectedFile!!.name else "",
                editingFileContent = if (state.selectedFile != null) {
                    editingFileContent = state.fileContent
                    editingFileContent
                } else "",
                onEditingFileNameChange = { editingFileName = it },
                onEditingFileContentChange = { editingFileContent = it }
            )
        }

        state.error?.let { error ->
            Text(text = error, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ProjectsList(
    projects: List<File>,
    onOpen: (File) -> Unit,
    onCreate: () -> Unit,
    newProjectName: String,
    onNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newProjectName,
                onValueChange = onNameChange,
                label = { Text("New project name") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCreate() })
            )
            Button(onClick = onCreate, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Create", modifier = Modifier.padding(start = 4.dp))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(projects) { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(project) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = PmAccent, modifier = Modifier.size(24.dp))
                    Text(project.name, color = PmText, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun FileBrowser(
    currentPath: File,
    selectedFile: File?,
    fileContent: String,
    onBack: () -> Unit,
    onSelect: (File) -> Unit,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onRename: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    newFileName: String,
    newFolderName: String,
    onNewFileNameChange: (String) -> Unit,
    onNewFolderNameChange: (String) -> Unit,
    editingFileName: String,
    editingFileContent: String,
    onEditingFileNameChange: (String) -> Unit,
    onEditingFileContentChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PmText)
            }
            Text(currentPath.absolutePath, color = PmText, fontFamily = FontFamily.Monospace, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }

        if (selectedFile != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = editingFileName,
                    onValueChange = onEditingFileNameChange,
                    label = { Text("File name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editingFileContent,
                    onValueChange = onEditingFileContentChange,
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = Int.MAX_VALUE
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSave) { Text("Save") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                    TextButton(onClick = onRename) { Text("Rename") }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newFileName, onValueChange = onNewFileNameChange, label = { Text("New file") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onCreateFile() }))
                Button(onClick = onCreateFile) { Text("Create File") }
                OutlinedTextField(value = newFolderName, onValueChange = onNewFolderNameChange, label = { Text("New folder") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onCreateFolder() }))
                Button(onClick = onCreateFolder) { Text("Create Folder") }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                val files = currentPath.listFiles()?.sortedBy { it.name } ?: emptyList()
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(file) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = if (file.isDirectory) PmAccent else PmText, modifier = Modifier.size(20.dp))
                        Text(file.name, color = PmText, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
