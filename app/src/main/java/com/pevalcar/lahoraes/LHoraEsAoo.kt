package com.pevalcar.lahoraes

import android.app.Application
import android.content.Context

class LHoraEsAoo : Application() {
    companion object {
        lateinit var context : Context
    }


    override fun onCreate() {
       super.onCreate()
        context = this
    }
}