package com.example.tadeoshopapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    object Messages : Screen("messages")
    object EditProfile : Screen("edit_profile")
    object Products : Screen("products")
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product")
    object Cart : Screen("cart")
    object CheckoutSuccess : Screen("checkout_success")
    object MyPurchases : Screen("my_purchases") // ⭐ NUEVO
    object MySales : Screen("my_sales") // ⭐ NUEVO
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModels compartidos
    val authViewModel: AuthViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val ordersViewModel: OrdersViewModel = viewModel() // ⭐ NUEVO

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
                productViewModel = productViewModel,
                cartViewModel = cartViewModel,
                initialTab = 2
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

        composable(Screen.EditProduct.route) {
            val selectedProduct by productViewModel.selectedProduct.collectAsState()

            if (selectedProduct != null) {
                EditProductScreen(
                    product = selectedProduct!!,
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

        composable(Screen.Cart.route) {
            CartScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onCheckoutSuccess = {
                    navController.navigate(Screen.CheckoutSuccess.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                },
                cartViewModel = cartViewModel
            )
        }

        composable(Screen.CheckoutSuccess.route) {
            CheckoutSuccessScreen(
                onBackToMenu = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onContactSeller = {
                    navController.navigate(Screen.Messages.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                cartViewModel = cartViewModel
            )
        }

        // ⭐ NUEVA PANTALLA - Mis Compras
        composable(Screen.MyPurchases.route) {
            MyPurchasesScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                ordersViewModel = ordersViewModel
            )
        }

        // ⭐ NUEVA PANTALLA - Mis Ventas
        composable(Screen.MySales.route) {
            MySalesScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                ordersViewModel = ordersViewModel
            )
        }

        composable(Screen.ProductDetail.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val allProducts by productViewModel.allProducts.collectAsState()
            val product = allProducts.find { it.id == productId }

            if (product != null) {
                ProductDetailScreen(
                    product = product,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onContactSeller = { sellerId ->
                        navController.navigate(Screen.Messages.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    onNavigateToMessages = {
                        navController.navigate(Screen.Messages.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    authViewModel = authViewModel,
                    cartViewModel = cartViewModel
                )
            } else {
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