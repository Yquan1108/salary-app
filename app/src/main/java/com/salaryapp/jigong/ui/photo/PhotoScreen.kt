@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.salaryapp.jigong.ui.photo

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.salaryapp.jigong.domain.model.PhotoBatch
import com.salaryapp.jigong.domain.model.PhotoItem
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.ConfirmDialog
import com.salaryapp.jigong.ui.common.EditorTextField
import com.salaryapp.jigong.ui.common.EmptyStateCard
import kotlinx.coroutines.delay

@Composable
fun PhotoRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JiGongApplication
    val viewModel: PhotoViewModel = viewModel(
        factory = PhotoViewModelFactory(
            app.appContainer.photoRepository,
            app.appContainer.siteRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.setPickedUris(uris)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) viewModel.setCapturedBitmap(bitmap)
    }

    LaunchedEffect(uiState.message, uiState.previewBatch) {
        if (uiState.previewBatch == null) {
            uiState.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeMessage()
            }
        }
    }

    PhotoScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDateSelected = { millis ->
            viewModel.updateUploadForm { current -> current.copy(workDate = millis) }
        },
        onSiteInput = { value ->
            viewModel.updateUploadForm { current ->
                val matched = uiState.sites.firstOrNull { it.siteName == value.trim() }
                current.copy(siteName = value, siteId = matched?.id)
            }
        },
        onSiteSelected = { site ->
            viewModel.updateUploadForm { current ->
                current.copy(siteId = site?.id, siteName = site?.siteName.orEmpty())
            }
        },
        onRemarkChange = { value ->
            viewModel.updateUploadForm { current -> current.copy(remark = value) }
        },
        onPickGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onTakePhoto = { cameraLauncher.launch(null) },
        onClearPending = viewModel::clearPendingPhotos,
        onSave = viewModel::savePendingBatch,
        onOpenPreview = viewModel::openPreview,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onShowDeleteConfirm = viewModel::showDeleteConfirm,
        onRequestDeleteBatch = viewModel::requestDeleteBatch,
        onDismissDelete = viewModel::dismissDeleteConfirm,
        onDeleteSelected = viewModel::deleteSelected,
        onClosePreview = viewModel::closePreview,
        onPreviewIndexChange = viewModel::updatePreviewIndex,
        onExportCurrent = viewModel::exportCurrentPhoto,
        onPreviewMessageShown = viewModel::consumeMessage
    )
}

@Composable
private fun PhotoScreen(
    uiState: PhotoUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onSiteInput: (String) -> Unit,
    onSiteSelected: (Site?) -> Unit,
    onRemarkChange: (String) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onClearPending: () -> Unit,
    onSave: () -> Unit,
    onOpenPreview: (PhotoBatch, Int) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onRequestDeleteBatch: (Long) -> Unit,
    onDismissDelete: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClosePreview: () -> Unit,
    onPreviewIndexChange: (Int) -> Unit,
    onExportCurrent: () -> Unit,
    onPreviewMessageShown: () -> Unit
) {
    AppScaffold(
        title = if (uiState.isSelectionMode) "已选 ${uiState.selectedBatchIds.size} 批" else "存工地照片",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = if (uiState.isSelectionMode) onClearSelection else onBack
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
                        HeaderCard(
                            title = "存工地照片",
                            subtitle = "拍照/上传，按工地归档"
                        )
                    }
                    item {
                        UploadFormCard(
                            uiState = uiState,
                            onDateSelected = onDateSelected,
                            onSiteInput = onSiteInput,
                            onSiteSelected = onSiteSelected,
                            onRemarkChange = onRemarkChange,
                            onPickGallery = onPickGallery,
                            onTakePhoto = onTakePhoto,
                            onClearPending = onClearPending,
                            onSave = onSave
                        )
                    }
                    item {
                        PhotoToolbar(
                            isSelectionMode = uiState.isSelectionMode,
                            selectedCount = uiState.selectedBatchIds.size,
                            onSelectAll = onSelectAll,
                            onDelete = onShowDeleteConfirm
                        )
                    }
                    if (uiState.batches.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "还没有存过照片",
                                description = "先拍照或从相册多选，填好工地和日期后，点“保存本次上传”。"
                            )
                        }
                    } else {
                        items(uiState.batches, key = { it.id }) { batch ->
                            PhotoBatchCard(
                                batch = batch,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = batch.id in uiState.selectedBatchIds,
                                onOpenPreview = onOpenPreview,
                                onLongPress = { onEnterSelectionMode(batch.id) },
                                onToggleSelection = { onToggleSelection(batch.id) },
                                onDelete = { onRequestDeleteBatch(batch.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.deleteConfirmCount?.let { count ->
        ConfirmDialog(
            title = "删除照片批次",
            text = "确认删除已选中的 $count 批照片吗？",
            confirmText = "确认删除",
            onConfirm = onDeleteSelected,
            onDismiss = onDismissDelete
        )
    }

    uiState.previewBatch?.let { batch ->
        PhotoPreviewDialog(
            batch = batch,
            initialIndex = uiState.previewIndex,
            isExporting = uiState.isExporting,
            feedbackMessage = uiState.message,
            onDismiss = onClosePreview,
            onPageChanged = onPreviewIndexChange,
            onDownload = onExportCurrent,
            onFeedbackShown = onPreviewMessageShown
        )
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String) {
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
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UploadFormCard(
    uiState: PhotoUiState,
    onDateSelected: (Long) -> Unit,
    onSiteInput: (String) -> Unit,
    onSiteSelected: (Site?) -> Unit,
    onRemarkChange: (String) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onClearPending: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("这次要存哪些照片", style = MaterialTheme.typography.titleLarge)
            DateField(date = uiState.uploadForm.workDate, onDateSelected = onDateSelected)
            SiteField(
                value = uiState.uploadForm.siteName,
                sites = uiState.sites,
                onValueChange = onSiteInput,
                onSelected = onSiteSelected
            )
            EditorTextField(
                label = "备注",
                value = uiState.uploadForm.remark,
                onValueChange = onRemarkChange
            )
            LargeActionButton(
                text = "拍照",
                icon = Icons.Outlined.PhotoCamera,
                filled = false,
                onClick = onTakePhoto
            )
            LargeActionButton(
                text = "相册多选",
                icon = Icons.Outlined.PhotoLibrary,
                filled = false,
                onClick = onPickGallery
            )
            PendingPhotosHint(
                pendingCount = uiState.pendingUris.size + if (uiState.pendingCameraBitmap != null) 1 else 0,
                onClearPending = onClearPending
            )
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text(if (uiState.isSaving) "正在保存..." else "保存本次上传")
            }
        }
    }
}

@Composable
private fun DateField(date: Long, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = "日期",
            value = formatDate(date),
            onValueChange = {},
            readOnly = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = {
                val localDate = millisToLocalDate(date)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected(localDateToMillis(java.time.LocalDate.of(year, month + 1, day)))
                    },
                    localDate.year,
                    localDate.monthValue - 1,
                    localDate.dayOfMonth
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
private fun SiteField(
    value: String,
    sites: List<Site>,
    onValueChange: (String) -> Unit,
    onSelected: (Site?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorTextField(
            label = "工地",
            value = value,
            onValueChange = onValueChange,
            required = true,
            emphasize = true
        )
        FilledTonalButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("从工地名单里选")
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
private fun PendingPhotosHint(pendingCount: Int, onClearPending: () -> Unit) {
    if (pendingCount <= 0) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("已选 $pendingCount 张待上传照片", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(
                onClick = onClearPending,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("清空这次选择")
            }
        }
    }
}

@Composable
private fun LargeActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
    if (filled) {
        Button(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text)
        }
    }
}

@Composable
private fun PhotoToolbar(
    isSelectionMode: Boolean,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (isSelectionMode) "已选 $selectedCount 批照片" else "已保存的照片",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                if (isSelectionMode) "先检查，再删除，避免误触。" else "长按一批照片可进入勾选模式。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSelectionMode) {
                LargeActionButton(
                    text = "全选",
                    icon = Icons.Outlined.SelectAll,
                    filled = false,
                    onClick = onSelectAll
                )
                LargeActionButton(
                    text = "删除已选照片",
                    icon = Icons.Outlined.Delete,
                    filled = true,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun PhotoBatchCard(
    batch: PhotoBatch,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpenPreview: (PhotoBatch, Int) -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit
) {
    val cover = batch.items.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection() else onOpenPreview(batch, 0)
                },
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (cover != null) {
                    PhotoThumbImage(model = cover.localPath, modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                if (isSelectionMode) {
                    IconButton(
                        onClick = onToggleSelection,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Icon(
                            if (isSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(batch.siteNameSnapshot, style = MaterialTheme.typography.titleLarge)
                Text(formatDate(batch.workDate), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "这一批一共 ${batch.photoCount} 张",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                batch.remark?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LargeActionButton(
                    text = "查看这批照片",
                    icon = Icons.Outlined.PhotoLibrary,
                    filled = false,
                    onClick = { onOpenPreview(batch, 0) }
                )
                if (!isSelectionMode) {
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
                        Text("删除这批照片")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbImage(model: Any, modifier: Modifier = Modifier) {
    GlideImage(
        model = model,
        modifier = modifier,
        scaleType = ImageView.ScaleType.CENTER_CROP,
        adjustViewBounds = false
    )
}

@Composable
private fun PreviewGlideImage(model: Any, modifier: Modifier = Modifier) {
    GlideImage(
        model = model,
        modifier = modifier,
        scaleType = ImageView.ScaleType.FIT_CENTER,
        adjustViewBounds = true
    )
}

@Composable
private fun GlideImage(
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
private fun PhotoPreviewDialog(
    batch: PhotoBatch,
    initialIndex: Int,
    isExporting: Boolean,
    feedbackMessage: String?,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onDownload: () -> Unit,
    onFeedbackShown: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex) { batch.items.size }
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(1800)
            onFeedbackShown()
        }
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
            feedbackMessage?.let {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PreviewGlideImage(
                        model = batch.items[page].localPath,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
