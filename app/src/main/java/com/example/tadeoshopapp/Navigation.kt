package com.example.tadeoshopapp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tadeoshopapp.ui.theme.AddProductScreen
import com.example.tadeoshopapp.ui.theme.EditProductScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object EditProfile : Screen("edit_profile")
    object Products : Screen("products") // Mis productos del vendedor
    object AddProduct : Screen("add_product") // Agregar producto
    object EditProduct : Screen("edit_product") // Editar producto
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = authViewModel,
                productViewModel = productViewModel
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        // Pantalla de productos del vendedor (Mis Productos)
        composable(Screen.Products.route) {
            ProductsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onAddProductClick = {
                    navController.navigate(Screen.AddProduct.route)
                },
                onEditProductClick = { product ->
                    navController.navigate(Screen.EditProduct.route)
                },
                authViewModel = authViewModel,
                productViewModel = productViewModel
            )
        }

        // Pantalla para agregar producto
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPublishSuccess = {
                    navController.popBackStack()
                },
                authViewModel = authViewModel,
                productViewModel = productViewModel
            )
        }

        // Pantalla para editar producto
        composable(Screen.EditProduct.route) {
            val selectedProduct = productViewModel.selectedProduct.value
            if (selectedProduct != null) {
                EditProductScreen(
                    product = selectedProduct,
                    onBackClick = {
                        productViewModel.clearSelectedProduct()
                        navController.popBackStack()
                    },
                    onSaveSuccess = {
                        productViewModel.clearSelectedProduct()
                        productViewModel.loadMyProducts()
                        navController.popBackStack()
                    },
                    authViewModel = authViewModel,
                    productViewModel = productViewModel
                )
            }
        }
    }
}