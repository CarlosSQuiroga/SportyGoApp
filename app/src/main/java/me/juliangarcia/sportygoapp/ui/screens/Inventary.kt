package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de paleta de colores para la pantalla Inventario
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val DarkImageBackground = Color(0xFF192534)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ButtonBorderGreen = Color(0xFF1B4D3E)

// Modelo de datos inmutable para representan un item de inventario
data class InventarioItem(
    val id: String,
    val nombre: String,
    val precio: String,
    val stock: Int,
    val imagenUrl: String
)

@Composable
fun InventaryScreen(
    modifier: Modifier = Modifier,
    // Valores de estado pasados por parámetro (Stateless)
    categoriaSeleccionada: String = "Bebidas",
    itemsInventario: List<InventarioItem> = getInventarioPredeterminado(),
    tabBottomActual: String = "Equipos",
    // Callbacks para eventos e interacción futura
    onBackClick: () -> Unit = {},
    onCategoriaTabClick: (String) -> Unit = {},
    onAgregarStockClick: (InventarioItem) -> Unit = {},
    onBottomNavTabClick: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Contenido principal scrolleable de la pantalla
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Inventario",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Pestañas de Categoría (Bebidas / Snacks)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkPillBackground)
                        .padding(4.dp)
                ) {
                    listOf("Bebidas", "Snacks").forEach { categoria ->
                        val isSelected = categoria == categoriaSeleccionada
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) PrimaryGreen else Color.Transparent)
                                .clickable { onCategoriaTabClick(categoria) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = categoria,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DarkBackground else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Items de Inventario
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = itemsInventario,
                        key = { item -> item.id }
                    ) { item ->
                        InventarioCardItem(
                            item = item,
                            onAgregarClick = { onAgregarStockClick(item) }
                        )
                    }
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
                BottomNavItem(
                    titulo = "Inicio",
                    icon = Icons.Outlined.Home,
                    isSelected = tabBottomActual == "Inicio",
                    onClick = { onBottomNavTabClick("Inicio") }
                )

                BottomNavItem(
                    titulo = "Grabaciones",
                    icon = Icons.Outlined.VideoLibrary,
                    isSelected = tabBottomActual == "Grabaciones",
                    onClick = { onBottomNavTabClick("Grabaciones") }
                )

                BottomNavItem(
                    titulo = "Equipos",
                    icon = Icons.Outlined.EmojiEvents,
                    isSelected = tabBottomActual == "Equipos",
                    onClick = { onBottomNavTabClick("Equipos") }
                )

                BottomNavItem(
                    titulo = "Perfil",
                    icon = Icons.Outlined.Person,
                    isSelected = tabBottomActual == "Perfil",
                    onClick = { onBottomNavTabClick("Perfil") }
                )
            }
        }
    }
}

// Composable para cada tarjeta de item en el inventario
@Composable
private fun InventarioCardItem(
    item: InventarioItem,
    onAgregarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto cargada mediante Coil
            AsyncImage(
                model = item.imagenUrl,
                contentDescription = item.nombre,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkImageBackground),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información del producto (Nombre y Precio)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.precio,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Stock disponible
            Text(
                text = item.stock.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Botón de acción "Agregar"
            OutlinedButton(
                onClick = onAgregarClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ButtonBorderGreen),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "Agregar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
            }
        }
    }
}

// Composable auxiliar para los ítems de la barra inferior
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

// Datos predeterminados según la captura de pantalla
private fun getInventarioPredeterminado(): List<InventarioItem> {
    return listOf(
        InventarioItem(
            id = "1",
            nombre = "Agua 500 ml",
            precio = "$2.000",
            stock = 24,
            imagenUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?q=80&w=400&auto=format&fit=crop"
        ),
        InventarioItem(
            id = "2",
            nombre = "Gatorade",
            precio = "$4.000",
            stock = 18,
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        InventarioItem(
            id = "3",
            nombre = "Coca-Cola",
            precio = "$3.500",
            stock = 12,
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        InventarioItem(
            id = "4",
            nombre = "Papas",
            precio = "$3.000",
            stock = 15,
            imagenUrl = "https://images.unsplash.com/photo-1566478989037-eec170784d0b?q=80&w=400&auto=format&fit=crop"
        ),
        InventarioItem(
            id = "5",
            nombre = "Gaseosa",
            precio = "$3.000",
            stock = 10,
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InventaryPreview() {
    InventaryScreen()
}
