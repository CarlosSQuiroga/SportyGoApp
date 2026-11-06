package me.juliangarcia.sportygoapp.pagos

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * Nequi DE PRUEBA: no mueve dinero real. Guarda las solicitudes de cobro en el teléfono y
 * la pantalla "Nequi (prueba)" permite aceptarlas o rechazarlas, como lo haría el jugador
 * en la app de Nequi. Se reemplaza por una implementación real de [PasarelaPagos].
 */
class NequiSimulado(context: Context) : PasarelaPagos {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun solicitarCobro(
        nequiOrigen: String,
        nequiDestino: String,
        monto: Int,
        descripcion: String
    ): SolicitudPago {
        val solicitud = SolicitudPago(
            id = UUID.randomUUID().toString(),
            nequiOrigen = nequiOrigen,
            nequiDestino = nequiDestino,
            monto = monto,
            descripcion = descripcion,
            creadaEn = System.currentTimeMillis()
        )
        guardar(cargar() + solicitud)
        return solicitud
    }

    override fun consultarEstado(pagoId: String): EstadoPago? =
        cargar().firstOrNull { it.id == pagoId }?.estado

    /** Solicitudes que le llegaron al Nequi [numero] (más recientes primero). */
    fun solicitudesDe(numero: String): List<SolicitudPago> =
        cargar().filter { it.nequiOrigen == numero }.sortedByDescending { it.creadaEn }

    /** El dueño del Nequi acepta o rechaza el cobro. Solo cambia los que siguen pendientes. */
    fun responder(pagoId: String, aceptar: Boolean) {
        guardar(cargar().map {
            if (it.id == pagoId && it.estado == EstadoPago.PENDIENTE) {
                it.copy(estado = if (aceptar) EstadoPago.APROBADO else EstadoPago.RECHAZADO)
            } else it
        })
    }

    private fun cargar(): List<SolicitudPago> {
        val raw = prefs.getString(KEY_SOLICITUDES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SolicitudPago(
                    id = o.getString("id"),
                    nequiOrigen = o.getString("nequiOrigen"),
                    nequiDestino = o.getString("nequiDestino"),
                    monto = o.getInt("monto"),
                    descripcion = o.getString("descripcion"),
                    creadaEn = o.getLong("creadaEn"),
                    estado = EstadoPago.entries.firstOrNull { it.name == o.optString("estado") }
                        ?: EstadoPago.PENDIENTE
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun guardar(solicitudes: List<SolicitudPago>) {
        val array = JSONArray()
        solicitudes.forEach { s ->
            array.put(JSONObject().apply {
                put("id", s.id)
                put("nequiOrigen", s.nequiOrigen)
                put("nequiDestino", s.nequiDestino)
                put("monto", s.monto)
                put("descripcion", s.descripcion)
                put("creadaEn", s.creadaEn)
                put("estado", s.estado.name)
            })
        }
        prefs.edit().putString(KEY_SOLICITUDES, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "nequi_simulado"
        const val KEY_SOLICITUDES = "solicitudes"
    }
}
