package me.juliangarcia.sportygoapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Address
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
import androidx.compose.runtime.rememberUpdatedState
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.coroutines.resume

/** Centro de Colombia: vista inicial cuando aún no se conoce ninguna ubicación. */
private val CENTRO_COLOMBIA = GeoPoint(4.5709, -74.2973)

/** Color de los marcadores de canchas (verde de la app). */
private const val COLOR_MARCADOR_CANCHA = 0xFF2DBE5C.toInt()

/** Color del punto "Estás aquí" (azul, distinto del verde de las canchas). */
private const val COLOR_MI_UBICACION = 0xFF2F80ED.toInt()

/**
 * Ícono único de "Estás aquí": punto azul con borde blanco y halo, el mismo para jugador y
 * dueño de cancha. Es distinto del pin de las canchas para no confundirlos.
 */
private fun iconoMiUbicacion(context: Context): Drawable {
    val densidad = context.resources.displayMetrics.density
    val lado = (34 * densidad).toInt()
    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = lado / 2f
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    // Halo
    pincel.color = COLOR_MI_UBICACION
    pincel.alpha = 60
    canvas.drawCircle(c, c, c, pincel)
    // Borde blanco
    pincel.color = android.graphics.Color.WHITE
    canvas.drawCircle(c, c, 9 * densidad, pincel)
    // Punto
    pincel.color = COLOR_MI_UBICACION
    canvas.drawCircle(c, c, 6.5f * densidad, pincel)
    return BitmapDrawable(context.resources, bitmap)
}

/** Marcador "Estás aquí" (siempre el mismo ícono, centrado en el punto). */
private fun marcadorMiUbicacion(mapView: MapView, punto: GeoPoint): Marker =
    Marker(mapView).apply {
        position = punto
        title = "Estás aquí"
        icon = iconoMiUbicacion(mapView.context)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }

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
            state.ciudad = nombreCiudad(context, ubicacion.latitude, ubicacion.longitude)
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

/** Busca la dirección de unas coordenadas (null si no hay conexión o servicio). */
private suspend fun buscarDireccion(context: Context, lat: Double, lon: Double): Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    return withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { lista ->
                        if (cont.isActive) cont.resume(lista.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** Ej. "Medellín, Antioquia". */
private suspend fun nombreCiudad(context: Context, lat: Double, lon: Double): String? =
    buscarDireccion(context, lat, lon)?.let { d ->
        listOfNotNull(d.locality ?: d.subAdminArea, d.adminArea)
            .distinct()
            .joinToString(", ")
            .ifEmpty { null }
    }

/** Ej. "Carrera 43A #1-50, Medellín". Null si no se pudo obtener. */
suspend fun direccionDe(context: Context, punto: GeoPoint): String? =
    buscarDireccion(context, punto.latitude, punto.longitude)?.let { d ->
        // El número a veces ya trae el "#" (ej. "# 49-45").
        val numero = d.subThoroughfare?.trim()?.removePrefix("#")?.trim()?.takeIf { it.isNotEmpty() }
        val calle = when {
            d.thoroughfare == null -> d.featureName
            numero == null -> d.thoroughfare
            else -> "${d.thoroughfare} #$numero"
        }
        listOfNotNull(calle, d.locality ?: d.subAdminArea)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .ifEmpty { null }
    }

/** Una cancha dibujada en el mapa. */
data class MarcadorCancha(val id: String, val punto: GeoPoint, val titulo: String)

/** Crea un MapView de OpenStreetMap atado al ciclo de vida de la pantalla. */
@Composable
private fun rememberMapView(): MapView {
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
            controller.setCenter(CENTRO_COLOMBIA)
        }
    }

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
    return mapView
}

@Composable
private fun MapaAndroidView(
    mapView: MapView,
    modifier: Modifier,
    interactivo: Boolean = true,
    onToque: () -> Unit = {}
) {
    val alTocar by rememberUpdatedState(onToque)
    AndroidView(
        factory = {
            mapView.apply {
                setOnTouchListener { v, _ ->
                    if (interactivo) {
                        alTocar()
                        // Dentro de una pantalla con scroll: el arrastre mueve el mapa, no la página.
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    } else {
                        true // Mapa de solo lectura: ignora los toques y deja hacer scroll a la página.
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Mapa de OpenStreetMap con un marcador "Estás aquí" en [ubicacion] y un marcador verde
 * por cada cancha de [canchas]. Al tocar una cancha se llama a [onCanchaClick] con su id.
 * Si no hay ubicación, se centra en la primera cancha (o en Colombia si no hay ninguna).
 */
@Composable
fun MapaCiudad(
    ubicacion: GeoPoint?,
    modifier: Modifier = Modifier,
    canchas: List<MarcadorCancha> = emptyList(),
    zoom: Double = 14.0,
    /** false = mapa fijo de solo lectura (ej. vista previa de la ubicación de la cancha). */
    interactivo: Boolean = true,
    /** true = centrar en el usuario; false = centrar en la primera cancha (si hay). */
    centrarEnUsuario: Boolean = true,
    onCanchaClick: (String) -> Unit = {}
) {
    val mapView = rememberMapView()
    val alTocarCancha by rememberUpdatedState(onCanchaClick)

    LaunchedEffect(mapView, ubicacion, canchas) {
        mapView.overlays.removeAll { it is Marker }

        // Primero el usuario y luego las canchas, para que las canchas queden encima y
        // reciban el toque aunque estén en el mismo punto.
        if (ubicacion != null) mapView.overlays.add(marcadorMiUbicacion(mapView, ubicacion))

        canchas.forEach { c ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = c.punto
                    title = c.titulo
                    icon = icon?.mutate()?.apply { setTint(COLOR_MARCADOR_CANCHA) }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        alTocarCancha(c.id)
                        true
                    }
                }
            )
        }

        val primeraCancha = canchas.firstOrNull()?.punto
        val centro = if (centrarEnUsuario) ubicacion ?: primeraCancha else primeraCancha ?: ubicacion
        if (centro != null) {
            mapView.controller.setZoom(zoom)
            mapView.controller.setCenter(centro)
        }
        mapView.invalidate()
    }

    MapaAndroidView(mapView, modifier, interactivo)
}

/**
 * Mapa para ELEGIR un punto: el usuario arrastra el mapa y el punto elegido es el centro
 * (la pantalla dibuja un indicador fijo encima). Cada vez que el centro cambia se llama a
 * [onCentroChange]. Cuando [irA] o [recentrar] cambian (ej. llega la ubicación del usuario o
 * se toca "mi ubicación"), el mapa se mueve a [irA].
 */
@Composable
fun MapaSeleccion(
    irA: GeoPoint?,
    onCentroChange: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    recentrar: Int = 0,
    /** Ubicación actual del usuario: se dibuja con el ícono "Estás aquí". */
    ubicacionUsuario: GeoPoint? = null,
    /** Se llama cuando el usuario toca/arrastra el mapa. */
    onToque: () -> Unit = {}
) {
    val mapView = rememberMapView()
    val alCambiarCentro by rememberUpdatedState(onCentroChange)

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            private fun avisar(): Boolean {
                val c = mapView.mapCenter
                alCambiarCentro(GeoPoint(c.latitude, c.longitude))
                return false
            }
            override fun onScroll(event: ScrollEvent?) = avisar()
            override fun onZoom(event: ZoomEvent?) = avisar()
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    LaunchedEffect(mapView, ubicacionUsuario) {
        mapView.overlays.removeAll { it is Marker }
        if (ubicacionUsuario != null) {
            mapView.overlays.add(marcadorMiUbicacion(mapView, ubicacionUsuario))
        }
        mapView.invalidate()
    }

    LaunchedEffect(mapView, irA, recentrar) {
        if (irA != null) {
            mapView.controller.setZoom(17.0)
            mapView.controller.setCenter(irA)
            alCambiarCentro(irA)
        }
    }

    MapaAndroidView(mapView, modifier, onToque = onToque)
}
