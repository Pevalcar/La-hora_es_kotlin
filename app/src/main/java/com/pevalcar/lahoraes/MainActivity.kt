package com.pevalcar.lahoraes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pevalcar.lahoraes.ui.theme.LaHoraEsTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaHoraEsTheme {
                TimeAnnouncerApp()
            }
        }
    }
}

@Composable
fun TimeAnnouncerApp(viewModel: TimeAnnouncerViewModel = viewModel()) {
    val context = LocalContext.current
    val interval by viewModel.interval.collectAsState()
    val wakeLockEnabled by viewModel.wakeLockEnabled.collectAsState()
    val serviceRunning by viewModel.serviceRunning.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Intervalo: ${interval} minutos",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = interval.toFloat(),
            onValueChange = { viewModel.updateInterval(it.toInt()) },
            valueRange = 1f..120f,
            steps = 119,
            modifier = Modifier.fillMaxWidth()
        )

        Switch(
            checked = wakeLockEnabled,
            onCheckedChange = { viewModel.updateWakeLock(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (serviceRunning) {
                    context.stopService(Intent(context, TimeService::class.java))
                    viewModel.updateServiceRunning(false)
                } else {
                    val serviceIntent = Intent(context, TimeService::class.java).apply {
                        putExtra("interval", interval * 60 * 1000)
                        putExtra("wakeLock", wakeLockEnabled)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    viewModel.updateServiceRunning(true)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (serviceRunning) "Detener" else "Iniciar")
        }
    }
}