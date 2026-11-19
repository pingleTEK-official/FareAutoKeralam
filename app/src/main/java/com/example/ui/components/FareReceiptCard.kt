package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.domain.FareBreakdown
import com.example.domain.FareCalculator

@Composable
fun FareReceiptCard(
  breakdown: FareBreakdown,
  fromLabel: String,
  toLabel: String,
  hasRouteDistance: Boolean,
  onShareReceipt: () -> Unit,
  onSaveTripLog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isNight = breakdown.isNightApplied
  val heroGradient =
    if (isNight) {
      Brush.linearGradient(
        listOf(
          MaterialTheme.colorScheme.tertiaryContainer,
          MaterialTheme.colorScheme.surface,
        )
      )
    } else {
      Brush.linearGradient(
        listOf(
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          MaterialTheme.colorScheme.surface,
        )
      )
    }

  ElevatedCard(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color =
            if (isNight) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
          shape = RoundedCornerShape(22.dp),
        ),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Digital Meter Header
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(heroGradient)
            .padding(horizontal = 20.dp, vertical = 18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
              color =
                if (isNight) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.secondary,
              contentColor =
                if (isNight) MaterialTheme.colorScheme.onTertiary
                else MaterialTheme.colorScheme.onSecondary,
              shape = RoundedCornerShape(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = if (isNight) "NIGHT RATE +50%" else "MORNING / DAY RATE",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
              }
            }

            Text(
              text =
                if (hasRouteDistance) {
                  "Estimated Auto Fare"
                } else {
                  "Minimum Flag-Down Fare"
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = FareCalculator.formatRupees(breakdown.totalFare),
              style = MaterialTheme.typography.displayLarge,
              color =
                if (isNight) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("total_fare_text"),
            )
            if (kotlin.math.abs(breakdown.totalFare - breakdown.roundedTotalFare) >= 0.01) {
              Text(
                text = "Rounded: ₹${breakdown.roundedTotalFare}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Itemized Breakdown
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (fromLabel.isNotBlank() && toLabel.isNotBlank()) {
          Text(
            text = "$fromLabel  →  $toLabel",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }

        FareRowItem(
          title = "Minimum Charge (up to 2.0 km)",
          subtitle = "Base day rate ₹30",
          amount = FareCalculator.formatRupees(breakdown.baseFareDay),
        )

        FareRowItem(
          title = "Extra Distance (${FareCalculator.formatKm(breakdown.extraDistanceKm)})",
          subtitle = "Above 2 km @ ₹15/km",
          amount = FareCalculator.formatRupees(breakdown.extraDistanceFareDay),
        )

        FareRowItem(
          title =
            if (breakdown.waitingMinutes > 0) {
              "Waiting (${breakdown.waitingMinutes} min • ${breakdown.waitingBlocks}×15m)"
            } else {
              "Waiting Charges (0 min)"
            },
          subtitle = "₹10 per 15 mins",
          amount = FareCalculator.formatRupees(breakdown.waitingFareDay),
        )

        AnimatedVisibility(visible = breakdown.isNightApplied) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            FareRowItem(
              title = "Night Fare Surcharge (+50%)",
              subtitle = "7:00 PM – 6:00 AM on distance fare",
              amount = "+${FareCalculator.formatRupees(breakdown.nightFareSurcharge)}",
              highlightColor = MaterialTheme.colorScheme.tertiary,
            )
            if (breakdown.nightWaitingSurcharge > 0.0) {
              FareRowItem(
                title = "Night Waiting Surcharge (+50%)",
                subtitle = "7:00 PM – 6:00 AM on waiting",
                amount = "+${FareCalculator.formatRupees(breakdown.nightWaitingSurcharge)}",
                highlightColor = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Total Payable (${FareCalculator.formatKm(breakdown.distanceKm)})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = FareCalculator.formatRupees(breakdown.totalFare),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color =
              if (isNight) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.secondary,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onSaveTripLog,
            enabled = hasRouteDistance,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("save_trip_button"),
          ) {
            Icon(
              imageVector = Icons.Default.BookmarkAdd,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Ride")
          }

          OutlinedButton(
            onClick = onShareReceipt,
            colors =
              ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
              ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("share_fare_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Fare")
          }
        }
      }
    }
  }
}

@Composable
private fun FareRowItem(
  title: String,
  subtitle: String,
  amount: String,
  highlightColor: androidx.compose.ui.graphics.Color? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = amount,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
    )
  }
}
