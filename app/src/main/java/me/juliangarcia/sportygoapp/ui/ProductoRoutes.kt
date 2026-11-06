package me.juliangarcia.sportygoapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import me.juliangarcia.sportygoapp.data.ArchivosLocales
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.CategoriaProducto
import me.juliangarcia.sportygoapp.ui.screens.CreateProductScreen
import java.io.File
import java.util.UUID

/**
 * Agregar o editar ([productoId] != null) un producto del kiosco de la cancha del dueño.
 * La foto puede venir de la galería o de la cámara; se copia a los archivos de la app.
 */
@Composable
fun ProductoFormRoute(
    auth: AuthState,
    productoId: String?,
    onTerminado: () -> Unit
) {
    val context = LocalContext.current
    val existente = remember(productoId) { productoId?.let { auth.productoPorId(it) } }

    // Si venía a editar un producto que ya no existe, vuelve al inventario.
    if (productoId != null && existente == null) {
        LaunchedEffect(Unit) { onTerminado() }
        return
    }

    // Id fijo desde el inicio: la foto se guarda con el nombre del producto.
    val id = rememberSaveable { productoId ?: UUID.randomUUID().toString() }
    var nombre by rememberSaveable { mutableStateOf(existente?.nombre ?: "") }
    var precio by rememberSaveable { mutableStateOf(existente?.precio?.toString() ?: "") }
    var categoria by rememberSaveable { mutableStateOf(existente?.categoria ?: CategoriaProducto.BEBIDA) }
    var imagen by rememberSaveable { mutableStateOf(existente?.imagenUri ?: "") }
    var iconoId by rememberSaveable { mutableStateOf(existente?.iconoId?.ifBlank { null } ?: "bebida") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Archivo temporal donde la cámara deja la foto.
    var fotoTemporal by rememberSaveable { mutableStateOf<String?>(null) }

    fun usarFoto(origen: Uri) {
        val copia = ArchivosLocales.copiar(context, origen, "productos", "producto-$id")
        if (copia != null) {
            imagen = copia
            error = null
        } else {
            Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
        }
    }

    // Galería: selector de fotos del sistema (no requiere permisos).
    val elegirDeGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) usarFoto(uri) }

    // Cámara: la app de cámara del teléfono guarda la foto en un archivo de la app.
    val tomarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val ruta = fotoTemporal
        if (ok && ruta != null) usarFoto(Uri.fromFile(File(ruta)))
        ruta?.let { File(it).delete() }
        fotoTemporal = null
    }

    fun abrirCamara() {
        val archivo = File(File(context.cacheDir, "fotos").apply { mkdirs() }, "producto-$id.jpg")
        fotoTemporal = archivo.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        try {
            tomarFoto.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "No hay una app de cámara disponible.", Toast.LENGTH_SHORT).show()
        }
    }

    // Como la app declara el permiso de cámara, Android exige concederlo para usar la cámara del sistema.
    val pedirPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirCamara()
        } else {
            Toast.makeText(
                context,
                "Sin permiso de cámara. Puedes elegir una foto de la galería o un ícono.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    CreateProductScreen(
        esEdicion = productoId != null,
        imagenUri = imagen,
        iconoSeleccionadoId = iconoId,
        nombre = nombre,
        precio = precio,
        categoria = categoria,
        errorMensaje = error,
        onBackClick = onTerminado,
        onGaleriaClick = {
            elegirDeGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onCamaraClick = {
            val tienePermiso = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (tienePermiso) abrirCamara() else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
        },
        onIconoSelected = { icono ->
            // Elegir un ícono reemplaza la foto.
            iconoId = icono.id
            imagen = ""
            if (existente == null && nombre.isBlank()) categoria = icono.categoria
            error = null
        },
        onNombreChange = { nombre = it.take(40); error = null },
        onPrecioChange = { valor ->
            precio = valor.filter { it.isDigit() }.take(9)
            error = null
        },
        onCategoriaChange = { categoria = it },
        onGuardarClick = {
            error = auth.guardarProducto(id, nombre, precio, categoria, imagen, iconoId)
            if (error == null) onTerminado()
        },
        onEliminarClick = {
            auth.eliminarProducto(id)
            onTerminado()
        }
    )
}
