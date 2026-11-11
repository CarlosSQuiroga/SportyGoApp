package me.juliangarcia.sportygoapp.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.juliangarcia.sportygoapp.data.Gol
import me.juliangarcia.sportygoapp.data.Grabacion

private val Fondo = Color(0xFF0B131D)
private val Tarjeta = Color(0xFF111A24)
private val Verde = Color(0xFF64F089)
private val Blanco = Color(0xFFFFFFFF)
private val Gris = Color(0xFF8C9BAE)

/** Segundos que se retrocede al ver una marca: el grito llega después de la jugada. */
private const val ANTES_DE_LA_MARCA_MS = 8_000L

/**
 * Se abre al detener la grabación del partido (y desde el reproductor, para corregirlo).
 * Muestra las marcas de posible gol que se detectaron en el audio; el usuario confirma cuáles
 * fueron gol y de qué equipo, y así queda guardado el marcador junto con el video.
 *
 * @param marcasPosibles momentos detectados en el audio (ms desde el inicio del video).
 * @param analizando true mientras se busca en el audio; [progreso] va de 0 a 1.
 * @param sinAudio el video no tiene sonido (o no se pudo leer): solo se pueden agregar a mano.
 */
@Composable
fun ConfirmarGolesScreen(
    grabacion: Grabacion,
    videoDisponible: Boolean,
    marcasPosibles: List<Long>,
    analizando: Boolean,
    progreso: Float,
    sinAudio: Boolean,
    onGuardar: (List<Gol>) -> Unit,
    onOmitir: () -> Unit
) {
    val context = LocalContext.current
    val videoView = remember {
        VideoView(context).apply {
            // Muestra el primer cuadro sin empezar a reproducir.
            setOnPreparedListener { seekTo(1) }
            if (videoDisponible) setVideoURI(Uri.parse(grabacion.videoUri))
        }
    }
    DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }

    // Equipo elegido para cada marca: 1, 2, o ausente = no fue gol.
    val equipoDe = remember {
        mutableStateMapOf<Long, Int>().apply { grabacion.goles.forEach { put(it.tiempoMs, it.equipo) } }
    }
    // Goles que el usuario agrega a mano (si la detección no captó alguno).
    val manuales = remember { mutableStateListOf<Long>() }

    val marcas = (marcasPosibles + grabacion.goles.map { it.tiempoMs } + manuales)
        .distinct()
        .sorted()
    val goles1 = equipoDe.values.count { it == 1 }
    val goles2 = equipoDe.values.count { it == 2 }

    fun ver(tiempoMs: Long) {
        videoView.seekTo((tiempoMs - ANTES_DE_LA_MARCA_MS).coerceAtLeast(0L).toInt())
        videoView.start()
    }

    BackHandler { onOmitir() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        // Encabezado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¿Cuáles fueron gol?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blanco
                )
                Text(
                    text = "Revisa los momentos con gritos de gol y elige de qué equipo fue.",
                    fontSize = 13.sp,
                    color = Gris
                )
            }
            TextButton(onClick = onOmitir) { Text("Más tarde", color = Gris) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Video para revisar cada marca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (videoDisponible) {
                AndroidView(factory = { videoView })
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.VideocamOff, contentDescription = null, tint = Gris)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "El video no está en este teléfono.",
                        color = Gris,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Marcador en vivo según lo que se va confirmando
        Marcador(
            equipo1 = grabacion.nombreEquipo1,
            equipo2 = grabacion.nombreEquipo2,
            goles1 = goles1,
            goles2 = goles2
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            if (analizando) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Buscando gritos de gol en el audio… ${(progreso * 100).toInt()}%",
                            color = Blanco,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progreso },
                            color = Verde,
                            trackColor = Tarjeta,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (marcas.isEmpty()) {
                item {
                    Text(
                        text = if (sinAudio) {
                            "Este video no tiene sonido, así que no se pudieron detectar goles. " +
                                "Agrégalos a mano desde el video."
                        } else {
                            "No se escucharon gritos de gol en el partido. Si hubo goles, " +
                                "búscalos en el video y agrégalos a mano."
                        },
                        color = Gris,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }

            items(marcas, key = { it }) { tiempo ->
                MarcaDeGol(
                    tiempoMs = tiempo,
                    manual = tiempo in manuales,
                    equipo1 = grabacion.nombreEquipo1,
                    equipo2 = grabacion.nombreEquipo2,
                    seleccion = equipoDe[tiempo],
                    habilitarVer = videoDisponible,
                    onVer = { ver(tiempo) },
                    onElegir = { equipo ->
                        if (equipo == null) equipoDe.remove(tiempo) else equipoDe[tiempo] = equipo
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        val t = videoView.currentPosition.toLong()
                        if (t !in marcas) manuales += t
                    },
                    enabled = videoDisponible,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Blanco)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agregar gol en el momento actual del video", color = Blanco)
                }
            }
        }

        Button(
            onClick = {
                videoView.pause()
                onGuardar(equipoDe.map { (t, e) -> Gol(t, e) })
            },
            enabled = !analizando,
            colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fondo),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(50.dp)
        ) {
            Text("Guardar marcador  $goles1 – $goles2", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Marcador(equipo1: String, equipo2: String, goles1: Int, goles2: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Tarjeta)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = equipo1,
            color = Blanco,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$goles1 – $goles2",
            color = Verde,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Text(
            text = equipo2,
            color = Blanco,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarcaDeGol(
    tiempoMs: Long,
    manual: Boolean,
    equipo1: String,
    equipo2: String,
    seleccion: Int?,
    habilitarVer: Boolean,
    onVer: () -> Unit,
    onElegir: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Tarjeta)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.SportsSoccer,
                contentDescription = null,
                tint = if (seleccion != null) Verde else Gris,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatoMarca(tiempoMs),
                    color = Blanco,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (manual) "Agregado a mano" else "Grito detectado en el audio",
                    color = Gris,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onVer, enabled = habilitarVer) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Verde)
                Text("Ver", color = Verde)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Opcion("No fue gol", seleccion == null, Modifier.weight(1f)) { onElegir(null) }
            Opcion(equipo1, seleccion == 1, Modifier.weight(1f)) { onElegir(1) }
            Opcion(equipo2, seleccion == 2, Modifier.weight(1f)) { onElegir(2) }
        }
    }
}

@Composable
private fun Opcion(texto: String, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = texto,
        color = if (elegida) Fondo else Blanco,
        fontSize = 13.sp,
        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (elegida) Verde else Color.Transparent)
            .border(1.dp, if (elegida) Verde else Gris.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp)
    )
}

/** 754_000 -> "12:34" (o "1:02:34" si pasa de una hora). */
internal fun formatoMarca(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val seg = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, seg) else "%d:%02d".format(m, seg)
}
