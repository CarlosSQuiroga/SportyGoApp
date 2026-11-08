package me.juliangarcia.sportygoapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.Mensaje
import me.juliangarcia.sportygoapp.ui.screens.ChatScreen
import me.juliangarcia.sportygoapp.ui.screens.ConversacionItem
import me.juliangarcia.sportygoapp.ui.screens.ConversacionesScreen
import me.juliangarcia.sportygoapp.ui.screens.MensajeItem

/**
 * Chat del jugador [jugadorId] con el dueño de la cancha [canchaId]. Lo abre el jugador
 * desde el detalle de la cancha, o el dueño desde su lista de mensajes.
 */
@Composable
fun ChatRoute(
    auth: AuthState,
    canchaId: String,
    jugadorId: String,
    onBackClick: () -> Unit
) {
    val yo = auth.usuario
    val cancha = auth.canchaPorId(canchaId)
    // Solo pueden entrar el jugador del chat y el dueño de la cancha.
    if (yo == null || cancha == null || (yo.id != jugadorId && yo.id != cancha.ownerId)) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val chatId = Mensaje.idChat(canchaId, jugadorId)
    var texto by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // Mientras el chat está en pantalla, sus mensajes nuevos no generan aviso en el teléfono.
    DisposableEffect(chatId) {
        auth.chatAbierto = chatId
        onDispose { if (auth.chatAbierto == chatId) auth.chatAbierto = null }
    }

    val mensajes = auth.mensajesDeChat(chatId)
    // Al ver mensajes nuevos, se marcan como leídos.
    LaunchedEffect(chatId, mensajes.size) { auth.marcarChatLeido(chatId) }

    val soyDuenio = yo.id == cancha.ownerId
    val titulo: String
    val subtitulo: String
    if (soyDuenio) {
        titulo = auth.nombreDeUsuario(jugadorId) ?: "Jugador"
        subtitulo = "Jugador · ${cancha.nombre}"
    } else {
        titulo = cancha.nombre
        subtitulo = "Dueño: ${auth.nombreDeUsuario(cancha.ownerId) ?: "dueño de la cancha"}"
    }

    ChatScreen(
        titulo = titulo,
        subtitulo = subtitulo,
        mensajes = mensajes.map {
            MensajeItem(it.id, it.texto, it.enviadoEn, esMio = it.autorId == yo.id)
        },
        texto = texto,
        errorMensaje = error,
        onTextoChange = {
            texto = it.take(1000)
            error = null
        },
        onEnviarClick = {
            error = auth.enviarMensaje(canchaId, jugadorId, texto)
            if (error == null) texto = ""
        },
        onBackClick = onBackClick
    )
}

/** Mensajes del dueño: una conversación por cada jugador que le escribió. */
@Composable
fun ConversacionesRoute(
    auth: AuthState,
    onConversacionClick: (canchaId: String, jugadorId: String) -> Unit,
    onBackClick: () -> Unit
) {
    val conversaciones = auth.misConversaciones()
    ConversacionesScreen(
        titulo = "Mensajes de jugadores",
        conversaciones = conversaciones.map { c ->
            val yoEscribi = c.ultimo.autorId == auth.usuario?.id
            ConversacionItem(
                id = c.chatId,
                titulo = auth.nombreDeUsuario(c.jugadorId) ?: "Jugador",
                ultimoMensaje = (if (yoEscribi) "Tú: " else "") + c.ultimo.texto,
                enviadoEn = c.ultimo.enviadoEn,
                noLeidos = c.noLeidos
            )
        },
        onConversacionClick = { id ->
            conversaciones.firstOrNull { it.chatId == id }?.let {
                onConversacionClick(it.canchaId, it.jugadorId)
            }
        },
        onBackClick = onBackClick
    )
}
