package com.salaryapp.jigong.ui.stats

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salaryapp.jigong.JiGongApplication
import com.salaryapp.jigong.core.util.formatDate
import com.salaryapp.jigong.core.util.localDateToMillis
import com.salaryapp.jigong.core.util.millisToLocalDate
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.domain.model.Worker
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SalaryStatsRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: SalaryStatsViewModel = viewModel(
        factory = SalaryStatsViewModelFactory(
            workRecordRepository = app.appContainer.workRecordRepository,
            workerRepository = app.appContainer.workerRepository,
            siteRepository = app.appContainer.siteRepository,
            exportRepository = app.appContainer.salaryStatsExportRepository
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

    SalaryStatsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onWorkerKeywordChange = viewModel::updateWorkerKeyword,
        onSiteKeywordChange = viewModel::updateSiteKeyword,
        onWorkerSelected = viewModel::fillWorkerKeyword,
        onSiteSelected = viewModel::fillSiteKeyword,
        onStartDateChange = viewModel::updateStartDate,
        onEndDateChange = viewModel::updateEndDate,
        onQuery = viewModel::applyFilters,
        onReset = viewModel::resetFilters,
        onExport = viewModel::export,
        onDismissExportSuccess = viewModel::dismissExportSuccess
    )
}

@Composable
private fun SalaryStatsScreen(
    uiState: SalaryStatsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onWorkerKeywordChange: (String) -> Unit,
    onSiteKeywordChange: (String) -> Unit,
    onWorkerSelected: (String) -> Unit,
    onSiteSelected: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onQuery: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onDismissExportSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate) }

    AppScaffold(
        title = "工资汇总",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBack
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("选人、选工地、选日期，就能查工资", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "查询结果会直接汇总，还可以一键导出 Excel。",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item {
                        FilterCard(
                            uiState = uiState,
                            onWorkerKeywordChange = onWorkerKeywordChange,
                            onSiteKeywordChange = onSiteKeywordChange,
                            onWorkerSelected = onWorkerSelected,
                            onSiteSelected = onSiteSelected,
                            onStartDateChange = onStartDateChange,
                            onEndDateChange = onEndDateChange,
                            onQuery = onQuery,
                            onReset = onReset
                        )
                    }
                    item {
                        SummaryCard(
                            recordCount = uiState.recordCount,
                            totalAmount = uiState.totalAmount,
                            invalidAmountCount = uiState.invalidAmountCount,
                            exporting = uiState.isExporting,
                            enabled = uiState.rows.isNotEmpty(),
                            onExport = onExport
                        )
                    }
                    if (uiState.rows.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "还没有查询结果",
                                description = "先输入条件再点“开始查询”，或者重置后重新查。"
                            )
                        }
                    } else {
                        items(uiState.rows, key = { it.id }) { row ->
                            ResultCard(row)
                        }
                    }
                }
            }
        }
    }

    if (uiState.exportedFileUri != null && uiState.exportedFileName != null) {
        AlertDialog(
            onDismissRequest = onDismissExportSuccess,
            title = { Text("导出成功", style = MaterialTheme.typography.titleLarge) },
            text = { Text("文件已保存到下载目录：${uiState.exportedFileName}", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    uiState.exportedFileUri.toString().toUri(),
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            onDismissExportSuccess()
                        } catch (_: ActivityNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar("打不开文件，请到下载目录里查看。")
                            }
                        } catch (_: Throwable) {
                            scope.launch {
                                snackbarHostState.showSnackbar("打开失败，请到下载目录里查看。")
                            }
                        }
                    }
                ) {
                    Text("打开文件")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExportSuccess) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun FilterCard(
    uiState: SalaryStatsUiState,
    onWorkerKeywordChange: (String) -> Unit,
    onSiteKeywordChange: (String) -> Unit,
    onWorkerSelected: (String) -> Unit,
    onSiteSelected: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onQuery: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SearchPickField(
                label = "工人",
                value = uiState.filterState.workerKeyword,
                onValueChange = onWorkerKeywordChange,
                options = uiState.workers,
                optionLabel = { it.name },
                onSelected = { worker -> onWorkerSelected(worker?.name.orEmpty()) }
            )
            SearchPickField(
                label = "工地",
                value = uiState.filterState.siteKeyword,
                onValueChange = onSiteKeywordChange,
                options = uiState.sites,
                optionLabel = { it.siteName },
                onSelected = { site -> onSiteSelected(site?.siteName.orEmpty()) }
            )
            DatePickerField(
                label = "开始日期",
                value = formatDate(uiState.filterState.startDate),
                onDateSelected = onStartDateChange
            )
            DatePickerField(
                label = "结束日期",
                value = formatDate(uiState.filterState.endDate),
                onDateSelected = onEndDateChange
            )
            Button(
                onClick = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text("开始查询")
            }
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("清空条件")
            }
        }
    }
}

@Composable
private fun <T> SearchPickField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T?) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            emphasize = true
        )
        FilledTonalButton(
            onClick = { expanded.value = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("从名单里选$label")
        }
    }
    if (expanded.value) {
        AlertDialog(
            onDismissRequest = { expanded.value = false },
            title = { Text("选择$label", style = MaterialTheme.typography.titleLarge) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        TextButton(
                            onClick = {
                                onSelected(null)
                                expanded.value = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("清空")
                        }
                    }
                    items(options.size) { index ->
                        val item = options[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(item)
                                    expanded.value = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                optionLabel(item),
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded.value = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val localDate = millisToLocalDate(parseDisplayDate(value))
    Box(
        modifier = Modifier.clickable {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(localDateToMillis(java.time.LocalDate.of(year, month + 1, dayOfMonth)))
                },
                localDate.year,
                localDate.monthValue - 1,
                localDate.dayOfMonth
            ).show()
        }
    ) {
        EditorTextField(
            label = label,
            value = value,
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
    }
}

@Composable
private fun SummaryCard(
    recordCount: Int,
    totalAmount: String,
    invalidAmountCount: Int,
    exporting: Boolean,
    enabled: Boolean,
    onExport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("共找到 $recordCount 条记录", style = MaterialTheme.typography.titleLarge)
            Text("工资合计：$totalAmount", style = MaterialTheme.typography.headlineMedium)
            if (invalidAmountCount > 0) {
                Text(
                    text = "有 $invalidAmountCount 条金额没算进合计",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = onExport,
                enabled = enabled && !exporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                if (exporting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                } else {
                    Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                }
                Text(if (exporting) "正在导出..." else "导出 Excel")
            }
        }
    }
}

@Composable
private fun ResultCard(row: SalaryStatsRowUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(row.workerName.ifBlank { "未填写工人" }, style = MaterialTheme.typography.titleLarge)
            Text("日期：${row.workDate}", style = MaterialTheme.typography.bodyLarge)
            Text("工地：${row.siteName.ifBlank { "-" }}", style = MaterialTheme.typography.bodyLarge)
            Text("时长：${row.duration.ifBlank { "-" }}", style = MaterialTheme.typography.bodyLarge)
            Text("电话：${row.phoneNumber.ifBlank { "-" }}", style = MaterialTheme.typography.bodyLarge)
            Text("工价：${row.unitPrice.ifBlank { "-" }}", style = MaterialTheme.typography.bodyLarge)
            Text("金额：${row.amount.ifBlank { "-" }}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun parseDisplayDate(value: String): Long {
    val parts = value.split("-")
    return localDateToMillis(
        java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    )
}
