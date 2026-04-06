package com.salaryapp.jigong.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salaryapp.jigong.data.repository.PhotoRepository
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.data.repository.WorkRecordRepository
import com.salaryapp.jigong.data.repository.WorkerRepository
import com.salaryapp.jigong.domain.model.FontScaleLevel
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.PrimaryInfoCard

@Composable
fun SettingsRoute(
    currentFontScaleLevel: FontScaleLevel,
    workerRepository: WorkerRepository,
    siteRepository: SiteRepository,
    workRecordRepository: WorkRecordRepository,
    photoRepository: PhotoRepository,
    onBack: () -> Unit,
    onFontScaleChange: (FontScaleLevel) -> Unit,
    onResetOnboarding: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            workerRepository = workerRepository,
            siteRepository = siteRepository,
            workRecordRepository = workRecordRepository,
            photoRepository = photoRepository
        )
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    SettingsScreen(
        currentFontScaleLevel = currentFontScaleLevel,
        dataOverview = uiState,
        onBack = onBack,
        onFontScaleChange = onFontScaleChange,
        onResetOnboarding = onResetOnboarding
    )
}

@Composable
fun SettingsScreen(
    currentFontScaleLevel: FontScaleLevel,
    dataOverview: SettingsUiState,
    onBack: () -> Unit,
    onFontScaleChange: (FontScaleLevel) -> Unit,
    onResetOnboarding: () -> Unit
) {
    AppScaffold(
        title = "设置",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("字大小、数据多少，都在这里看", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "所有设置都会马上生效，页面会跟着一起变大或变小。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("数据一览", style = MaterialTheme.typography.titleLarge)
                    SettingsSummaryCard(label = "工人数量", value = dataOverview.workerCount.toString())
                    SettingsSummaryCard(label = "工地数量", value = dataOverview.siteCount.toString())
                    SettingsSummaryCard(label = "记工记录", value = dataOverview.workRecordCount.toString())
                    SettingsSummaryCard(label = "照片批次", value = dataOverview.photoBatchCount.toString())
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("字大小", style = MaterialTheme.typography.titleLarge)
                }
            }
            items(FontScaleLevel.entries) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFontScaleChange(item) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = when (item) {
                                    FontScaleLevel.STANDARD -> "默认大小"
                                    FontScaleLevel.LARGE -> "更容易看清"
                                    FontScaleLevel.EXTRA_LARGE -> "适合需要超大字"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = currentFontScaleLevel == item,
                            onClick = { onFontScaleChange(item) }
                        )
                    }
                }
            }
            item {
                PrimaryInfoCard(
                    icon = Icons.Outlined.Refresh,
                    title = "重新看引导",
                    description = "重新打开第一次使用时的说明页，数据不会丢。",
                    onClick = onResetOnboarding
                )
            }
            item {
                PrimaryInfoCard(
                    icon = Icons.Outlined.Storage,
                    title = "数据保存说明",
                    description = "记工、照片和设置都保存在本机里，导出文件会写到系统相册或下载目录。",
                    onClick = {}
                )
            }
            item {
                PrimaryInfoCard(
                    icon = Icons.Outlined.Info,
                    title = "应用版本",
                    description = "JiGong Salary App 1.0 (1)",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun SettingsSummaryCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
