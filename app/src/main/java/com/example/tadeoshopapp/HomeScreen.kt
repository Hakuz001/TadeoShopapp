package com.example.tadeoshopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TadeoShop") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¡Bienvenido!",
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            currentUser?.let { user ->
                Text(text = "Nombre: ${user.nombres} ${user.apellidos}")
                Text(text = "Email: ${user.email}")
                Text(text = "Tipo: ${user.tipoUsuario}")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { viewModel.logout() }) {
                Text("Cerrar Sesión")
            }
        }
    }
}