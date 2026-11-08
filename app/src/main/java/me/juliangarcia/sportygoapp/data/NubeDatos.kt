package me.juliangarcia.sportygoapp.data

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Datos de la app en Firebase (Firestore + Storage).
 *
 * Mientras hay una sesión iniciada, cada colección se escucha en tiempo real y se guarda una
 * copia en memoria. Así el resto de la app puede leer de forma inmediata (como antes con
 * SharedPreferences) y ve al instante lo que otros usuarios cambian en otros teléfonos.
 * Firestore además guarda una caché en el teléfono, así que la app funciona sin conexión y
 * sincroniza los cambios cuando vuelve la red.
 *
 * Colecciones:
 *  - usuarios/{uid}          perfil público de cada cuenta (la contraseña la maneja Firebase Auth)
 *  - nombresUsuario/{nombre} nombre en minúsculas -> uid y correo (login con nombre de usuario)
 *  - canchas/{id}, productos/{id}, equipos/{id}, reservas/{id}
 *  - grabaciones/{id}        solo se descargan las del usuario en sesión
 *  - notificaciones/{id}     solo se descargan las del usuario en sesión
 *  - mensajes/{id}           chat jugador-dueño; solo los de los chats del usuario en sesión
 */
object NubeDatos {

    private const val TAG = "NubeDatos"

    val auth by lazy { Firebase.auth }
    private val db by lazy { Firebase.firestore }
    private val storage by lazy { Firebase.storage }

    /** Se llama cuando llegan cambios desde la nube (de este u otro teléfono). */
    var onCambio: (() -> Unit)? = null

    private var uidEscuchado: String? = null
    private val escuchas = mutableListOf<ListenerRegistration>()

    val usuarios = Coleccion("usuarios", ::leerUsuario, ::escribirUsuario)
    val canchas = Coleccion("canchas", ::leerCancha, ::escribirCancha)
    val productos = Coleccion("productos", ::leerProducto, ::escribirProducto)
    val equipos = Coleccion("equipos", ::leerEquipo, ::escribirEquipo)
    val reservas = Coleccion("reservas", ::leerReserva, ::escribirReserva)
    val grabaciones = Coleccion("grabaciones", ::leerGrabacion, ::escribirGrabacion)
    val notificaciones = Coleccion("notificaciones", ::leerNotificacion, ::escribirNotificacion)
    val mensajes = Coleccion("mensajes", ::leerMensaje, ::escribirMensaje)

    // ---------------------------------------------------------------
    // Escucha en tiempo real
    // ---------------------------------------------------------------

    /**
     * Empieza a escuchar los datos que necesita el usuario [uid] y espera la primera carga
     * (máximo 15 s; si no hay red se sigue con lo que haya en la caché del teléfono).
     */
    suspend fun escuchar(uid: String) {
        if (uidEscuchado == uid && escuchas.isNotEmpty()) return
        detener()
        uidEscuchado = uid
        val cargas = listOf(
            usuarios.escuchar(db.collection("usuarios")),
            canchas.escuchar(db.collection("canchas")),
            productos.escuchar(db.collection("productos")),
            equipos.escuchar(db.collection("equipos")),
            reservas.escuchar(db.collection("reservas")),
            grabaciones.escuchar(db.collection("grabaciones").whereEqualTo("usuarioId", uid)),
            notificaciones.escuchar(db.collection("notificaciones").whereEqualTo("usuarioId", uid)),
            // Chats: solo los mensajes de las conversaciones en las que participa.
            mensajes.escuchar(db.collection("mensajes").whereArrayContains("participantes", uid))
        )
        withTimeoutOrNull(15_000) { cargas.awaitAll() }
    }

    /** Deja de escuchar y borra la copia en memoria (al cerrar sesión). */
    fun detener() {
        escuchas.forEach { it.remove() }
        escuchas.clear()
        uidEscuchado = null
        listOf(usuarios, canchas, productos, equipos, reservas, grabaciones, notificaciones, mensajes)
            .forEach { it.limpiar() }
    }

    /**
     * Una colección de Firestore con su copia en memoria. Las escrituras actualizan la copia
     * al instante y se envían a la nube; si la nube las rechaza (p. ej. por las reglas de
     * seguridad), la siguiente actualización del listener deja la copia como en la nube.
     */
    class Coleccion<T>(
        val nombre: String,
        private val leer: (DocumentSnapshot) -> T,
        private val escribir: (T) -> Map<String, Any?>
    ) {
        var datos: Map<String, T> = emptyMap()
            private set

        val todos: List<T> get() = datos.values.toList()

        operator fun get(id: String): T? = datos[id]

        internal fun limpiar() {
            datos = emptyMap()
        }

        internal fun escuchar(query: Query): CompletableDeferred<Unit> {
            val primeraCarga = CompletableDeferred<Unit>()
            escuchas += query.addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "No se pudo leer '$nombre'", error)
                    primeraCarga.complete(Unit)
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                datos = snap.documents.mapNotNull { doc -> leerSeguro(doc)?.let { doc.id to it } }
                    .toMap()
                primeraCarga.complete(Unit)
                onCambio?.invoke()
            }
            return primeraCarga
        }

        /** Lee un documento; si está dañado (falta un campo obligatorio) se omite. */
        internal fun leerSeguro(doc: DocumentSnapshot): T? = try {
            leer(doc)
        } catch (e: Exception) {
            Log.w(TAG, "Documento ${doc.reference.path} dañado; se omite", e)
            null
        }

        /** Crea o reemplaza el documento [id]. */
        fun guardar(id: String, item: T) {
            datos = datos + (id to item)
            Firebase.firestore.collection(nombre).document(id).set(escribir(item))
                .addOnFailureListener { Log.e(TAG, "No se pudo guardar $nombre/$id", it) }
        }

        /** Cambia solo algunos campos del documento [id] (y la copia en memoria con [local]). */
        fun actualizar(id: String, campos: Map<String, Any?>, local: (T) -> T) {
            datos[id]?.let { datos = datos + (id to local(it)) }
            Firebase.firestore.collection(nombre).document(id).set(campos, SetOptions.merge())
                .addOnFailureListener { Log.e(TAG, "No se pudo actualizar $nombre/$id", it) }
        }

        fun borrar(id: String) {
            datos = datos - id
            Firebase.firestore.collection(nombre).document(id).delete()
                .addOnFailureListener { Log.e(TAG, "No se pudo borrar $nombre/$id", it) }
        }

        /** Lectura puntual a la nube (para los avisos que llegan con la app cerrada). */
        suspend fun leerRemoto(id: String): T? = try {
            Firebase.firestore.collection(nombre).document(id).get().await()
                .takeIf { it.exists() }?.let { leerSeguro(it) }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer $nombre/$id", e)
            null
        }

        /** Consulta puntual a la nube: documentos donde [campo] == [valor]. */
        suspend fun buscarRemoto(campo: String, valor: Any): List<T> = try {
            Firebase.firestore.collection(nombre).whereEqualTo(campo, valor).get().await()
                .documents.mapNotNull { leerSeguro(it) }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo consultar $nombre", e)
            emptyList()
        }
    }

    // ---------------------------------------------------------------
    // Nombres de usuario (para iniciar sesión con el nombre en vez del correo)
    // ---------------------------------------------------------------

    suspend fun correoDeNombre(nombreUsuario: String): String? =
        db.collection("nombresUsuario").document(nombreUsuario.lowercase()).get().await()
            .getString("correo")

    suspend fun nombreDisponible(nombreUsuario: String): Boolean =
        !db.collection("nombresUsuario").document(nombreUsuario.lowercase()).get().await().exists()

    /** Crea el perfil y reserva el nombre de usuario en una sola operación. */
    suspend fun crearPerfil(usuario: User) {
        db.runBatch { batch ->
            batch.set(db.collection("usuarios").document(usuario.id), escribirUsuario(usuario))
            batch.set(
                db.collection("nombresUsuario").document(usuario.nombreUsuario.lowercase()),
                mapOf("uid" to usuario.id, "correo" to usuario.correo)
            )
        }.await()
    }

    // ---------------------------------------------------------------
    // Imágenes (Cloud Storage)
    // ---------------------------------------------------------------

    /**
     * Sube a Cloud Storage una imagen que está en el teléfono (file:// o content://) y, cuando
     * termina, entrega su URL pública con [alSubir], para que otros teléfonos también la vean.
     * Si Storage no está activado en el proyecto, la imagen se queda solo en este teléfono.
     */
    fun subirImagen(uriLocal: String, ruta: String, alSubir: (String) -> Unit) {
        if (!uriLocal.startsWith("file:") && !uriLocal.startsWith("content:")) return
        val ref = storage.reference.child(ruta)
        ref.putFile(Uri.parse(uriLocal), storageMetadata { contentType = "image/jpeg" })
            .continueWithTask { tarea ->
                tarea.exception?.let { throw it }
                ref.downloadUrl
            }
            .addOnSuccessListener { alSubir(it.toString()) }
            .addOnFailureListener { Log.w(TAG, "No se pudo subir la imagen a $ruta", it) }
    }

    // ---------------------------------------------------------------
    // Conversión entre documentos de Firestore y los modelos de la app
    // ---------------------------------------------------------------

    private fun DocumentSnapshot.texto(campo: String): String = getString(campo) ?: ""
    private fun DocumentSnapshot.obligatorio(campo: String): String =
        getString(campo) ?: throw IllegalStateException("Falta '$campo'")
    private fun DocumentSnapshot.entero(campo: String): Int = getLong(campo)?.toInt() ?: 0
    private fun DocumentSnapshot.largo(campo: String): Long = getLong(campo) ?: 0L

    private fun leerUsuario(d: DocumentSnapshot) = User(
        id = d.id,
        nombreUsuario = d.obligatorio("nombreUsuario"),
        correo = d.texto("correo"),
        fechaNacimiento = d.texto("fechaNacimiento"),
        rol = UserRole.entries.firstOrNull { it.name == d.getString("rol") },
        fotoPerfilUri = d.getString("fotoPerfilUri")?.ifEmpty { null },
        nequi = d.getString("nequi")?.ifEmpty { null }
    )

    private fun escribirUsuario(u: User) = mapOf(
        "nombreUsuario" to u.nombreUsuario,
        "correo" to u.correo,
        "fechaNacimiento" to u.fechaNacimiento,
        "rol" to u.rol?.name,
        "fotoPerfilUri" to u.fotoPerfilUri,
        "nequi" to u.nequi
    )

    private fun leerCancha(d: DocumentSnapshot) = Cancha(
        id = d.id,
        ownerId = d.obligatorio("ownerId"),
        imagenUri = d.texto("imagenUri"),
        nombre = d.obligatorio("nombre"),
        descripcion = d.texto("descripcion"),
        capacidad = d.entero("capacidad"),
        horaApertura = d.texto("horaApertura"),
        horaCierre = d.texto("horaCierre"),
        precioHora = d.entero("precioHora"),
        latitud = d.getDouble("latitud"),
        longitud = d.getDouble("longitud"),
        direccion = d.texto("direccion"),
        kiosco = d.texto("kiosco")
    )

    private fun escribirCancha(c: Cancha) = mapOf(
        "ownerId" to c.ownerId,
        "imagenUri" to c.imagenUri,
        "nombre" to c.nombre,
        "descripcion" to c.descripcion,
        "capacidad" to c.capacidad,
        "horaApertura" to c.horaApertura,
        "horaCierre" to c.horaCierre,
        "precioHora" to c.precioHora,
        "latitud" to c.latitud,
        "longitud" to c.longitud,
        "direccion" to c.direccion,
        "kiosco" to c.kiosco
    )

    private fun leerProducto(d: DocumentSnapshot) = Producto(
        id = d.id,
        canchaId = d.obligatorio("canchaId"),
        nombre = d.obligatorio("nombre"),
        precio = d.entero("precio"),
        categoria = CategoriaProducto.entries.firstOrNull { it.name == d.getString("categoria") }
            ?: CategoriaProducto.BEBIDA,
        imagenUri = d.texto("imagenUri"),
        iconoId = d.texto("iconoId")
    )

    private fun escribirProducto(p: Producto) = mapOf(
        "canchaId" to p.canchaId,
        "nombre" to p.nombre,
        "precio" to p.precio,
        "categoria" to p.categoria.name,
        "imagenUri" to p.imagenUri,
        "iconoId" to p.iconoId
    )

    private fun leerEquipo(d: DocumentSnapshot) = Equipo(
        id = d.id,
        creadorId = d.obligatorio("creadorId"),
        nombre = d.obligatorio("nombre"),
        escudoUri = d.texto("escudoUri"),
        creadoEn = d.largo("creadoEn")
    )

    private fun escribirEquipo(e: Equipo) = mapOf(
        "creadorId" to e.creadorId,
        "nombre" to e.nombre,
        "escudoUri" to e.escudoUri,
        "creadoEn" to e.creadoEn
    )

    private fun leerReserva(d: DocumentSnapshot): Reserva {
        @Suppress("UNCHECKED_CAST")
        val items = (d.get("productos") as? List<Map<String, Any?>>).orEmpty()
        return Reserva(
            id = d.id,
            usuarioId = d.obligatorio("usuarioId"),
            canchaId = d.obligatorio("canchaId"),
            nombreCancha = d.texto("nombreCancha"),
            fecha = d.obligatorio("fecha"),
            hora = d.obligatorio("hora"),
            duracionHoras = d.entero("duracionHoras").coerceAtLeast(1),
            precio = d.entero("precio"),
            // Un producto dañado no invalida la reserva completa.
            productos = items.mapNotNull { m ->
                ItemCompra(
                    productoId = m["productoId"] as? String ?: return@mapNotNull null,
                    nombre = m["nombre"] as? String ?: return@mapNotNull null,
                    precioUnitario = (m["precioUnitario"] as? Number)?.toInt() ?: 0,
                    cantidad = (m["cantidad"] as? Number)?.toInt() ?: 0
                )
            },
            estado = EstadoReserva.entries.firstOrNull { it.name == d.getString("estado") }
                ?: EstadoReserva.CONFIRMADA,
            pagoId = d.texto("pagoId"),
            nequiPagador = d.texto("nequiPagador"),
            nequiDestino = d.texto("nequiDestino"),
            equipoId = d.texto("equipoId"),
            nombreEquipo = d.texto("nombreEquipo"),
            sinGrabar = d.getBoolean("sinGrabar") ?: false
        )
    }

    private fun escribirReserva(r: Reserva) = mapOf(
        "usuarioId" to r.usuarioId,
        "canchaId" to r.canchaId,
        "nombreCancha" to r.nombreCancha,
        "fecha" to r.fecha,
        "hora" to r.hora,
        "duracionHoras" to r.duracionHoras,
        "precio" to r.precio,
        "productos" to r.productos.map {
            mapOf(
                "productoId" to it.productoId,
                "nombre" to it.nombre,
                "precioUnitario" to it.precioUnitario,
                "cantidad" to it.cantidad
            )
        },
        "estado" to r.estado.name,
        "pagoId" to r.pagoId,
        "nequiPagador" to r.nequiPagador,
        "nequiDestino" to r.nequiDestino,
        "equipoId" to r.equipoId,
        "nombreEquipo" to r.nombreEquipo,
        "sinGrabar" to r.sinGrabar
    )

    private fun leerGrabacion(d: DocumentSnapshot) = Grabacion(
        id = d.id,
        usuarioId = d.obligatorio("usuarioId"),
        videoUri = d.obligatorio("videoUri"),
        fechaGuardado = d.texto("fechaGuardado"),
        horaReserva = d.texto("horaReserva"),
        nombreCancha = d.texto("nombreCancha"),
        reservaId = d.texto("reservaId"),
        equipoId = d.texto("equipoId"),
        nombreEquipo = d.texto("nombreEquipo"),
        rivalId = d.texto("rivalId"),
        nombreRival = d.texto("nombreRival"),
        nombre = d.texto("nombre")
    )

    private fun escribirGrabacion(g: Grabacion) = mapOf(
        "usuarioId" to g.usuarioId,
        "videoUri" to g.videoUri,
        "fechaGuardado" to g.fechaGuardado,
        "horaReserva" to g.horaReserva,
        "nombreCancha" to g.nombreCancha,
        "reservaId" to g.reservaId,
        "equipoId" to g.equipoId,
        "nombreEquipo" to g.nombreEquipo,
        "rivalId" to g.rivalId,
        "nombreRival" to g.nombreRival,
        "nombre" to g.nombre
    )

    private fun leerNotificacion(d: DocumentSnapshot) = Notificacion(
        id = d.id,
        usuarioId = d.obligatorio("usuarioId"),
        tipo = TipoNotificacion.entries.firstOrNull { it.name == d.getString("tipo") }
            ?: TipoNotificacion.CUENTA_CREADA,
        titulo = d.texto("titulo"),
        mensaje = d.texto("mensaje"),
        creadaEn = d.largo("creadaEn"),
        leida = d.getBoolean("leida") ?: false,
        mostrada = d.getBoolean("mostrada") ?: false
    )

    private fun leerMensaje(d: DocumentSnapshot) = Mensaje(
        id = d.id,
        chatId = d.obligatorio("chatId"),
        canchaId = d.obligatorio("canchaId"),
        jugadorId = d.obligatorio("jugadorId"),
        duenioId = d.obligatorio("duenioId"),
        autorId = d.obligatorio("autorId"),
        texto = d.texto("texto"),
        enviadoEn = d.largo("enviadoEn"),
        leido = d.getBoolean("leido") ?: false
    )

    private fun escribirMensaje(m: Mensaje) = mapOf(
        "chatId" to m.chatId,
        "canchaId" to m.canchaId,
        "jugadorId" to m.jugadorId,
        "duenioId" to m.duenioId,
        // Para las reglas de seguridad y para escuchar solo los chats propios.
        "participantes" to listOf(m.jugadorId, m.duenioId),
        "autorId" to m.autorId,
        "texto" to m.texto,
        "enviadoEn" to m.enviadoEn,
        "leido" to m.leido
    )

    private fun escribirNotificacion(n: Notificacion) = mapOf(
        "usuarioId" to n.usuarioId,
        "tipo" to n.tipo.name,
        "titulo" to n.titulo,
        "mensaje" to n.mensaje,
        "creadaEn" to n.creadaEn,
        "leida" to n.leida,
        "mostrada" to n.mostrada
    )
}
