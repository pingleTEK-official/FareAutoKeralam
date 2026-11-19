package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CompletedTrip
import com.example.data.LocationTracker
import com.example.data.MapRoutingService
import com.example.data.PlacePoint
import com.example.data.RouteResult
import com.example.domain.FareBreakdown
import com.example.domain.FareCalculator
import com.example.domain.TariffMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

enum class AppTab {
  ROUTE_CALCULATOR,
  LIVE_METER,
  RATE_CARD,
}

enum class MapPickTarget {
  NONE,
  PICK_FROM,
  PICK_TO,
}

data class AutoMeterUiState(
  val selectedTab: AppTab = AppTab.ROUTE_CALCULATOR,
  val hasLocationPermission: Boolean = false,
  val isLocating: Boolean = false,
  val locationStatusMessage: String? = null,
  val currentGpsPoint: PlacePoint? = null,
  val mapCenter: GeoPoint = GeoPoint(12.9716, 77.5946), // Default Bengaluru center until GPS locks
  val mapPickTarget: MapPickTarget = MapPickTarget.NONE,

  // From & To Route Fields
  val fromQuery: String = "",
  val fromPoint: PlacePoint? = null,
  val fromSuggestions: List<PlacePoint> = emptyList(),
  val isSearchingFrom: Boolean = false,
  val showFromDropdown: Boolean = false,

  val toQuery: String = "",
  val toPoint: PlacePoint? = null,
  val toSuggestions: List<PlacePoint> = emptyList(),
  val isSearchingTo: Boolean = false,
  val showToDropdown: Boolean = false,

  // Route & Distance
  val routeResult: RouteResult? = null,
  val isAnalyzingRoute: Boolean = false,
  val routeError: String? = null,
  val manualDistanceKm: Double? = null,

  // Waiting & Tariff
  val waitingMinutes: Int = 0,
  val isWaitingStopwatchRunning: Boolean = false,
  val waitingStopwatchSeconds: Int = 0,
  val tariffMode: TariffMode = TariffMode.AUTO_TIME,
  val currentHour24: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
  val currentTimeFormatted: String = "",

  // Computed Route Fare
  val routeFareBreakdown: FareBreakdown =
    FareCalculator.calculate(
      distanceKm = 0.0,
      waitingMinutes = 0,
      tariffMode = TariffMode.AUTO_TIME,
      currentHour24 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    ),

  // Live GPS Meter State
  val isLiveMeterRunning: Boolean = false,
  val isLiveMeterPaused: Boolean = false,
  val isLiveWaitingOn: Boolean = false,
  val liveDistanceKm: Double = 0.0,
  val liveElapsedSeconds: Int = 0,
  val liveWaitingSeconds: Int = 0,
  val livePathPoints: List<GeoPoint> = emptyList(),
  val liveFareBreakdown: FareBreakdown =
    FareCalculator.calculate(
      distanceKm = 0.0,
      waitingMinutes = 0,
      tariffMode = TariffMode.AUTO_TIME,
      currentHour24 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    ),

  // Recent Session Trips & Quick Destinations
  val recentPlaces: List<PlacePoint> = emptyList(),
  val sessionTrips: List<CompletedTrip> = emptyList(),
)

class AutoMeterViewModel(application: Application) : AndroidViewModel(application) {

  private val locationTracker = LocationTracker(application.applicationContext)
  private val mapRoutingService = MapRoutingService(application.applicationContext)

  private val _uiState = MutableStateFlow(AutoMeterUiState())
  val uiState: StateFlow<AutoMeterUiState> = _uiState.asStateFlow()

  private var fromSearchJob: Job? = null
  private var toSearchJob: Job? = null
  private var routeJob: Job? = null
  private var waitingStopwatchJob: Job? = null
  private var liveMeterTickerJob: Job? = null
  private var liveLocationJob: Job? = null
  private var lastLiveLocation: Location? = null

  init {
    refreshClock()
    startClockTicker()
    val permitted = locationTracker.hasLocationPermission()
    _uiState.update { it.copy(hasLocationPermission = permitted) }
    if (permitted) {
      detectCurrentLocation(setAsFromIfEmpty = true)
    }
  }

  private fun startClockTicker() {
    viewModelScope.launch {
      while (isActive) {
        refreshClock()
        delay(15_000L)
      }
    }
  }

  private fun refreshClock() {
    val now = Calendar.getInstance()
    val hour24 = now.get(Calendar.HOUR_OF_DAY)
    val formatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now.time)
    _uiState.update { state ->
      val updated = state.copy(currentHour24 = hour24, currentTimeFormatted = formatted)
      recomputeFares(updated)
    }
  }

  fun selectTab(tab: AppTab) {
    _uiState.update { it.copy(selectedTab = tab) }
  }

  fun onLocationPermissionResult(granted: Boolean) {
    _uiState.update {
      it.copy(
        hasLocationPermission = granted,
        locationStatusMessage =
          if (granted) null else "Location permission denied. Type pickup address or tap map.",
      )
    }
    if (granted) {
      detectCurrentLocation(setAsFromIfEmpty = true)
    }
  }

  fun detectCurrentLocation(setAsFromIfEmpty: Boolean = true) {
    viewModelScope.launch {
      if (!locationTracker.hasLocationPermission()) {
        _uiState.update {
          it.copy(
            hasLocationPermission = false,
            locationStatusMessage = "Tap GPS to grant location access.",
          )
        }
        return@launch
      }

      _uiState.update { it.copy(isLocating = true, locationStatusMessage = "Locating GPS...") }
      val loc = locationTracker.fetchCurrentLocation()
      if (loc == null) {
        _uiState.update {
          it.copy(
            isLocating = false,
            locationStatusMessage = "GPS signal unavailable. Enter pickup or tap on map.",
          )
        }
        return@launch
      }

      val resolvedPlace = mapRoutingService.reverseGeocode(loc.latitude, loc.longitude)
      _uiState.update { state ->
        val shouldFillFrom = setAsFromIfEmpty || state.fromPoint?.isCurrentGps == true || state.fromQuery.isBlank()
        val nextFromPoint = if (shouldFillFrom) resolvedPlace else state.fromPoint
        val nextFromQuery = if (shouldFillFrom) resolvedPlace.shortName else state.fromQuery
        state.copy(
          isLocating = false,
          locationStatusMessage = null,
          currentGpsPoint = resolvedPlace,
          mapCenter = resolvedPlace.toGeoPoint(),
          fromPoint = nextFromPoint,
          fromQuery = nextFromQuery,
          showFromDropdown = false,
        )
      }

      val currentState = _uiState.value
      if (currentState.fromPoint != null && currentState.toPoint != null) {
        analyzeRoute(currentState.fromPoint, currentState.toPoint)
      }
    }
  }

  fun onFromQueryChanged(query: String) {
    _uiState.update {
      it.copy(
        fromQuery = query,
        fromPoint = if (it.fromPoint?.shortName == query) it.fromPoint else null,
        showFromDropdown = true,
      )
    }
    fromSearchJob?.cancel()
    if (query.trim().length < 2) {
      _uiState.update { it.copy(fromSuggestions = emptyList(), isSearchingFrom = false) }
      return
    }
    fromSearchJob =
      viewModelScope.launch {
        _uiState.update { it.copy(isSearchingFrom = true) }
        delay(350L)
        val gps = _uiState.value.currentGpsPoint
        val results = mapRoutingService.searchPlaces(query, gps?.latitude, gps?.longitude)
        _uiState.update { it.copy(fromSuggestions = results, isSearchingFrom = false) }
      }
  }

  fun onToQueryChanged(query: String) {
    _uiState.update {
      it.copy(
        toQuery = query,
        toPoint = if (it.toPoint?.shortName == query) it.toPoint else null,
        showToDropdown = true,
        routeError = null,
      )
    }
    toSearchJob?.cancel()
    if (query.trim().length < 2) {
      _uiState.update { it.copy(toSuggestions = emptyList(), isSearchingTo = false) }
      return
    }
    toSearchJob =
      viewModelScope.launch {
        _uiState.update { it.copy(isSearchingTo = true) }
        delay(350L)
        val bias = _uiState.value.fromPoint ?: _uiState.value.currentGpsPoint
        val results = mapRoutingService.searchPlaces(query, bias?.latitude, bias?.longitude)
        _uiState.update { it.copy(toSuggestions = results, isSearchingTo = false) }
      }
  }

  fun selectFromPlace(place: PlacePoint) {
    fromSearchJob?.cancel()
    _uiState.update { state ->
      state.copy(
        fromQuery = place.shortName,
        fromPoint = place,
        showFromDropdown = false,
        isSearchingFrom = false,
        mapCenter = place.toGeoPoint(),
        recentPlaces = (listOf(place) + state.recentPlaces.filterNot { it.shortName == place.shortName }).take(6),
      )
    }
    val to = _uiState.value.toPoint
    if (to != null) {
      analyzeRoute(place, to)
    }
  }

  fun selectToPlace(place: PlacePoint) {
    toSearchJob?.cancel()
    _uiState.update { state ->
      state.copy(
        toQuery = place.shortName,
        toPoint = place,
        showToDropdown = false,
        isSearchingTo = false,
        manualDistanceKm = null,
        recentPlaces = (listOf(place) + state.recentPlaces.filterNot { it.shortName == place.shortName }).take(6),
      )
    }
    val from = _uiState.value.fromPoint
    if (from != null) {
      analyzeRoute(from, place)
    } else if (_uiState.value.hasLocationPermission) {
      detectCurrentLocation(setAsFromIfEmpty = true)
    }
  }

  fun submitDestinationSearch() {
    val state = _uiState.value
    if (state.toPoint != null && state.fromPoint != null) {
      analyzeRoute(state.fromPoint, state.toPoint)
      return
    }
    val firstSuggestion = state.toSuggestions.firstOrNull()
    if (firstSuggestion != null) {
      selectToPlace(firstSuggestion)
      return
    }
    val query = state.toQuery.trim()
    if (query.length < 2) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingTo = true) }
      val bias = _uiState.value.fromPoint ?: _uiState.value.currentGpsPoint
      val results = mapRoutingService.searchPlaces(query, bias?.latitude, bias?.longitude)
      _uiState.update { it.copy(toSuggestions = results, isSearchingTo = false) }
      val top = results.firstOrNull()
      if (top != null) {
        selectToPlace(top)
      } else {
        _uiState.update {
          it.copy(routeError = "Could not find \"$query\". Try a landmark or tap on the map.")
        }
      }
    }
  }

  fun setDropdownVisibility(isFrom: Boolean, visible: Boolean) {
    _uiState.update {
      if (isFrom) it.copy(showFromDropdown = visible) else it.copy(showToDropdown = visible)
    }
  }

  fun swapFromAndTo() {
    val state = _uiState.value
    val oldFrom = state.fromPoint
    val oldFromQuery = state.fromQuery
    val oldTo = state.toPoint
    val oldToQuery = state.toQuery

    _uiState.update {
      it.copy(
        fromPoint = oldTo,
        fromQuery = oldToQuery,
        toPoint = oldFrom,
        toQuery = oldFromQuery,
        showFromDropdown = false,
        showToDropdown = false,
      )
    }
    if (oldTo != null && oldFrom != null) {
      analyzeRoute(oldTo, oldFrom)
    }
  }

  fun clearFrom() {
    _uiState.update {
      it.copy(
        fromQuery = "",
        fromPoint = null,
        fromSuggestions = emptyList(),
        routeResult = null,
        showFromDropdown = false,
      ).let(::recomputeFares)
    }
  }

  fun clearTo() {
    _uiState.update {
      it.copy(
        toQuery = "",
        toPoint = null,
        toSuggestions = emptyList(),
        routeResult = null,
        manualDistanceKm = null,
        routeError = null,
        showToDropdown = false,
      ).let(::recomputeFares)
    }
  }

  fun toggleMapPickMode(target: MapPickTarget) {
    _uiState.update {
      it.copy(mapPickTarget = if (it.mapPickTarget == target) MapPickTarget.NONE else target)
    }
  }

  fun onMapTapped(geoPoint: GeoPoint) {
    val target = _uiState.value.mapPickTarget
    if (target == MapPickTarget.NONE) return

    _uiState.update { it.copy(mapPickTarget = MapPickTarget.NONE) }
    viewModelScope.launch {
      val place =
        mapRoutingService.reverseGeocode(geoPoint.latitude, geoPoint.longitude).copy(isCurrentGps = false)
      when (target) {
        MapPickTarget.PICK_FROM -> selectFromPlace(place)
        MapPickTarget.PICK_TO -> selectToPlace(place)
        MapPickTarget.NONE -> Unit
      }
    }
  }

  private fun analyzeRoute(from: PlacePoint, to: PlacePoint) {
    routeJob?.cancel()
    routeJob =
      viewModelScope.launch {
        _uiState.update {
          it.copy(isAnalyzingRoute = true, routeError = null, showFromDropdown = false, showToDropdown = false)
        }
        val result = mapRoutingService.fetchDrivingRoute(from, to)
        _uiState.update { state ->
          val updated =
            state.copy(
              isAnalyzingRoute = false,
              routeResult = result,
              manualDistanceKm = null,
              routeError =
                if (result.isEstimatedFallback) {
                  "Offline estimate (1.25× road factor)"
                } else {
                  null
                },
            )
          recomputeFares(updated)
        }
      }
  }

  fun setManualDistanceKm(distanceKm: Double?) {
    _uiState.update { state ->
      val clamped = distanceKm?.coerceIn(0.0, 200.0)
      recomputeFares(state.copy(manualDistanceKm = clamped))
    }
  }

  fun adjustWaitingMinutes(delta: Int) {
    _uiState.update { state ->
      val next = (state.waitingMinutes + delta).coerceIn(0, 600)
      recomputeFares(state.copy(waitingMinutes = next))
    }
  }

  fun setWaitingMinutes(minutes: Int) {
    _uiState.update { state ->
      recomputeFares(state.copy(waitingMinutes = minutes.coerceIn(0, 600)))
    }
  }

  fun toggleWaitingStopwatch() {
    val currentlyRunning = _uiState.value.isWaitingStopwatchRunning
    if (currentlyRunning) {
      waitingStopwatchJob?.cancel()
      _uiState.update { it.copy(isWaitingStopwatchRunning = false) }
    } else {
      _uiState.update { it.copy(isWaitingStopwatchRunning = true) }
      waitingStopwatchJob =
        viewModelScope.launch {
          while (isActive) {
            delay(1000L)
            _uiState.update { state ->
              val nextSec = state.waitingStopwatchSeconds + 1
              val stopwatchMins = (nextSec + 59) / 60
              val effectiveWait = maxOf(state.waitingMinutes, stopwatchMins)
              recomputeFares(
                state.copy(
                  waitingStopwatchSeconds = nextSec,
                  waitingMinutes = effectiveWait,
                )
              )
            }
          }
        }
    }
  }

  fun resetWaitingStopwatch() {
    waitingStopwatchJob?.cancel()
    _uiState.update { state ->
      recomputeFares(
        state.copy(
          isWaitingStopwatchRunning = false,
          waitingStopwatchSeconds = 0,
          waitingMinutes = 0,
        )
      )
    }
  }

  fun setTariffMode(mode: TariffMode) {
    _uiState.update { state -> recomputeFares(state.copy(tariffMode = mode)) }
  }

  fun saveCurrentRouteTripToLog() {
    val state = _uiState.value
    val dist = state.manualDistanceKm ?: state.routeResult?.distanceKm ?: 0.0
    if (dist <= 0.0) return
    val fromLabel = state.fromPoint?.shortName ?: state.fromQuery.ifBlank { "Current Pickup" }
    val toLabel = state.toPoint?.shortName ?: state.toQuery.ifBlank { "Destination" }
    val stamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    val trip =
      CompletedTrip(
        id = UUID.randomUUID().toString(),
        timestampLabel = stamp,
        fromLabel = fromLabel,
        toLabel = toLabel,
        breakdown = state.routeFareBreakdown,
      )
    _uiState.update { it.copy(sessionTrips = listOf(trip) + it.sessionTrips) }
  }

  // --- Live GPS Meter Actions ---

  fun startOrResumeLiveMeter() {
    if (!locationTracker.hasLocationPermission()) {
      _uiState.update {
        it.copy(locationStatusMessage = "Grant GPS location permission to run Live Meter.")
      }
      return
    }
    _uiState.update {
      it.copy(
        isLiveMeterRunning = true,
        isLiveMeterPaused = false,
      )
    }
    startLiveMeterTicker()
    startLiveGpsCollection()
  }

  fun pauseLiveMeter() {
    liveMeterTickerJob?.cancel()
    liveLocationJob?.cancel()
    _uiState.update { it.copy(isLiveMeterPaused = true) }
  }

  fun toggleLiveWaiting() {
    _uiState.update { it.copy(isLiveWaitingOn = !it.isLiveWaitingOn) }
  }

  fun finishAndResetLiveMeter(saveToLog: Boolean) {
    val state = _uiState.value
    if (saveToLog && (state.liveDistanceKm > 0.0 || state.liveElapsedSeconds > 0)) {
      val stamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
      val trip =
        CompletedTrip(
          id = UUID.randomUUID().toString(),
          timestampLabel = stamp,
          fromLabel = state.currentGpsPoint?.shortName ?: "Live Meter Start",
          toLabel = "Live Trip (${FareCalculator.formatKm(state.liveDistanceKm)})",
          breakdown = state.liveFareBreakdown,
        )
      _uiState.update { it.copy(sessionTrips = listOf(trip) + it.sessionTrips) }
    }

    liveMeterTickerJob?.cancel()
    liveLocationJob?.cancel()
    lastLiveLocation = null
    _uiState.update { current ->
      recomputeFares(
        current.copy(
          isLiveMeterRunning = false,
          isLiveMeterPaused = false,
          isLiveWaitingOn = false,
          liveDistanceKm = 0.0,
          liveElapsedSeconds = 0,
          liveWaitingSeconds = 0,
          livePathPoints = emptyList(),
        )
      )
    }
  }

  private fun startLiveMeterTicker() {
    liveMeterTickerJob?.cancel()
    liveMeterTickerJob =
      viewModelScope.launch {
        while (isActive) {
          delay(1000L)
          _uiState.update { state ->
            if (!state.isLiveMeterRunning || state.isLiveMeterPaused) return@update state
            val nextElapsed = state.liveElapsedSeconds + 1
            val nextWaitingSec =
              if (state.isLiveWaitingOn) state.liveWaitingSeconds + 1 else state.liveWaitingSeconds
            recomputeFares(
              state.copy(
                liveElapsedSeconds = nextElapsed,
                liveWaitingSeconds = nextWaitingSec,
              )
            )
          }
        }
      }
  }

  private fun startLiveGpsCollection() {
    liveLocationJob?.cancel()
    liveLocationJob =
      viewModelScope.launch {
        locationTracker.observeLiveLocation(2500L).collect { newLoc ->
          if (newLoc.accuracy > 50f) return@collect
          val prev = lastLiveLocation
          val addedKm =
            if (prev != null) {
              val distMeters = prev.distanceTo(newLoc)
              if (distMeters >= 6f) distMeters / 1000.0 else 0.0
            } else {
              0.0
            }
          if (prev == null || addedKm > 0.0) {
            lastLiveLocation = newLoc
          }
          _uiState.update { state ->
            val point = GeoPoint(newLoc.latitude, newLoc.longitude)
            val updatedPath =
              if (state.livePathPoints.isEmpty() || addedKm > 0.0) {
                state.livePathPoints + point
              } else {
                state.livePathPoints
              }
            recomputeFares(
              state.copy(
                liveDistanceKm = state.liveDistanceKm + addedKm,
                livePathPoints = updatedPath,
                mapCenter = point,
              )
            )
          }
        }
      }
  }

  private fun recomputeFares(state: AutoMeterUiState): AutoMeterUiState {
    val effectiveRouteDistance = state.manualDistanceKm ?: state.routeResult?.distanceKm ?: 0.0
    val routeBreakdown =
      FareCalculator.calculate(
        distanceKm = effectiveRouteDistance,
        waitingMinutes = state.waitingMinutes,
        tariffMode = state.tariffMode,
        currentHour24 = state.currentHour24,
      )

    val liveWaitMinutes =
      if (state.liveWaitingSeconds <= 0) 0 else (state.liveWaitingSeconds + 59) / 60
    val liveBreakdown =
      FareCalculator.calculate(
        distanceKm = state.liveDistanceKm,
        waitingMinutes = liveWaitMinutes,
        tariffMode = state.tariffMode,
        currentHour24 = state.currentHour24,
      )

    return state.copy(
      routeFareBreakdown = routeBreakdown,
      liveFareBreakdown = liveBreakdown,
    )
  }

  override fun onCleared() {
    super.onCleared()
    fromSearchJob?.cancel()
    toSearchJob?.cancel()
    routeJob?.cancel()
    waitingStopwatchJob?.cancel()
    liveMeterTickerJob?.cancel()
    liveLocationJob?.cancel()
  }
}
