package com.example.tadeoshopapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    object Messages : Screen("messages") // Pantalla de mensajes
    object EditProfile : Screen("edit_profile")
    object Products : Screen("products") // Mis productos del vendedor
    object AddProduct : Screen("add_product") // Agregar producto
    object EditProduct : Screen("edit_product") // Editar producto
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
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

        composable(Screen.Messages.route) {
            MessagesScreen(
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

        // Pantalla de detalles del producto
        composable(Screen.ProductDetail.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val product = productViewModel.allProducts.value.find { it.id == productId }
            
            if (product != null) {
                ProductDetailScreen(
                    product = product,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onContactSeller = { sellerId ->
                        // Implementar funcionalidad de contacto
                        // Por ahora solo mostramos un mensaje
                    },
                    onNavigateToMessages = {
                        // Navegar a la pantalla de mensajes
                        navController.navigate(Screen.Messages.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    authViewModel = authViewModel
                )
            } else {
                // Mostrar pantalla de error si no se encuentra el producto
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Producto no encontrado",
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}