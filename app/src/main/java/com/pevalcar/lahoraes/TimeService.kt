package com.pevalcar.lahoraes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimeService : Service() {
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private var lastAnnouncedTime: String = ""
    private var currentInterval: Int = 600000
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_SPEAK_NOW" -> announceCurrentTime()
            }
        }
    }
    private val runnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                // Tu lógica existente aquí
                val currentTime = SimpleDateFormat(getTimePattern(), Locale.getDefault()).format(Date())
                if (shouldAnnounce(currentTime)) {
                    speakTime(currentTime)
                    lastAnnouncedTime = currentTime
                }
                handler.postDelayed(this, 3000) // Verificación cada 30 segundos
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        // Registrar receptor de broadcast
        val filter = IntentFilter().apply {
            addAction("ACTION_SPEAK_NOW")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                filter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun announceCurrentTime() {
        val pattern = if (TimeSettingsRepository.getTimeFormat()) "HH:mm" else "hh:mm a"
        val time = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
        speakTime(time)
        updateNotification() // Actualizar notificación con próximo horario
    }
    private fun updateNotification() {
        val notification = createNotification()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "time_channel")
            .setContentTitle("Anunciador activo - ${TimeSettingsRepository.getInterval()} min")
            .setContentText("Próximo anuncio: ${calculateNextAnnouncementTime()}")
            // ... (resto de la configuración igual)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialInterval = intent?.getIntExtra("interval", 5) ?: 5
        val initialFormat = intent?.getBooleanExtra("use24Format", true) ?: true
        TimeSettingsRepository.updateInterval(initialInterval)
        TimeSettingsRepository.updateTimeFormat(initialFormat)
        currentInterval = intent?.getIntExtra("interval", 600000) ?: 600000
        val useWakeLock = intent?.getBooleanExtra("wakeLock", false) ?: false

        initializeTTS()
        setupWakeLock(useWakeLock)
        startForeground()
        scheduleAnnouncements()

        return START_STICKY
    }

    private fun initializeTTS() {
        val currentTime = getCurrentTime()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                speakTime(currentTime)
            }
        }
    }

    private fun setupWakeLock(enable: Boolean) {
        if (enable) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TimeAnnouncer::WakeLock"
            ).apply {
                acquire()
            }
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, "time_channel")
            .setContentTitle("Anunciador de hora activo")
            .setContentText("Próximo anuncio: ${calculateNextAnnouncementTime()}")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Hacerla no descartable
            .setShowWhen(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            .build()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }
    private fun calculateNextAnnouncementTime(): String {
        val now = Calendar.getInstance()
        val interval = TimeSettingsRepository.getInterval()
        val currentMinutes = now.get(Calendar.MINUTE)
        val nextMinutes = ((currentMinutes / interval) + 1) * interval
        now.set(Calendar.MINUTE, nextMinutes % 60)
        now.add(Calendar.HOUR, nextMinutes / 60)
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val channel = NotificationChannel(
                "time_channel",
                "Anuncios de hora",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleAnnouncements() {
        handler.post(runnable) // Iniciar el ciclo
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun shouldAnnounce(currentTime: String): Boolean {
        if (lastAnnouncedTime.isEmpty()) return true

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val last = sdf.parse(lastAnnouncedTime)
        val current = sdf.parse(currentTime)

        val diff = (current?.time ?:  0) - (last?.time ?: 0)
        return diff >= currentInterval
    }

    private fun speakTime(time: String) {
        tts?.speak("Son las $time", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        tts?.stop()
        tts?.shutdown()
        wakeLock?.release()
        unregisterReceiver(receiver)
        super.onDestroy()
    }
    private fun getCurrentInterval(): Int {
        return TimeSettingsRepository.getInterval()
    }

    private fun getTimePattern(): String {
        return if (TimeSettingsRepository.getTimeFormat()) "HH:mm" else "hh:mm a"
    }
    override fun onBind(intent: Intent?): IBinder? = null
}