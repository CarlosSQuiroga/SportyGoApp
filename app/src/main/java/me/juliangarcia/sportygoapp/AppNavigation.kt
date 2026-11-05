package me.juliangarcia.sportygoapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import me.juliangarcia.sportygoapp.data.Reserva
import me.juliangarcia.sportygoapp.data.Validaciones
import me.juliangarcia.sportygoapp.ui.BuscarCanchasRoute
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
import me.juliangarcia.sportygoapp.ui.RegistroRoute
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
import me.juliangarcia.sportygoapp.ui.screens.RecordVideoScreen
import me.juliangarcia.sportygoapp.ui.screens.RegisterFieldScreen
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
fun SportyGoNavHost(auth: AuthState, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = remember { auth.rutaInicial() }

    // El dueño debe registrar la ubicación antes de poder llenar los datos de la cancha.
    var ubicacionRegistrada by rememberSaveable { mutableStateOf(false) }

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
                RegisterFieldScreen(
                    onRegistrarUbicacionClick = {
                        ubicacionRegistrada = true
                        navController.irA(Routes.ADD_FIELD)
                    }
                )
            }
        }

        composable(route = Routes.ADD_FIELD) {
            RutaProtegida(Acceso.DUENIO, auth, navController) {
                if (ubicacionRegistrada) {
                    AddFieldRoute(
                        auth = auth,
                        onCanchaRegistrada = {
                            ubicacionRegistrada = false
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
                    nombreCancha = auth.cancha?.nombre ?: "",
                    reservasHoy = resumen.reservasHoy,
                    ingresosMonto = formatearPrecio(resumen.ingresosSemana),
                    ingresosCambio = "Esta semana",
                    reservasCount = resumen.totalReservas.toString(),
                    reservasHoySubtexto = "${resumen.reservasHoy.size} hoy",
                    ingresosSemana = resumen.barrasSemana,
                    filtroIngresosTab = filtroIngresos,
                    onFiltroIngresosTabClick = { filtroIngresos = it },
                    onInventarioClick = { navController.irA(Routes.INVENTARY_OWNER) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(route = Routes.INVENTARY_OWNER) {
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                var categoria by rememberSaveable { mutableStateOf("Bebidas") }
                InventaryOwnerScreen(
                    categoriaSeleccionada = categoria,
                    tabBottomActual = "Inicio",
                    onBackClick = { navController.popBackStack() },
                    onCategoriaTabClick = { categoria = it },
                    onAgregarProductoClick = { navController.irA(Routes.CREATE_PRODUCT) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }

        composable(route = Routes.CREATE_PRODUCT) {
            RutaProtegida(Acceso.DUENIO_CON_CANCHA, auth, navController) {
                var iconoId by rememberSaveable { mutableStateOf("1") }
                CreateProductScreen(
                    iconoSeleccionadoId = iconoId,
                    onBackClick = { navController.popBackStack() },
                    onIconoSelected = { iconoId = it.id },
                    onGuardarIconoClick = { navController.popBackStack() }
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
                    }
                )
            }
        }

        composable(route = Routes.MY_RESERVE) {
            RutaProtegida(Acceso.JUGADOR, auth, navController) {
                MyReserveScreen(
                    reservas = auth.misReservas,
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
                NotificationsScreen(
                    tabSeleccionada = tabActual,
                    onBackClick = { navController.popBackStack() },
                    onTabSelected = { tabActual = it }
                )
            }
        }

        composable(route = Routes.TEAMS) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                var tabActual by rememberSaveable { mutableStateOf("Mis equipos") }
                TeamsScreen(
                    tabSeleccionada = tabActual,
                    onBackClick = { navController.popBackStack() },
                    onTabSelected = { tabActual = it }
                )
            }
        }

        composable(route = Routes.RECORD_VIDEO) {
            RutaProtegida(Acceso.CUALQUIER_ROL, auth, navController) {
                var tabActual by rememberSaveable { mutableStateOf("Mis partidos") }
                RecordVideoScreen(
                    tabSeleccionada = tabActual,
                    grabaciones = auth.misGrabaciones,
                    onBackClick = { navController.popBackStack() },
                    onTabSelected = { tabActual = it },
                    onGrabacionClick = { navController.irA(Routes.MATCH_VIDEO) },
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
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
                    onBottomNavTabClick = { tab -> navController.irATab(tab, auth) }
                )
            }
        }
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
            val inicio = Validaciones.minutosDelDia(r.hora) ?: 0
            val fin = (inicio + 60) % (24 * 60)
            ReservaHoyItem(
                "${r.hora} – ${fin / 60}:${(fin % 60).toString().padStart(2, '0')}",
                "Confirmada"
            )
        }

    // Ingresos por día de la semana actual (lunes a domingo).
    val porDia = IntArray(7)
    reservas.forEach { r ->
        val f = fechaDe(r) ?: return@forEach
        if (!f.isBefore(lunes) && f.isBefore(lunes.plusDays(7))) {
            porDia[f.dayOfWeek.value - 1] += r.precio
        }
    }
    val maximo = porDia.maxOrNull()?.takeIf { it > 0 } ?: 1
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")

    return ResumenDuenio(
        reservasHoy = deHoy,
        totalReservas = reservas.size,
        ingresosSemana = porDia.sum(),
        barrasSemana = letras.mapIndexed { i, l -> l to porDia[i].toFloat() / maximo }
    )
}
