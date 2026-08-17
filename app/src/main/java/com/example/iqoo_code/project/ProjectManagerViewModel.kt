package com.example.iqoo_code.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iqoo_code.fs.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ProjectUiState(
    val projects: List<File> = emptyList(),
    val currentPath: File? = null,
    val selectedFile: File? = null,
    val fileContent: String = "",
    val error: String? = null
)

class ProjectManagerViewModel(private val workspace: Workspace) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        refreshProjects()
    }

    fun refreshProjects() {
        _uiState.update { it.copy(projects = workspace.projectsDir.listFiles()?.sortedBy { it.name } ?: emptyList(), currentPath = null, selectedFile = null, fileContent = "", error = null) }
    }

    fun createProject(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Project name cannot be empty") }
            return
        }
        val target = File(workspace.projectsDir, name)
        if (target.exists()) {
            _uiState.update { it.copy(error = "Project already exists") }
            return
        }
        viewModelScope.launch {
            if (target.mkdirs()) {
                refreshProjects()
            } else {
                _uiState.update { it.copy(error = "Failed to create project") }
            }
        }
    }

    fun openProject(project: File) {
        _uiState.update { it.copy(currentPath = project, selectedFile = null, fileContent = "", error = null) }
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath ?: return
        val parent = current.parentFile
        if (parent != null && workspace.isWithinRoot(parent)) {
            _uiState.update { it.copy(currentPath = parent, selectedFile = null, fileContent = "") }
        }
    }

    fun createFile(name: String) {
        val current = _uiState.value.currentPath ?: return
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "File name cannot be empty") }
            return
        }
        val target = File(current, name)
        if (target.exists()) {
            _uiState.update { it.copy(error = "File already exists") }
            return
        }
        viewModelScope.launch {
            try {
                target.createNewFile()
                _uiState.update { it.copy(currentPath = target, selectedFile = target, fileContent = "", error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create file") }
            }
        }
    }

    fun createFolder(name: String) {
        val current = _uiState.value.currentPath ?: return
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Folder name cannot be empty") }
            return
        }
        val target = File(current, name)
        if (target.exists()) {
            _uiState.update { it.copy(error = "Folder already exists") }
            return
        }
        viewModelScope.launch {
            if (target.mkdirs()) {
                _uiState.update { it.copy(currentPath = target, error = null) }
            } else {
                _uiState.update { it.copy(error = "Failed to create folder") }
            }
        }
    }

    fun selectFile(file: File) {
        if (file.isDirectory) {
            _uiState.update { it.copy(currentPath = file, selectedFile = null, fileContent = "") }
        } else {
            try {
                val content = file.readText()
                _uiState.update { it.copy(selectedFile = file, fileContent = content, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to read file") }
            }
        }
    }

    fun renameFile(newName: String) {
        val current = _uiState.value.currentPath ?: return
        val selected = _uiState.value.selectedFile ?: return
        val newFile = File(selected.parentFile, newName)
        if (newFile.exists()) {
            _uiState.update { it.copy(error = "File already exists") }
            return
        }
        viewModelScope.launch {
            if (selected.renameTo(newFile)) {
                _uiState.update { it.copy(selectedFile = newFile, error = null) }
            } else {
                _uiState.update { it.copy(error = "Failed to rename") }
            }
        }
    }

    fun saveFile(content: String) {
        val selected = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            try {
                selected.writeText(content)
                _uiState.update { it.copy(fileContent = content, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save file") }
            }
        }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            if (selected.delete()) {
                _uiState.update { it.copy(selectedFile = null, fileContent = "", error = null) }
            } else {
                _uiState.update { it.copy(error = "Failed to delete") }
            }
        }
    }

    class Factory(private val workspace: Workspace) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProjectManagerViewModel(workspace) as T
        }
    }
}
