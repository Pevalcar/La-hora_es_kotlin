// Clase principal de la actividad que sirve como punto de entrada de la aplicación
package com.pevalcar.lahoraes

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pevalcar.lahoraes.ui.theme.LaHoraEsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Actividad principal que hereda de ComponentActivity (base para actividades con Compose)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configuración del contenido usando Jetpack Compose
        setContent {
            // Aplicación del tema personalizado a toda la UI
            LaHoraEsTheme {
                // Componente raíz de la aplicación
                TimeAnnouncerApp()
            }
        }
    }
}

// Componente principal de la interfaz de usuario con Compose
@Composable
fun TimeAnnouncerApp(viewModel: TimeAnnouncerViewModel = viewModel()) {
    // Obtiene el contexto actual para operaciones con Android
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Manejar resultado del permiso
    }

    // Solicitar permiso cuando sea necesario
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Estados observables del ViewModel convertidos en estado de Compose
    val interval by viewModel.interval.collectAsState()        // Intervalo en minutos
    val wakeLockEnabled by viewModel.wakeLockEnabled.collectAsState()  // Estado del WakeLock
    val serviceRunning by viewModel.serviceRunning.collectAsState()    // Estado del servicio
    val currentTime by viewModel.currentTime.collectAsState()


    // Diseño principal en columna vertical
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selector de formato horario
        TimeFormatSelector(viewModel)

        // Reloj grande
        DigitalClockDisplay(viewModel)

        // Selector de intervalos
        IntervalSelector(viewModel)

        // Controles de servicio
        ServiceControls(viewModel, context)
    }
}
@Composable
private fun IntervalSelector(viewModel: TimeAnnouncerViewModel) {
    val selectedInterval by viewModel.selectedInterval.collectAsState()

    Column {
        Text("Intervalo de anuncio:", style = MaterialTheme.typography.titleSmall)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.availableIntervals) { interval ->
                IntervalButton(
                    interval = interval,
                    isSelected = interval == selectedInterval,
                    onSelect = { viewModel.updateSelectedInterval(interval) }
                )
            }
        }
    }
}

@Composable
private fun IntervalButton(interval: Int, isSelected: Boolean, onSelect: () -> Unit) {
    Button(
        onClick = onSelect,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.height(50.dp)
    ) {
        Text(
            text = when (interval) {
                1 -> "1 minuto"
                60 -> "1 hora"
                else -> "$interval minutos"
            },
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DigitalClockDisplay(viewModel: TimeAnnouncerViewModel) {
    val currentTime by viewModel.currentTime.collectAsState()
    val use24hFormat by viewModel.use24HourFormat.collectAsState()

    val pattern = if (use24hFormat) "HH:mm" else "hh:mm a"
    val formattedTime = remember(currentTime) {
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    Text(
        text = formattedTime,
        style = MaterialTheme.typography.displayLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TimeFormatSelector(viewModel: TimeAnnouncerViewModel) {
    val use24hFormat by viewModel.use24HourFormat.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("24 horas", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = use24hFormat,
            onCheckedChange = { viewModel.toggleTimeFormat() }
        )
        Text("12 horas", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ServiceControls(
    viewModel: TimeAnnouncerViewModel,
    context: Context
) {
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val wakeLockEnabled by viewModel.wakeLockEnabled.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Botón principal de control del servicio
        Button(
            onClick = {
                if (serviceRunning) {
                    context.stopService(Intent(context, TimeService::class.java))
                    viewModel.updateServiceRunning(false)
                } else {
                    val serviceIntent = Intent(context, TimeService::class.java).apply {
                        putExtra("interval", viewModel.selectedInterval.value)
                        putExtra("use24Format", viewModel.use24HourFormat.value)
                        putExtra("wakeLock", wakeLockEnabled)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    viewModel.updateServiceRunning(true)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (serviceRunning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (serviceRunning) "DETENER SERVICIO"
                else "INICIAR SERVICIO",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Control de WakeLock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Mantener dispositivo activo",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = wakeLockEnabled,
                onCheckedChange = { viewModel.updateWakeLock(it) }
            )
        }
    }
}