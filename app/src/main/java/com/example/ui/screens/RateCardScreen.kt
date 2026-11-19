package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompletedTrip
import com.example.domain.FareCalculator
import com.example.domain.TariffMode

@Composable
fun RateCardScreen(
  sessionTrips: List<CompletedTrip>,
  modifier: Modifier = Modifier,
) {
  val sampleDistances = listOf(2, 3, 4, 5, 8, 10, 15, 20)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Banner Card using generated artwork
    item {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(165.dp)
            .clip(RoundedCornerShape(22.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_auto_banner),
          contentDescription = "AutoMeter official tariff card artwork",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Transparent, Color(0xCC101419))
                )
              )
        )
        Column(
          modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Official Auto Fare Tariff",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Text(
            text = "Min ₹30 (2 km)  •  ₹15/km Extra  •  ₹10/15m Wait  •  +50% Night",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE5E7EB),
          )
        }
      }
    }

    // Morning vs Night Comparison Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TariffRuleCard(
          title = "Morning / Day",
          hours = "06:00 AM – 06:59 PM",
          minFare = "₹30 (up to 2 km)",
          extraKm = "₹15 / km",
          waiting = "₹10 / 15 mins",
          isNight = false,
          modifier = Modifier.weight(1f),
        )
        TariffRuleCard(
          title = "Night (+50%)",
          hours = "07:00 PM – 05:59 AM",
          minFare = "₹45 (up to 2 km)",
          extraKm = "₹22.50 / km",
          waiting = "₹15 / 15 mins",
          isNight = true,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Quick Fare Reference Chart
    item {
      ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Quick Fare Chart (No Waiting)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Distance", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("Day Rate", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("Night (+50%)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          sampleDistances.forEach { km ->
            val dayFare = FareCalculator.calculate(km.toDouble(), 0, TariffMode.MORNING_DAY, 10)
            val nightFare = FareCalculator.calculate(km.toDouble(), 0, TariffMode.NIGHT_SURCHARGE, 21)
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (km == 2) "Up to 2 km" else "$km km",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = FareCalculator.formatRupees(dayFare.totalFare),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = FareCalculator.formatRupees(nightFare.totalFare),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    // Session Trip Log
    item {
      Text(
        text = "Today's Logged Rides (${sessionTrips.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (sessionTrips.isEmpty()) {
      item {
        ElevatedCard(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.ReceiptLong,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "No rides logged in this session yet",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Tap 'Log Ride' on any calculated route or finish a Live Meter trip to see it here.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(sessionTrips, key = { it.id }) { trip ->
        ElevatedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text(
                  text = "${trip.fromLabel} → ${trip.toLabel}",
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text =
                    "${trip.timestampLabel} • ${FareCalculator.formatKm(trip.breakdown.distanceKm)} • ${trip.breakdown.waitingMinutes}m wait" +
                      if (trip.breakdown.isNightApplied) " • Night +50%" else "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = FareCalculator.formatRupees(trip.breakdown.totalFare),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun TariffRuleCard(
  title: String,
  hours: String,
  minFare: String,
  extraKm: String,
  waiting: String,
  isNight: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    color =
      if (isNight) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
      else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    shape = RoundedCornerShape(18.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
          contentDescription = null,
          tint =
            if (isNight) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
      Text(
        text = hours,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      Text(text = "Min: $minFare", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
      Text(text = "Extra: $extraKm", style = MaterialTheme.typography.bodyMedium)
      Text(text = "Wait: $waiting", style = MaterialTheme.typography.bodyMedium)
    }
  }
}
