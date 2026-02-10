package com.yourapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourapp.features.auth.LoginScreen
import com.yourapp.features.auth.SignupScreen
import com.yourapp.ui.screens.ChatScreen
import com.yourapp.ui.viewmodel.ChatViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.Login
) {
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Login) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.Chat) },
                onSignupClick = { navController.navigate(Routes.Signup) }
            )
        }
        composable(Routes.Signup) {
            SignupScreen(
                onSignupComplete = { navController.navigate(Routes.Chat) },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.Chat) {
            ChatScreen(viewModel = chatViewModel)
        }
    }
}
