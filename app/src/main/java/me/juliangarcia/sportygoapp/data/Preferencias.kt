package me.juliangarcia.sportygoapp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Ajustes de la app guardados en este teléfono (pantalla "Configuración").
 * Son observables desde Compose: al cambiarlos, la pantalla se actualiza sola.
 */
class Preferencias(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("configuracion", Context.MODE_PRIVATE)

    /** Mostrar en el teléfono los mensajes nuevos del chat. */
    var avisosChat: Boolean by mutableStateOf(prefs.getBoolean(KEY_AVISOS_CHAT, true))
        private set

    /** Abrir la cámara automáticamente al entrar a la app durante una reserva. */
    var camaraAutomatica: Boolean by mutableStateOf(prefs.getBoolean(KEY_CAMARA_AUTO, true))
        private set

    fun cambiarAvisosChat(valor: Boolean) {
        avisosChat = valor
        prefs.edit().putBoolean(KEY_AVISOS_CHAT, valor).apply()
    }

    fun cambiarCamaraAutomatica(valor: Boolean) {
        camaraAutomatica = valor
        prefs.edit().putBoolean(KEY_CAMARA_AUTO, valor).apply()
    }

    private companion object {
        const val KEY_AVISOS_CHAT = "avisos_chat"
        const val KEY_CAMARA_AUTO = "camara_automatica"
    }
}
