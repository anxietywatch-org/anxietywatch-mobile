package com.anxietywatch.mobile.ui.home

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home del paciente (E06), portado 1:1 desde el HTML/CSS real del Stitch
 * (home_paciente_ritmo_cardiaco_con_respiracion/code.html) -- mismos colores exactos,
 * misma jerarquia, mismo texto. [state] llega vacio/mock por ahora -- se conecta a datos
 * reales de telemetria cuando construyamos el repositorio que lee los lotes que ya sube
 * PhoneDataLayerListenerService.
 */
data class HomePatientUiState(
    val bpm: Int = 70,
    val statusLabel: String = "Estado: Normal",
    val statusMessage: String = "Tu ritmo cardíaco es estable. Estás haciendo un gran " +
        "trabajo manteniendo la calma hoy.",
    val breathingRate: Int = 14,
    val sleepHours: Double = 7.5,
)

// Tonos exactos del mockup que no son parte de la paleta M3 base -- el propio diseño los
// usa como overrides directos (bg-[#EBDBD3], bg-[#8F917C], etc.), asi que los replico igual.
private val HeroCardBackground = Color(0xFFEBDBD3)
private val StatusPillBackground = Color(0xFF8F917C)
private val RelajarmeButtonBackground = Color(0xFFD0BEA3)
private val SecondaryButtonBackground = Color(0xFFEBDBD3)

@Composable
fun HomePatientScreen(
    state: HomePatientUiState = HomePatientUiState(),
    onRelajarmeClick: () -> Unit = {},
    onHistorialClick: () -> Unit = {},
    onAjustesClick: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            HeartRateHeroCard(state)
            Spacer(Modifier.height(20.dp))
            QuickInsightsRow(state)
            Spacer(Modifier.height(20.dp))
            QuickActionsSection(onRelajarmeClick, onHistorialClick, onAjustesClick)
            Spacer(Modifier.height(20.dp))
            BitacoraRecienteCard()
            Spacer(Modifier.height(88.dp)) // deja aire sobre la barra inferior
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                "AnxietyWatch",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            Icons.Filled.Notifications,
            contentDescription = "Notificaciones",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeartRateHeroCard(state: HomePatientUiState) {
    Surface(
        color = HeroCardBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Frecuencia Cardiaca",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            BreathingRing(bpm = state.bpm)
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .background(StatusPillBackground, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(state.statusLabel, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Las 3 capas concentricas que "respiran" (animation: pulse 4s infinite en el CSS
 * original, con delays escalonados de 0s/0.5s/1s) + el circulo blanco central con el BPM.
 */
@Composable
private fun BreathingRing(bpm: Int) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scaleOuter by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale-outer",
    )

    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scaleOuter; scaleY = scaleOuter }
                .clip(CircleShape)
                .background(StatusPillBackground.copy(alpha = 0.20f)),
        )
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(StatusPillBackground.copy(alpha = 0.30f)),
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(StatusPillBackground.copy(alpha = 0.40f)),
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$bpm",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "BPM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickInsightsRow(state: HomePatientUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        InsightCard(
            icon = Icons.Filled.Air,
            label = "Respiración",
            value = "${state.breathingRate}",
            unit = "rpm",
            modifier = Modifier.weight(1f),
        )
        InsightCard(
            icon = Icons.Filled.Bedtime,
            label = "Sueño",
            value = "${state.sleepHours}",
            unit = "hrs",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onRelajarmeClick: () -> Unit,
    onHistorialClick: () -> Unit,
    onAjustesClick: () -> Unit,
) {
    Column {
        Text(
            "ACCIONES RÁPIDAS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        QuickActionButton(
            icon = Icons.Filled.SelfImprovement,
            label = "Relajarme",
            background = RelajarmeButtonBackground,
            onClick = onRelajarmeClick,
        )
        Spacer(Modifier.height(12.dp))
        QuickActionButton(
            icon = Icons.Filled.HistoryEdu,
            label = "Historial",
            background = SecondaryButtonBackground,
            onClick = onHistorialClick,
        )
        Spacer(Modifier.height(12.dp))
        QuickActionButton(
            icon = Icons.Filled.Settings,
            label = "Ajustes",
            background = SecondaryButtonBackground,
            onClick = onAjustesClick,
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF1F1F1F),
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(label, style = MaterialTheme.typography.headlineLarge, color = Color(0xFF1F1F1F))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF1F1F1F))
        }
    }
}

@Composable
private fun BitacoraRecienteCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Bitácora Reciente", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Ver todo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            BitacoraItem(
                dotColor = MaterialTheme.colorScheme.tertiary,
                title = "Respiración Guiada",
                subtitle = "Hoy, 10:30 AM • 5 min",
            )
            Spacer(Modifier.height(12.dp))
            BitacoraItem(
                dotColor = MaterialTheme.colorScheme.outlineVariant,
                title = "Registro de Ánimo",
                subtitle = "Ayer, 8:45 PM • Calma",
            )
        }
    }
}

@Composable
private fun BitacoraItem(dotColor: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Barra inferior: Home (activa) / Historial / Ajustes -- mismos 3 destinos del mockup. */
@Composable
fun HomeBottomNavBar(
    selected: HomeBottomTab,
    onSelect: (HomeBottomTab) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(Icons.Filled.Dashboard, "Home", selected == HomeBottomTab.Home) {
                onSelect(HomeBottomTab.Home)
            }
            BottomNavItem(Icons.Filled.HistoryEdu, "Historial", selected == HomeBottomTab.Historial) {
                onSelect(HomeBottomTab.Historial)
            }
            BottomNavItem(Icons.Filled.Settings, "Ajustes", selected == HomeBottomTab.Ajustes) {
                onSelect(HomeBottomTab.Ajustes)
            }
        }
    }
}

enum class HomeBottomTab { Home, Historial, Ajustes }

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = content)
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}
