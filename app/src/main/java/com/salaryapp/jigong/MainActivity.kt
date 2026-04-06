package com.salaryapp.jigong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salaryapp.jigong.navigation.JiGongApp
import com.salaryapp.jigong.ui.RootViewModel
import com.salaryapp.jigong.ui.RootViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as JiGongApplication
            val viewModel: RootViewModel = viewModel(
                factory = RootViewModelFactory(app.appContainer.preferenceRepository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            JiGongApp(
                uiState = uiState,
                onFinishOnboarding = viewModel::finishOnboarding,
                onResetOnboarding = viewModel::resetOnboarding,
                onFontScaleChange = viewModel::updateFontScale
            )
        }
    }
}
