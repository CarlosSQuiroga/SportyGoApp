package me.juliangarcia.sportygoapp.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.juliangarcia.sportygoapp.data.NubeDatos
import me.juliangarcia.sportygoapp.data.Notificacion
import me.juliangarcia.sportygoapp.data.TipoNotificacion
import java.util.UUID

/**
 * Llega a la hora de inicio de una reserva (aunque la app esté cerrada) y avisa al anfitrión
 * que posicione el teléfono para grabar. Al abrir la app se muestra la cámara.
 */
class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reservaId = intent.getStringExtra(EXTRA_RESERVA_ID) ?: return
        // Solo avisa al usuario que tiene la sesión abierta en este teléfono.
        val uid = NubeDatos.auth.currentUser?.uid ?: return

        // Consultar Firestore toma un momento: se mantiene vivo el receptor mientras tanto.
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withTimeoutOrNull(8_000) { avisar(context.applicationContext, reservaId, uid) }
            } finally {
                pendiente.finish()
            }
        }
    }

    private suspend fun avisar(context: Context, reservaId: String, uid: String) {
        val reserva = NubeDatos.reservas.leerRemoto(reservaId) ?: return
        if (reserva.usuarioId != uid || reserva.sinGrabar) return
        val yaGrabada = NubeDatos.grabaciones.buscarRemoto("usuarioId", uid)
            .any { it.reservaId == reservaId }
        if (yaGrabada) return

        val notificacion = Notificacion(
            id = UUID.randomUUID().toString(),
            usuarioId = reserva.usuarioId,
            tipo = TipoNotificacion.GRABAR_PARTIDO,
            titulo = "¡Tu partido empieza ahora!",
            mensaje = "Como anfitrión, posiciona el teléfono para grabar el partido en " +
                "${reserva.nombreCancha}. Abre SportyGo para empezar a grabar.",
            creadaEn = System.currentTimeMillis(),
            mostrada = true
        )
        NubeDatos.notificaciones.guardar(notificacion.id, notificacion)
        Avisos(context).apply {
            crearCanales()
            mostrar(notificacion)
        }
    }

    companion object {
        const val EXTRA_RESERVA_ID = "reservaId"
    }
}
