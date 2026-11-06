package me.juliangarcia.sportygoapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.juliangarcia.sportygoapp.data.CategoriaProducto

// Definición de colores principales
private val DarkBackground = Color(0xFF0B131D)
private val DarkCardBackground = Color(0xFF111A24)
private val DarkSelectedHighlight = Color(0xFF1B3B32)
private val BorderColor = Color(0xFF243242)
private val PrimaryGreen = Color(0xFF64F089)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8C9BAE)
private val ErrorRed = Color(0xFFFF6B6B)

/**
 * Formulario para agregar (o editar) un producto del kiosco de la cancha.
 * La imagen puede ser una foto (galería o cámara) o uno de los íconos de la app.
 */
@Composable
fun CreateProductScreen(
    modifier: Modifier = Modifier,
    esEdicion: Boolean = false,
    imagenUri: String = "",
    iconoSeleccionadoId: String = "",
    nombre: String = "",
    precio: String = "",
    categoria: CategoriaProducto = CategoriaProducto.BEBIDA,
    errorMensaje: String? = null,
    onBackClick: () -> Unit = {},
    onGaleriaClick: () -> Unit = {},
    onCamaraClick: () -> Unit = {},
    onIconoSelected: (IconoProducto) -> Unit = {},
    onNombreChange: (String) -> Unit = {},
    onPrecioChange: (String) -> Unit = {},
    onCategoriaChange: (CategoriaProducto) -> Unit = {},
    onGuardarClick: () -> Unit = {},
    onEliminarClick: () -> Unit = {}
) {
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
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
                    Text(
                        text = if (esEdicion) "Editar producto" else "Agregar producto a la tienda",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vista previa de la imagen del producto
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ImagenProducto(
                        imagenUri = imagenUri,
                        iconoId = iconoSeleccionadoId,
                        contentDescription = "Imagen del producto",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tamanoIcono = 64.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Foto desde la galería o la cámara
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BotonFoto(
                        texto = "Galería",
                        icono = Icons.Outlined.PhotoLibrary,
                        onClick = onGaleriaClick,
                        modifier = Modifier.weight(1f)
                    )
                    BotonFoto(
                        texto = "Cámara",
                        icono = Icons.Outlined.PhotoCamera,
                        onClick = onCamaraClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Etiqueta("O elige un ícono")
                Spacer(modifier = Modifier.height(8.dp))

                // Íconos predefinidos de la app (5 por fila)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardBackground)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconosProducto.todos.chunked(5).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { icono ->
                                // Si hay foto, el ícono no está en uso.
                                val seleccionado = imagenUri.isBlank() && icono.id == iconoSeleccionadoId
                                OpcionIcono(
                                    icono = icono,
                                    seleccionado = seleccionado,
                                    onClick = { onIconoSelected(icono) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(5 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Etiqueta("Nombre")
                Spacer(modifier = Modifier.height(8.dp))
                CampoTexto(
                    valor = nombre,
                    onCambio = onNombreChange,
                    placeholder = "Ej. Agua 500 ml"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Etiqueta("Precio")
                Spacer(modifier = Modifier.height(8.dp))
                CampoTexto(
                    valor = precio,
                    onCambio = onPrecioChange,
                    placeholder = "Ej. 2500",
                    prefijo = "$",
                    teclado = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(16.dp))

                Etiqueta("Categoría")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoriaProducto.entries.forEach { c ->
                        val seleccionada = c == categoria
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (seleccionada) DarkSelectedHighlight else DarkCardBackground)
                                .border(
                                    1.dp,
                                    if (seleccionada) PrimaryGreen else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onCategoriaChange(c) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = c.etiqueta,
                                fontSize = 14.sp,
                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                color = if (seleccionada) PrimaryGreen else TextSecondary
                            )
                        }
                    }
                }

                if (errorMensaje != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMensaje, fontSize = 14.sp, color = ErrorRed)
                }

                if (esEdicion) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { confirmarEliminar = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Eliminar producto", color = ErrorRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onGuardarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (esEdicion) "Guardar cambios" else "Agregar producto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = DarkCardBackground,
            title = { Text("¿Eliminar producto?", color = TextWhite) },
            text = {
                Text(
                    "Los jugadores ya no podrán comprarlo. Los pedidos ya hechos no cambian.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    onEliminarClick()
                }) { Text("Eliminar", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(text = texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
}

@Composable
private fun BotonFoto(
    texto: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = texto, fontSize = 14.sp, color = TextWhite)
    }
}

@Composable
private fun OpcionIcono(
    icono: IconoProducto,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (seleccionado) DarkSelectedHighlight else Color.Transparent)
            .border(
                1.dp,
                if (seleccionado) PrimaryGreen else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono.icono,
                contentDescription = icono.nombre,
                tint = icono.color,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = icono.nombre,
            fontSize = 10.sp,
            color = if (seleccionado) PrimaryGreen else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    prefijo: String? = null,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSecondary) },
        prefix = prefijo?.let { { Text(it, color = TextWhite) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = DarkCardBackground,
            unfocusedContainerColor = DarkCardBackground,
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = PrimaryGreen
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateProductPreview() {
    CreateProductScreen(iconoSeleccionadoId = "agua", nombre = "Agua 500 ml", precio = "2000")
}
