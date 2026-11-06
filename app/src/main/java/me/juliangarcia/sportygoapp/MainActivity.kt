package me.juliangarcia.sportygoapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import me.juliangarcia.sportygoapp.avisos.Avisos
import me.juliangarcia.sportygoapp.data.AccountStorage
import me.juliangarcia.sportygoapp.data.AuthRepository
import me.juliangarcia.sportygoapp.data.AuthState
import me.juliangarcia.sportygoapp.pagos.NequiSimulado
import me.juliangarcia.sportygoapp.ui.theme.SportyGoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app es oscura: íconos claros en las barras del sistema sin importar el tema del teléfono.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // Cuentas y sesión guardadas en el dispositivo.
        val repo = AuthRepository(AccountStorage(applicationContext))
        val avisos = Avisos(applicationContext).apply {
            crearCanales()
            // Asegura que todas las reservas futuras tengan su aviso para grabar.
            reprogramar(repo.todasLasReservas())
        }
        // Cobros por Nequi: de prueba hasta conectar la app con Nequi a través de un servidor.
        val nequi = NequiSimulado(applicationContext)
        val auth = AuthState(repo, avisos, nequi)

        setContent {
            // Android 13+: hay que pedir permiso para mostrar notificaciones.
            val pedirPermiso = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            SportyGoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B131D)
                ) {
                    SportyGoNavHost(
                        auth = auth,
                        nequi = nequi,
                        // Evita que el contenido quede debajo de la barra de estado,
                        // la barra de navegación y el teclado.
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    )
                }
            }
        }
    }
}
