package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AppViewModelProvider
import com.example.ui.DirectoryScreen
import com.example.ui.DirectoryViewModel
import com.example.ui.NoteDetailViewModel
import com.example.ui.NoteScreen
import com.example.ui.theme.AppTheme

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as NoteMaxApplication
            val themeMode by app.settingsManager.themeMode.collectAsStateWithLifecycle()
            val useDynamicColor by app.settingsManager.useDynamicColor.collectAsStateWithLifecycle()
            val interfaceDensity by app.settingsManager.interfaceDensity.collectAsStateWithLifecycle()

            AppTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                interfaceDensity = interfaceDensity
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController, 
                        startDestination = "directory?folderId={folderId}",
                        enterTransition = {
                            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { 300 },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                        },
                        exitTransition = {
                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { -300 },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                        },
                        popEnterTransition = {
                            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { -300 },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                        },
                        popExitTransition = {
                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { 300 },
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                        }
                    ) {
                        composable(
                            route = "directory?folderId={folderId}",
                            arguments = listOf(navArgument("folderId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val folderIdString = backStackEntry.arguments?.getString("folderId")
                            val folderId = folderIdString?.toLongOrNull()
                            
                            val directoryViewModel: DirectoryViewModel = viewModel(
                                factory = AppViewModelProvider.factory(app.repository)
                            )
                            androidx.compose.runtime.LaunchedEffect(folderId) {
                                directoryViewModel.navigateToFolder(folderId)
                            }
                            
                            DirectoryScreen(
                                viewModel = directoryViewModel,
                                onNavigateToFolder = { newFolderId ->
                                    navController.navigate("directory?folderId=$newFolderId")
                                },
                                onNavigateUp = {
                                    navController.popBackStack()
                                },
                                onNavigateToNote = { noteId ->
                                    navController.navigate("note/$noteId")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable(
                            route = "note/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                            enterTransition = {
                                androidx.compose.animation.scaleIn(
                                    initialScale = 0.9f, 
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                )
                            },
                            exitTransition = {
                                androidx.compose.animation.scaleOut(
                                    targetScale = 0.9f, 
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                )
                            },
                            popEnterTransition = {
                                androidx.compose.animation.scaleIn(
                                    initialScale = 0.9f, 
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                )
                            },
                            popExitTransition = {
                                androidx.compose.animation.scaleOut(
                                    targetScale = 0.9f, 
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(
                                    animationSpec = androidx.compose.animation.core.tween(300)
                                )
                            }
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                            val noteViewModel: NoteDetailViewModel = viewModel(
                                factory = AppViewModelProvider.factory(app.repository)
                            )
                            androidx.compose.runtime.LaunchedEffect(noteId) {
                                noteViewModel.loadNote(noteId)
                            }
                            
                            NoteScreen(
                                viewModel = noteViewModel,
                                onNavigateUp = { navController.popBackStack() },
                                onNavigateToNote = { id -> navController.navigate("note/$id") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settingsManager = app.settingsManager,
                                onNavigateUp = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
