package com.salaryapp.jigong.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salaryapp.jigong.JiGongApplication
import com.salaryapp.jigong.domain.model.Worker
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.ConfirmDialog
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard
import com.salaryapp.jigong.ui.common.ManagementItemCard

@Composable
fun WorkerRoute(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: WorkerViewModel = viewModel(
        factory = WorkerViewModelFactory(app.appContainer.workerRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    WorkerScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddClick = viewModel::showAddDialog,
        onEditClick = viewModel::showEditDialog,
        onDeleteClick = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissEditor = viewModel::dismissEditor,
        onEditorChange = viewModel::updateEditor,
        onSave = { allowDuplicate -> viewModel.saveWorker(allowDuplicate) }
    )
}

@Composable
private fun WorkerScreen(
    uiState: WorkerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Worker) -> Unit,
    onDeleteClick: (Worker) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissEditor: () -> Unit,
    onEditorChange: ((WorkerEditorState) -> WorkerEditorState) -> Unit,
    onSave: (Boolean) -> Unit
) {
    AppScaffold(
        title = "工人名单",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBack
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("先把常用工人存好", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "后面记工时可以直接点选，不用反复输入名字和工价。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Text("新增工人")
                    }
                }
                if (uiState.workers.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "还没有工人",
                            description = "点上面的“新增工人”，先把常用工人录进去。"
                        )
                    }
                } else {
                    items(uiState.workers, key = { it.id }) { worker ->
                        ManagementItemCard(
                            title = worker.name,
                            subtitle = worker.defaultWage?.let { "默认工价：$it" } ?: "默认工价：还没填",
                            extraLines = listOfNotNull(
                                worker.phone?.takeIf { it.isNotBlank() }?.let { "电话：$it" },
                                worker.note?.takeIf { it.isNotBlank() }?.let { "备注：$it" }
                            ),
                            onClick = { onEditClick(worker) },
                            onDeleteClick = { onDeleteClick(worker) }
                        )
                    }
                }
            }
        }
    }

    uiState.editorState?.let { editorState ->
        AlertDialog(
            onDismissRequest = onDismissEditor,
            title = { Text(if (editorState.id == null) "新增工人" else "修改工人", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorTextField(
                        label = "姓名",
                        value = editorState.name,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(name = value) }
                        },
                        required = true,
                        emphasize = true
                    )
                    EditorTextField(
                        label = "默认工价",
                        value = editorState.defaultWage,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(defaultWage = value) }
                        }
                    )
                    EditorTextField(
                        label = "电话",
                        value = editorState.phone,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(phone = value) }
                        }
                    )
                    EditorTextField(
                        label = "备注",
                        value = editorState.note,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(note = value) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(false) }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditor) {
                    Text("取消")
                }
            }
        )
    }

    if (uiState.duplicatePending) {
        ConfirmDialog(
            title = "姓名重复提醒",
            text = "已经有同名工人了，确认后仍然可以继续保存。",
            confirmText = "仍然保存",
            onConfirm = { onSave(true) },
            onDismiss = onDismissEditor
        )
    }

    uiState.deleteTarget?.let {
        ConfirmDialog(
            title = "删除工人",
            text = "确认删除“${it.name}”吗？以前的记工记录不会跟着消失。",
            confirmText = "确认删除",
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}
