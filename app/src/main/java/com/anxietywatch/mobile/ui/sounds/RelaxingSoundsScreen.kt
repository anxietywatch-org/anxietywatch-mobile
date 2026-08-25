package com.anxietywatch.mobile.ui.sounds

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class AudioTrack(val id: Long, val title: String, val uri: android.net.Uri)

@Composable
fun RelaxingSoundsScreen() {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var tracks by remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    var selectedTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    DisposableEffect(Unit) { onDispose { player.value?.release() } }
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
    }
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) tracks = queryAudioTracks(context)
    }

    fun toggle(track: AudioTrack) {
        if (selectedTrack?.id == track.id && player.value?.isPlaying == true) {
            player.value?.pause()
            playing = false
            return
        }
        player.value?.release()
        player.value = MediaPlayer.create(context, track.uri)?.apply {
            setOnCompletionListener { playing = false }
            start()
        }
        selectedTrack = track
        playing = player.value != null
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Sonidos relajantes", style = MaterialTheme.typography.headlineLarge)
        Text("Elige un audio para acompañar tu momento de calma.", modifier = Modifier.padding(top = 8.dp))
        Text("Tu música", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        if (!permissionGranted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permite acceso a tus archivos de audio para mostrar tu música.")
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                                else Manifest.permission.READ_EXTERNAL_STORAGE,
                            )
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Permitir acceso") }
                }
            }
        } else if (tracks.isEmpty()) {
            Text("No encontramos música en el dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tracks, key = { it.id }) { track ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(track.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                            IconButton(onClick = { toggle(track) }) {
                                Icon(
                                    if (selectedTrack?.id == track.id && playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (selectedTrack?.id == track.id && playing) "Pausar" else "Reproducir",
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("Sonidos de AnxietyWatch", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Text(
            "Próximamente: el equipo de diseño debe aportar los archivos .mp3/.ogg para esta sección.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // TODO: agregar pistas reales en res/raw cuando producto entregue los assets de audio.
    }
}

private fun queryAudioTracks(context: android.content.Context): List<AudioTrack> {
    val result = mutableListOf<AudioTrack>()
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE),
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.TITLE} ASC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            result += AudioTrack(id, cursor.getString(titleIndex).orEmpty(), ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
        }
    }
    return result
}
