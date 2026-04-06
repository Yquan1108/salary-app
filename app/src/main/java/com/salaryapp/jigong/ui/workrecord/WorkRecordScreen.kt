package com.salaryapp.jigong.ui.workrecord

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salaryapp.jigong.JiGongApplication
import com.salaryapp.jigong.core.util.formatDate
import com.salaryapp.jigong.core.util.localDateToMillis
import com.salaryapp.jigong.core.util.millisToLocalDate
import com.salaryapp.jigong.domain.model.WorkRecord
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.ConfirmDialog
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard

@Composable
fun WorkRecordRoute(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    justSaved: Boolean
) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: WorkRecordViewModel = viewModel(
        factory = WorkRecordViewModelFactory(
            app.appContainer.workRecordRepository,
            app.appContainer.workerRepository,
            app.appContainer.siteRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(justSaved) {
        if (justSaved) viewModel.notifySaved()
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    WorkRecordScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onDateFilterChange = viewModel::updateDateFilter,
        onWorkerFilterChange = viewModel::updateWorkerFilter,
        onSiteFilterChange = viewModel::updateSiteFilter,
        onClearFilters = viewModel::clearFilters,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onShowDeleteConfirm = viewModel::showDeleteConfirm,
        onDismissDelete = viewModel::dismissDeleteConfirm,
        onDeleteSelected = viewModel::deleteSelected
    )
}

@Composable
private fun WorkRecordScreen(
    uiState: WorkRecordUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDateFilterChange: (Long?) -> Unit,
    onWorkerFilterChange: (Long?) -> Unit,
    onSiteFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onDismissDelete: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    AppScaffold(
        title = if (uiState.isSelectionMode) "已选 ${uiState.selectedIds.size} 条" else "记今天做工",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = if (uiState.isSelectionMode) onClearSelection else onBack
    ) {
        Scaffold(
            containerColor = Color.Transparent,
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
                        Text("按日期、人、工地来找记录", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "所有筛选和结果都改成单列大卡片，点哪里更清楚。",
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
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("新增记工")
                    }
                }
                item {
                    FiltersSection(
                        uiState = uiState,
                        onDateFilterChange = onDateFilterChange,
                        onWorkerFilterChange = onWorkerFilterChange,
                        onSiteFilterChange = onSiteFilterChange,
                        onClearFilters = onClearFilters
                    )
                }
                if (uiState.isSelectionMode) {
                    item {
                        SelectionToolbar(
                            selectedCount = uiState.selectedIds.size,
                            onSelectAll = onSelectAll,
                            onDelete = onShowDeleteConfirm
                        )
                    }
                }
                if (uiState.visibleRecords.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "还没有记工记录",
                            description = "点“新增记工”先录一条，后面就能在这里查看和修改。"
                        )
                    }
                } else {
                    items(uiState.visibleRecords, key = { it.id }) { record ->
                        RecordCard(
                            record = record,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = record.id in uiState.selectedIds,
                            onClick = {
                                if (uiState.isSelectionMode) onToggleSelection(record.id) else onEditClick(record.id)
                            },
                            onLongPressSelect = { onEnterSelectionMode(record.id) },
                            onToggleSelection = { onToggleSelection(record.id) }
                        )
                    }
                }
            }
        }
    }

    uiState.pendingDeleteCount?.let { count ->
        ConfirmDialog(
            title = "删除记录",
            text = "确认删除已选中的 $count 条记录吗？",
            confirmText = "确认删除",
            onConfirm = onDeleteSelected,
            onDismiss = onDismissDelete
        )
    }
}

@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("已选 $selectedCount 条记录", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(
                onClick = onSelectAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Icon(Icons.Outlined.SelectAll, contentDescription = null)
                Text("全选")
            }
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text("删除已选记录")
            }
        }
    }
}

@Composable
private fun FiltersSection(
    uiState: WorkRecordUiState,
    onDateFilterChange: (Long?) -> Unit,
    onWorkerFilterChange: (Long?) -> Unit,
    onSiteFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("筛选条件", style = MaterialTheme.typography.titleLarge)
            DateFilterPicker(
                label = "日期",
                selectedDate = uiState.filterState.date,
                onDateSelected = onDateFilterChange
            )
            PickerField(
                label = "工人",
                value = uiState.workers.firstOrNull { it.id == uiState.filterState.workerId }?.name ?: "全部工人",
                options = listOf(null to "全部工人") + uiState.workers.map { it.id to it.name },
                onSelected = onWorkerFilterChange
            )
            PickerField(
                label = "工地",
                value = uiState.sites.firstOrNull { it.id == uiState.filterState.siteId }?.siteName ?: "全部工地",
                options = listOf(null to "全部工地") + uiState.sites.map { it.id to it.siteName },
                onSelected = onSiteFilterChange
            )
            FilledTonalButton(
                onClick = onClearFilters,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Icon(Icons.Outlined.FilterAltOff, contentDescription = null)
                Text("清空筛选")
            }
        }
    }
}

@Composable
private fun DateFilterPicker(
    label: String,
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = label,
            value = selectedDate?.let(::formatDate) ?: "全部日期",
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = {
                val seed = selectedDate?.let(::millisToLocalDate) ?: java.time.LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected(localDateToMillis(java.time.LocalDate.of(year, month + 1, day)))
                    },
                    seed.year,
                    seed.monthValue - 1,
                    seed.dayOfMonth
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("选择日期")
        }
        TextButton(onClick = { onDateSelected(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("改回全部日期")
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    options: List<Pair<Long?, String>>,
    onSelected: (Long?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = label,
            value = value,
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("选择$label")
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择$label", style = MaterialTheme.typography.titleLarge) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option.first)
                                    showDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                option.second,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("关闭") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordCard(
    record: WorkRecord,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPressSelect: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPressSelect
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelectionMode) {
                IconButton(onClick = onToggleSelection) {
                    Icon(
                        if (isSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null
                    )
                }
            }
            Text(record.workerNameSnapshot, style = MaterialTheme.typography.titleLarge)
            Text("日期：${formatDate(record.workDate)}", style = MaterialTheme.typography.bodyLarge)
            Text("工地：${record.siteNameSnapshot ?: "-"}", style = MaterialTheme.typography.bodyLarge)
            Text("时长：${record.durationText ?: "-"}", style = MaterialTheme.typography.bodyLarge)
            Text("工价：${record.unitPriceText ?: "-"}", style = MaterialTheme.typography.bodyLarge)
            Text("金额：${record.amount}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun WorkRecordEditorRoute(
    recordId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: WorkRecordEditorViewModel = viewModel(
        factory = WorkRecordEditorViewModelFactory(
            recordId,
            app.appContainer.workRecordRepository,
            app.appContainer.workerRepository,
            app.appContainer.siteRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) onSaved()
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        WorkRecordEditorScreen(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onEditorChange = viewModel::updateEditor,
            onSave = { viewModel.save(false) },
            onSaveAndContinue = { viewModel.save(true) }
        )
    }
}

@Composable
private fun WorkRecordEditorScreen(
    uiState: WorkRecordEditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditorChange: ((WorkRecordEditorState) -> WorkRecordEditorState) -> Unit,
    onSave: () -> Unit,
    onSaveAndContinue: () -> Unit
) {
    AppScaffold(
        title = if (uiState.editorState.id == null) "新增记工" else "修改记工",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBack
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FormSectionCard(title = "基础信息", description = "先把日期、工人、工地填好。") {
                        DateEditorField(
                            selectedDate = uiState.editorState.workDate,
                            onDateSelected = { millis ->
                                onEditorChange { current -> current.copy(workDate = millis) }
                            }
                        )
                        ArchiveAssistField(
                            label = "工人",
                            value = uiState.editorState.workerName,
                            options = uiState.workers.map { it.id to it.name },
                            required = true,
                            helperText = "可以手填，也可以从工人名单里选。",
                            onValueChange = { value ->
                                onEditorChange { current ->
                                    val trimmed = value.trim()
                                    val matched = uiState.workers.firstOrNull { it.name == trimmed }
                                    current.copy(
                                        workerName = value,
                                        workerId = matched?.id,
                                        phoneNumber = matched?.phone ?: current.phoneNumber,
                                        unitPriceText = matched?.defaultWage ?: current.unitPriceText
                                    )
                                }
                            },
                            onSelected = { id ->
                                val selectedWorker = uiState.workers.firstOrNull { it.id == id }
                                onEditorChange { current ->
                                    current.copy(
                                        workerId = id,
                                        workerName = selectedWorker?.name.orEmpty(),
                                        phoneNumber = selectedWorker?.phone.orEmpty(),
                                        unitPriceText = selectedWorker?.defaultWage.orEmpty()
                                    )
                                }
                            }
                        )
                        EditorTextField(
                            label = "电话",
                            value = uiState.editorState.phoneNumber,
                            onValueChange = { value ->
                                onEditorChange { current -> current.copy(phoneNumber = value) }
                            }
                        )
                        ArchiveAssistField(
                            label = "工地",
                            value = uiState.editorState.siteName,
                            options = listOf(null to "不选工地") + uiState.sites.map { it.id to it.siteName },
                            required = false,
                            helperText = "可以手填，也可以从工地名单里选。",
                            onValueChange = { value ->
                                onEditorChange { current ->
                                    val trimmed = value.trim()
                                    val matched = uiState.sites.firstOrNull { it.siteName == trimmed }
                                    current.copy(siteName = value, siteId = matched?.id)
                                }
                            },
                            onSelected = { id ->
                                val selectedName = uiState.sites.firstOrNull { it.id == id }?.siteName.orEmpty()
                                onEditorChange { current ->
                                    current.copy(siteId = id, siteName = if (id == null) "" else selectedName)
                                }
                            }
                        )
                    }
                }
                item {
                    FormSectionCard(title = "做工信息", description = "再把时长、工价和金额填好。") {
                        EditorTextField(
                            label = "做工时长",
                            value = uiState.editorState.durationText,
                            onValueChange = { value ->
                                onEditorChange { current -> current.copy(durationText = value) }
                            }
                        )
                        EditorTextField(
                            label = "工价",
                            value = uiState.editorState.unitPriceText,
                            onValueChange = { value ->
                                onEditorChange { current -> current.copy(unitPriceText = value) }
                            }
                        )
                        EditorTextField(
                            label = "金额",
                            value = uiState.editorState.amount,
                            onValueChange = { value ->
                                onEditorChange { current -> current.copy(amount = value) }
                            },
                            required = true,
                            emphasize = true
                        )
                    }
                }
                item {
                    FormSectionCard(title = "备注", description = "没特殊情况也可以不填。") {
                        EditorTextField(
                            label = "备注",
                            value = uiState.editorState.remark,
                            onValueChange = { value ->
                                onEditorChange { current -> current.copy(remark = value) }
                            }
                        )
                    }
                }
                item {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text("保存")
                    }
                }
                if (uiState.editorState.id == null) {
                    item {
                        FilledTonalButton(
                            onClick = onSaveAndContinue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text("保存后继续新增")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun DateEditorField(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = "日期",
            value = formatDate(selectedDate),
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = {
                val date = millisToLocalDate(selectedDate)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected(localDateToMillis(java.time.LocalDate.of(year, month + 1, day)))
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("重新选日期")
        }
    }
}

@Composable
private fun ArchiveAssistField(
    label: String,
    value: String,
    options: List<Pair<Long?, String>>,
    required: Boolean,
    helperText: String,
    onValueChange: (String) -> Unit,
    onSelected: (Long?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var keyword by remember(showDialog) { mutableStateOf("") }
    val filteredOptions = options.filter { it.second.contains(keyword.trim(), ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            required = required,
            emphasize = required
        )
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
            Text("从名单里选")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择$label", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorTextField(
                        label = "搜索$label",
                        value = keyword,
                        onValueChange = { keyword = it },
                        emphasize = true
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredOptions) { option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(option.first)
                                        showDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = option.second,
                                    modifier = Modifier.padding(18.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
