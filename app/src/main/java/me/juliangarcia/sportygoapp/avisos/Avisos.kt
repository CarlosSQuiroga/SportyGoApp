package me.juliangarcia.sportygoapp.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.juliangarcia.sportygoapp.MainActivity
import me.juliangarcia.sportygoapp.R
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.EstadoReserva
import me.juliangarcia.sportygoapp.data.Notificacion
import me.juliangarcia.sportygoapp.data.Reserva
import me.juliangarcia.sportygoapp.data.TipoNotificacion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Notificaciones del sistema y alarmas de la app:
 * - muestra en el teléfono las notificaciones de la bandeja del usuario;
 * - programa el aviso que llega a la hora de cada reserva para que el anfitrión grabe el partido.
 */
class Avisos(context: Context) {

    private val context = context.applicationContext
    private val alarmas = this.context.getSystemService(AlarmManager::class.java)

    /** Crea los canales (Android 8+ los exige; crearlos varias veces no tiene efecto). */
    fun crearCanales() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_GENERAL,
                "Cuenta y reservas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Cuenta creada y reservas hechas o recibidas." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_GRABACION,
                "Grabar partido",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Aviso a la hora de tu reserva para grabar el partido." }
        )
    }

    /** Quita del teléfono la notificación con ese id (sigue en la bandeja de la app). */
    fun quitar(notificacionId: String) {
        NotificationManagerCompat.from(context).cancel(notificacionId.hashCode())
    }

    /** Muestra [n] en el teléfono. Si no hay permiso, solo queda en la bandeja de la app. */
    fun mostrar(n: Notificacion) {
        if (!puedeNotificar()) return
        val esGrabacion = n.tipo == TipoNotificacion.GRABAR_PARTIDO

        // Al tocarla se abre la app; si es hora de grabar, la app va directo a la cámara.
        val abrirApp = PendingIntent.getActivity(
            context,
            n.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(
            context,
            if (esGrabacion) CANAL_GRABACION else CANAL_GENERAL
        )
            .setSmallIcon(if (esGrabacion) R.drawable.ic_stat_grabar else R.drawable.ic_stat_notificacion)
            .setContentTitle(n.titulo)
            .setContentText(n.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.mensaje))
            .setColor(0xFF64F089.toInt())
            .setWhen(n.creadaEn)
            .setShowWhen(true)
            .setPriority(
                if (esGrabacion) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(
                if (esGrabacion) NotificationCompat.CATEGORY_REMINDER
                else NotificationCompat.CATEGORY_STATUS
            )
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(n.id.hashCode(), notificacion)
        } catch (e: SecurityException) {
            // El usuario quitó el permiso justo ahora: la notificación sigue en la bandeja.
        }
    }

    fun puedeNotificar(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Programa el aviso de grabación para la hora de inicio de [reserva] (si aún no pasó). */
    fun programarGrabacion(reserva: Reserva) {
        val inicio = inicioDe(reserva) ?: return
        val millis = inicio.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return

        val pendiente = intentAlarma(reserva.id)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendiente)
        } else {
            // Sin permiso de alarma exacta el sistema puede retrasarla unos minutos.
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendiente)
        }
    }

    /** Vuelve a programar todas las reservas futuras (las alarmas se pierden al reiniciar). */
    fun reprogramar(reservas: List<Reserva>) = reservas
        .filter { it.estado == EstadoReserva.CONFIRMADA } // solo las ya pagadas
        .forEach { programarGrabacion(it) }

    private fun intentAlarma(reservaId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        reservaId.hashCode(),
        Intent(context, RecordatorioReceiver::class.java)
            .putExtra(RecordatorioReceiver.EXTRA_RESERVA_ID, reservaId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val CANAL_GENERAL = "general"
        const val CANAL_GRABACION = "grabacion"

        /** Minutos que dura cada hora reservada (la reserva dura duracionHoras de estas). */
        const val DURACION_MINUTOS = 60L

        private val FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm")

        /** Fecha y hora de inicio de la reserva, o null si los datos no son válidos. */
        fun inicioDe(reserva: Reserva): LocalDateTime? = runCatching {
            LocalDateTime.of(
                LocalDate.parse(reserva.fecha, AuthState.FORMATO_FECHA),
                LocalTime.parse(reserva.hora, FORMATO_HORA)
            )
        }.getOrNull()

        /** true si [ahora] está dentro del horario reservado (todas sus horas). */
        fun enCurso(reserva: Reserva, ahora: LocalDateTime = LocalDateTime.now()): Boolean {
            val inicio = inicioDe(reserva) ?: return false
            val fin = inicio.plusMinutes(DURACION_MINUTOS * reserva.duracionHoras.coerceAtLeast(1))
            return !ahora.isBefore(inicio) && ahora.isBefore(fin)
        }
    }
}
