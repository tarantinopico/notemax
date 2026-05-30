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
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application as NoteMaxApplication
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "directory?folderId={folderId}") {
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
                            // Load the target folder when composable enters
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
                                }
                            )
                        }

                        composable(
                            route = "note/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
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
                                onNavigateUp = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
