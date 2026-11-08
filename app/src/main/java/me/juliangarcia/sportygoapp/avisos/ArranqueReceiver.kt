package me.juliangarcia.sportygoapp.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.juliangarcia.sportygoapp.data.NubeDatos

class ArranqueReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val uid = NubeDatos.auth.currentUser?.uid ?: return
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withTimeoutOrNull(8_000) {
                    val reservas = NubeDatos.reservas.buscarRemoto("usuarioId", uid)
                    Avisos(context.applicationContext).reprogramar(reservas)
                }
            } finally {
                pendiente.finish()
            }
        }
    }
}
