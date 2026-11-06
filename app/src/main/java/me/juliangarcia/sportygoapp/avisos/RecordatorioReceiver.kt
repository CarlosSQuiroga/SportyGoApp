package me.juliangarcia.sportygoapp.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.juliangarcia.sportygoapp.data.AccountStorage
import me.juliangarcia.sportygoapp.data.AuthRepository
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
        val repo = AuthRepository(AccountStorage(context))
        val reserva = repo.reservaPorId(reservaId) ?: return
        if (repo.reservaYaGrabada(reservaId) || reservaId in repo.reservasSinGrabar()) return

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
        repo.guardarNotificacion(notificacion)
        Avisos(context).apply {
            crearCanales()
            mostrar(notificacion)
        }
    }

    companion object {
        const val EXTRA_RESERVA_ID = "reservaId"
    }
}
