package me.juliangarcia.sportygoapp.pagos

enum class EstadoPago { PENDIENTE, APROBADO, RECHAZADO }

/** Cobro enviado al Nequi de un jugador para pagar una reserva (y sus productos). */
data class SolicitudPago(
    val id: String,
    /** Nequi del jugador: aquí le llega la solicitud para aceptarla o rechazarla. */
    val nequiOrigen: String,
    /** Nequi del dueño de la cancha: aquí llega el dinero si el jugador acepta. */
    val nequiDestino: String,
    val monto: Int,
    val descripcion: String,
    val creadaEn: Long,
    val estado: EstadoPago = EstadoPago.PENDIENTE
)

/**
 * Pasarela de pagos que usa la app. Hoy la implementa [NequiSimulado].
 *
 * Para cobrar de verdad, esta interfaz se implementa llamando a un servidor propio
 * (las credenciales de Nequi nunca deben ir dentro de la app). Ese servidor envía la
 * solicitud push al Nequi del jugador y responde el estado del cobro; el resto de la app
 * (reservas, notificaciones, pantallas) no cambia.
 */
interface PasarelaPagos {

    /** Envía una solicitud de cobro al Nequi [nequiOrigen]. */
    fun solicitarCobro(
        nequiOrigen: String,
        nequiDestino: String,
        monto: Int,
        descripcion: String
    ): SolicitudPago

    /** Estado actual del cobro, o null si la pasarela no lo conoce. */
    fun consultarEstado(pagoId: String): EstadoPago?
}
