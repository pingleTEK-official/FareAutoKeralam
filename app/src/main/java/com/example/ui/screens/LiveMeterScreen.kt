package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FareCalculator
import com.example.domain.TariffMode
import com.example.ui.AutoMeterUiState

@Composable
fun LiveMeterScreen(
  uiState: AutoMeterUiState,
  onStartOrResume: () -> Unit,
  onPause: () -> Unit,
  onToggleWaiting: () -> Unit,
  onFinishAndReset: (Boolean) -> Unit,
  onTariffModeSelected: (TariffMode) -> Unit,
  onRequestLocation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val breakdown = uiState.liveFareBreakdown
  val isNight = breakdown.isNightApplied

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Digital Cockpit Meter Display
    ElevatedCard(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .border(
            width = 1.5.dp,
            color =
              if (uiState.isLiveMeterRunning && !uiState.isLiveMeterPaused) {
                MaterialTheme.colorScheme.secondary
              } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
              },
            shape = RoundedCornerShape(24.dp),
          ),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                listOf(
                  if (isNight) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                  else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                  MaterialTheme.colorScheme.surface,
                )
              )
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(12.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      uiState.isLiveMeterRunning && !uiState.isLiveMeterPaused ->
                        MaterialTheme.colorScheme.secondary
                      uiState.isLiveMeterPaused -> MaterialTheme.colorScheme.primary
                      else -> MaterialTheme.colorScheme.outline
                    }
                  )
            )
            Text(
              text =
                when {
                  uiState.isLiveMeterRunning && !uiState.isLiveMeterPaused -> "FOR HIRE • METER ON"
                  uiState.isLiveMeterPaused -> "METER PAUSED"
                  else -> "STANDBY • READY"
                },
              style = MaterialTheme.typography.labelLarge,
            )
          }

          Surface(
            color =
              if (isNight) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.secondary,
            contentColor =
              if (isNight) MaterialTheme.colorScheme.onTertiary
              else MaterialTheme.colorScheme.onSecondary,
            shape = RoundedCornerShape(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = if (isNight) "NIGHT +50%" else "DAY RATE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }

        // Giant Fare Display
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "TOTAL FARE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = FareCalculator.formatRupees(breakdown.totalFare),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("live_meter_total_fare"),
          )
          Text(
            text =
              if (isNight) {
                "Min ₹45 (2 km) • ₹22.50/km • Wait ₹15/15m"
              } else {
                "Min ₹30 (2 km) • ₹15/km • Wait ₹10/15m"
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // 3 Telemetry Boxes: Distance, Trip Time, Waiting Time
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MeterMetricTile(
            label = "GPS DISTANCE",
            value = FareCalculator.formatKm(uiState.liveDistanceKm),
            modifier = Modifier.weight(1f),
          )
          MeterMetricTile(
            label = "TRIP TIME",
            value = formatSeconds(uiState.liveElapsedSeconds),
            modifier = Modifier.weight(1f),
          )
          MeterMetricTile(
            label = if (uiState.isLiveWaitingOn) "WAIT (ON)" else "WAITING",
            value = formatSeconds(uiState.liveWaitingSeconds),
            highlight = uiState.isLiveWaitingOn,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Tariff Toggle Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TariffMode.entries.forEach { mode ->
        FilterChip(
          selected = uiState.tariffMode == mode,
          onClick = { onTariffModeSelected(mode) },
          label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    if (!uiState.hasLocationPermission) {
      ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Enable GPS permission to track live km automatically.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = onRequestLocation) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enable GPS")
          }
        }
      }
    }

    // Driver Control Buttons (Large 56dp+ touch targets)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!uiState.isLiveMeterRunning || uiState.isLiveMeterPaused) {
        Button(
          onClick = onStartOrResume,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(vertical = 16.dp),
          modifier = Modifier.weight(1f).testTag("live_start_button"),
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (uiState.isLiveMeterPaused) "Resume Trip" else "Start Meter",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      } else {
        Button(
          onClick = onPause,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(vertical = 16.dp),
          modifier = Modifier.weight(1f).testTag("live_pause_button"),
        ) {
          Icon(Icons.Default.Pause, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Pause Meter", style = MaterialTheme.typography.titleMedium)
        }
      }

      Button(
        onClick = onToggleWaiting,
        colors =
          ButtonDefaults.buttonColors(
            containerColor =
              if (uiState.isLiveWaitingOn) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
              if (uiState.isLiveWaitingOn) MaterialTheme.colorScheme.onTertiary
              else MaterialTheme.colorScheme.onSurface,
          ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.weight(1f).testTag("live_waiting_toggle_button"),
      ) {
        Icon(Icons.Default.Schedule, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (uiState.isLiveWaitingOn) "Waiting: ON" else "Wait Charge",
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }

    if (uiState.isLiveMeterRunning || uiState.liveDistanceKm > 0.0 || uiState.liveElapsedSeconds > 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = { onFinishAndReset(true) },
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(vertical = 14.dp),
          modifier = Modifier.weight(1f).testTag("live_finish_button"),
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Finish & Log Trip")
        }

        OutlinedButton(
          onClick = { onFinishAndReset(false) },
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(vertical = 14.dp),
          modifier = Modifier.weight(1f).testTag("live_reset_button"),
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reset")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun MeterMetricTile(
  label: String,
  value: String,
  highlight: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Surface(
    color =
      if (highlight) MaterialTheme.colorScheme.tertiaryContainer
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    }
  }
}

private fun formatSeconds(totalSeconds: Int): String {
  val mins = totalSeconds / 60
  val secs = totalSeconds % 60
  return "%02d:%02d".format(mins, secs)
}
