package com.progresstracker.bodylens.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIN keypad component with number pad and backspace
 */
@Composable
fun PinKeypad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Numbers 1-9
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                for (col in 1..3) {
                    val number = (row * 3 + col).toString()
                    NumberButton(
                        number = number,
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }

        // Bottom row: empty, 0, backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.size(72.dp))

            NumberButton(
                number = "0",
                onClick = { onNumberClick("0") }
            )

            FilledIconButton(
                onClick = onBackspaceClick,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace"
                )
            }
        }
    }
}

/**
 * Individual number button
 */
@Composable
private fun NumberButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * PIN dots display
 */
@Composable
fun PinDisplay(
    pinLength: Int,
    currentLength: Int,
    modifier: Modifier = Modifier,
    hasError: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(pinLength) { index ->
            val isFilled = index < currentLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(if (isFilled) 16.dp else 12.dp),
                    shape = CircleShape,
                    color = when {
                        hasError -> MaterialTheme.colorScheme.error
                        isFilled -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                ) {}
            }
        }
    }
}
