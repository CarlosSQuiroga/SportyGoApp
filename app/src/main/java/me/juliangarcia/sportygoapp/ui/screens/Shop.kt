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
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Definición de paleta de colores para la pantalla Tienda
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkPillBackground = Color(0xFF131E2B)
private val DarkImageBackground = Color(0xFF192534)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)

// Modelo de datos inmutable para representar un producto
data class ProductoItem(
    val id: String,
    val nombre: String,
    val precio: String,
    val imagenUrl: String
)

@Composable
fun ShopScreen(
    modifier: Modifier = Modifier,
    // Estado/Valores pasados por parámetro (Stateless)
    categoriaSeleccionada: String = "Bebidas",
    productos: List<ProductoItem> = getProductosPredeterminados(),
    carritoTexto: String = "Ver carrito (2)",
    carritoTotal: String = "$4.000",
    // Callbacks para eventos e interacción futura
    onBackClick: () -> Unit = {},
    onCategoriaTabClick: (String) -> Unit = {},
    onAddProductoClick: (ProductoItem) -> Unit = {},
    onVerCarritoClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    text = "Tienda",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Categorías (Bebidas / Snacks / Otros)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(DarkPillBackground)
                    .padding(4.dp)
            ) {
                listOf("Bebidas", "Snacks", "Otros").forEach { categoria ->
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

            // Lista de Productos de la Tienda
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = productos,
                    key = { item -> item.id }
                ) { producto ->
                    ProductoCardItem(
                        producto = producto,
                        onAddClick = { onAddProductoClick(producto) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón flotante inferior del Carrito de Compras
            Button(
                onClick = onVerCarritoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = carritoTexto,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBackground
                    )

                    Text(
                        text = carritoTotal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Composable auxiliar para cada producto
@Composable
private fun ProductoCardItem(
    producto: ProductoItem,
    onAddClick: () -> Unit
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
                model = producto.imagenUrl,
                contentDescription = producto.nombre,
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
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = producto.precio,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Botón de agregar (+)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Agregar ${producto.nombre}",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// Datos predeterminados según la captura
private fun getProductosPredeterminados(): List<ProductoItem> {
    return listOf(
        ProductoItem(
            id = "1",
            nombre = "Agua 500 ml",
            precio = "$2.000",
            imagenUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?q=80&w=400&auto=format&fit=crop"
        ),
        ProductoItem(
            id = "2",
            nombre = "Gatorade",
            precio = "$4.000",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        ProductoItem(
            id = "3",
            nombre = "Coca-Cola",
            precio = "$3.500",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        ProductoItem(
            id = "4",
            nombre = "Papas",
            precio = "$3.000",
            imagenUrl = "https://images.unsplash.com/photo-1566478989037-eec170784d0b?q=80&w=400&auto=format&fit=crop"
        ),
        ProductoItem(
            id = "5",
            nombre = "Gaseosa",
            precio = "$3.000",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShopPreview() {
    ShopScreen()
}
