package me.juliangarcia.sportygoapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.coroutines.resume

/** Estado de la ubicación del usuario para el mapa de canchas. */
@Stable
class UbicacionUsuarioState {
    var permisoConcedido by mutableStateOf(false)
        internal set
    var punto: GeoPoint? by mutableStateOf(null)
        internal set
    /** Ej. "Medellín, Antioquia". */
    var ciudad: String? by mutableStateOf(null)
        internal set
    var buscando by mutableStateOf(false)
        internal set

    /** Pide el permiso (si falta) y vuelve a obtener la ubicación. */
    var solicitar: () -> Unit = {}
        internal set
}

/**
 * Pide el permiso de ubicación al entrar y obtiene la posición actual y el nombre de la ciudad.
 */
@Composable
fun rememberUbicacionUsuario(): UbicacionUsuarioState {
    val context = LocalContext.current
    val state = remember { UbicacionUsuarioState() }
    var intento by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        state.permisoConcedido = resultado.values.any { it }
        if (state.permisoConcedido) intento++
    }

    state.solicitar = {
        if (tienePermisoUbicacion(context)) {
            state.permisoConcedido = true
            intento++
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Al abrir la pantalla: pedir permiso / ubicación una vez.
    LaunchedEffect(Unit) { state.solicitar() }

    LaunchedEffect(intento) {
        if (intento == 0 || !tienePermisoUbicacion(context)) return@LaunchedEffect
        state.buscando = true
        val ubicacion = obtenerUbicacion(context)
        if (ubicacion != null) {
            state.punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)
            state.ciudad = nombreCiudad(context, ubicacion)
        }
        state.buscando = false
    }

    return state
}

private fun tienePermisoUbicacion(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@SuppressLint("MissingPermission") // Se verifica con tienePermisoUbicacion antes de llamar.
private suspend fun obtenerUbicacion(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val proveedores = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    ).filter { lm.isProviderEnabled(it) }

    // 1) Ubicación actual (más precisa).
    for (proveedor in proveedores) {
        val actual = suspendCancellableCoroutine<Location?> { cont ->
            val cancel = android.os.CancellationSignal()
            cont.invokeOnCancellation { cancel.cancel() }
            LocationManagerCompat.getCurrentLocation(
                lm, proveedor, cancel, ContextCompat.getMainExecutor(context)
            ) { loc -> if (cont.isActive) cont.resume(loc) }
        }
        if (actual != null) return actual
    }
    // 2) Última ubicación conocida.
    return proveedores.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
}

private suspend fun nombreCiudad(context: Context, loc: Location): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    return withContext(Dispatchers.IO) {
        try {
            val direccion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { lista ->
                        if (cont.isActive) cont.resume(lista.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
            }
            direccion?.let { d ->
                listOfNotNull(d.locality ?: d.subAdminArea, d.adminArea)
                    .distinct()
                    .joinToString(", ")
                    .ifEmpty { null }
            }
        } catch (e: Exception) {
            null // Sin conexión o servicio no disponible: se muestra solo el mapa.
        }
    }
}

/**
 * Mapa de OpenStreetMap centrado en [ubicacion] con un marcador "Estás aquí".
 * Si [ubicacion] es null, muestra el mapa de Colombia completo.
 */
@Composable
fun MapaCiudad(
    ubicacion: GeoPoint?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // OpenStreetMap exige un User-Agent que identifique a la app.
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
            )
            controller.setZoom(5.5)
            controller.setCenter(GeoPoint(4.5709, -74.2973)) // Colombia
        }
    }

    // El MapView necesita seguir el ciclo de vida de la pantalla.
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Solo se recentra cuando cambia la ubicación (no en cada recomposición).
    LaunchedEffect(mapView, ubicacion) {
        mapView.overlays.removeAll { it is Marker }
        if (ubicacion != null) {
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = ubicacion
                    title = "Estás aquí"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            mapView.controller.setZoom(14.0)
            mapView.controller.animateTo(ubicacion)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = {
            mapView.apply {
                // Dentro de una pantalla con scroll: el arrastre mueve el mapa, no la página.
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
        },
        modifier = modifier
    )
}
