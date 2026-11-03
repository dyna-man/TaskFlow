package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BottomNavigationBar
import com.example.ui.components.ModernNotificationBanner
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusModeDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun TaskFlowNavGraph(
    viewModel: TaskViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "loading"
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationEvent by viewModel.notificationEvent.collectAsStateWithLifecycle()

    var showFocusDialog by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute in listOf("home", "today", "calendar", "insights")

    if (showFocusDialog) {
        FocusModeDialog(
            viewModel = viewModel,
            onDismiss = { showFocusDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = {
                        navController.navigate("add_task")
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "loading"
            ) {
                composable("loading") {
                    LoadingScreen(
                        themeMode = themeMode,
                        onLoadingComplete = {
                            navController.navigate("home") {
                                popUpTo("loading") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToToday = {
                            navController.navigate("today") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onStartFocus = {
                            viewModel.startFocusSession()
                            showFocusDialog = true
                        }
                    )
                }

                composable("today") {
                    TodayScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onStartFocus = {
                            viewModel.startFocusSession()
                            showFocusDialog = true
                        }
                    )
                }

                composable("add_task") {
                    NewTaskScreen(
                        viewModel = viewModel,
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("calendar") {
                    CalendarScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onQuickAdd = {
                            navController.navigate("add_task")
                        }
                    )
                }

                composable("insights") {
                    InsightsScreen(
                        viewModel = viewModel
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onPreviewLoading = {
                            navController.navigate("loading")
                        }
                    )
                }
            }

            ModernNotificationBanner(
                event = notificationEvent,
                onDismiss = { viewModel.dismissNotification() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
