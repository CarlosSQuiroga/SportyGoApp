package me.juliangarcia.sportygoapp

object Routes {
    // Pantalla de bienvenida
    const val OPEN_SCREEN = "open_screen"

    // Autenticación y Perfil de Usuario
    const val LOGIN = "login"
    const val SELECT_ACCOUNT = "select_account"
    const val SELECT_PROFILE = "select_profile"

    // Búsqueda y Detalle de Canchas
    const val SEARCH_FIELDS = "search_fields"
    const val DETALLES_CANCHA = "detalles_cancha"
    const val ARG_CANCHA_ID = "canchaId"
    const val DETALLES_CANCHA_PATRON = "$DETALLES_CANCHA/{$ARG_CANCHA_ID}"
    fun detallesCancha(canchaId: String) = "$DETALLES_CANCHA/$canchaId"
    const val MY_RESERVE = "my_reserve"

    // Notificaciones, Equipos y Perfil
    const val NOTIFICATIONS = "notifications"
    const val TEAMS = "teams"
    const val PROFILE = "profile"

    // Tienda, Productos e Inventario
    const val SHOP = "shop"
    const val ADD_PRODUCT = "add_product"
    const val CREATE_PRODUCT = "create_product"
    const val INVENTARY = "inventary"
    const val INVENTARY_OWNER = "inventary_owner"

    // Videos, Transmisión e Interacción
    const val RECORD_VIDEO = "record_video"
    const val MATCH_VIDEO = "match_video"
    const val LIVE_MATCH = "live_match"

    // Gestión del Propietario / Cancha
    const val REGISTER_FIELD = "register_field"
    const val ADD_FIELD = "add_field"
    const val OWNERS_PANEL = "owners_panel"
}
