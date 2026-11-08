package me.juliangarcia.sportygoapp.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

sealed interface AuthResult {
    data class Exito(val usuario: User) : AuthResult
    data class Error(val mensaje: String) : AuthResult
}

/**
 * Cuentas (Firebase Authentication) y datos de la app (Firestore, ver [NubeDatos]).
 * Las lecturas son inmediatas: salen de la copia en memoria que NubeDatos mantiene al día.
 */
class AuthRepository(private val nube: NubeDatos = NubeDatos) {

    /** uid de la sesión guardada por Firebase (sobrevive a cerrar la app), o null. */
    fun uidEnSesion(): String? = nube.auth.currentUser?.uid

    fun usuarioEnSesion(): User? = uidEnSesion()?.let { nube.usuarios[it] }

    /** Empieza a sincronizar los datos del usuario [uid] y espera la primera carga. */
    suspend fun cargarDatos(uid: String) = nube.escuchar(uid)

    /** Crea la cuenta y deja la sesión iniciada (el siguiente paso es elegir el perfil). */
    suspend fun registrar(
        nombreUsuario: String,
        correo: String,
        contrasena: String,
        fechaNacimiento: String
    ): AuthResult {
        val nombre = nombreUsuario.trim()
        val email = correo.trim()
        val fecha = fechaNacimiento.trim()

        Validaciones.nombreUsuario(nombre)?.let { return AuthResult.Error(it) }
        Validaciones.correo(email)?.let { return AuthResult.Error(it) }
        Validaciones.contrasena(contrasena)?.let { return AuthResult.Error(it) }
        Validaciones.fechaNacimiento(fecha)?.let { return AuthResult.Error(it) }

        return try {
            if (!nube.nombreDisponible(nombre)) {
                return AuthResult.Error("Ese nombre de usuario ya está en uso.")
            }
            val cuenta = nube.auth.createUserWithEmailAndPassword(email, contrasena).await().user
                ?: return AuthResult.Error("No se pudo crear la cuenta. Intenta de nuevo.")
            val nuevo = User(
                id = cuenta.uid,
                nombreUsuario = nombre,
                correo = email,
                fechaNacimiento = fecha
            )
            try {
                nube.crearPerfil(nuevo)
            } catch (e: Exception) {
                // Sin perfil la cuenta queda inservible: se borra para poder intentarlo de nuevo.
                Log.e(TAG, "No se pudo crear el perfil", e)
                runCatching { cuenta.delete().await() }
                nube.auth.signOut()
                return AuthResult.Error(
                    "No se pudo crear la cuenta. Puede que ese nombre de usuario ya esté en uso."
                )
            }
            AuthResult.Exito(nuevo)
        } catch (e: Exception) {
            AuthResult.Error(mensajeDeError(e, registro = true))
        }
    }

    /** Entra con el nombre de usuario o el correo de una cuenta ya creada. */
    suspend fun iniciarSesion(identificador: String, contrasena: String): AuthResult {
        val ident = identificador.trim()
        if (ident.isEmpty() || contrasena.isEmpty()) {
            return AuthResult.Error("Completa todos los campos.")
        }
        return try {
            val correo = if ("@" in ident) ident else nube.correoDeNombre(ident)
                ?: return AuthResult.Error(
                    "No existe una cuenta con ese usuario o correo. Crea una cuenta primero."
                )
            val uid = nube.auth.signInWithEmailAndPassword(correo, contrasena).await().user?.uid
                ?: return AuthResult.Error("No se pudo iniciar sesión. Intenta de nuevo.")
            nube.escuchar(uid)
            val usuario = nube.usuarios[uid]
            if (usuario == null) {
                cerrarSesion()
                return AuthResult.Error("No se encontró el perfil de esta cuenta.")
            }
            AuthResult.Exito(usuario)
        } catch (e: Exception) {
            AuthResult.Error(mensajeDeError(e, registro = false))
        }
    }

    /** Envía el correo de Firebase para cambiar la contraseña. Devuelve un error o null. */
    suspend fun enviarRestablecerContrasena(correo: String): String? = try {
        nube.auth.sendPasswordResetEmail(correo).await()
        null
    } catch (e: FirebaseNetworkException) {
        "Sin conexión a internet. Revisa tu red e intenta de nuevo."
    } catch (e: FirebaseTooManyRequestsException) {
        "Demasiados intentos. Espera unos minutos e intenta de nuevo."
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo enviar el correo de contraseña", e)
        "No se pudo enviar el correo. Intenta de nuevo."
    }

    fun cerrarSesion() {
        nube.detener()
        nube.auth.signOut()
    }

    private fun mensajeDeError(e: Exception, registro: Boolean): String {
        Log.w(TAG, "Error de autenticación", e)
        return when (e) {
            is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red e intenta de nuevo."
            is FirebaseTooManyRequestsException ->
                "Demasiados intentos. Espera unos minutos e intenta de nuevo."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usa una más larga."
            is FirebaseAuthInvalidUserException ->
                "No existe una cuenta con ese usuario o correo. Crea una cuenta primero."
            is FirebaseAuthInvalidCredentialsException ->
                if (registro) "El correo no es válido." else "El usuario o la contraseña son incorrectos."
            else -> if (registro) "No se pudo crear la cuenta. Intenta de nuevo."
            else "No se pudo iniciar sesión. Intenta de nuevo."
        }
    }

    // ---------- Perfil ----------

    fun elegirRol(usuarioId: String, rol: UserRole): User? =
        actualizarUsuario(usuarioId, mapOf("rol" to rol.name)) { it.copy(rol = rol) }

    fun actualizarFotoPerfil(usuarioId: String, fotoUri: String?): User? {
        val actualizado = actualizarUsuario(usuarioId, mapOf("fotoPerfilUri" to fotoUri)) {
            it.copy(fotoPerfilUri = fotoUri)
        }
        if (fotoUri != null) {
            nube.subirImagen(fotoUri, "usuarios/$usuarioId/perfil-${System.currentTimeMillis()}") { url ->
                // Solo si no la cambió otra vez mientras se subía.
                if (nube.usuarios[usuarioId]?.fotoPerfilUri == fotoUri) {
                    actualizarUsuario(usuarioId, mapOf("fotoPerfilUri" to url)) {
                        it.copy(fotoPerfilUri = url)
                    }
                }
            }
        }
        return actualizado
    }

    fun actualizarNequi(usuarioId: String, nequi: String?): User? =
        actualizarUsuario(usuarioId, mapOf("nequi" to nequi)) { it.copy(nequi = nequi) }

    private fun actualizarUsuario(
        usuarioId: String,
        campos: Map<String, Any?>,
        cambio: (User) -> User
    ): User? {
        if (nube.usuarios[usuarioId] == null) return null
        nube.usuarios.actualizar(usuarioId, campos, cambio)
        return nube.usuarios[usuarioId]
    }

    fun usuarioPorId(id: String): User? = nube.usuarios[id]

    // ---------- Canchas ----------

    fun canchaDe(ownerId: String): Cancha? = nube.canchas.todos.firstOrNull { it.ownerId == ownerId }

    fun canchaPorId(id: String): Cancha? = nube.canchas[id]

    /** Todas las canchas registradas por los dueños (las que ve el jugador para reservar). */
    fun todasLasCanchas(): List<Cancha> = nube.canchas.todos

    /**
     * Crea o actualiza la cancha del dueño (una por dueño). Si el dueño ya tenía cancha se
     * conserva su id, para que sus reservas y productos sigan apuntando a ella.
     */
    fun guardarCancha(cancha: Cancha) {
        val anterior = canchaDe(cancha.ownerId)
        val aGuardar = if (anterior != null) cancha.copy(id = anterior.id) else cancha
        nube.canchas.guardar(aGuardar.id, aGuardar)
        val id = aGuardar.id
        nube.subirImagen(aGuardar.imagenUri, "canchas/${aGuardar.ownerId}/$id-${System.currentTimeMillis()}") { url ->
            if (nube.canchas[id]?.imagenUri == aGuardar.imagenUri) {
                nube.canchas.actualizar(id, mapOf("imagenUri" to url)) { it.copy(imagenUri = url) }
            }
        }
    }

    // ---------- Equipos ----------

    fun todosLosEquipos(): List<Equipo> = nube.equipos.todos

    fun equiposDe(usuarioId: String): List<Equipo> =
        nube.equipos.todos.filter { it.creadorId == usuarioId }

    fun equipoPorId(id: String): Equipo? = nube.equipos[id]

    /** Crea el equipo o lo reemplaza si ya existe uno con el mismo id. */
    fun guardarEquipo(equipo: Equipo) {
        nube.equipos.guardar(equipo.id, equipo)
        nube.subirImagen(equipo.escudoUri, "equipos/${equipo.creadorId}/${equipo.id}-${System.currentTimeMillis()}") { url ->
            if (nube.equipos[equipo.id]?.escudoUri == equipo.escudoUri) {
                nube.equipos.actualizar(equipo.id, mapOf("escudoUri" to url)) { it.copy(escudoUri = url) }
            }
        }
    }

    // ---------- Productos del kiosco ----------

    fun productosDeCancha(canchaId: String): List<Producto> =
        nube.productos.todos.filter { it.canchaId == canchaId }

    /** Crea el producto o lo reemplaza si ya existe uno con el mismo id. */
    fun guardarProducto(producto: Producto) {
        nube.productos.guardar(producto.id, producto)
        val ownerId = nube.canchas[producto.canchaId]?.ownerId ?: return
        nube.subirImagen(producto.imagenUri, "productos/$ownerId/${producto.id}-${System.currentTimeMillis()}") { url ->
            if (nube.productos[producto.id]?.imagenUri == producto.imagenUri) {
                nube.productos.actualizar(producto.id, mapOf("imagenUri" to url)) { it.copy(imagenUri = url) }
            }
        }
    }

    fun eliminarProducto(id: String) = nube.productos.borrar(id)

    // ---------- Reservas ----------

    fun reservasDeUsuario(usuarioId: String): List<Reserva> =
        nube.reservas.todos.filter { it.usuarioId == usuarioId }

    fun reservasDeCancha(canchaId: String): List<Reserva> =
        nube.reservas.todos.filter { it.canchaId == canchaId }

    fun guardarReserva(reserva: Reserva) = nube.reservas.guardar(reserva.id, reserva)

    fun reservaPorId(id: String): Reserva? = nube.reservas[id]

    fun actualizarReserva(reserva: Reserva) = nube.reservas.guardar(reserva.id, reserva)

    // ---------- Grabaciones ----------

    fun grabacionesDeUsuario(usuarioId: String): List<Grabacion> =
        nube.grabaciones.todos.filter { it.usuarioId == usuarioId }

    fun grabacionPorId(id: String): Grabacion? = nube.grabaciones[id]

    fun reservaYaGrabada(reservaId: String): Boolean =
        nube.grabaciones.todos.any { it.reservaId == reservaId }

    /** Los videos se quedan en el teléfono que grabó; en la nube solo se guarda el registro. */
    fun guardarGrabacion(grabacion: Grabacion) = nube.grabaciones.guardar(grabacion.id, grabacion)

    fun renombrarGrabacion(id: String, nombre: String) =
        nube.grabaciones.actualizar(id, mapOf("nombre" to nombre)) { it.copy(nombre = nombre) }

    fun eliminarGrabacion(id: String) = nube.grabaciones.borrar(id)

    fun reservasSinGrabar(): Set<String> =
        nube.reservas.todos.filter { it.sinGrabar }.map { it.id }.toSet()

    fun marcarSinGrabar(reservaId: String) =
        nube.reservas.actualizar(reservaId, mapOf("sinGrabar" to true)) { it.copy(sinGrabar = true) }

    // ---------- Chat jugador-dueño ----------

    /** Mensajes de los chats en los que participa el usuario en sesión. */
    fun misMensajes(): List<Mensaje> = nube.mensajes.todos

    fun guardarMensaje(mensaje: Mensaje) = nube.mensajes.guardar(mensaje.id, mensaje)

    fun marcarMensajeLeido(id: String) =
        nube.mensajes.actualizar(id, mapOf("leido" to true)) { it.copy(leido = true) }

    // ---------- Notificaciones ----------

    fun notificacionesDe(usuarioId: String): List<Notificacion> =
        nube.notificaciones.todos
            .filter { it.usuarioId == usuarioId }
            .sortedByDescending { it.creadaEn }

    /** Guarda la notificación en la bandeja de su usuario (puede ser otra persona). */
    fun guardarNotificacion(notificacion: Notificacion) =
        nube.notificaciones.guardar(notificacion.id, notificacion)

    /** Aplica [cambio] a las notificaciones de [usuarioId] que cumplan [filtro]. */
    fun actualizarNotificaciones(
        usuarioId: String,
        filtro: (Notificacion) -> Boolean,
        cambio: (Notificacion) -> Notificacion
    ) {
        nube.notificaciones.todos
            .filter { it.usuarioId == usuarioId && filtro(it) }
            .forEach { nube.notificaciones.guardar(it.id, cambio(it)) }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
