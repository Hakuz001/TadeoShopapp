package com.example.tadeoshopapp

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNextClick = {
                    // Aquí es para que pasemos al siguiente modulo
                    // navController.navigate(Screen.NextScreen.route)
                }
            )
        }
    }
}