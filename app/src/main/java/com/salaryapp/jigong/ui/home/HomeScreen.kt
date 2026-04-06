package com.salaryapp.jigong.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salaryapp.jigong.core.ui.theme.cardElevation
import com.salaryapp.jigong.ui.common.AppScaffold
import com.salaryapp.jigong.ui.common.PrimaryInfoCard
import com.salaryapp.jigong.ui.common.SectionDescription

private data class HomeCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val accent: Color,
    val primary: Boolean,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onWorkRecordClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onPhotoSearchClick: () -> Unit,
    onSalaryStatsClick: () -> Unit,
    onWorkerClick: () -> Unit,
    onSiteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val cards = listOf(
        HomeCard(
            title = "记今天的做工",
            description = "新增和查看每天做工情况，打开就能记。",
            icon = Icons.Outlined.Today,
            tint = colors.onPrimary,
            accent = colors.primary,
            primary = true,
            onClick = onWorkRecordClick
        ),
        HomeCard(
            title = "存工地照片",
            description = "拍照或从相册选图，按工地存好。",
            icon = Icons.Outlined.PhotoCamera,
            tint = colors.onPrimary,
            accent = colors.primary,
            primary = true,
            onClick = onPhotoClick
        ),
        HomeCard(
            title = "找工地照片",
            description = "按工地、日期、备注快速查。",
            icon = Icons.Outlined.ImageSearch,
            tint = colors.onPrimary,
            accent = colors.primary,
            primary = true,
            onClick = onPhotoSearchClick
        ),
        HomeCard(
            title = "看工资汇总",
            description = "按工地和时间看总数，也能导出。",
            icon = Icons.Outlined.Payments,
            tint = colors.onPrimary,
            accent = colors.primary,
            primary = true,
            onClick = onSalaryStatsClick
        ),
        HomeCard(
            title = "工人名单",
            description = "把常用工人名字存下来，后面直接选。",
            icon = Icons.Outlined.Badge,
            tint = colors.secondary,
            accent = colors.secondaryContainer,
            primary = false,
            onClick = onWorkerClick
        ),
        HomeCard(
            title = "工地名单",
            description = "把工地名称存下来，记工和照片都能用。",
            icon = Icons.Outlined.LocationCity,
            tint = colors.secondary,
            accent = colors.secondaryContainer,
            primary = false,
            onClick = onSiteClick
        ),
        HomeCard(
            title = "设置",
            description = "调字大小，查看数据，重新看引导。",
            icon = Icons.Outlined.Settings,
            tint = colors.secondary,
            accent = colors.secondaryContainer,
            primary = false,
            onClick = onSettingsClick
        )
    )

    AppScaffold(title = "首页", navigationIcon = null, onNavigationClick = null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HomeHero()
            }
            items(cards) { card ->
                if (card.primary) {
                    MainFunctionCard(card = card)
                } else {
                    PrimaryInfoCard(
                        icon = card.icon,
                        title = card.title,
                        description = card.description,
                        onClick = card.onClick,
                        tintColor = card.tint,
                        accentColor = card.accent
                    )
                }
            }
            item {
                SectionDescription("所有入口都改成单列大按钮，按从上到下的顺序找就行，不用来回切换。")
            }
        }
    }
}

@Composable
private fun HomeHero() {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.98f)),
        elevation = cardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "记工、存照片、查工资，都在这里",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground
            )
            Text(
                text = "界面按大字、大按钮和单列顺序整理过，拿起手机就能直接用。",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MainFunctionCard(card: HomeCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .clickable(onClick = card.onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = card.accent),
        elevation = cardElevation()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(68.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = card.tint,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = card.tint
                )
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = card.tint.copy(alpha = 0.92f)
                )
            }
        }
    }
}
