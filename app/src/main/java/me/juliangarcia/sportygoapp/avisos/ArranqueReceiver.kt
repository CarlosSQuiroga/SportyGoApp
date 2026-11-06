package me.juliangarcia.sportygoapp.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.juliangarcia.sportygoapp.data.AccountStorage
import me.juliangarcia.sportygoapp.data.AuthRepository

/** Android borra las alarmas al reiniciar: las vuelve a programar desde las reservas guardadas. */
class ArranqueReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = AuthRepository(AccountStorage(context))
        Avisos(context).reprogramar(repo.todasLasReservas())
    }
}
