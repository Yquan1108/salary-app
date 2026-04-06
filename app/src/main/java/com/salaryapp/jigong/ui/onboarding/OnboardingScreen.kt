package com.salaryapp.jigong.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
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
import com.salaryapp.jigong.core.ui.theme.JiGongHeroBrush

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun OnboardingScreen(
    onEnterApp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val pages = listOf(
        OnboardingPage(
            title = "先记做工",
            description = "每天记一笔，后面查工资更省事。",
            icon = Icons.Outlined.Construction,
            accent = colorScheme.primary
        ),
        OnboardingPage(
            title = "再存照片",
            description = "拍照或上传后，按工地存好，回头一找就有。",
            icon = Icons.Outlined.PhotoLibrary,
            accent = colorScheme.secondary
        ),
        OnboardingPage(
            title = "最后看汇总",
            description = "选人、选工地、选日期，就能查工资和导出表格。",
            icon = Icons.Outlined.Assessment,
            accent = colorScheme.tertiary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.98f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JiGongHeroBrush(colorScheme))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "记工、存照片、查工资，都很简单",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "按下面顺序用就行，不用学习复杂操作。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            pages.forEachIndexed { index, item ->
                StepCard(
                    step = index + 1,
                    item = item
                )
            }

            Button(
                onClick = onEnterApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("开始使用")
            }
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    item: OnboardingPage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(item.accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text("第 $step 步：${item.title}", style = MaterialTheme.typography.titleLarge)
            Text(
                item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
