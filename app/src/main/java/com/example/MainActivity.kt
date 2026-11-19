package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.FareCalculator
import com.example.ui.AppTab
import com.example.ui.AutoMeterViewModel
import com.example.ui.screens.LiveMeterScreen
import com.example.ui.screens.RateCardScreen
import com.example.ui.screens.RouteCalculatorScreen
import com.example.ui.theme.AutoMeterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AutoMeterTheme {
        AutoMeterApp()
      }
    }
  }
}

@Composable
fun AutoMeterApp(viewModel: AutoMeterViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
      val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
      val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      viewModel.onLocationPermissionResult(fineGranted || coarseGranted)
    }

  LaunchedEffect(Unit) {
    if (!uiState.hasLocationPermission) {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  val requestOrRefreshGps: () -> Unit = {
    if (uiState.hasLocationPermission) {
      viewModel.detectCurrentLocation(setAsFromIfEmpty = true)
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  // Handle back button on secondary tabs by returning to Route Calculator
  BackHandler(enabled = uiState.selectedTab != AppTab.ROUTE_CALCULATOR) {
    viewModel.selectTab(AppTab.ROUTE_CALCULATOR)
  }

  val shareFareSummary: () -> Unit = {
    val b = uiState.routeFareBreakdown
    val fromName = uiState.fromPoint?.shortName ?: uiState.fromQuery.ifBlank { "Current Location" }
    val toName = uiState.toPoint?.shortName ?: uiState.toQuery.ifBlank { "Destination" }
    val tariffLabel = if (b.isNightApplied) "Night (+50%)" else "Morning/Day"
    val summary =
      buildString {
        appendLine("AutoMeter Fare Summary")
        appendLine("From: $fromName")
        appendLine("To: $toName")
        appendLine("Distance: ${FareCalculator.formatKm(b.distanceKm)}")
        appendLine("Waiting: ${b.waitingMinutes} mins (${b.waitingBlocks} x 15m)")
        appendLine("Tariff: $tariffLabel")
        appendLine("Total Fare: ${FareCalculator.formatRupees(b.totalFare)}")
      }
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, summary)
      }
    context.startActivity(Intent.createChooser(intent, "Share Auto Fare"))
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 840.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        AutoMeterTopBar(
          isNight = uiState.routeFareBreakdown.isNightApplied,
          currentTime = uiState.currentTimeFormatted,
        )
      },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            AppTab.entries.forEach { tab ->
              val selected = uiState.selectedTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = { TabIcon(tab = tab, selected = selected) },
                label = { Text(tabLabel(tab)) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            AppTab.entries.forEach { tab ->
              val selected = uiState.selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = { TabIcon(tab = tab, selected = selected) },
                label = { Text(tabLabel(tab)) },
                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }

        when (uiState.selectedTab) {
          AppTab.ROUTE_CALCULATOR ->
            RouteCalculatorScreen(
              uiState = uiState,
              onRequestLocation = requestOrRefreshGps,
              onFromQueryChanged = viewModel::onFromQueryChanged,
              onToQueryChanged = viewModel::onToQueryChanged,
              onSelectFromPlace = viewModel::selectFromPlace,
              onSelectToPlace = viewModel::selectToPlace,
              onSubmitDestination = viewModel::submitDestinationSearch,
              onSwapFromTo = viewModel::swapFromAndTo,
              onClearFrom = viewModel::clearFrom,
              onClearTo = viewModel::clearTo,
              onToggleMapPick = viewModel::toggleMapPickMode,
              onMapTapped = viewModel::onMapTapped,
              onManualDistanceChanged = viewModel::setManualDistanceKm,
              onWaitingMinutesChanged = viewModel::setWaitingMinutes,
              onAdjustWaitingMinutes = viewModel::adjustWaitingMinutes,
              onToggleWaitingStopwatch = viewModel::toggleWaitingStopwatch,
              onResetWaitingStopwatch = viewModel::resetWaitingStopwatch,
              onTariffModeSelected = viewModel::setTariffMode,
              onShareReceipt = shareFareSummary,
              onSaveTripLog = {
                viewModel.saveCurrentRouteTripToLog()
                scope.launch {
                  snackbarHostState.showSnackbar("Ride saved to today's log")
                }
              },
            )

          AppTab.LIVE_METER ->
            LiveMeterScreen(
              uiState = uiState,
              onStartOrResume = viewModel::startOrResumeLiveMeter,
              onPause = viewModel::pauseLiveMeter,
              onToggleWaiting = viewModel::toggleLiveWaiting,
              onFinishAndReset = { save ->
                viewModel.finishAndResetLiveMeter(save)
                if (save) {
                  scope.launch {
                    snackbarHostState.showSnackbar("Live trip saved to today's log")
                  }
                }
              },
              onTariffModeSelected = viewModel::setTariffMode,
              onRequestLocation = requestOrRefreshGps,
            )

          AppTab.RATE_CARD ->
            RateCardScreen(sessionTrips = uiState.sessionTrips)
        }
      }
    }
  }
}

@Composable
private fun AutoMeterTopBar(
  isNight: Boolean,
  currentTime: String,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.size(38.dp),
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
          )
        }
        Column {
          Text(
            text = "AutoMeter",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "₹30/2km • ₹15/km • ₹10/15m wait",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Surface(
        color =
          if (isNight) MaterialTheme.colorScheme.tertiaryContainer
          else MaterialTheme.colorScheme.secondaryContainer,
        contentColor =
          if (isNight) MaterialTheme.colorScheme.onTertiaryContainer
          else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(
          text =
            if (isNight) "${currentTime.ifBlank { "Night" }} • +50%"
            else "${currentTime.ifBlank { "Day" }} • Std",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun TabIcon(tab: AppTab, selected: Boolean) {
  val icon =
    when (tab) {
      AppTab.ROUTE_CALCULATOR -> if (selected) Icons.Filled.Route else Icons.Outlined.Route
      AppTab.LIVE_METER -> if (selected) Icons.Filled.Speed else Icons.Outlined.Speed
      AppTab.RATE_CARD -> if (selected) Icons.Filled.MenuBook else Icons.Outlined.MenuBook
    }
  Icon(imageVector = icon, contentDescription = tabLabel(tab))
}

private fun tabLabel(tab: AppTab): String =
  when (tab) {
    AppTab.ROUTE_CALCULATOR -> "Route Fare"
    AppTab.LIVE_METER -> "Live Meter"
    AppTab.RATE_CARD -> "Rate Card"
  }
