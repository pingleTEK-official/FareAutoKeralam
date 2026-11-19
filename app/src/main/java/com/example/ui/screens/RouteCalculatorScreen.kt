package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlacePoint
import com.example.domain.FareCalculator
import com.example.domain.TariffMode
import com.example.ui.AutoMeterUiState
import com.example.ui.MapPickTarget
import com.example.ui.components.FareReceiptCard
import com.example.ui.components.OsmMapView
import kotlin.math.roundToInt
import org.osmdroid.util.GeoPoint

@Composable
fun RouteCalculatorScreen(
  uiState: AutoMeterUiState,
  onRequestLocation: () -> Unit,
  onFromQueryChanged: (String) -> Unit,
  onToQueryChanged: (String) -> Unit,
  onSelectFromPlace: (PlacePoint) -> Unit,
  onSelectToPlace: (PlacePoint) -> Unit,
  onSubmitDestination: () -> Unit,
  onSwapFromTo: () -> Unit,
  onClearFrom: () -> Unit,
  onClearTo: () -> Unit,
  onToggleMapPick: (MapPickTarget) -> Unit,
  onMapTapped: (GeoPoint) -> Unit,
  onManualDistanceChanged: (Double?) -> Unit,
  onWaitingMinutesChanged: (Int) -> Unit,
  onAdjustWaitingMinutes: (Int) -> Unit,
  onToggleWaitingStopwatch: () -> Unit,
  onResetWaitingStopwatch: () -> Unit,
  onTariffModeSelected: (TariffMode) -> Unit,
  onShareReceipt: () -> Unit,
  onSaveTripLog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWideScreen = maxWidth >= 840.dp

    if (isWideScreen) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Left Column: From/To + Waiting + Tariff Controls
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          item {
            GpsAndTariffHeaderCard(
              uiState = uiState,
              onRequestLocation = onRequestLocation,
              onTariffModeSelected = onTariffModeSelected,
            )
          }
          item {
            FromToInputColumnCard(
              uiState = uiState,
              onRequestLocation = onRequestLocation,
              onFromQueryChanged = onFromQueryChanged,
              onToQueryChanged = onToQueryChanged,
              onSelectFromPlace = onSelectFromPlace,
              onSelectToPlace = onSelectToPlace,
              onSubmitDestination = onSubmitDestination,
              onSwapFromTo = onSwapFromTo,
              onClearFrom = onClearFrom,
              onClearTo = onClearTo,
              onToggleMapPick = onToggleMapPick,
            )
          }
          item {
            DistanceAndWaitingCard(
              uiState = uiState,
              onManualDistanceChanged = onManualDistanceChanged,
              onWaitingMinutesChanged = onWaitingMinutesChanged,
              onAdjustWaitingMinutes = onAdjustWaitingMinutes,
              onToggleWaitingStopwatch = onToggleWaitingStopwatch,
              onResetWaitingStopwatch = onResetWaitingStopwatch,
            )
          }
        }

        // Right Column: Map + Fare Receipt
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          item {
            OsmMapView(
              mapCenter = uiState.mapCenter,
              currentGpsPoint = uiState.currentGpsPoint,
              fromPoint = uiState.fromPoint,
              toPoint = uiState.toPoint,
              routeResult = uiState.routeResult,
              mapPickTarget = uiState.mapPickTarget,
              onMapTapped = onMapTapped,
              onRecenterClick = onRequestLocation,
              onTogglePickDestination = { onToggleMapPick(MapPickTarget.PICK_TO) },
              modifier = Modifier.fillMaxWidth().height(320.dp),
            )
          }
          item {
            val hasRoute = (uiState.manualDistanceKm ?: uiState.routeResult?.distanceKm ?: 0.0) > 0.0
            FareReceiptCard(
              breakdown = uiState.routeFareBreakdown,
              fromLabel = uiState.fromPoint?.shortName ?: uiState.fromQuery,
              toLabel = uiState.toPoint?.shortName ?: uiState.toQuery,
              hasRouteDistance = hasRoute,
              onShareReceipt = onShareReceipt,
              onSaveTripLog = onSaveTripLog,
            )
          }
        }
      }
    } else {
      // Compact Handheld Layout: Smooth Scrollable Column
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          GpsAndTariffHeaderCard(
            uiState = uiState,
            onRequestLocation = onRequestLocation,
            onTariffModeSelected = onTariffModeSelected,
          )
        }

        item {
          OsmMapView(
            mapCenter = uiState.mapCenter,
            currentGpsPoint = uiState.currentGpsPoint,
            fromPoint = uiState.fromPoint,
            toPoint = uiState.toPoint,
            routeResult = uiState.routeResult,
            mapPickTarget = uiState.mapPickTarget,
            onMapTapped = onMapTapped,
            onRecenterClick = onRequestLocation,
            onTogglePickDestination = { onToggleMapPick(MapPickTarget.PICK_TO) },
            modifier = Modifier.fillMaxWidth().height(235.dp),
          )
        }

        item {
          FromToInputColumnCard(
            uiState = uiState,
            onRequestLocation = onRequestLocation,
            onFromQueryChanged = onFromQueryChanged,
            onToQueryChanged = onToQueryChanged,
            onSelectFromPlace = onSelectFromPlace,
            onSelectToPlace = onSelectToPlace,
            onSubmitDestination = onSubmitDestination,
            onSwapFromTo = onSwapFromTo,
            onClearFrom = onClearFrom,
            onClearTo = onClearTo,
            onToggleMapPick = onToggleMapPick,
          )
        }

        item {
          DistanceAndWaitingCard(
            uiState = uiState,
            onManualDistanceChanged = onManualDistanceChanged,
            onWaitingMinutesChanged = onWaitingMinutesChanged,
            onAdjustWaitingMinutes = onAdjustWaitingMinutes,
            onToggleWaitingStopwatch = onToggleWaitingStopwatch,
            onResetWaitingStopwatch = onResetWaitingStopwatch,
          )
        }

        item {
          val hasRoute = (uiState.manualDistanceKm ?: uiState.routeResult?.distanceKm ?: 0.0) > 0.0
          FareReceiptCard(
            breakdown = uiState.routeFareBreakdown,
            fromLabel = uiState.fromPoint?.shortName ?: uiState.fromQuery,
            toLabel = uiState.toPoint?.shortName ?: uiState.toQuery,
            hasRouteDistance = hasRoute,
            onShareReceipt = onShareReceipt,
            onSaveTripLog = onSaveTripLog,
          )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun GpsAndTariffHeaderCard(
  uiState: AutoMeterUiState,
  onRequestLocation: () -> Unit,
  onTariffModeSelected: (TariffMode) -> Unit,
) {
  val isNightActive = uiState.routeFareBreakdown.isNightApplied
  ElevatedCard(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor =
          if (isNightActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.surface
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (isNightActive) Icons.Default.NightsStay else Icons.Default.WbSunny,
            contentDescription = null,
            tint =
              if (isNightActive) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text =
                if (isNightActive) "Night Tariff Active (+50%)"
                else "Morning / Day Tariff",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                "${uiState.currentTimeFormatted.ifBlank { "Now" }} • Night applies 7:00 PM – 6:00 AM",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        FilledTonalButton(
          onClick = onRequestLocation,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("gps_standing_button"),
        ) {
          if (uiState.isLocating) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "Detect standing GPS location",
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (uiState.currentGpsPoint != null) "GPS Live" else "Get GPS",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      if (uiState.locationStatusMessage != null) {
        Text(
          text = uiState.locationStatusMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      // Tariff Mode Scrollable / Pill Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TariffMode.entries.forEach { mode ->
          val selected = uiState.tariffMode == mode
          FilterChip(
            selected = selected,
            onClick = { onTariffModeSelected(mode) },
            label = {
              Text(
                text = mode.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                  if (mode == TariffMode.NIGHT_SURCHARGE || (mode == TariffMode.AUTO_TIME && isNightActive)) {
                    MaterialTheme.colorScheme.tertiary
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
                selectedLabelColor =
                  if (mode == TariffMode.NIGHT_SURCHARGE || (mode == TariffMode.AUTO_TIME && isNightActive)) {
                    MaterialTheme.colorScheme.onTertiary
                  } else {
                    MaterialTheme.colorScheme.onPrimary
                  },
              ),
            modifier = Modifier.weight(1f).testTag("tariff_chip_${mode.name.lowercase()}"),
          )
        }
      }
    }
  }
}

@Composable
private fun FromToInputColumnCard(
  uiState: AutoMeterUiState,
  onRequestLocation: () -> Unit,
  onFromQueryChanged: (String) -> Unit,
  onToQueryChanged: (String) -> Unit,
  onSelectFromPlace: (PlacePoint) -> Unit,
  onSelectToPlace: (PlacePoint) -> Unit,
  onSubmitDestination: () -> Unit,
  onSwapFromTo: () -> Unit,
  onClearFrom: () -> Unit,
  onClearTo: () -> Unit,
  onToggleMapPick: (MapPickTarget) -> Unit,
) {
  val focusManager = LocalFocusManager.current

  ElevatedCard(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Trip Route (From → To)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        TextButton(
          onClick = { onToggleMapPick(MapPickTarget.PICK_TO) },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AddLocationAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (uiState.mapPickTarget == MapPickTarget.PICK_TO) "Tap Map Now" else "Pick on Map",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      // FROM Input Field
      OutlinedTextField(
        value = uiState.fromQuery,
        onValueChange = onFromQueryChanged,
        label = { Text("From (Standing / Pickup)") },
        placeholder = { Text("Detecting current location or type pickup...") },
        singleLine = true,
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.TripOrigin,
            contentDescription = "Pickup point",
            tint = MaterialTheme.colorScheme.secondary,
          )
        },
        trailingIcon = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.isSearchingFrom) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            if (uiState.fromQuery.isNotEmpty()) {
              IconButton(onClick = onClearFrom, modifier = Modifier.testTag("clear_from_button")) {
                Icon(Icons.Default.Close, contentDescription = "Clear pickup")
              }
            }
            IconButton(
              onClick = {
                val gps = uiState.currentGpsPoint
                if (gps != null) onSelectFromPlace(gps) else onRequestLocation()
              },
              modifier = Modifier.testTag("use_gps_from_button"),
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Use current GPS standing location",
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          ),
        modifier = Modifier.fillMaxWidth().testTag("from_location_input"),
      )

      // Scroll-down Suggestions for FROM
      AnimatedVisibility(
        visible = uiState.showFromDropdown && (uiState.fromSuggestions.isNotEmpty() || uiState.currentGpsPoint != null)
      ) {
        SuggestionScrollPanel(
          suggestions = uiState.fromSuggestions,
          gpsPoint = uiState.currentGpsPoint,
          recentPlaces = uiState.recentPlaces,
          onSelect = {
            focusManager.clearFocus()
            onSelectFromPlace(it)
          },
          onPickOnMap = {
            focusManager.clearFocus()
            onToggleMapPick(MapPickTarget.PICK_FROM)
          },
        )
      }

      // Swap Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HorizontalDivider(
          modifier = Modifier.weight(1f),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        )
        FilledTonalIconButton(
          onClick = onSwapFromTo,
          modifier = Modifier.padding(horizontal = 8.dp).size(40.dp).testTag("swap_locations_button"),
        ) {
          Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = "Swap pickup and destination",
          )
        }
        HorizontalDivider(
          modifier = Modifier.weight(1f),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        )
      }

      // TO (Destination) Input Field
      OutlinedTextField(
        value = uiState.toQuery,
        onValueChange = onToQueryChanged,
        label = { Text("To (Destination)") },
        placeholder = { Text("Enter destination to analyse km & fare...") },
        singleLine = true,
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Destination point",
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        trailingIcon = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.isSearchingTo) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            if (uiState.toQuery.isNotEmpty()) {
              IconButton(onClick = onClearTo, modifier = Modifier.testTag("clear_to_button")) {
                Icon(Icons.Default.Close, contentDescription = "Clear destination")
              }
            }
            IconButton(
              onClick = {
                focusManager.clearFocus()
                onSubmitDestination()
              },
              modifier = Modifier.testTag("search_destination_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Analyse destination route",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = {
              focusManager.clearFocus()
              onSubmitDestination()
            }
          ),
        shape = RoundedCornerShape(14.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          ),
        modifier = Modifier.fillMaxWidth().testTag("to_location_input"),
      )

      // Scroll-down Suggestions for TO
      AnimatedVisibility(
        visible = uiState.showToDropdown && (uiState.toSuggestions.isNotEmpty() || uiState.recentPlaces.isNotEmpty())
      ) {
        SuggestionScrollPanel(
          suggestions = uiState.toSuggestions,
          gpsPoint = null,
          recentPlaces = uiState.recentPlaces,
          onSelect = {
            focusManager.clearFocus()
            onSelectToPlace(it)
          },
          onPickOnMap = {
            focusManager.clearFocus()
            onToggleMapPick(MapPickTarget.PICK_TO)
          },
        )
      }

      if (uiState.isAnalyzingRoute) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(top = 4.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Text(
            text = "Analysing road distance & route...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      if (uiState.routeError != null) {
        Text(
          text = uiState.routeError,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SuggestionScrollPanel(
  suggestions: List<PlacePoint>,
  gpsPoint: PlacePoint?,
  recentPlaces: List<PlacePoint>,
  onSelect: (PlacePoint) -> Unit,
  onPickOnMap: () -> Unit,
) {
  val itemsToShow = if (suggestions.isNotEmpty()) suggestions else recentPlaces
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    modifier =
      Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState())
    ) {
      if (gpsPoint != null) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { onSelect(gpsPoint) }
              .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Current Standing GPS (${gpsPoint.shortName})",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
              text = gpsPoint.fullAddress,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }

      itemsToShow.forEachIndexed { index, place ->
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { onSelect(place) }
              .padding(horizontal = 14.dp, vertical = 11.dp)
              .testTag("suggestion_item_$index"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = place.shortName,
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = place.fullAddress,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      }

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { onPickOnMap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.AddLocationAlt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Tap directly on the map instead",
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistanceAndWaitingCard(
  uiState: AutoMeterUiState,
  onManualDistanceChanged: (Double?) -> Unit,
  onWaitingMinutesChanged: (Int) -> Unit,
  onAdjustWaitingMinutes: (Int) -> Unit,
  onToggleWaitingStopwatch: () -> Unit,
  onResetWaitingStopwatch: () -> Unit,
) {
  val routeDistance = uiState.routeResult?.distanceKm ?: 0.0
  val activeDistance = uiState.manualDistanceKm ?: routeDistance

  val waitingPresets = listOf(0, 5, 10, 15, 30, 45, 60, 75, 90, 120)
  var waitingDropdownExpanded by remember { mutableStateOf(false) }

  ElevatedCard(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Distance Covered Analysis + Scroll Slider
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Route,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Column {
            Text(
              text = "Distance Covered",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                when {
                  uiState.manualDistanceKm != null -> "Manual / adjusted km"
                  uiState.routeResult != null -> "Map route • ~${uiState.routeResult.durationMinutes} min"
                  else -> "Enter destination or slide km"
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (uiState.manualDistanceKm != null && uiState.routeResult != null) {
            TextButton(onClick = { onManualDistanceChanged(null) }) {
              Text("Reset")
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = FareCalculator.formatKm(activeDistance),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).testTag("distance_badge"),
            )
          }
        }
      }

      // Smooth Scroll Slider + Quick +/- 0.5 km steppers
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalIconButton(
          onClick = {
            val next = ((activeDistance - 0.5).coerceAtLeast(0.0) * 10.0).roundToInt() / 10.0
            onManualDistanceChanged(next)
          },
          modifier = Modifier.size(48.dp).testTag("decrease_km_button"),
        ) {
          Icon(Icons.Default.Remove, contentDescription = "Decrease distance by 0.5 km")
        }

        Slider(
          value = activeDistance.toFloat().coerceIn(0f, 40f),
          onValueChange = { raw ->
            val snapped = (raw * 10f).roundToInt() / 10.0
            onManualDistanceChanged(snapped)
          },
          valueRange = 0f..40f,
          colors =
            SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.secondary,
              activeTrackColor = MaterialTheme.colorScheme.secondary,
            ),
          modifier = Modifier.weight(1f).testTag("distance_slider"),
        )

        FilledTonalIconButton(
          onClick = {
            val next = ((activeDistance + 0.5).coerceAtMost(200.0) * 10.0).roundToInt() / 10.0
            onManualDistanceChanged(next)
          },
          modifier = Modifier.size(48.dp).testTag("increase_km_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = "Increase distance by 0.5 km")
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

      // Waiting Time Section (Scroll-down Dropdown + Steppers + Live Stopwatch)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = "Waiting Time",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                if (uiState.routeFareBreakdown.isNightApplied) {
                  "₹15 per 15 mins (Night +50%)"
                } else {
                  "₹10 per 15 mins (Day rate)"
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Stepper -5 / +5
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilledTonalIconButton(
            onClick = { onAdjustWaitingMinutes(-5) },
            modifier = Modifier.size(48.dp).testTag("decrease_wait_button"),
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Subtract 5 waiting minutes")
          }
          Text(
            text = "${uiState.waitingMinutes}m",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 4.dp).testTag("waiting_minutes_text"),
          )
          FilledTonalIconButton(
            onClick = { onAdjustWaitingMinutes(5) },
            modifier = Modifier.size(48.dp).testTag("increase_wait_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add 5 waiting minutes")
          }
        }
      }

      // Scroll-Down Dropdown for Waiting Presets
      ExposedDropdownMenuBox(
        expanded = waitingDropdownExpanded,
        onExpandedChange = { waitingDropdownExpanded = !waitingDropdownExpanded },
        modifier = Modifier.fillMaxWidth(),
      ) {
        val currentWaitText =
          if (uiState.waitingMinutes == 0) {
            "0 mins (No waiting charge)"
          } else {
            val blocks = uiState.routeFareBreakdown.waitingBlocks
            val waitCost =
              if (uiState.routeFareBreakdown.isNightApplied) {
                blocks * 15
              } else {
                blocks * 10
              }
            "${uiState.waitingMinutes} mins  •  ₹$waitCost ($blocks × 15m slab)"
          }

        OutlinedTextField(
          value = currentWaitText,
          onValueChange = {},
          readOnly = true,
          label = { Text("Select Waiting Duration") },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = waitingDropdownExpanded)
          },
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("waiting_dropdown"),
        )

        ExposedDropdownMenu(
          expanded = waitingDropdownExpanded,
          onDismissRequest = { waitingDropdownExpanded = false },
        ) {
          waitingPresets.forEach { mins ->
            val blocks = if (mins == 0) 0 else (mins + 14) / 15
            val cost =
              if (uiState.routeFareBreakdown.isNightApplied) blocks * 15 else blocks * 10
            DropdownMenuItem(
              text = {
                Text(
                  text =
                    if (mins == 0) "0 mins — ₹0"
                    else "$mins mins — ₹$cost ($blocks × 15m)",
                  style = MaterialTheme.typography.bodyLarge,
                )
              },
              onClick = {
                onWaitingMinutesChanged(mins)
                waitingDropdownExpanded = false
              },
            )
          }
        }
      }

      // Live Waiting Stopwatch Bar
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(
                    if (uiState.isWaitingStopwatchRunning) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outline
                  )
            )
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            val mins = uiState.waitingStopwatchSeconds / 60
            val secs = uiState.waitingStopwatchSeconds % 60
            Text(
              text = "Timer: %02d:%02d".format(mins, secs),
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
              onClick = onToggleWaitingStopwatch,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("toggle_waiting_timer_button"),
            ) {
              Icon(
                imageVector =
                  if (uiState.isWaitingStopwatchRunning) Icons.Default.Pause
                  else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (uiState.isWaitingStopwatchRunning) "Pause" else "Start Wait")
            }

            if (uiState.waitingStopwatchSeconds > 0 || uiState.waitingMinutes > 0) {
              IconButton(
                onClick = onResetWaitingStopwatch,
                modifier = Modifier.size(48.dp).testTag("reset_waiting_timer_button"),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset waiting timer")
              }
            }
          }
        }
      }
    }
  }
}
