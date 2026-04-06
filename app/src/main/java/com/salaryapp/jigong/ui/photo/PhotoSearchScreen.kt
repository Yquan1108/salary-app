@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.salaryapp.jigong.ui.photo

import android.app.DatePickerDialog
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.salaryapp.jigong.JiGongApplication
import com.salaryapp.jigong.core.ui.theme.JiGongHeroBrush
import com.salaryapp.jigong.core.util.formatDate
import com.salaryapp.jigong.core.util.localDateToMillis
import com.salaryapp.jigong.core.util.millisToLocalDate
import com.salaryapp.jigong.core.util.todayMillis
import com.salaryapp.jigong.domain.model.PhotoBatch
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard

@Composable
fun PhotoSearchRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: PhotoSearchViewModel = viewModel(
        factory = PhotoSearchViewModelFactory(
            photoRepository = app.appContainer.photoRepository,
            siteRepository = app.appContainer.siteRepository
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

    PhotoSearchScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onKeywordChange = viewModel::updateKeyword,
        onSiteSelected = viewModel::updateSite,
        onStartDateChange = viewModel::updateStartDate,
        onEndDateChange = viewModel::updateEndDate,
        onClearFilters = viewModel::clearFilters,
        onOpenPreview = viewModel::openPreview,
        onClosePreview = viewModel::closePreview,
        onPreviewIndexChange = viewModel::updatePreviewIndex,
        onExportCurrent = viewModel::exportCurrentPhoto
    )
}

@Composable
private fun PhotoSearchScreen(
    uiState: PhotoSearchUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSiteSelected: (Site?) -> Unit,
    onStartDateChange: (Long?) -> Unit,
    onEndDateChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenPreview: (PhotoBatch, Int) -> Unit,
    onClosePreview: () -> Unit,
    onPreviewIndexChange: (Int) -> Unit,
    onExportCurrent: () -> Unit
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = "找工地照片",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBack
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        SearchHeaderCard()
                    }
                    item {
                        SearchEntryCard(
                            keyword = uiState.keyword,
                            resultCount = uiState.filteredBatches.size,
                            onKeywordChange = onKeywordChange
                        )
                    }
                    item {
                        FilterToggleCard(
                            showFilters = showFilters,
                            onToggleFilters = { showFilters = !showFilters }
                        )
                    }
                    item {
                        AnimatedVisibility(visible = showFilters) {
                            SearchFilterCard(
                                uiState = uiState,
                                onSiteSelected = onSiteSelected,
                                onStartDateChange = onStartDateChange,
                                onEndDateChange = onEndDateChange,
                                onClearFilters = onClearFilters
                            )
                        }
                    }
                    if (uiState.filteredBatches.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "没找到符合条件的照片",
                                description = "可以换个关键词，或者展开筛选重新选工地和日期。"
                            )
                        }
                    } else {
                        items(uiState.filteredBatches, key = { it.id }) { batch ->
                            SearchResultBatchCard(
                                batch = batch,
                                onOpenPreview = onOpenPreview
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.previewBatch?.let { batch ->
        SearchPreviewDialog(
            batch = batch,
            initialIndex = uiState.previewIndex,
            isExporting = uiState.isExporting,
            onDismiss = onClosePreview,
            onPageChanged = onPreviewIndexChange,
            onDownload = onExportCurrent
        )
    }
}

@Composable
private fun SearchHeaderCard() {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JiGongHeroBrush(colors))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("找工地照片", style = MaterialTheme.typography.headlineLarge)
            Text(
                "输关键词/选工地/日期，查照片",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchEntryCard(
    keyword: String,
    resultCount: Int,
    onKeywordChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorTextField(
                label = "输入工地名、备注或照片名",
                value = keyword,
                onValueChange = onKeywordChange,
                emphasize = true
            )
            Text(
                "已找到 $resultCount 批照片",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterToggleCard(
    showFilters: Boolean,
    onToggleFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleFilters,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(
                    if (showFilters) Icons.Outlined.FilterAltOff else Icons.Outlined.FilterAlt,
                    contentDescription = null
                )
                Text(if (showFilters) "收起筛选" else "展开筛选")
            }
        }
    }
}

@Composable
private fun SearchFilterCard(
    uiState: PhotoSearchUiState,
    onSiteSelected: (Site?) -> Unit,
    onStartDateChange: (Long?) -> Unit,
    onEndDateChange: (Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("筛选条件", style = MaterialTheme.typography.titleLarge)
            SearchSitePicker(
                value = uiState.selectedSiteName,
                sites = uiState.sites,
                onSelected = onSiteSelected
            )
            SearchDateField(
                label = "开始日期",
                value = uiState.startDate,
                onDateChange = onStartDateChange
            )
            SearchDateField(
                label = "结束日期",
                value = uiState.endDate,
                onDateChange = onEndDateChange
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
private fun SearchSitePicker(
    value: String,
    sites: List<Site>,
    onSelected: (Site?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = "工地",
            value = value.ifBlank { "全部工地" },
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
            Icon(Icons.Outlined.LocationCity, contentDescription = null)
            Text("选择工地")
        }
        TextButton(
            onClick = { onSelected(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("改回全部工地")
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择工地", style = MaterialTheme.typography.titleLarge) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sites) { site ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(site)
                                    showDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                site.siteName,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
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

@Composable
private fun SearchDateField(
    label: String,
    value: Long?,
    onDateChange: (Long?) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = label,
            value = value?.let(::formatDate).orEmpty(),
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = {
                val initialDate = millisToLocalDate(value ?: todayMillis())
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateChange(localDateToMillis(java.time.LocalDate.of(year, month + 1, day)))
                    },
                    initialDate.year,
                    initialDate.monthValue - 1,
                    initialDate.dayOfMonth
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Text("选择日期")
        }
        TextButton(
            onClick = { onDateChange(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("不限日期")
        }
    }
}

@Composable
private fun SearchResultBatchCard(
    batch: PhotoBatch,
    onOpenPreview: (PhotoBatch, Int) -> Unit
) {
    val cover = batch.items.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (cover != null) {
                    SearchGlideImage(model = cover.localPath, modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(batch.siteNameSnapshot, style = MaterialTheme.typography.titleLarge)
                Text(formatDate(batch.workDate), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "这一批一共 ${batch.items.size} 张",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                batch.remark?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onOpenPreview(batch, 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                    Text("查看这批照片")
                }
            }
        }
    }
}

@Composable
private fun SearchGlideImage(model: Any, modifier: Modifier = Modifier) {
    SearchGlideImage(
        model = model,
        modifier = modifier,
        scaleType = ImageView.ScaleType.CENTER_CROP,
        adjustViewBounds = false
    )
}

@Composable
private fun SearchPreviewGlideImage(model: Any, modifier: Modifier = Modifier) {
    SearchGlideImage(
        model = model,
        modifier = modifier,
        scaleType = ImageView.ScaleType.FIT_CENTER,
        adjustViewBounds = true
    )
}

@Composable
private fun SearchGlideImage(
    model: Any,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType,
    adjustViewBounds: Boolean
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            ImageView(it).apply {
                this.scaleType = scaleType
                this.adjustViewBounds = adjustViewBounds
            }
        },
        update = { view ->
            view.scaleType = scaleType
            view.adjustViewBounds = adjustViewBounds
            Glide.with(context)
                .load(model)
                .into(view)
        }
    )
}

@Composable
private fun SearchPreviewDialog(
    batch: PhotoBatch,
    initialIndex: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onDownload: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex) { batch.items.size }
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.94f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Text("${pagerState.currentPage + 1} / ${batch.items.size}", color = Color.White)
                IconButton(onClick = onDownload, enabled = !isExporting) {
                    Icon(Icons.Outlined.Download, contentDescription = "保存到相册", tint = Color.White)
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SearchPreviewGlideImage(
                        model = batch.items[page].localPath,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
