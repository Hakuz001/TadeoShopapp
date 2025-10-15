package com.example.tadeoshopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                title = { Text(stringResource(R.string.home_title)) }
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
                text = stringResource(R.string.welcome_message),
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            currentUser?.let { user ->
                Text(text = stringResource(R.string.name_label, user.nombres, user.apellidos))
                Text(text = stringResource(R.string.email_label_display, user.email))
                Text(text = stringResource(R.string.user_type_label_display, user.tipoUsuario))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { viewModel.logout() }) {
                Text(stringResource(R.string.logout_button))
            }
        }
    }
}