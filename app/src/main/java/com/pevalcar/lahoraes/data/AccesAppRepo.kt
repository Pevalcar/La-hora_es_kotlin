package com.pevalcar.lahoraes.data

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.pevalcar.lahoraes.LHoraEsAoo.Companion.context
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccesAppRepo @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    private val MIN_VERSION = "min_version"

    fun getCurrentVersion(): List<Int> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName?.split(".")?.map { it.toInt() } ?: listOf(0, 0, 0)

        } catch (e: Exception) {
            listOf(0, 0, 0)
        }
    }

    suspend fun getMinAllowedVersion(): List<Int> {
        remoteConfig.fetch(0)
        remoteConfig.activate().await()
        val minVersion = remoteConfig.getString(MIN_VERSION)
        return if (minVersion.isBlank()) listOf(0, 0, 0)
        else minVersion.split(".").map { it.toInt() }


    }
}