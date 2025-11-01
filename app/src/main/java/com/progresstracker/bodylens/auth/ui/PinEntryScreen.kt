package com.progresstracker.bodylens.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progresstracker.bodylens.auth.data.AuthRepository

/**
 * Screen for entering PIN to unlock the app
 */
@Composable
fun PinEntryScreen(
    onPinVerified: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    val currentPin by viewModel.currentPin.collectAsState()
    val pinState by viewModel.pinState.collectAsState()

    // Handle PIN state changes
    LaunchedEffect(pinState) {
        when (pinState) {
            is PinState.Success -> {
                onPinVerified()
            }

            is PinState.Error -> {
                // Error is shown in UI and PIN is cleared after delay
            }

            else -> {}
        }
    }

    // Auto-verify when PIN is complete
    LaunchedEffect(currentPin) {
        if (currentPin.length == AuthRepository.PIN_LENGTH) {
            kotlinx.coroutines.delay(100)
            viewModel.verifyPin(currentPin)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BodyLens",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Enter Your PIN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // PIN Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PinDisplay(
                    pinLength = AuthRepository.PIN_LENGTH,
                    currentLength = currentPin.length,
                    hasError = pinState is PinState.Error
                )

                // Error message
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pinState is PinState.Error) {
                        Text(
                            text = (pinState as PinState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Keypad
            PinKeypad(
                onNumberClick = { digit ->
                    if (pinState !is PinState.Loading) {
                        viewModel.addDigit(digit)
                    }
                },
                onBackspaceClick = {
                    if (pinState !is PinState.Loading) {
                        viewModel.removeDigit()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
