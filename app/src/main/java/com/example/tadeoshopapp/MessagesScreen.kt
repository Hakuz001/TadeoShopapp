package com.example.tadeoshopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController


// esto es temporal hasta comenzar el modulo de mensajes no tocar muchachos

@Composable
fun MessagesScreen(
    navController: NavHostController,
    viewModel: AuthViewModel,
    productViewModel: ProductViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mensajes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                backgroundColor = Color.White,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dog_logo),
                contentDescription = "Mensajes",
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF00ACC1)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "📬 Mensajes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Próximamente...",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        }
    }
}