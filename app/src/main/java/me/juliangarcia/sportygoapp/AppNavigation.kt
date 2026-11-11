package me.juliangarcia.sportygoapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ErrorOutline
import me.juliangarcia.sportygoapp.data.EstadoReserva
import me.juliangarcia.sportygoapp.pagos.NequiSimulado
import me.juliangarcia.sportygoapp.ui.screens.MetodoPagoScreen
import me.juliangarcia.sportygoapp.ui.screens.NequiPruebaScreen
import me.juliangarcia.sportygoapp.ui.ProductoFormRoute
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.juliangarcia.sportygoapp.data.Notificacion
import me.juliangarcia.sportygoapp.data.TipoNotificacion
import me.juliangarcia.sportygoapp.ui.screens.GrabarPartidoScreen
import me.juliangarcia.sportygoapp.ui.screens.NotificacionItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.navigation.NavType
import androidx.navigation.navArgument
import me.juliangarcia.sportygoapp.data.Reserva
import me.juliangarcia.sportygoapp.data.Validaciones
import me.juliangarcia.sportygoapp.ui.BuscarCanchasRoute
import me.juliangarcia.sportygoapp.ui.ChatRoute
import me.juliangarcia.sportygoapp.ui.ConfiguracionRoute
import me.juliangarcia.sportygoapp.ui.ConfirmarGolesRoute
import me.juliangarcia.sportygoapp.ui.GrabacionesRoute
import me.juliangarcia.sportygoapp.ui.ReproducirGrabacionRoute
import me.juliangarcia.sportygoapp.ui.ConversacionesRoute
import me.juliangarcia.sportygoapp.ui.CrearEquipoRoute
import me.juliangarcia.sportygoapp.ui.DetallesCanchaRoute
import me.juliangarcia.sportygoapp.ui.PerfilRoute
import me.juliangarcia.sportygoapp.ui.screens.ReservaHoyItem
import me.juliangarcia.sportygoapp.ui.screens.formatearPrecio
import java.time.DayOfWeek
import java.time.LocalDate
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.data.UserRole
import me.juliangarcia.sportygoapp.ui.AddFieldRoute
import me.juliangarcia.sportygoapp.ui.InicioSesionRoute
import me.juliangarcia.sportygoapp.ui.RegistrarUbicacionRoute
import me.juliangarcia.sportygoapp.ui.RegistroRoute
import me.juliangarcia.sportygoapp.ui.UbicacionCancha
import me.juliangarcia.sportygoapp.ui.screens.AddProductScreen
import me.juliangarcia.sportygoapp.ui.screens.CreateProductScreen
import me.juliangarcia.sportygoapp.ui.screens.InventaryOwnerScreen
import me.juliangarcia.sportygoapp.ui.screens.InventaryScreen
import me.juliangarcia.sportygoapp.ui.screens.LiveMatchScreen
import me.juliangarcia.sportygoapp.ui.screens.MatchVideoScreen
import me.juliangarcia.sportygoapp.ui.screens.MyReserveScreen
import me.juliangarcia.sportygoapp.ui.screens.NotificationsScreen
import me.juliangarcia.sportygoapp.ui.screens.OpenScreen
import me.juliangarcia.sportygoapp.ui.screens.OwnersPanelScreen
import me.juliangarcia.sportygoapp.ui.screens.SelectProfileScreen
import me.juliangarcia.sportygoapp.ui.screens.ShopScreen
import me.juliangarcia.sportygoapp.ui.screens.TeamsScreen

/** Quién puede entrar a una pantalla. */
private enum class Acceso {
    /** Cualquier usuario con sesión iniciada (aunque aún no haya elegido perfil). */
    SESION,

    /** Usuario con sesión y con perfil ya elegido (jugador o dueño). */
    CUALQUIER_ROL,

    JUGADOR,

    /** Dueño que todavía puede estar registrando su cancha. */
    DUENIO,

    /** Dueño que ya registró su cancha. */
    DUENIO_CON_CANCHA
}

@Composable
fun SportyGoNavHost(auth: AuthState, nequi: NequiSimulado, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = remember { auth.rutaInicial() }

    // El dueño debe marcar la ubicación en el mapa antes de llenar los datos de la cancha.
    var ubicacionLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var ubicacionLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var ubicacionDireccion by rememberSaveable { mutableStateOf("") }
    val ubicacionCancha = ubicacionLat?.let { lat ->
        ubicacionLon?.let { lon -> UbicacionCancha(lat, lon, ubicacionDireccion) }
    }

    // ---------------------------------------------------------------
    // Grabación automática: si al entrar a la app el anfitrión tiene una reserva en curso
    // (sin grabar), se abre directamente la cámara.
    // ---------------------------------------------------------------
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    var revisarGrabacion by remember { mutableStateOf(true) }

    // Cada vez que se entra (o se vuelve) a la app: recargar datos (pudo llegar un aviso).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        auth.refrescar()
        revisarGrabacion = true
    }
    // También justo después de iniciar sesión.
    LaunchedEffect(auth.usuario?.id) {
        if (auth.usuario != null) revisarGrabacion = true
    }
    LaunchedEffect(revisarGrabacion, rutaActual) {
        // Espera a estar en una pantalla con sesión (no en login ni elegir perfil).
        if (!revisarGrabacion || rutaActual == null || rutaActual in RUTAS_SIN_GRABACION_AUTOMATICA) {
            return@LaunchedEffect
        }
        revisarGrabacion = false
        // El usuario puede desactivarlo en Configuración.
        if (auth.preferencias?.camaraAutomatica == false) return@LaunchedEffect
        val reserva = auth.reservaParaGrabar() ?: return@LaunchedEffect
        if (rutaActual == Routes.GRABAR_PARTIDO_PATRON) return@LaunchedEffect
        // No interrumpir mientras se confirman los goles de otra grabación.
        if (rutaActual == Routes.CONFIRMAR_GOLES_PATRON) return@LaunchedEffect
        navController.navigate(Routes.grabarPartido(reserva.id)) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ---------------------------------------------------------------
        // RUTAS PÚBLICAS (solo sin sesión; con sesión se redirige al inicio)
        // ---------------------------------------------------------------
        composable(route = Routes.OPEN_SCREEN) {
            RutaPublica(auth, navController) {
                OpenScreen(
                    onIniciarSesionClick = { navController.irA(Routes.SELECT_ACCOUNT) },
                    onCrearCuentaClick = { navController.irA(Routes.LOGIN) }
                )
            }
        }

        // LOGIN = "Crea tu cuenta". Al crearla, la sesión queda iniciada y
        // RutaPublica redirige a SELECT_PROFILE.
        composable(route = Routes.LOGIN) {
            RutaPublica(auth, navController) { RegistroRoute(auth) }
        }

        // SELECT_ACCOUNT = "Inicia con tu cuenta". Solo funciona con una cuenta ya creada.
        composable(route = Routes.SELECT_ACCOUNT) {
            RutaPublica(auth, navController) { InicioSesionRoute(auth) }
        }

        // ---------------------------------------------------------------
        // ELEGIR PERFIL (requiere haber creado cuenta / iniciado sesión)
        // ---------------------------------------------------------------
        composable(route = Routes.SELECT_PROFILE) {
            RutaProtegida(Acceso.SESION, auth, navController) {
                SelectProfileScreen(
                    onJugadorClick = {
                        auth.elegirRol(UserRole.JUGADOR)
                        navController.irAInicio(auth)
                    },
                    onDuenioClick = {
                        auth.elegirRol(UserRole.DUENIO)
                        // Si ya tenía cancha va al panel; si no, a registrar su cancha.
                        navController.irAInicio(auth)
                    }
                )
            }
        }

        // ---------------------------------------------------------------
        // FLUJO DEL DUEÑO: ubicación -> datos de la cancha -> panel
        // ---------------------------------------------------------------
        composable(route = Routes.REGISTER_FIELD) {
            RutaProtegida(Acceso.DUENIO, auth, navController) {
                RegistrarUbicacionRoute(
                    inicial = ubicacionCancha,
                    onUbicacionRegistrada = { u ->
                        ubicacionLat = u.latitud
                        ubicacionLon = u.longitud
                        ubicacionDireccion = u.direccion
                        // Si venía de "Cambiar", vuelve a los datos (que siguen en el historial).
                        if (!navController.popBackStack(Routes.ADD_FIELD, inclusive = false)) {
                            navController.irA(Routes.ADD_FIELD)
                        }
                    }
                )
            }
        }

        composable(route = Routes.ADD_FIELD) {
            RutaProtegida(Acceso.DUENIO, auth, navController) {
                if (ubicacionCancha != null) {
                    AddFieldRoute(
                        auth = auth,
                        ubicacion = ubicacionCancha,
                        onCambiarUbicacionClick = { navController.irA(Routes.REGISTER_FIELD) },
                        onCanchaRegistrada = {
                            ubicacionLat = null
                            ubicacionLon = null
                            ubicacionDireccion = ""
                            navController.irAInicio(auth) // -> OWNERS_PANEL
                        }
                    )
                } else {
                    // No se puede saltar el paso de la ubicación.
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.REGISTER_FIELD) {
                            popUpTo(Routes.REGISTER_FIELD) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(route = Routes.OWNERS_PANEL) {
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                var filtroIngresos by rememberSaveable { mutableStateOf("Semana") }
                val resumen = resumenDuenio(auth.reservasDeMiCancha)
                OwnersPanelScreen(
                    canchaActiva = auth.usuario?.nequi != null,
                    onRegistrarNequiClick = { navController.irA(Routes.METODO_PAGO) },
                    hayNotificacionesSinLeer = auth.misNotificaciones.any { !it.leida },
                    onNotificacionesClick = { navController.irA(Routes.NOTIFICATIONS) },
                    nombreCancha = auth.cancha?.nombre ?: "",
                    reservasHoy = resumen.reservasHoy,
                    ingresosMonto = formatearPrecio(
                        if (filtroIngresos == "Mes") resumen.ingresosMes else resumen.ingresosSemana
                    ),
                    ingresosCambio = if (filtroIngresos == "Mes") "Este mes" else "Esta semana",
                    reservasCount = resumen.totalReservas.toString(),
                    reservasHoySubtexto = "${resumen.reservasHoy.size} hoy",
                    ingresosSemana = resumen.barrasSemana,
                    filtroIngresosTab = filtroIngresos,
                    onFiltroIngresosTabClick = { filtroIngresos = it },
                    onInventarioClick = { navController.irA(Routes.INVENTARY_OWNER) },
                    mensajesSinLeer = auth.totalMensajesSinLeer(),
                    onMensajesClick = { navController.irA(Routes.CHATS) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(route = Routes.INVENTARY_OWNER) {
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                var categoria by rememberSaveable { mutableStateOf("Todos") }
                InventaryOwnerScreen(
                    productos = auth.misProductos,
                    ubicacionKiosco = auth.cancha?.kiosco.orEmpty(),
                    categoriaSeleccionada = categoria,
                    tabBottomActual = "Inicio",
                    onBackClick = { navController.popBackStack() },
                    onCategoriaTabClick = { categoria = it },
                    onAgregarProductoClick = { navController.irA(Routes.CREATE_PRODUCT) },
                    onProductoClick = { p -> navController.irA(Routes.editarProducto(p.id)) },
                    onGuardarKiosco = { auth.actualizarKiosco(it) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(
            route = Routes.CREATE_PRODUCT_PATRON,
            arguments = listOf(navArgument(Routes.ARG_PRODUCTO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                ProductoFormRoute(
                    auth = auth,
                    productoId = entry.arguments?.getString(Routes.ARG_PRODUCTO_ID),
                    onTerminado = { navController.popBackStack() }
                )
            }
        }

        // ---------------------------------------------------------------
        // FLUJO DEL JUGADOR
        // ---------------------------------------------------------------
        composable(route = Routes.SEARCH_FIELDS) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                BuscarCanchasRoute(
                    auth = auth,
                    onNotificacionesClick = { navController.irA(Routes.NOTIFICATIONS) },
                    onCanchaClick = { cancha ->
                        navController.irA(Routes.detallesCancha(cancha.id))
                    },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(
            route = Routes.DETALLES_CANCHA_PATRON,
            arguments = listOf(navArgument(Routes.ARG_CANCHA_ID) { type = NavType.StringType })
        ) { entry ->
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                DetallesCanchaRoute(
                    auth = auth,
                    canchaId = entry.arguments?.getString(Routes.ARG_CANCHA_ID).orEmpty(),
                    onBackClick = { navController.popBackStack() },
                    onReservaCreada = {
                        // Muestra "Mis reservas" y al volver regresa al inicio (no al detalle).
                        navController.navigate(Routes.MY_RESERVE) {
                            popUpTo(Routes.SEARCH_FIELDS) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    // Al crear el equipo vuelve al detalle con el equipo ya disponible.
                    onCrearEquipoClick = { navController.irA(Routes.CREAR_EQUIPO) },
                    onChatClick = {
                        val canchaId = entry.arguments?.getString(Routes.ARG_CANCHA_ID).orEmpty()
                        auth.usuario?.id?.let { yo -> navController.irA(Routes.chat(canchaId, yo)) }
                    }
                )
            }
        }

        // ---------------------------------------------------------------
        // CHAT JUGADOR - DUEÑO
        // ---------------------------------------------------------------
        composable(
            route = Routes.CHAT_PATRON,
            arguments = listOf(
                navArgument(Routes.ARG_CANCHA_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_JUGADOR_ID) { type = NavType.StringType }
            )
        ) { entry ->
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                ChatRoute(
                    auth = auth,
                    canchaId = entry.arguments?.getString(Routes.ARG_CANCHA_ID).orEmpty(),
                    jugadorId = entry.arguments?.getString(Routes.ARG_JUGADOR_ID).orEmpty(),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(route = Routes.CHATS) {
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                ConversacionesRoute(
                    auth = auth,
                    onConversacionClick = { canchaId, jugadorId ->
                        navController.irA(Routes.chat(canchaId, jugadorId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(route = Routes.MY_RESERVE) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                val kioscos = remember(auth.misReservas) {
                    auth.misReservas.map { it.canchaId }.distinct().associateWith { id ->
                        auth.canchaPorId(id)?.kiosco.orEmpty()
                    }
                }
                MyReserveScreen(
                    reservas = auth.misReservas.sortedByDescending { it.estado == EstadoReserva.PENDIENTE_PAGO },
                    kioscos = kioscos,
                    onPagarClick = { navController.irA(Routes.NEQUI_PRUEBA) },
                    onBackClick = { navController.popBackStack() },
                    onBuscarCanchasClick = { navController.irAInicio(auth) }
                )
            }
        }

        composable(route = Routes.SHOP) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                var categoria by rememberSaveable { mutableStateOf("Bebidas") }
                ShopScreen(
                    categoriaSeleccionada = categoria,
                    onBackClick = { navController.popBackStack() },
                    onCategoriaTabClick = { categoria = it },
                    onAddProductoClick = { navController.irA(Routes.ADD_PRODUCT) },
                    onVerCarritoClick = { navController.irA(Routes.ADD_PRODUCT) }
                )
            }
        }

        composable(route = Routes.ADD_PRODUCT) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                AddProductScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddToCartClick = { navController.popBackStack() }
                )
            }
        }

        composable(route = Routes.INVENTARY) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                var categoria by rememberSaveable { mutableStateOf("Bebidas") }
                InventaryScreen(
                    categoriaSeleccionada = categoria,
                    tabBottomActual = "Inicio",
                    onBackClick = { navController.popBackStack() },
                    onCategoriaTabClick = { categoria = it },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(route = Routes.LIVE_MATCH) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                LiveMatchScreen(
                    onBackClick = { navController.popBackStack() },
                    onGolesClick = { navController.irA(Routes.MATCH_VIDEO) },
                    onEstadisticasClick = { navController.irA(Routes.MATCH_VIDEO) }
                )
            }
        }

        // ---------------------------------------------------------------
        // PANTALLAS COMPARTIDAS (jugador y dueño, con sesión y perfil elegido)
        // ---------------------------------------------------------------
        composable(route = Routes.MATCH_VIDEO) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                MatchVideoScreen(onBackClick = { navController.popBackStack() })
            }
        }

        composable(route = Routes.NOTIFICATIONS) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                var tabActual by rememberSaveable { mutableStateOf("Todas") }
                val visibles = auth.misNotificaciones
                    .filter { tabActual == "Todas" || !it.leida }
                NotificationsScreen(
                    tabSeleccionada = tabActual,
                    notificaciones = visibles.map { it.aItem() },
                    onBackClick = { navController.popBackStack() },
                    onTabSelected = { tabActual = it },
                    onNotificacionClick = { item ->
                        auth.marcarNotificacionLeida(item.id)
                        val tipo = visibles.firstOrNull { it.id == item.id }?.tipo
                        when (tipo) {
                            TipoNotificacion.VINCULAR_NEQUI ->
                                navController.irA(Routes.METODO_PAGO)
                            TipoNotificacion.PAGO_PENDIENTE ->
                                navController.irA(Routes.NEQUI_PRUEBA)
                            TipoNotificacion.GRABAR_PARTIDO ->
                                auth.reservaParaGrabar()?.let {
                                    navController.irA(Routes.grabarPartido(it.id))
                                }
                            TipoNotificacion.RESERVA_HECHA ->
                                if (auth.usuario?.rol == UserRole.JUGADOR) {
                                    navController.irA(Routes.MY_RESERVE)
                                }
                            else -> Unit
                        }
                    }
                )
            }
        }

        composable(route = Routes.TEAMS) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                var tabActual by rememberSaveable { mutableStateOf("Mis equipos") }
                val miId = auth.usuario?.id
                // "Descubrir": equipos creados por otros usuarios (se releen al cambiar mis equipos).
                val otros = remember(auth.misEquipos) {
                    auth.todosLosEquipos().filter { it.creadorId != miId }
                }
                val creadores = remember(otros) {
                    otros.map { it.creadorId }.distinct().associateWith { id ->
                        auth.nombreDeUsuario(id).orEmpty()
                    }.filterValues { it.isNotBlank() }
                }
                TeamsScreen(
                    tabSeleccionada = tabActual,
                    equipos = if (tabActual == "Mis equipos") auth.misEquipos else otros,
                    creadores = creadores,
                    onBackClick = { navController.popBackStack() },
                    onTabSelected = { tabActual = it },
                    onCrearEquipoClick = { navController.irA(Routes.CREAR_EQUIPO) }
                )
            }
        }

        composable(route = Routes.CREAR_EQUIPO) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                CrearEquipoRoute(
                    auth = auth,
                    onBackClick = { navController.popBackStack() },
                    onEquipoCreado = { navController.popBackStack() }
                )
            }
        }

        composable(route = Routes.RECORD_VIDEO) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                GrabacionesRoute(
                    auth = auth,
                    onBackClick = { navController.popBackStack() },
                    onVerGrabacion = { g -> navController.irA(Routes.reproducir(g.id)) },
                    onMomentoClave = { g ->
                        navController.irA(Routes.reproducir(g.id, momento = true))
                    },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        // Cámara para grabar el partido (el anfitrión es el jugador que reservó).
        composable(
            route = Routes.GRABAR_PARTIDO_PATRON,
            arguments = listOf(navArgument(Routes.ARG_RESERVA_ID) { type = NavType.StringType })
        ) { entry ->
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                val reservaId = entry.arguments?.getString(Routes.ARG_RESERVA_ID).orEmpty()
                val reserva = remember(reservaId) { auth.reservaPorId(reservaId) }
                if (reserva == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    // Equipo contrincante elegido antes o durante la grabación.
                    var rivalId by rememberSaveable { mutableStateOf<String?>(null) }
                    val equipos = remember { auth.todosLosEquipos() }
                    GrabarPartidoScreen(
                        reserva = reserva,
                        // El contrincante no puede ser el mismo equipo que reservó.
                        rivales = equipos.filter { it.id != reserva.equipoId },
                        escudoEquipo = equipos.firstOrNull { it.id == reserva.equipoId }?.escudoUri.orEmpty(),
                        rivalSeleccionadoId = rivalId,
                        onRivalSelected = { rivalId = it },
                        onVideoGuardado = { uri -> auth.guardarGrabacion(reserva, uri, rivalId) },
                        onTerminado = { grabacionId ->
                            // Al detener: confirmar los goles detectados en el audio.
                            val destino = grabacionId
                                ?.let { Routes.confirmarGoles(it, alGrabar = true) }
                                ?: Routes.RECORD_VIDEO
                            navController.navigate(destino) {
                                popUpTo(Routes.GRABAR_PARTIDO_PATRON) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onAhoraNo = {
                            auth.omitirGrabacion(reserva.id)
                            navController.popBackStack()
                        },
                        onSalir = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(
            route = Routes.REPRODUCIR_PATRON,
            arguments = listOf(
                navArgument(Routes.ARG_GRABACION_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_MOMENTO) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                ReproducirGrabacionRoute(
                    auth = auth,
                    grabacionId = entry.arguments?.getString(Routes.ARG_GRABACION_ID).orEmpty(),
                    abrirEnMomentoClave = entry.arguments?.getBoolean(Routes.ARG_MOMENTO) ?: false,
                    onBackClick = { navController.popBackStack() },
                    onGolesClick = { id -> navController.irA(Routes.confirmarGoles(id)) }
                )
            }
        }

        // Confirmar cuáles marcas de posible gol (detectadas en el audio) fueron gol.
        composable(
            route = Routes.CONFIRMAR_GOLES_PATRON,
            arguments = listOf(
                navArgument(Routes.ARG_GRABACION_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_AL_GRABAR) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                val alGrabar = entry.arguments?.getBoolean(Routes.ARG_AL_GRABAR) ?: false
                ConfirmarGolesRoute(
                    auth = auth,
                    grabacionId = entry.arguments?.getString(Routes.ARG_GRABACION_ID).orEmpty(),
                    onTerminado = {
                        if (alGrabar) {
                            navController.navigate(Routes.RECORD_VIDEO) {
                                popUpTo(Routes.CONFIRMAR_GOLES_PATRON) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }

        // Registrar el número Nequi (jugador: para pagar; dueño: para recibir pagos).
        composable(route = Routes.METODO_PAGO) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                val registrado = auth.usuario?.nequi
                var numero by rememberSaveable { mutableStateOf(registrado.orEmpty()) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }
                var exito by rememberSaveable { mutableStateOf<String?>(null) }
                MetodoPagoScreen(
                    esDuenio = auth.usuario?.rol == UserRole.DUENIO,
                    nequiRegistrado = registrado,
                    numero = numero,
                    errorMensaje = error,
                    mensajeExito = exito,
                    onBackClick = { navController.popBackStack() },
                    onNumeroChange = {
                        numero = it.filter { c -> c.isDigit() }.take(10)
                        error = null
                        exito = null
                    },
                    onGuardarClick = {
                        if (numero.isBlank()) {
                            error = "Escribe tu número Nequi."
                        } else {
                            error = auth.actualizarNequi(numero)
                            exito = if (error == null) "Número Nequi guardado." else null
                        }
                    },
                    onQuitarClick = {
                        auth.actualizarNequi("")
                        numero = ""
                        error = null
                        exito = "Número quitado."
                    },
                    onVerSolicitudesClick = { navController.irA(Routes.NEQUI_PRUEBA) }
                )
            }
        }

        // Nequi de prueba: aceptar o rechazar los cobros enviados al número del jugador.
        composable(route = Routes.NEQUI_PRUEBA) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                val numero = auth.usuario?.nequi
                var version by remember { mutableStateOf(0) }
                val solicitudes = remember(numero, version) {
                    numero?.let { nequi.solicitudesDe(it) } ?: emptyList()
                }
                fun responder(pagoId: String, aceptar: Boolean) {
                    nequi.responder(pagoId, aceptar)
                    // Igual que al volver de la app de Nequi: la reserva se confirma o se libera.
                    auth.sincronizarPagos()
                    version++
                }
                NequiPruebaScreen(
                    numero = numero,
                    solicitudes = solicitudes,
                    onBackClick = { navController.popBackStack() },
                    onRegistrarNequiClick = { navController.irA(Routes.METODO_PAGO) },
                    onAceptar = { responder(it.id, true) },
                    onRechazar = { responder(it.id, false) }
                )
            }
        }

        composable(route = Routes.PROFILE) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                PerfilRoute(
                    auth = auth,
                    onBackClick = { navController.popBackStack() },
                    onMisReservasClick = { navController.irA(Routes.MY_RESERVE) },
                    onMisGrabacionesClick = { navController.irA(Routes.RECORD_VIDEO) },
                    onMisEquiposClick = { navController.irA(Routes.TEAMS) },
                    onMetodosPagoClick = { navController.irA(Routes.METODO_PAGO) },
                    onConfiguracionClick = { navController.irA(Routes.CONFIGURACION) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(route = Routes.CONFIGURACION) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                ConfiguracionRoute(
                    auth = auth,
                    onBackClick = { navController.popBackStack() },
                    onMetodosPagoClick = { navController.irA(Routes.METODO_PAGO) }
                )
            }
        }
    }
}

/** Pantallas sobre las que NO se abre la cámara automáticamente. */
private val RUTAS_SIN_GRABACION_AUTOMATICA = setOf(
    Routes.OPEN_SCREEN,
    Routes.LOGIN,
    Routes.SELECT_ACCOUNT,
    Routes.SELECT_PROFILE
)

// =====================================================================
// Notificaciones: modelo guardado -> fila de la pantalla
// =====================================================================

private fun Notificacion.aItem(): NotificacionItem {
    val (icono, color) = when (tipo) {
        TipoNotificacion.CUENTA_CREADA -> Icons.Outlined.CheckCircle to Color(0xFF64F089)
        TipoNotificacion.VINCULAR_NEQUI -> Icons.Outlined.AccountBalanceWallet to Color(0xFFA78BFA)
        TipoNotificacion.PAGO_PENDIENTE -> Icons.Outlined.AccountBalanceWallet to Color(0xFFE5B83B)
        TipoNotificacion.PAGO_RECHAZADO -> Icons.Outlined.ErrorOutline to Color(0xFFFF6B6B)
        TipoNotificacion.RESERVA_HECHA -> Icons.Outlined.CalendarToday to Color(0xFF63A0E8)
        TipoNotificacion.RESERVA_RECIBIDA -> Icons.Outlined.CalendarToday to Color(0xFFE5B83B)
        TipoNotificacion.PEDIDO_RECIBIDO -> Icons.Outlined.Storefront to Color(0xFFA78BFA)
        TipoNotificacion.GRABAR_PARTIDO -> Icons.Outlined.Videocam to Color(0xFFE07048)
        TipoNotificacion.MENSAJE_NUEVO -> Icons.Outlined.ChatBubbleOutline to Color(0xFF63A0E8)
    }
    return NotificacionItem(
        id = id,
        titulo = titulo,
        descripcion = mensaje,
        tiempo = tiempoRelativo(creadaEn),
        icon = icono,
        iconTint = color,
        leida = leida
    )
}

private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun tiempoRelativo(millis: Long): String {
    val minutos = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutos < 1 -> "Ahora"
        minutos < 60 -> "Hace $minutos min"
        minutos < 24 * 60 -> "Hace ${minutos / 60} h"
        else -> Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(FORMATO_FECHA_HORA)
    }
}

// =====================================================================
// Guardas de navegación
// =====================================================================

/** Pantallas para quien NO tiene sesión. Si ya hay sesión, redirige a su inicio. */
@Composable
private fun RutaPublica(
    auth: AuthState,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val usuario = auth.usuario
    if (usuario == null) {
        content()
    } else {
        LaunchedEffect(usuario.id, usuario.rol) { navController.irAInicio(auth) }
    }
}

/** Solo muestra [content] si se cumple [acceso]; si no, redirige a donde corresponda. */
@Composable
private fun RutaProtegida(
    acceso: Acceso,
    auth: AuthState,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val usuario = auth.usuario
    val permitido = when (acceso) {
        Acceso.SESION -> usuario != null
        Acceso.CUALQUIER_ROL -> usuario?.rol != null
        Acceso.JUGADOR -> usuario?.rol == UserRole.JUGADOR
        Acceso.DUENIO -> usuario?.rol == UserRole.DUENIO
        Acceso.DUENIO_CON_CANCHA -> usuario?.rol == UserRole.DUENIO && auth.cancha != null
    }

    if (permitido) {
        content()
    } else {
        val destino = auth.rutaInicial()
        LaunchedEffect(destino) { navController.irAInicio(auth) }
    }
}

// =====================================================================
// Helpers de navegación
// =====================================================================

/** Navega evitando duplicar la pantalla si se toca dos veces rápido. */
private fun NavHostController.irA(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Va al inicio que corresponde a la sesión actual y limpia todo el historial. */
private fun NavHostController.irAInicio(auth: AuthState) {
    navigate(auth.rutaInicial()) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** Barra inferior: cambia de pestaña sin apilar pantallas (el "Inicio" depende del rol). */
private fun NavHostController.irATab(tab: String, auth: AuthState) {
    val inicio = auth.rutaInicial()
    val destino = when (tab) {
        "Inicio" -> inicio
        "Grabaciones" -> Routes.RECORD_VIDEO
        "Equipos" -> Routes.TEAMS
        "Perfil" -> Routes.PROFILE
        else -> return
    }
    navigate(destino) {
        popUpTo(inicio) { inclusive = false }
        launchSingleTop = true
    }
}

// =====================================================================
// Resumen del panel del dueño (a partir de las reservas reales de su cancha)
// =====================================================================

private class ResumenDuenio(
    val reservasHoy: List<ReservaHoyItem>,
    val totalReservas: Int,
    val ingresosSemana: Int,
    val ingresosMes: Int,
    val barrasSemana: List<Pair<String, Float>>
)

private fun resumenDuenio(reservas: List<Reserva>): ResumenDuenio {
    val hoy = LocalDate.now()
    val lunes = hoy.with(DayOfWeek.MONDAY)
    val fechaDe = { r: Reserva ->
        runCatching { LocalDate.parse(r.fecha, AuthState.FORMATO_FECHA) }.getOrNull()
    }

    val deHoy = reservas
        .filter { fechaDe(it) == hoy }
        .sortedBy { Validaciones.minutosDelDia(it.hora) ?: 0 }
        .map { r ->
            ReservaHoyItem(
                "${r.hora} – ${r.horaFin}",
                if (r.duracionHoras == 1) "Confirmada" else "Confirmada · ${r.duracionHoras} h"
            )
        }

    // El dinero entra el día en que el jugador paga (no el día del partido). Las reservas
    // antiguas, sin fecha de pago guardada, se cuentan por la fecha del partido.
    val diaDelPago = { r: Reserva ->
        if (r.pagadaEn > 0L) {
            Instant.ofEpochMilli(r.pagadaEn).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            fechaDe(r)
        }
    }

    // Ingresos (cancha + productos del kiosco) por día de la semana actual (lunes a domingo).
    val porDia = IntArray(7)
    var ingresosMes = 0
    reservas.forEach { r ->
        val f = diaDelPago(r) ?: return@forEach
        if (!f.isBefore(lunes) && f.isBefore(lunes.plusDays(7))) {
            porDia[f.dayOfWeek.value - 1] += r.total
        }
        if (f.year == hoy.year && f.month == hoy.month) ingresosMes += r.total
    }
    val maximo = porDia.maxOrNull()?.takeIf { it > 0 } ?: 1
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")

    return ResumenDuenio(
        reservasHoy = deHoy,
        totalReservas = reservas.size,
        ingresosSemana = porDia.sum(),
        ingresosMes = ingresosMes,
        barrasSemana = letras.mapIndexed { i, l -> l to porDia[i].toFloat() / maximo }
    )
}
