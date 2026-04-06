package com.salaryapp.jigong.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.salaryapp.jigong.core.ui.theme.JiGongScreenBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    navigationIcon: ImageVector?,
    onNavigationClick: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background.copy(alpha = 0.98f),
                    titleContentColor = colors.onBackground,
                    navigationIconContentColor = colors.onBackground
                ),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = "返回"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JiGongScreenBrush(colors))
                .padding(innerPadding)
        ) {
            content(PaddingValues())
        }
    }
}
