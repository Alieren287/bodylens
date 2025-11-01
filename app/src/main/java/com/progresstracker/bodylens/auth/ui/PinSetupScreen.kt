package com.progresstracker.bodylens.auth.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progresstracker.bodylens.auth.data.AuthRepository

/**
 * Screen for setting up a new PIN (first-time users)
 */
@Composable
fun PinSetupScreen(
    onPinSetupComplete: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    var setupStep by remember { mutableStateOf(SetupStep.ENTER_PIN) }
    var firstPin by remember { mutableStateOf("") }
    val currentPin by viewModel.currentPin.collectAsState()
    val pinState by viewModel.pinState.collectAsState()

    // Handle PIN state changes
    LaunchedEffect(pinState) {
        when (pinState) {
            is PinState.Success -> {
                onPinSetupComplete()
            }

            is PinState.Error -> {
                // Error is shown in UI
            }

            else -> {}
        }
    }

    // Auto-advance when PIN is complete
    LaunchedEffect(currentPin) {
        if (currentPin.length == AuthRepository.PIN_LENGTH) {
            when (setupStep) {
                SetupStep.ENTER_PIN -> {
                    kotlinx.coroutines.delay(200)
                    firstPin = currentPin
                    viewModel.clearPin()
                    setupStep = SetupStep.CONFIRM_PIN
                }

                SetupStep.CONFIRM_PIN -> {
                    kotlinx.coroutines.delay(200)
                    viewModel.setupPin(firstPin, currentPin)
                }
            }
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
                    text = when (setupStep) {
                        SetupStep.ENTER_PIN -> "Create Your PIN"
                        SetupStep.CONFIRM_PIN -> "Confirm Your PIN"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Secure your progress photos with a 4-digit PIN",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
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
                if (pinState is PinState.Error) {
                    Text(
                        text = (pinState as PinState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
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

/**
 * Steps in PIN setup process
 */
private enum class SetupStep {
    ENTER_PIN,
    CONFIRM_PIN
}
