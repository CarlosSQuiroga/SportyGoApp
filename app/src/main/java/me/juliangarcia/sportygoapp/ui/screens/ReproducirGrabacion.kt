package me.juliangarcia.sportygoapp.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.juliangarcia.sportygoapp.data.Grabacion

private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

/** Reproduce una grabación guardada en el teléfono. */
@Composable
fun ReproducirGrabacionScreen(grabacion: Grabacion, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val videoView = remember {
        VideoView(context).apply {
            val controles = MediaController(context)
            controles.setAnchorView(this)
            setMediaController(controles)
            setOnPreparedListener { start() }
            setVideoURI(Uri.parse(grabacion.videoUri))
        }
    }
    DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = grabacion.nombreCancha.ifBlank { "Partido" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = listOf(grabacion.fechaGuardado, grabacion.horaReserva)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(factory = { videoView })
        }
    }
}
