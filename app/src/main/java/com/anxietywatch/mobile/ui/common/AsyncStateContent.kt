package com.anxietywatch.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed interface AsyncUiState<out T> {
    data object Loading : AsyncUiState<Nothing>
    data object Empty : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String) : AsyncUiState<Nothing>
}

@Composable
fun LoadingState(message: String = "Cargando...", modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    StateCard(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        title = title,
        message = message,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    StateCard(
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.primary,
        title = title,
        message = description.orEmpty(),
        modifier = modifier,
    )
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    StateCard(
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = "No pudimos cargar la información",
        message = message,
        modifier = modifier,
        action = onRetry?.let { retry ->
            { Button(onClick = retry) { Text("Reintentar") } }
        },
    )
}

@Composable
private fun StateCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    modifier: Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp), tint = iconTint)
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                action?.invoke()
            }
        }
    }
}
