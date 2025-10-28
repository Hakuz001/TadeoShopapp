package com.example.tadeoshopapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CheckoutSuccessScreen(
    onBackToMenu: () -> Unit,
    onContactSeller: () -> Unit,
    cartViewModel: CartViewModel = viewModel()
) {
    val createdOrders by cartViewModel.createdOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = stringResource(R.string.checkout_success_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Círculo con mascota e íconos
        Surface(
            modifier = Modifier.size(250.dp),
            shape = RoundedCornerShape(125.dp),
            color = Color.White,
            elevation = 12.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(40.dp)
            ) {
                //  mascota de TadeoShop
                // Por ahora uso el ícono del logo
                Icon(
                    painter = painterResource(id = R.drawable.ic_dog_logo),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    tint = Color(0xFF00ACC1)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mensaje de confirmación
        Text(
            text = stringResource(R.string.checkout_success_message),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Descripción
        Text(
            text = stringResource(R.string.checkout_success_description),
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Botón contactar vendedor
        Button(
            onClick = onContactSeller,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF00ACC1)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = stringResource(R.string.contact_seller_button),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón volver al menú
        OutlinedButton(
            onClick = {
                cartViewModel.clearCreatedOrders()
                onBackToMenu()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF00ACC1)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = stringResource(R.string.back_to_menu_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}