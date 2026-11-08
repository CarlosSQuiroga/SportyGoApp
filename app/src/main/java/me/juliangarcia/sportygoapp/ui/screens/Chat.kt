package me.juliangarcia.sportygoapp.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF131D2A)
private val MyBubble = Color(0xFF1B3B32)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/** Un mensaje tal como se dibuja en la conversación. */
data class MensajeItem(
    val id: String,
    val texto: String,
    val enviadoEn: Long,
    /** true = lo escribió el usuario en sesión (burbuja a la derecha). */
    val esMio: Boolean
)

/** Una conversación en la lista de chats. */
data class ConversacionItem(
    val id: String,
    val titulo: String,
    val ultimoMensaje: String,
    val enviadoEn: Long,
    val noLeidos: Int
)

/** Conversación entre un jugador y el dueño de una cancha. */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    titulo: String = "",
    subtitulo: String = "",
    mensajes: List<MensajeItem> = emptyList(),
    texto: String = "",
    errorMensaje: String? = null,
    onTextoChange: (String) -> Unit = {},
    onEnviarClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val lista = rememberLazyListState()
    // Siempre muestra el mensaje más reciente.
    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex)
    }

    Surface(modifier = modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Encabezado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBackground)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite
                    )
                }
                Avatar(titulo)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitulo.isNotBlank()) {
                        Text(
                            text = subtitulo,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Mensajes
            if (mensajes.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.ChatBubbleOutline,
                    titulo = "Aún no hay mensajes",
                    descripcion = "Escribe tu primer mensaje. Te responderán por aquí mismo.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = lista,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mensajes, key = { it.id }) { m -> Burbuja(m) }
                }
            }

            if (errorMensaje != null) {
                Text(
                    text = errorMensaje,
                    fontSize = 13.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Escribir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = texto,
                    onValueChange = onTextoChange,
                    placeholder = { Text("Escribe un mensaje...", color = TextSecondary) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCardBackground,
                        unfocusedContainerColor = DarkCardBackground,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = PrimaryGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val puedeEnviar = texto.isNotBlank()
                IconButton(
                    onClick = onEnviarClick,
                    enabled = puedeEnviar,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (puedeEnviar) PrimaryGreen else PrimaryGreen.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = DarkBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun Burbuja(m: MensajeItem) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (m.esMio) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (m.esMio) 16.dp else 4.dp,
                        bottomEnd = if (m.esMio) 4.dp else 16.dp
                    )
                )
                .background(if (m.esMio) MyBubble else DarkCardBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = m.texto, fontSize = 15.sp, color = TextWhite)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = horaMensaje(m.enviadoEn),
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/** Lista de conversaciones (el dueño ve aquí los chats con los jugadores). */
@Composable
fun ConversacionesScreen(
    modifier: Modifier = Modifier,
    titulo: String = "Mensajes",
    conversaciones: List<ConversacionItem> = emptyList(),
    onConversacionClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite
                    )
                }
                Text(
                    text = titulo,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            if (conversaciones.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.ChatBubbleOutline,
                    titulo = "Sin mensajes todavía",
                    descripcion = "Cuando un jugador te escriba desde tu cancha, la conversación aparecerá aquí."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversaciones, key = { it.id }) { c ->
                        FilaConversacion(c, onClick = { onConversacionClick(c.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaConversacion(c: ConversacionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardBackground)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(c.titulo)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = c.titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = c.ultimoMensaje,
                fontSize = 13.sp,
                color = if (c.noLeidos > 0) TextWhite else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = horaMensaje(c.enviadoEn), fontSize = 11.sp, color = TextSecondary)
            if (c.noLeidos > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(PrimaryGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (c.noLeidos > 9) "9+" else c.noLeidos.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(nombre: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MyBubble, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nombre.trim().firstOrNull()?.uppercase() ?: "?",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
    }
}

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** "14:05" si es de hoy; "03/11 14:05" si es de otro día. */
private fun horaMensaje(millis: Long): String {
    val momento = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return if (momento.toLocalDate() == LocalDate.now()) momento.format(FORMATO_HORA)
    else momento.format(FORMATO_DIA)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatPreview() {
    ChatScreen(
        titulo = "Cancha El Golazo",
        subtitulo = "Dueño: carlos",
        mensajes = listOf(
            MensajeItem("1", "Hola, ¿tienen balones para prestar?", 0L, esMio = true),
            MensajeItem("2", "¡Hola! Sí, en el kiosco te prestamos uno.", 60_000L, esMio = false)
        )
    )
}
