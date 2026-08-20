package com.anxietywatch.mobile.network

import android.content.Context
import android.content.Intent

object HistoryExporter {
    fun shareHistory(context: Context, episodes: List<EpisodeSummary>, summary: DashboardSummary?) {
        val builder = StringBuilder()
        builder.appendLine("Historial de bienestar — AnxietyWatch")
        builder.appendLine()
        summary?.let {
            builder.appendLine("Resumen actual:")
            builder.appendLine("- Nivel de ansiedad: ${it.anxietyLevel.current} (${it.anxietyLevel.trend})")
            builder.appendLine("- Registros esta semana: ${it.weeklyRecords.used}/${it.weeklyRecords.limit}")
            builder.appendLine("- Racha: ${it.streakDays} días")
            builder.appendLine("- Ejercicios completados: ${it.exercisesCompleted}")
            builder.appendLine()
        }
        builder.appendLine("Eventos registrados: ${episodes.size}")
        for (episode in episodes) {
            builder.appendLine("- ${episode.date ?: "Sin fecha"}: ${episode.severity ?: "Sin severidad"} (${episode.durationMinutes ?: 0} min)")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, builder.toString())
            putExtra(Intent.EXTRA_SUBJECT, "Mi historial de AnxietyWatch")
        }
        context.startActivity(Intent.createChooser(intent, "Compartir historial"))
    }
}