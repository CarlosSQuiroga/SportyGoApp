package me.juliangarcia.sportygoapp.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import me.juliangarcia.sportygoapp.data.Grabacion

private val PanelBackground = Color(0xFF0B131D)
private val ButtonBackground = Color(0xFF111A24)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/** Duración máxima de un momento clave (para que se pueda enviar por chat sin problema). */
private const val MAX_MOMENTO_MS = 60_000L
private const val MIN_MOMENTO_MS = 1_000L

/**
 * Reproduce una grabación guardada en el teléfono. Desde aquí se comparte el partido completo
 * o un momento clave (se elige el inicio y el fin con un control deslizante), y se renombra
 * o elimina el video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproducirGrabacionScreen(
    grabacion: Grabacion,
    onBackClick: () -> Unit,
    /** false si el video no está en este teléfono (se grabó en otro): no se puede recortar ni compartir. */
    videoDisponible: Boolean = true,
    /**
     * Lo que se reproduce: el archivo de este teléfono o, si no está aquí, el video en la nube.
     * null = no hay video que mostrar.
     */
    fuenteVideo: String? = grabacion.videoUri.takeIf { videoDisponible },
    /** Dueño de la cancha: solo ve el video (sin compartir, goles, renombrar ni eliminar). */
    soloLectura: Boolean = false,
    /** Duración del video (ms) leída del archivo; se usa hasta que el reproductor la tenga. */
    duracionInicialMs: Long = 0L,
    /** true = abrir directamente el modo "momento clave". */
    abrirEnMomentoClave: Boolean = false,
    /** true mientras se recorta el momento clave. */
    preparandoClip: Boolean = false,
    onCompartirCompletoClick: () -> Unit = {},
    onCompartirMomentoClick: (inicioMs: Long, finMs: Long) -> Unit = { _, _ -> },
    onRenombrarClick: () -> Unit = {},
    onEliminarClick: () -> Unit = {},
    /** Revisar o corregir los goles y el marcador. */
    onGolesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var duracionMs by remember { mutableLongStateOf(duracionInicialMs) }
    val videoView = remember {
        VideoView(context).apply {
            val controles = MediaController(context)
            controles.setAnchorView(this)
            setMediaController(controles)
            setOnPreparedListener { mp ->
                if (mp.duration > 0) duracionMs = mp.duration.toLong()
                start()
            }
            if (fuenteVideo != null) setVideoURI(Uri.parse(fuenteVideo))
        }
    }
    DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }

    var modoMomento by remember { mutableStateOf(abrirEnMomentoClave && videoDisponible) }
    var inicioMs by remember { mutableLongStateOf(0L) }
    var finMs by remember { mutableLongStateOf(0L) }
    var previsualizando by remember { mutableStateOf(false) }

    /** Momento inicial: 20 s alrededor de donde va el video. */
    fun prepararMomento() {
        val total = duracionMs.coerceAtLeast(MIN_MOMENTO_MS)
        val actual = videoView.currentPosition.toLong().coerceIn(0L, total)
        inicioMs = (actual - 10_000L).coerceAtLeast(0L)
        finMs = (inicioMs + 20_000L).coerceAtMost(total)
        inicioMs = (finMs - 20_000L).coerceAtLeast(0L)
    }

    // Al conocer la duración (o al abrir en modo momento), se propone un rango.
    LaunchedEffect(modoMomento, duracionMs) {
        if (modoMomento && duracionMs > 0 && finMs == 0L) prepararMomento()
    }

    // Vista previa: reproduce el momento y se detiene al llegar al final.
    LaunchedEffect(previsualizando) {
        if (!previsualizando) return@LaunchedEffect
        videoView.seekTo(inicioMs.toInt())
        videoView.start()
        while (previsualizando) {
            if (videoView.currentPosition >= finMs) {
                videoView.pause()
                previsualizando = false
            }
            delay(150)
        }
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grabacion.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        grabacion.nombreCancha.takeIf { grabacion.nombre.isNotBlank() },
                        grabacion.enfrentamiento,
                        grabacion.fechaGuardado,
                        grabacion.horaReserva
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (fuenteVideo != null) {
                AndroidView(factory = { videoView })
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideocamOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Este video no está en este teléfono",
                        color = TextWhite,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Los videos se guardan en el teléfono con el que se grabaron.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Panel inferior: acciones o selección del momento clave
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBackground)
                .padding(16.dp)
        ) {
            if (modoMomento && duracionMs > 0) {
                Text(
                    text = "Momento clave",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Elige el inicio y el fin de la jugada (máximo 1 minuto).",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                RangeSlider(
                    value = inicioMs.toFloat()..finMs.toFloat(),
                    onValueChange = { rango ->
                        previsualizando = false
                        var nuevoInicio = rango.start.toLong()
                        var nuevoFin = rango.endInclusive.toLong()
                        val movioInicio = nuevoInicio != inicioMs
                        // Se respeta la duración mínima y máxima moviendo el otro extremo.
                        if (nuevoFin - nuevoInicio > MAX_MOMENTO_MS) {
                            if (movioInicio) nuevoFin = nuevoInicio + MAX_MOMENTO_MS
                            else nuevoInicio = nuevoFin - MAX_MOMENTO_MS
                        }
                        if (nuevoFin - nuevoInicio < MIN_MOMENTO_MS) {
                            if (movioInicio) nuevoInicio = nuevoFin - MIN_MOMENTO_MS
                            else nuevoFin = nuevoInicio + MIN_MOMENTO_MS
                        }
                        inicioMs = nuevoInicio.coerceIn(0L, duracionMs)
                        finMs = nuevoFin.coerceIn(0L, duracionMs)
                        // Muestra el cuadro del extremo que se está moviendo.
                        videoView.seekTo((if (movioInicio) inicioMs else finMs).toInt())
                    },
                    valueRange = 0f..duracionMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen,
                        inactiveTrackColor = ButtonBackground
                    )
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(formatoTiempo(inicioMs), color = TextWhite, fontSize = 13.sp)
                    Text(
                        text = "Duración ${formatoTiempo(finMs - inicioMs)}",
                        color = PrimaryGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatoTiempo(finMs), color = TextWhite, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { previsualizando = !previsualizando },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (previsualizando) "Detener" else "Ver momento", color = TextWhite)
                    }
                    OutlinedButton(
                        onClick = {
                            // Empieza donde va el video, conservando la duración elegida.
                            val largo = finMs - inicioMs
                            inicioMs = videoView.currentPosition.toLong().coerceIn(0L, duracionMs)
                            finMs = (inicioMs + largo).coerceAtMost(duracionMs)
                            if (finMs - inicioMs < MIN_MOMENTO_MS) {
                                inicioMs = (finMs - MIN_MOMENTO_MS).coerceAtLeast(0L)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Iniciar aquí", color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        previsualizando = false
                        videoView.pause()
                        onCompartirMomentoClick(inicioMs, finMs)
                    },
                    enabled = !preparandoClip,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (preparandoClip) {
                        CircularProgressIndicator(
                            color = PanelBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Preparando el clip...", color = PanelBackground, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = PanelBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Compartir momento (${formatoTiempo(finMs - inicioMs)})",
                            color = PanelBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Cancelar",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !preparandoClip) {
                            previsualizando = false
                            modoMomento = false
                        }
                        .padding(top = 12.dp)
                )
            } else {
                // Marcador y goles: tocar un gol lleva al video unos segundos antes.
                grabacion.marcador?.let { marcador ->
                    Text(
                        text = marcador,
                        color = PrimaryGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (grabacion.goles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            grabacion.goles.forEach { gol ->
                                val equipo = if (gol.equipo == 1) grabacion.nombreEquipo1
                                else grabacion.nombreEquipo2
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ButtonBackground)
                                        .clickable(enabled = fuenteVideo != null) {
                                            videoView.seekTo(
                                                (gol.tiempoMs - 8_000L).coerceAtLeast(0L).toInt()
                                            )
                                            videoView.start()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.SportsSoccer,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${formatoMarca(gol.tiempoMs)} · $equipo",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (!soloLectura) Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccionVideo("Compartir", Icons.Outlined.Share, videoDisponible, Modifier.weight(1f)) {
                        onCompartirCompletoClick()
                    }
                    AccionVideo("Goles", Icons.Outlined.SportsSoccer, true, Modifier.weight(1f)) {
                        videoView.pause()
                        onGolesClick()
                    }
                    AccionVideo("Momento", Icons.Outlined.ContentCut, videoDisponible, Modifier.weight(1f)) {
                        modoMomento = true
                        if (duracionMs > 0) prepararMomento()
                    }
                    AccionVideo("Renombrar", Icons.Outlined.Edit, true, Modifier.weight(1f)) {
                        onRenombrarClick()
                    }
                    AccionVideo("Eliminar", Icons.Outlined.Delete, true, Modifier.weight(1f), ErrorRed) {
                        onEliminarClick()
                    }
                }
            }
        }
    }
}

@Composable
private fun AccionVideo(
    texto: String,
    icono: ImageVector,
    habilitado: Boolean,
    modifier: Modifier,
    color: Color = TextWhite,
    onClick: () -> Unit
) {
    val tinte = if (habilitado) color else TextSecondary.copy(alpha = 0.4f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ButtonBackground)
            .clickable(enabled = habilitado) { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(texto, color = tinte, fontSize = 12.sp)
    }
}

/** 75_000 -> "1:15" */
private fun formatoTiempo(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
