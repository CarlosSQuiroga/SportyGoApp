package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

private val PlaceholderBackground = Color(0xFF182330)
private val PlaceholderIcon = Color(0xFF8C9BAE)
private val EmptyTitle = Color(0xFFFFFFFF)
private val EmptyText = Color(0xFF8C9BAE)

/** Imagen de una cancha; si no tiene imagen muestra un ícono de cancha. */
@Composable
fun ImagenCancha(
    imagenUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (imagenUri.isBlank()) {
        Box(
            modifier = modifier.background(PlaceholderBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SportsSoccer,
                contentDescription = contentDescription,
                tint = PlaceholderIcon,
                modifier = Modifier.size(36.dp)
            )
        }
    } else {
        AsyncImage(
            model = imagenUri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

/** Mensaje para listas vacías (cuenta nueva sin datos). */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = EmptyText,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = titulo,
            fontSize = 17.sp,
            color = EmptyTitle,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = descripcion,
            fontSize = 14.sp,
            color = EmptyText,
            textAlign = TextAlign.Center
        )
    }
}

/** 80000 -> "$80.000" */
fun formatearPrecio(valor: Int): String =
    "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(valor)
