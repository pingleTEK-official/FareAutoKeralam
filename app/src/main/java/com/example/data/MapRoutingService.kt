package com.example.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

class MapRoutingService(private val context: Context) {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .build()

  suspend fun searchPlaces(
    query: String,
    biasLat: Double? = null,
    biasLon: Double? = null,
  ): List<PlacePoint> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@withContext emptyList()

      val nominatimResults = runCatching { queryNominatimSearch(trimmed, biasLat, biasLon) }.getOrNull()
      if (!nominatimResults.isNullOrEmpty()) {
        return@withContext nominatimResults
      }

      // Fallback to device Geocoder if Nominatim is unreachable
      runCatching { queryAndroidGeocoder(trimmed) }.getOrDefault(emptyList())
    }

  suspend fun reverseGeocode(latitude: Double, longitude: Double): PlacePoint =
    withContext(Dispatchers.IO) {
      val nominatimPlace = runCatching { queryNominatimReverse(latitude, longitude) }.getOrNull()
      if (nominatimPlace != null) return@withContext nominatimPlace

      val androidAddress = runCatching { reverseAndroidGeocoder(latitude, longitude) }.getOrNull()
      if (androidAddress != null) return@withContext androidAddress

      val coordLabel = "GPS (%.4f, %.4f)".format(Locale.US, latitude, longitude)
      PlacePoint(
        id = "gps-${UUID.randomUUID()}",
        shortName = "Current Location",
        fullAddress = coordLabel,
        latitude = latitude,
        longitude = longitude,
        isCurrentGps = true,
      )
    }

  suspend fun fetchDrivingRoute(from: PlacePoint, to: PlacePoint): RouteResult =
    withContext(Dispatchers.IO) {
      val osrmRoute = runCatching { queryOsrmRoute(from, to) }.getOrNull()
      if (osrmRoute != null) return@withContext osrmRoute

      // Fallback to road-adjusted Haversine estimate so driver is never blocked offline
      val straightKm = haversineKm(from.latitude, from.longitude, to.latitude, to.longitude)
      val estimatedRoadKm = max(0.2, (straightKm * 1.25 * 100.0).roundToInt() / 100.0)
      val estimatedMinutes = max(1, (estimatedRoadKm / 24.0 * 60.0).roundToInt())
      RouteResult(
        distanceKm = estimatedRoadKm,
        durationMinutes = estimatedMinutes,
        polylinePoints = listOf(from.toGeoPoint(), to.toGeoPoint()),
        isEstimatedFallback = true,
      )
    }

  private fun queryNominatimSearch(
    query: String,
    biasLat: Double?,
    biasLon: Double?,
  ): List<PlacePoint> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val viewboxParam =
      if (biasLat != null && biasLon != null) {
        val delta = 0.45
        val left = biasLon - delta
        val top = biasLat + delta
        val right = biasLon + delta
        val bottom = biasLat - delta
        "&viewbox=$left,$top,$right,$bottom&bounded=0"
      } else {
        ""
      }

    val url =
      "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&limit=6&addressdetails=1$viewboxParam"
    val request =
      Request.Builder()
        .url(url)
        .header("User-Agent", "AutoMeterAndroidApp/1.0 (contact: support@autometer.local)")
        .header("Accept-Language", "en")
        .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return emptyList()
      val body = response.body?.string() ?: return emptyList()
      val array = JSONArray(body)
      val results = mutableListOf<PlacePoint>()
      for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        val lat = item.optDouble("lat", Double.NaN)
        val lon = item.optDouble("lon", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) continue
        val displayName = item.optString("display_name", "Selected Place")
        val name = item.optString("name", "").ifBlank {
          displayName.split(",").firstOrNull()?.trim().orEmpty().ifBlank { "Location" }
        }
        results +=
          PlacePoint(
            id = item.optString("place_id", UUID.randomUUID().toString()),
            shortName = name,
            fullAddress = displayName,
            latitude = lat,
            longitude = lon,
          )
      }
      return results
    }
  }

  private fun queryNominatimReverse(latitude: Double, longitude: Double): PlacePoint? {
    val url =
      "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=jsonv2&addressdetails=1"
    val request =
      Request.Builder()
        .url(url)
        .header("User-Agent", "AutoMeterAndroidApp/1.0 (contact: support@autometer.local)")
        .header("Accept-Language", "en")
        .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return null
      val body = response.body?.string() ?: return null
      val json = JSONObject(body)
      val displayName = json.optString("display_name", "")
      if (displayName.isBlank()) return null

      val addressObj = json.optJSONObject("address")
      val roadOrSuburb =
        addressObj?.let { addr ->
          listOf("road", "neighbourhood", "suburb", "village", "town", "city_district", "city")
            .firstNotNullOfOrNull { key -> addr.optString(key, "").takeIf { it.isNotBlank() } }
        }
      val shortName =
        json.optString("name", "").takeIf { it.isNotBlank() }
          ?: roadOrSuburb
          ?: displayName.split(",").firstOrNull()?.trim()
          ?: "Current Location"

      return PlacePoint(
        id = "gps-rev-${latitude}_${longitude}",
        shortName = shortName,
        fullAddress = displayName,
        latitude = latitude,
        longitude = longitude,
        isCurrentGps = true,
      )
    }
  }

  private fun queryOsrmRoute(from: PlacePoint, to: PlacePoint): RouteResult? {
    val url =
      "https://router.project-osrm.org/route/v1/driving/" +
        "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
        "?overview=full&geometries=geojson"
    val request =
      Request.Builder()
        .url(url)
        .header("User-Agent", "AutoMeterAndroidApp/1.0")
        .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return null
      val body = response.body?.string() ?: return null
      val json = JSONObject(body)
      if (json.optString("code") != "Ok") return null
      val routes = json.optJSONArray("routes") ?: return null
      if (routes.length() == 0) return null

      val route = routes.getJSONObject(0)
      val distanceMeters = route.optDouble("distance", 0.0)
      val durationSeconds = route.optDouble("duration", 0.0)
      val geometry = route.optJSONObject("geometry")
      val coords = geometry?.optJSONArray("coordinates")

      val points = mutableListOf<GeoPoint>()
      if (coords != null) {
        for (i in 0 until coords.length()) {
          val pair = coords.getJSONArray(i)
          val lon = pair.getDouble(0)
          val lat = pair.getDouble(1)
          points.add(GeoPoint(lat, lon))
        }
      }
      if (points.isEmpty()) {
        points.add(from.toGeoPoint())
        points.add(to.toGeoPoint())
      }

      val km = (distanceMeters / 1000.0 * 100.0).roundToInt() / 100.0
      val mins = max(1, (durationSeconds / 60.0).roundToInt())

      return RouteResult(
        distanceKm = km,
        durationMinutes = mins,
        polylinePoints = points,
        isEstimatedFallback = false,
      )
    }
  }

  private suspend fun queryAndroidGeocoder(query: String): List<PlacePoint> {
    if (!Geocoder.isPresent()) return emptyList()
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address> =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
          geocoder.getFromLocationName(query, 5) { list ->
            if (cont.isActive) cont.resume(list)
          }
        }
      } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(query, 5).orEmpty()
      }

    return addresses.mapIndexed { index, addr ->
      val line = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
      val title = addr.featureName ?: addr.thoroughfare ?: addr.locality ?: "Location"
      PlacePoint(
        id = "geo-$index-${addr.latitude}-${addr.longitude}",
        shortName = title,
        fullAddress = line.ifBlank { title },
        latitude = addr.latitude,
        longitude = addr.longitude,
      )
    }
  }

  private suspend fun reverseAndroidGeocoder(lat: Double, lon: Double): PlacePoint? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address> =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
          geocoder.getFromLocation(lat, lon, 1) { list ->
            if (cont.isActive) cont.resume(list)
          }
        }
      } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(lat, lon, 1).orEmpty()
      }

    val addr = addresses.firstOrNull() ?: return null
    val line = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
    val short = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Current Location"
    return PlacePoint(
      id = "geo-rev-${lat}_${lon}",
      shortName = short,
      fullAddress = line.ifBlank { short },
      latitude = lat,
      longitude = lon,
      isCurrentGps = true,
    )
  }

  companion object {
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val r = 6371.0
      val dLat = Math.toRadians(lat2 - lat1)
      val dLon = Math.toRadians(lon2 - lon1)
      val a =
        sin(dLat / 2) * sin(dLat / 2) +
          cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
      val c = 2 * atan2(sqrt(a), sqrt(1 - a))
      return r * c
    }
  }
}
