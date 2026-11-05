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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkSelectedHighlight = Color(0xFF243242)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)

// Modelo de datos inmutable para las opciones de iconos
data class IconoProductoItem(
    val id: String,
    val nombre: String,
    val imagenUrl: String
)

@Composable
fun CreateProductScreen(
    modifier: Modifier = Modifier,
    // Estado/Valores pasados por parámetro (Stateless)
    iconoSeleccionadoId: String = "1",
    iconosDisponibles: List<IconoProductoItem> = getIconosPredeterminados(),
    // Callbacks para eventos e interacción futura
    onBackClick: () -> Unit = {},
    onIconoSelected: (IconoProductoItem) -> Unit = {},
    onGuardarIconoClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
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
                    text = "Agregar producto a la tienda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta principal del selector de iconos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardBackground
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    iconosDisponibles.forEach { icono ->
                        val isSelected = icono.id == iconoSeleccionadoId

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(if (isSelected) DarkSelectedHighlight else Color.Transparent)
                                .clickable { onIconoSelected(icono) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Imagen del icono cargada con Coil
                            AsyncImage(
                                model = icono.imagenUrl,
                                contentDescription = icono.nombre,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón principal de acción "Guardar Icono"
            Button(
                onClick = onGuardarIconoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Guardar Icono",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Datos predeterminados de iconos de productos
private fun getIconosPredeterminados(): List<IconoProductoItem> {
    return listOf(
        IconoProductoItem(
            id = "1",
            nombre = "Botella Azul",
            imagenUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?q=80&w=400&auto=format&fit=crop"
        ),
        IconoProductoItem(
            id = "2",
            nombre = "Botella Naranja",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        IconoProductoItem(
            id = "3",
            nombre = "Botella Roja",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=400&auto=format&fit=crop"
        ),
        IconoProductoItem(
            id = "4",
            nombre = "Botella Amarilla",
            imagenUrl = "https://images.unsplash.com/photo-1566478989037-eec170784d0b?q=80&w=400&auto=format&fit=crop"
        ),
        IconoProductoItem(
            id = "5",
            nombre = "Botella Blanca",
            imagenUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?q=80&w=400&auto=format&fit=crop"
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateProductPreview() {
    CreateProductScreen()
}
