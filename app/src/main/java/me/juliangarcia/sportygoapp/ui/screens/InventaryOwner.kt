package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.juliangarcia.sportygoapp.data.CategoriaProducto
import me.juliangarcia.sportygoapp.data.Producto

// Definición de paleta de colores para Tienda de la Cancha (Dueño)
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

private const val TAB_TODOS = "Todos"

/**
 * Inventario del kiosco de la cancha (dueño). Una cancha nueva empieza sin productos.
 * Tocar un producto lo abre para editarlo.
 */
@Composable
fun InventaryOwnerScreen(
    modifier: Modifier = Modifier,
    productos: List<Producto> = emptyList(),
    /** Ubicación del kiosco donde los jugadores reclaman sus compras. */
    ubicacionKiosco: String = "",
    categoriaSeleccionada: String = TAB_TODOS,
    tabBottomActual: String = "Inicio",
    onBackClick: () -> Unit = {},
    onCategoriaTabClick: (String) -> Unit = {},
    onAgregarProductoClick: () -> Unit = {},
    onProductoClick: (Producto) -> Unit = {},
    onGuardarKiosco: (String) -> Unit = {},
    onBottomNavTabClick: (String) -> Unit = {}
) {
    var editandoKiosco by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf(TAB_TODOS) + CategoriaProducto.entries.map { it.etiqueta }
    val visibles = productos.filter {
        categoriaSeleccionada == TAB_TODOS || it.categoria.etiqueta == categoriaSeleccionada
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Barra superior con botón volver y título
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tienda de la Cancha",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = when (productos.size) {
                                0 -> "Sin productos"
                                1 -> "1 producto"
                                else -> "${productos.size} productos"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dónde reclaman los jugadores sus compras
                TarjetaKiosco(
                    ubicacion = ubicacionKiosco,
                    onClick = { editandoKiosco = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filtro por categoría
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkPillBackground)
                        .padding(4.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == categoriaSeleccionada
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) PrimaryGreen else Color.Transparent)
                                .clickable { onCategoriaTabClick(tab) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DarkBackground else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (visibles.isEmpty()) {
                        item {
                            EstadoVacio(
                                icono = Icons.Outlined.Inventory2,
                                titulo = if (productos.isEmpty()) "Tu kiosco aún no tiene productos"
                                else "No hay productos en esta categoría",
                                descripcion = "Agrega bebidas y snacks para que los jugadores " +
                                    "los compren al reservar y los reclamen en tu kiosco."
                            )
                        }
                    }

                    items(items = visibles, key = { it.id }) { producto ->
                        ProductoDuenioCard(producto = producto, onClick = { onProductoClick(producto) })
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                Button(
                    onClick = onAgregarProductoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agregar producto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBackground
                    )
                }
            }

            // Barra de Navegación Inferior (Bottom Navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBackground)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem("Inicio", Icons.Outlined.Home, tabBottomActual == "Inicio") {
                    onBottomNavTabClick("Inicio")
                }
                BottomNavItem("Grabaciones", Icons.Outlined.VideoLibrary, tabBottomActual == "Grabaciones") {
                    onBottomNavTabClick("Grabaciones")
                }
                BottomNavItem("Equipos", Icons.Outlined.EmojiEvents, tabBottomActual == "Equipos") {
                    onBottomNavTabClick("Equipos")
                }
                BottomNavItem("Perfil", Icons.Outlined.Person, tabBottomActual == "Perfil") {
                    onBottomNavTabClick("Perfil")
                }
            }
        }
    }

    if (editandoKiosco) {
        DialogoKiosco(
            actual = ubicacionKiosco,
            onCancelar = { editandoKiosco = false },
            onGuardar = {
                onGuardarKiosco(it)
                editandoKiosco = false
            }
        )
    }
}

@Composable
private fun TarjetaKiosco(ubicacion: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kiosco para reclamar productos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = ubicacion.ifBlank { "Toca para indicar dónde está (ej. junto a la entrada)" },
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Editar kiosco",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DialogoKiosco(actual: String, onCancelar: () -> Unit, onGuardar: (String) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(actual) }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = DarkCardBackground,
        title = { Text("¿Dónde está el kiosco?", color = TextWhite) },
        text = {
            Column {
                Text(
                    text = "Los jugadores verán este lugar para reclamar lo que compraron.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(80) },
                    singleLine = true,
                    placeholder = { Text("Ej. Junto a la entrada", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = DarkPillBackground,
                        cursorColor = PrimaryGreen
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(texto) }) { Text("Guardar", color = PrimaryGreen) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextSecondary) }
        }
    )
}

@Composable
private fun ProductoDuenioCard(producto: Producto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenProducto(
                producto = producto,
                modifier = Modifier
                    .size(56.dp)
                    .clip(FormaProducto)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatearPrecio(producto.precio)} · ${producto.categoria.etiqueta}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Editar",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    titulo: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titulo,
            tint = if (isSelected) PrimaryGreen else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = titulo,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGreen else TextSecondary
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InventaryOwnerPreview() {
    InventaryOwnerScreen(
        productos = listOf(
            Producto(id = "1", nombre = "Agua 500 ml", precio = 2000, iconoId = "agua"),
            Producto(id = "2", nombre = "Papas", precio = 3000, iconoId = "snack", categoria = CategoriaProducto.SNACK)
        ),
        ubicacionKiosco = "Junto a la entrada"
    )
}
