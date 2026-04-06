package com.salaryapp.jigong.ui.site

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
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.ConfirmDialog
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard
import com.salaryapp.jigong.ui.common.ManagementItemCard

@Composable
fun SiteRoute(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: SiteViewModel = viewModel(
        factory = SiteViewModelFactory(app.appContainer.siteRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    SiteScreen(
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
        onSave = { allowDuplicate -> viewModel.saveSite(allowDuplicate) }
    )
}

@Composable
private fun SiteScreen(
    uiState: SiteUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Site) -> Unit,
    onDeleteClick: (Site) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissEditor: () -> Unit,
    onEditorChange: ((SiteEditorState) -> SiteEditorState) -> Unit,
    onSave: (Boolean) -> Unit
) {
    AppScaffold(
        title = "工地名单",
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
                        Text("先把常用工地存好", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "记工、存照片、查照片时都能直接点选，不容易输错。",
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
                        Text("新增工地")
                    }
                }
                if (uiState.sites.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "还没有工地",
                            description = "点上面的“新增工地”，先把常用工地录进去。"
                        )
                    }
                } else {
                    items(uiState.sites, key = { it.id }) { site ->
                        ManagementItemCard(
                            title = site.siteName,
                            subtitle = site.addressOrAlias?.let { "地址或别名：$it" } ?: "地址或别名：还没填",
                            extraLines = listOfNotNull(
                                site.note?.takeIf { it.isNotBlank() }?.let { "备注：$it" }
                            ),
                            onClick = { onEditClick(site) },
                            onDeleteClick = { onDeleteClick(site) }
                        )
                    }
                }
            }
        }
    }

    uiState.editorState?.let { editorState ->
        AlertDialog(
            onDismissRequest = onDismissEditor,
            title = { Text(if (editorState.id == null) "新增工地" else "修改工地", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorTextField(
                        label = "工地名称",
                        value = editorState.siteName,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(siteName = value) }
                        },
                        required = true,
                        emphasize = true
                    )
                    EditorTextField(
                        label = "地址或别名",
                        value = editorState.addressOrAlias,
                        onValueChange = { value ->
                            onEditorChange { current -> current.copy(addressOrAlias = value) }
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
            title = "工地名重复提醒",
            text = "已经有同名工地了，确认后仍然可以继续保存。",
            confirmText = "仍然保存",
            onConfirm = { onSave(true) },
            onDismiss = onDismissEditor
        )
    }

    uiState.deleteTarget?.let {
        ConfirmDialog(
            title = "删除工地",
            text = "确认删除“${it.siteName}”吗？",
            confirmText = "确认删除",
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}
