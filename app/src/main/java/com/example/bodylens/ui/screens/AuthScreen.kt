package com.example.bodylens.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bodylens.ui.viewmodel.AuthState
import com.example.bodylens.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // App Icon and Title
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "BodyLens",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auth state message
            when (authState) {
                is AuthState.FirstLaunch, is AuthState.SetupRequired -> {
                    Text(
                        text = "Set up your 4-digit PIN (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Or skip to continue without PIN protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.ConfirmPin -> {
                    Text(
                        text = "Confirm your PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.PinMismatch -> {
                    Text(
                        text = "PINs don't match. Try again.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.resetPinMismatch()
                    }
                }
                is AuthState.Locked -> {
                    Text(
                        text = "Enter your PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.InvalidPin -> {
                    Text(
                        text = "Incorrect PIN. Try again.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.resetInvalidPin()
                    }
                }
                else -> {}
            }
            
            // PIN dots display
            PinDotsDisplay(pinLength = pinInput.length)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // PIN number pad
            PinNumberPad(
                onNumberClick = { viewModel.onPinDigitEntered(it) },
                onBackspaceClick = { viewModel.onPinBackspace() }
            )
            
            // Skip PIN button (only show during setup)
            if (authState is AuthState.FirstLaunch || authState is AuthState.SetupRequired) {
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { viewModel.skipPinSetup() },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Skip PIN Setup")
                }
            }
        }
    }
}

@Composable
fun PinDotsDisplay(pinLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pinLength)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun PinNumberPad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rows 1-3
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 1..3) {
                    val number = row * 3 + col
                    PinButton(
                        text = number.toString(),
                        onClick = { onNumberClick(number.toString()) }
                    )
                }
            }
        }
        
        // Bottom row with 0 and backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(80.dp))
            
            PinButton(
                text = "0",
                onClick = { onNumberClick("0") }
            )
            
            IconButton(
                onClick = onBackspaceClick,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardBackspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

