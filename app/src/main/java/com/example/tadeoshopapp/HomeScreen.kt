package com.example.tadeoshopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    cartViewModel: CartViewModel,
    initialTab: Int = 2
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                // Mensajes
                BottomNavigationItem(
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dog_logo),
                                contentDescription = stringResource(R.string.nav_messages),
                                modifier = Modifier.size(28.dp),
                                tint = if (selectedTab == 0) Color(0xFF00ACC1) else Color(0xFF999999)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_messages),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )

                // Perfil
                BottomNavigationItem(
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dog_logo),
                                contentDescription = stringResource(R.string.nav_profile),
                                modifier = Modifier.size(28.dp),
                                tint = if (selectedTab == 1) Color(0xFF00ACC1) else Color(0xFF999999)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_profile),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )

                // Productos (Marketplace menu)
                BottomNavigationItem(
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = stringResource(R.string.nav_products),
                                modifier = Modifier.size(28.dp),
                                tint = if (selectedTab == 2) Color(0xFF00ACC1) else Color(0xFF999999)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_products),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    selectedContentColor = Color(0xFF00ACC1),
                    unselectedContentColor = Color(0xFF999999)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> MessagesPlaceholderScreen()
                1 -> ProfileScreen(
                    onEditProfileClick = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onLogoutClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onViewProductsClick = {
                        navController.navigate(Screen.Products.route)
                    },
                    //Callbacks para órdenes
                    onMyPurchasesClick = {
                        navController.navigate(Screen.MyPurchases.route)
                    },
                    onMySalesClick = {
                        navController.navigate(Screen.MySales.route)
                    },
                    viewModel = viewModel
                )
                2 -> MarktekPlaceScreen(
                    navController = navController,
                    onCartClick = {
                        navController.navigate(Screen.Cart.route)
                    },
                    productViewModel = productViewModel,
                    cartViewModel = cartViewModel
                )
            }
        }
    }
}

@Composable
fun MessagesPlaceholderScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📬 Mensajes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Próximamente...",
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}