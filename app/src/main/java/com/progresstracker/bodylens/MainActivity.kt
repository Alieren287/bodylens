package com.progresstracker.bodylens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.progresstracker.bodylens.ai.ComparisonResultScreen
import com.progresstracker.bodylens.ai.ComparisonSelectionScreen
import com.progresstracker.bodylens.ai.ComparisonViewModel
import com.progresstracker.bodylens.auth.data.AuthRepository
import com.progresstracker.bodylens.auth.ui.PinEntryScreen
import com.progresstracker.bodylens.auth.ui.PinSetupScreen
import com.progresstracker.bodylens.gallery.PhotoViewerScreen
import com.progresstracker.bodylens.gallery.SessionDetailScreen
import com.progresstracker.bodylens.gallery.SessionDetailViewModel
import com.progresstracker.bodylens.gallery.SessionDetailViewModelFactory
import com.progresstracker.bodylens.home.HomeScreen
import com.progresstracker.bodylens.importphoto.PhotoImportScreen
import com.progresstracker.bodylens.navigation.Routes
import com.progresstracker.bodylens.session.ImprovedPhotoSessionScreen
import com.progresstracker.bodylens.settings.AddBodyPartScreen
import com.progresstracker.bodylens.settings.BodyPartsScreen
import com.progresstracker.bodylens.ui.theme.BodyLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyLensTheme {
                BodyLensApp()
            }
        }
    }
}

@Composable
fun BodyLensApp() {
    val navController = rememberNavController()

    // Determine starting destination based on PIN setup status
    val authRepository = AuthRepository(navController.context)
    val startDestination = if (authRepository.isPinConfigured()) {
        Routes.PIN_ENTRY
    } else {
        Routes.PIN_SETUP
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // PIN Setup Screen (first-time users)
        composable(Routes.PIN_SETUP) {
            PinSetupScreen(
                onPinSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PIN_SETUP) { inclusive = true }
                    }
                }
            )
        }

        // PIN Entry Screen (returning users)
        composable(Routes.PIN_ENTRY) {
            PinEntryScreen(
                onPinVerified = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PIN_ENTRY) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen (after authentication)
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.BODY_PARTS)
                },
                onStartSession = {
                    navController.navigate(Routes.PHOTO_SESSION)
                },
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.sessionDetail(sessionId))
                },
                onCompareClick = {
                    navController.navigate(Routes.COMPARISON_SELECTION)
                },
                onImportClick = {
                    navController.navigate(Routes.PHOTO_IMPORT)
                }
            )
        }

        // Body Parts Configuration Screen
        composable(Routes.BODY_PARTS) {
            BodyPartsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onAddBodyPart = {
                    navController.navigate(Routes.ADD_BODY_PART)
                }
            )
        }

        // Add Body Part Screen
        composable(Routes.ADD_BODY_PART) {
            AddBodyPartScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Photo Session Screen
        composable(Routes.PHOTO_SESSION) {
            ImprovedPhotoSessionScreen(
                onComplete = { sessionId ->
                    // Navigate back to home with session completed
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.navigateUp()
                }
            )
        }

        // Session Detail Screen
        composable(
            route = Routes.SESSION_DETAIL,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val viewModel: SessionDetailViewModel = viewModel(
                factory = SessionDetailViewModelFactory(
                    application = navController.context.applicationContext as android.app.Application,
                    sessionId = sessionId
                )
            )
            SessionDetailScreen(
                sessionId = sessionId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onPhotoClick = { photoIndex ->
                    navController.navigate(Routes.photoViewer(sessionId, photoIndex))
                }
            )
        }

        // Photo Viewer Screen
        composable(
            route = Routes.PHOTO_VIEWER,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("photoIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val photoIndex = backStackEntry.arguments?.getInt("photoIndex") ?: 0
            val viewModel: SessionDetailViewModel = viewModel(
                factory = SessionDetailViewModelFactory(
                    application = navController.context.applicationContext as android.app.Application,
                    sessionId = sessionId
                )
            )
            PhotoViewerScreen(
                sessionId = sessionId,
                photoIndex = photoIndex,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Comparison Selection Screen
        composable(Routes.COMPARISON_SELECTION) {
            val viewModel: ComparisonViewModel = viewModel()
            ComparisonSelectionScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onCompareClick = {
                    navController.navigate(Routes.COMPARISON_RESULT)
                },
                viewModel = viewModel
            )
        }

        // Comparison Result Screen
        composable(Routes.COMPARISON_RESULT) {
            // Share the same ViewModel instance from the selection screen
            val parentEntry = navController.getBackStackEntry(Routes.COMPARISON_SELECTION)
            val viewModel: ComparisonViewModel = viewModel(parentEntry)
            ComparisonResultScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                viewModel = viewModel
            )
        }

        // Photo Import Screen
        composable(Routes.PHOTO_IMPORT) {
            PhotoImportScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}