package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.juliangarcia.sportygoapp.data.CategoriaProducto
import me.juliangarcia.sportygoapp.data.Producto

private val FondoIcono = Color(0xFF192534)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val StepperBackground = Color(0xFF182330)

/** Ícono predefinido que el dueño puede usar en vez de una foto. */
data class IconoProducto(
    val id: String,
    val nombre: String,
    val icono: ImageVector,
    val color: Color,
    /** Categoría que se sugiere al elegir este ícono. */
    val categoria: CategoriaProducto
)

/** Íconos que trae la app para los productos del kiosco. */
object IconosProducto {
    val todos: List<IconoProducto> = listOf(
        IconoProducto("agua", "Agua", Icons.Outlined.WaterDrop, Color(0xFF63A0E8), CategoriaProducto.BEBIDA),
        IconoProducto("bebida", "Bebida", Icons.Outlined.LocalDrink, Color(0xFF64F089), CategoriaProducto.BEBIDA),
        IconoProducto("gaseosa", "Gaseosa", Icons.Outlined.EmojiFoodBeverage, Color(0xFFE07048), CategoriaProducto.BEBIDA),
        IconoProducto("cerveza", "Cerveza", Icons.Outlined.SportsBar, Color(0xFFE5B83B), CategoriaProducto.BEBIDA),
        IconoProducto("cafe", "Café", Icons.Outlined.LocalCafe, Color(0xFFC08B5C), CategoriaProducto.BEBIDA),
        IconoProducto("vino", "Vino", Icons.Outlined.WineBar, Color(0xFFD9667B), CategoriaProducto.BEBIDA),
        IconoProducto("snack", "Snack", Icons.Outlined.Cookie, Color(0xFFE5B83B), CategoriaProducto.SNACK),
        IconoProducto("comida", "Comida rápida", Icons.Outlined.Fastfood, Color(0xFFE07048), CategoriaProducto.SNACK),
        IconoProducto("hamburguesa", "Hamburguesa", Icons.Outlined.LunchDining, Color(0xFFC08B5C), CategoriaProducto.SNACK),
        IconoProducto("pizza", "Pizza", Icons.Outlined.LocalPizza, Color(0xFFE07048), CategoriaProducto.SNACK),
        IconoProducto("panaderia", "Panadería", Icons.Outlined.BakeryDining, Color(0xFFE5B83B), CategoriaProducto.SNACK),
        IconoProducto("helado", "Helado", Icons.Outlined.Icecream, Color(0xFFA78BFA), CategoriaProducto.SNACK),
        IconoProducto("balon", "Implemento", Icons.Outlined.SportsSoccer, Color(0xFFFFFFFF), CategoriaProducto.SNACK)
    )

    fun porId(id: String): IconoProducto? = todos.firstOrNull { it.id == id }
}

/** Foto del producto o, si no tiene, su ícono predefinido. */
@Composable
fun ImagenProducto(
    imagenUri: String,
    iconoId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tamanoIcono: Dp = 28.dp
) {
    if (imagenUri.isNotBlank()) {
        AsyncImage(
            model = imagenUri,
            contentDescription = contentDescription,
            modifier = modifier.background(FondoIcono),
            contentScale = ContentScale.Crop
        )
    } else {
        val icono = IconosProducto.porId(iconoId)
        Box(modifier = modifier.background(FondoIcono), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icono?.icono ?: Icons.Outlined.Inventory2,
                contentDescription = contentDescription,
                tint = icono?.color ?: TextSecondary,
                modifier = Modifier.size(tamanoIcono)
            )
        }
    }
}

@Composable
fun ImagenProducto(producto: Producto, modifier: Modifier = Modifier, tamanoIcono: Dp = 28.dp) =
    ImagenProducto(producto.imagenUri, producto.iconoId, producto.nombre, modifier, tamanoIcono)

/** Selector de cantidad "−  2  +". */
@Composable
fun SelectorCantidad(
    cantidad: Int,
    onCambiar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maximo: Int = 99
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BotonCantidad(
            icono = Icons.Outlined.Remove,
            descripcion = "Quitar uno",
            habilitado = cantidad > 0,
            resaltado = false,
            onClick = { onCambiar(cantidad - 1) }
        )
        Text(
            text = cantidad.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (cantidad > 0) TextWhite else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp)
        )
        BotonCantidad(
            icono = Icons.Filled.Add,
            descripcion = "Agregar uno",
            habilitado = cantidad < maximo,
            resaltado = true,
            onClick = { onCambiar(cantidad + 1) }
        )
    }
}

@Composable
private fun BotonCantidad(
    icono: ImageVector,
    descripcion: String,
    habilitado: Boolean,
    resaltado: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (resaltado && habilitado) PrimaryGreen else StepperBackground)
            .border(1.dp, if (habilitado) Color.Transparent else StepperBackground, CircleShape)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = when {
                !habilitado -> TextSecondary.copy(alpha = 0.4f)
                resaltado -> Color(0xFF0B131D)
                else -> TextWhite
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Esquinas redondeadas usadas en las miniaturas de productos. */
val FormaProducto = RoundedCornerShape(12.dp)
