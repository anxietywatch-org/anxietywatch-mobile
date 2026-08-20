package com.anxietywatch.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anxietywatch.mobile.R

private data class Track(val id: String, val title: String, val uri: Uri, val isBundled: Boolean)

private fun getBundledTracks(): List<Track> {
    val fields = R.raw::class.java.fields
    val result = mutableListOf<Track>()
    for (field in fields) {
        try {
            val resId = field.getInt(null)
            val uri = Uri.parse("android.resource://com.anxietywatch.mobile/$resId")
            result.add(Track(id = field.name, title = field.name.replace("_", " ").replaceFirstChar { it.uppercase() }, uri = uri, isBundled = true))
        } catch (e: Exception) {
        }
    }
    return result
}

private fun getDeviceTracks(context: android.content.Context): List<Track> {
    val hasPermission = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    if (!hasPermission) return emptyList()

    val result = mutableListOf<Track>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE)
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Pista sin nombre"
            val uri = Uri.withAppendedPath(collection, id.toString())
            result.add(Track(id = id.toString(), title = title, uri = uri, isBundled = false))
        }
    }
    return result
}

@Composable
fun MusicScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    var deviceTracks by remember { mutableStateOf(getDeviceTracks(context)) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    val bundledTracks = remember { getBundledTracks() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                deviceTracks = getDeviceTracks(context)
            } else {
                permissionDenied = true
            }
        }
    )

    fun playTrack(track: Track) {
        mediaPlayer?.release()
        val player = MediaPlayer()
        try {
            player.setDataSource(context, track.uri)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener { playingTrackId = null }
            player.prepareAsync()
            mediaPlayer = player
            playingTrackId = track.id
        } catch (e: Exception) {
            playingTrackId = null
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingTrackId = null
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { stopPlayback(); onBack() }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(text = "Sonidos relajantes", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            text = "Sonidos de AnxietyWatch",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )
        if (bundledTracks.isEmpty()) {
            Text(
                text = "Aún no hay sonidos incluidos en la app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (track in bundledTracks) {
                TrackRow(track = track, isPlaying = playingTrackId == track.id, onPlayPause = {
                    if (playingTrackId == track.id) stopPlayback() else playTrack(track)
                })
            }
        }

        Text(
            text = "Tu música",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        if (deviceTracks.isEmpty() && !permissionDenied) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Da acceso a tu música para reproducirla aquí.", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text("Permitir acceso a música")
                    }
                }
            }
        } else if (deviceTracks.isEmpty() && permissionDenied) {
            Text(
                text = "No se concedió acceso a tu música. Puedes habilitarlo desde los ajustes de Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (track in deviceTracks) {
                TrackRow(track = track, isPlaying = playingTrackId == track.id, onPlayPause = {
                    if (playingTrackId == track.id) stopPlayback() else playTrack(track)
                })
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onPlayPause),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(text = track.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp).weight(1f))
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir"
            )
        }
    }
}