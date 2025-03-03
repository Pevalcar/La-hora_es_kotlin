package com.pevalcar.lahoraes.domain

import android.util.Log
import com.pevalcar.lahoraes.data.AccesAppRepo
import javax.inject.Inject

class CanAccesToApp @Inject constructor(private val repository: AccesAppRepo) {


    suspend operator fun invoke(): Boolean {

        val currentVersion = repository.getCurrentVersion()
        val minAllowedVersion = repository.getMinAllowedVersion()
        Log.i(
            "Pevalcar-vesionCheck",
            "currentVersion: $currentVersion , minAllowedVersion: $minAllowedVersion"
        )
        for ((currentPart, minAllowedVersionPart) in currentVersion.zip(minAllowedVersion)) {
            if (currentPart != minAllowedVersionPart) {
                return currentPart > minAllowedVersionPart
            }
        }

        return true
    }
}