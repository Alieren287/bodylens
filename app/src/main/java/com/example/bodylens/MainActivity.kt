package com.example.bodylens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.bodylens.ui.navigation.Screen
import com.example.bodylens.ui.screens.*
import com.example.bodylens.ui.theme.BodyLensTheme
import com.example.bodylens.ui.viewmodel.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BodyLensNavigation()
                }
            }
        }
    }
}

@Composable
fun BodyLensNavigation() {
    val app = BodyLensApp.instance
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    // Initialize database with a default passphrase (from PIN)
    // In a production app, this would be derived from the user's PIN
    LaunchedEffect(Unit) {
        scope.launch {
            val isPinSet = app.userPreferences.isPinSet.first()
            if (isPinSet) {
                // Initialize with a derived key from PIN
                // For now, using a default passphrase
                app.initializeDatabase("bodylens_default_key_2024")
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(app.userPreferences)
            )
            
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    // Initialize database after authentication
                    app.initializeDatabase("bodylens_default_key_2024")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            app.repository?.let { repository ->
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(repository)
                )
                
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCamera = { entryId ->
                        navController.navigate(Screen.Camera.createRoute(entryId))
                    },
                    onNavigateToProgress = { entryId ->
                        navController.navigate(Screen.Progress.createRoute(entryId))
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.AIInsights.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
        }
        
        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            
            app.repository?.let { repository ->
                val cameraViewModel: CameraViewModel = viewModel(
                    factory = CameraViewModelFactory(repository, app.userPreferences)
                )
                
                // Reset captured photos when entering camera screen with new entry
                LaunchedEffect(entryId) {
                    cameraViewModel.resetCapturedPhotos()
                }
                
                CameraScreen(
                    entryId = entryId,
                    viewModel = cameraViewModel,
                    onNavigateBack = { 
                        cameraViewModel.resetCapturedPhotos()
                        navController.popBackStack()
                    },
                    onComplete = { 
                        cameraViewModel.resetCapturedPhotos()
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(
            route = Screen.Progress.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            
            app.repository?.let { repository ->
                val progressViewModel: ProgressViewModel = viewModel(
                    factory = ProgressViewModelFactory(repository)
                )
                
                ProgressDetailScreen(
                    entryId = entryId,
                    viewModel = progressViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        composable(Screen.AIInsights.route) {
            app.repository?.let { repository ->
                val aiInsightsViewModel: AIInsightsViewModel = viewModel(
                    factory = AIInsightsViewModelFactory(repository, app.aiEngine)
                )
                
                AIInsightsScreen(
                    viewModel = aiInsightsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    app.closeDatabase()
                    scope.launch {
                        app.userPreferences.clearAllData()
                        app.encryptedFileManager.deleteAllFiles()
                    }
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ViewModelFactories
class AuthViewModelFactory(
    private val userPreferences: com.example.bodylens.data.preferences.UserPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(userPreferences) as T
    }
}

class HomeViewModelFactory(
    private val repository: com.example.bodylens.data.repository.ProgressRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}

class CameraViewModelFactory(
    private val repository: com.example.bodylens.data.repository.ProgressRepository,
    private val userPreferences: com.example.bodylens.data.preferences.UserPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CameraViewModel(repository, userPreferences) as T
    }
}

class ProgressViewModelFactory(
    private val repository: com.example.bodylens.data.repository.ProgressRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProgressViewModel(repository) as T
    }
}

class AIInsightsViewModelFactory(
    private val repository: com.example.bodylens.data.repository.ProgressRepository,
    private val aiEngine: com.example.bodylens.ai.AIAnalysisEngine
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AIInsightsViewModel(repository, aiEngine) as T
    }
}