package me.juliangarcia.sportygoapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Persistencia local simple (SharedPreferences + JSON), sin dependencias extra.
 * Más adelante se puede reemplazar por Room o un backend manteniendo [AuthRepository].
 */
class AccountStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Usuarios ----------

    fun loadUsers(): List<User> = loadList(KEY_USERS) { o ->
        User(
            id = o.getString("id"),
            nombreUsuario = o.getString("nombreUsuario"),
            correo = o.getString("correo"),
            fechaNacimiento = o.getString("fechaNacimiento"),
            passwordHash = o.getString("passwordHash"),
            salt = o.getString("salt"),
            rol = parseRol(o.optString("rol", "")),
            fotoPerfilUri = o.optString("fotoPerfilUri", "").ifEmpty { null }
        )
    }

    fun saveUsers(users: List<User>) = saveList(KEY_USERS, users) { u ->
        put("id", u.id)
        put("nombreUsuario", u.nombreUsuario)
        put("correo", u.correo)
        put("fechaNacimiento", u.fechaNacimiento)
        put("passwordHash", u.passwordHash)
        put("salt", u.salt)
        put("rol", u.rol?.name ?: "")
        put("fotoPerfilUri", u.fotoPerfilUri ?: "")
    }

    // ---------- Sesión ----------

    fun getSessionUserId(): String? = prefs.getString(KEY_SESSION, null)

    fun setSessionUserId(id: String?) {
        val editor = prefs.edit()
        if (id == null) editor.remove(KEY_SESSION) else editor.putString(KEY_SESSION, id)
        editor.apply()
    }

    // ---------- Canchas ----------

    fun loadCanchas(): List<Cancha> = loadList(KEY_CANCHAS) { o ->
        val ownerId = o.getString("ownerId")
        Cancha(
            // Las canchas guardadas antes de existir el id usan el id del dueño (1 cancha por dueño).
            id = o.optString("id", "").ifEmpty { ownerId },
            ownerId = ownerId,
            imagenUri = o.optString("imagenUri", ""),
            nombre = o.getString("nombre"),
            descripcion = o.getString("descripcion"),
            capacidad = o.getInt("capacidad"),
            horaApertura = o.getString("horaApertura"),
            horaCierre = o.getString("horaCierre"),
            precioHora = o.getInt("precioHora")
        )
    }

    fun saveCanchas(canchas: List<Cancha>) = saveList(KEY_CANCHAS, canchas) { c ->
        put("id", c.id)
        put("ownerId", c.ownerId)
        put("imagenUri", c.imagenUri)
        put("nombre", c.nombre)
        put("descripcion", c.descripcion)
        put("capacidad", c.capacidad)
        put("horaApertura", c.horaApertura)
        put("horaCierre", c.horaCierre)
        put("precioHora", c.precioHora)
    }

    // ---------- Reservas ----------

    fun loadReservas(): List<Reserva> = loadList(KEY_RESERVAS) { o ->
        Reserva(
            id = o.getString("id"),
            usuarioId = o.getString("usuarioId"),
            canchaId = o.getString("canchaId"),
            nombreCancha = o.getString("nombreCancha"),
            fecha = o.getString("fecha"),
            hora = o.getString("hora"),
            precio = o.getInt("precio")
        )
    }

    fun saveReservas(reservas: List<Reserva>) = saveList(KEY_RESERVAS, reservas) { r ->
        put("id", r.id)
        put("usuarioId", r.usuarioId)
        put("canchaId", r.canchaId)
        put("nombreCancha", r.nombreCancha)
        put("fecha", r.fecha)
        put("hora", r.hora)
        put("precio", r.precio)
    }

    // ---------- Grabaciones ----------

    fun loadGrabaciones(): List<Grabacion> = loadList(KEY_GRABACIONES) { o ->
        Grabacion(
            id = o.getString("id"),
            usuarioId = o.getString("usuarioId"),
            videoUri = o.getString("videoUri"),
            fechaGuardado = o.getString("fechaGuardado"),
            horaReserva = o.getString("horaReserva"),
            nombreCancha = o.getString("nombreCancha")
        )
    }

    fun saveGrabaciones(grabaciones: List<Grabacion>) =
        saveList(KEY_GRABACIONES, grabaciones) { g ->
            put("id", g.id)
            put("usuarioId", g.usuarioId)
            put("videoUri", g.videoUri)
            put("fechaGuardado", g.fechaGuardado)
            put("horaReserva", g.horaReserva)
            put("nombreCancha", g.nombreCancha)
        }

    // ---------- Helpers JSON ----------

    private fun <T> loadList(key: String, parse: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> parse(array.getJSONObject(i)) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun <T> saveList(key: String, items: List<T>, write: JSONObject.(T) -> Unit) {
        val array = JSONArray()
        items.forEach { item -> array.put(JSONObject().apply { write(item) }) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun parseRol(valor: String): UserRole? =
        UserRole.values().firstOrNull { it.name == valor }

    private companion object {
        const val PREFS_NAME = "sportygo_accounts"
        const val KEY_USERS = "users"
        const val KEY_SESSION = "session_user_id"
        const val KEY_CANCHAS = "canchas"
        const val KEY_RESERVAS = "reservas"
        const val KEY_GRABACIONES = "grabaciones"
    }
}
