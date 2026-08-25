package com.anxietywatch.mobile.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Se muestra un instante arriba de TokenEntryScreen cuando el token vence --
 * para que la persona entienda POR QUÉ la mandamos de vuelta al inicio, en vez de que
 * parezca que la app se cerró sola sin explicación.
 */
@Composable
fun SessionExpiredMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Tu sesión expiró",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "Por seguridad, tu sesión ya no es válida. Pide un código nuevo a quien te lo compartió.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
