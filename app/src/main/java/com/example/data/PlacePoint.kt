package com.example.data

import com.example.domain.FareBreakdown
import org.osmdroid.util.GeoPoint

data class PlacePoint(
  val id: String,
  val shortName: String,
  val fullAddress: String,
  val latitude: Double,
  val longitude: Double,
  val isCurrentGps: Boolean = false,
) {
  fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
}

data class RouteResult(
  val distanceKm: Double,
  val durationMinutes: Int,
  val polylinePoints: List<GeoPoint>,
  val isEstimatedFallback: Boolean = false,
)

data class CompletedTrip(
  val id: String,
  val timestampLabel: String,
  val fromLabel: String,
  val toLabel: String,
  val breakdown: FareBreakdown,
)
