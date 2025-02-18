package com.pevalcar.lahoraes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeService : Service() {
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val interval = intent?.getIntExtra("interval", 600000) ?: 600000
        val useWakeLock = intent?.getBooleanExtra("wakeLock", false) ?: false

        initializeTTS()
        setupWakeLock(useWakeLock)
        startForeground()
        scheduleAnnouncements(interval)

        return START_STICKY
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                speakTime()
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
            .setContentTitle("Anunciador de hora")
            .setContentText("En funcionamiento")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        createNotificationChannel()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "time_channel",
                "Anuncios de hora",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleAnnouncements(interval: Int) {
        runnable = object : Runnable {
            override fun run() {
                speakTime()
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.postDelayed(runnable, interval.toLong())
    }

    private fun speakTime() {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tts?.speak("Son las $time", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        tts?.stop()
        tts?.shutdown()
        wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}