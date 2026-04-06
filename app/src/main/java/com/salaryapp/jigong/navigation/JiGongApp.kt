package com.salaryapp.jigong.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salaryapp.jigong.JiGongApplication
import com.salaryapp.jigong.core.ui.theme.JiGongTheme
import com.salaryapp.jigong.domain.model.FontScaleLevel
import com.salaryapp.jigong.ui.RootUiState
import com.salaryapp.jigong.ui.home.HomeScreen
import com.salaryapp.jigong.ui.onboarding.OnboardingScreen
import com.salaryapp.jigong.ui.photo.PhotoRoute
import com.salaryapp.jigong.ui.photo.PhotoSearchRoute
import com.salaryapp.jigong.ui.settings.SettingsRoute
import com.salaryapp.jigong.ui.site.SiteRoute
import com.salaryapp.jigong.ui.stats.SalaryStatsRoute
import com.salaryapp.jigong.ui.worker.WorkerRoute
import com.salaryapp.jigong.ui.workrecord.WorkRecordEditorRoute
import com.salaryapp.jigong.ui.workrecord.WorkRecordRoute

@Composable
fun JiGongApp(
    uiState: RootUiState,
    onFinishOnboarding: () -> Unit,
    onResetOnboarding: () -> Unit,
    onFontScaleChange: (FontScaleLevel) -> Unit
) {
    JiGongTheme(fontScaleLevel = uiState.fontScaleLevel) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@JiGongTheme
        }

        val app = LocalContext.current.applicationContext as JiGongApplication
        val navController = rememberNavController()
        val startDestination = if (uiState.hasCompletedOnboarding) {
            AppDestination.Home.route
        } else {
            AppDestination.Onboarding.route
        }

        LaunchedEffect(uiState.hasCompletedOnboarding) {
            if (uiState.hasCompletedOnboarding) {
                if (navController.currentDestination?.route == AppDestination.Onboarding.route) {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                    }
                }
            } else if (navController.currentDestination?.route != AppDestination.Onboarding.route) {
                navController.navigate(AppDestination.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(AppDestination.Onboarding.route) {
                OnboardingScreen(
                    onEnterApp = {
                        onFinishOnboarding()
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onWorkRecordClick = { navController.navigate(AppDestination.WorkRecord.route) },
                    onPhotoClick = { navController.navigate(AppDestination.Photo.route) },
                    onPhotoSearchClick = { navController.navigate(AppDestination.PhotoSearch.route) },
                    onSalaryStatsClick = { navController.navigate(AppDestination.SalaryStats.route) },
                    onWorkerClick = { navController.navigate(AppDestination.Worker.route) },
                    onSiteClick = { navController.navigate(AppDestination.Site.route) },
                    onSettingsClick = { navController.navigate(AppDestination.Settings.route) }
                )
            }
            composable(AppDestination.WorkRecord.route) { backStackEntry ->
                val justSaved = backStackEntry.savedStateHandle.get<Boolean>("work_record_saved") == true
                if (justSaved) {
                    backStackEntry.savedStateHandle["work_record_saved"] = false
                }
                WorkRecordRoute(
                    onBack = { navController.popBackStack() },
                    onAddClick = { navController.navigate(AppDestination.WorkRecord.editorRoute()) },
                    onEditClick = { id -> navController.navigate(AppDestination.WorkRecord.editorRoute(id)) },
                    justSaved = justSaved
                )
            }
            composable(
                route = AppDestination.WorkRecordEditor.route,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("saved") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId")?.takeIf { it >= 0 }
                WorkRecordEditorRoute(
                    recordId = recordId,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("work_record_saved", true)
                        navController.popBackStack()
                    }
                )
            }
            composable(AppDestination.Photo.route) {
                PhotoRoute(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.PhotoSearch.route) {
                PhotoSearchRoute(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.SalaryStats.route) {
                SalaryStatsRoute(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Worker.route) {
                WorkerRoute(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Site.route) {
                SiteRoute(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    currentFontScaleLevel = uiState.fontScaleLevel,
                    workerRepository = app.appContainer.workerRepository,
                    siteRepository = app.appContainer.siteRepository,
                    workRecordRepository = app.appContainer.workRecordRepository,
                    photoRepository = app.appContainer.photoRepository,
                    onBack = { navController.popBackStack() },
                    onFontScaleChange = onFontScaleChange,
                    onResetOnboarding = onResetOnboarding
                )
            }
        }
    }
}
