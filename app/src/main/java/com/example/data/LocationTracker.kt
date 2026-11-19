package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationTracker(private val context: Context) {

  private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

  fun hasLocationPermission(): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(): Location? {
    if (!hasLocationPermission()) return null

    val fresh =
      suspendCancellableCoroutine<Location?> { cont ->
        val tokenSource = CancellationTokenSource()
        cont.invokeOnCancellation { tokenSource.cancel() }
        fusedClient
          .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
          .addOnSuccessListener { loc ->
            if (cont.isActive) cont.resume(loc)
          }
          .addOnFailureListener {
            if (cont.isActive) cont.resume(null)
          }
      }

    if (fresh != null) return fresh

    return suspendCancellableCoroutine { cont ->
      fusedClient.lastLocation
        .addOnSuccessListener { loc ->
          if (cont.isActive) cont.resume(loc)
        }
        .addOnFailureListener {
          if (cont.isActive) cont.resume(null)
        }
    }
  }

  @SuppressLint("MissingPermission")
  fun observeLiveLocation(intervalMillis: Long = 2500L): Flow<Location> = callbackFlow {
    if (!hasLocationPermission()) {
      close()
      return@callbackFlow
    }

    val request =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
        .setMinUpdateIntervalMillis(1500L)
        .setMinUpdateDistanceMeters(5f)
        .build()

    val callback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { trySend(it) }
        }
      }

    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { fusedClient.removeLocationUpdates(callback) }
  }
}
