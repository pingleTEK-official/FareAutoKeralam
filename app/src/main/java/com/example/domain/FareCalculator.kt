package com.example.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class TariffMode(val label: String) {
  AUTO_TIME("Auto (Time)"),
  MORNING_DAY("Morning / Day"),
  NIGHT_SURCHARGE("Night (+50%)"),
}

data class FareBreakdown(
  val distanceKm: Double,
  val extraDistanceKm: Double,
  val waitingMinutes: Int,
  val waitingBlocks: Int,
  val isNightApplied: Boolean,
  val baseFareDay: Double,
  val extraDistanceFareDay: Double,
  val waitingFareDay: Double,
  val daySubtotal: Double,
  val nightFareSurcharge: Double,
  val nightWaitingSurcharge: Double,
  val totalNightSurcharge: Double,
  val baseFareEffective: Double,
  val extraKmRateEffective: Double,
  val waitingBlockRateEffective: Double,
  val totalFare: Double,
  val roundedTotalFare: Int,
)

object FareCalculator {
  const val MIN_FARE_DAY = 30.0
  const val MIN_DISTANCE_KM = 2.0
  const val EXTRA_KM_RATE_DAY = 15.0
  const val WAITING_BLOCK_MINUTES = 15
  const val WAITING_CHARGE_PER_BLOCK_DAY = 10.0
  const val NIGHT_MULTIPLIER = 1.5
  const val NIGHT_START_HOUR = 19 // 7:00 PM
  const val NIGHT_END_HOUR = 6    // 6:00 AM

  /**
   * Night surcharge applies from 07:00 PM (19:00) to 06:00 AM (before 06:00).
   */
  fun isNightHour(hour24: Int): Boolean {
    val normalized = ((hour24 % 24) + 24) % 24
    return normalized >= NIGHT_START_HOUR || normalized < NIGHT_END_HOUR
  }

  fun calculate(
    distanceKm: Double,
    waitingMinutes: Int,
    tariffMode: TariffMode,
    currentHour24: Int,
  ): FareBreakdown {
    val safeDistance = max(0.0, distanceKm)
    val safeWaiting = max(0, waitingMinutes)

    val isNightApplied =
      when (tariffMode) {
        TariffMode.AUTO_TIME -> isNightHour(currentHour24)
        TariffMode.MORNING_DAY -> false
        TariffMode.NIGHT_SURCHARGE -> true
      }

    val extraDistanceKm = max(0.0, safeDistance - MIN_DISTANCE_KM)
    val baseFareDay = MIN_FARE_DAY
    val extraDistanceFareDay = extraDistanceKm * EXTRA_KM_RATE_DAY

    val waitingBlocks =
      if (safeWaiting <= 0) 0
      else ceil(safeWaiting.toDouble() / WAITING_BLOCK_MINUTES.toDouble()).toInt()
    val waitingFareDay = waitingBlocks * WAITING_CHARGE_PER_BLOCK_DAY

    val daySubtotal = baseFareDay + extraDistanceFareDay + waitingFareDay

    val nightFareSurcharge =
      if (isNightApplied) (baseFareDay + extraDistanceFareDay) * 0.5 else 0.0
    val nightWaitingSurcharge =
      if (isNightApplied) waitingFareDay * 0.5 else 0.0
    val totalNightSurcharge = nightFareSurcharge + nightWaitingSurcharge

    val totalFare = if (isNightApplied) daySubtotal * NIGHT_MULTIPLIER else daySubtotal

    return FareBreakdown(
      distanceKm = safeDistance,
      extraDistanceKm = extraDistanceKm,
      waitingMinutes = safeWaiting,
      waitingBlocks = waitingBlocks,
      isNightApplied = isNightApplied,
      baseFareDay = baseFareDay,
      extraDistanceFareDay = extraDistanceFareDay,
      waitingFareDay = waitingFareDay,
      daySubtotal = daySubtotal,
      nightFareSurcharge = nightFareSurcharge,
      nightWaitingSurcharge = nightWaitingSurcharge,
      totalNightSurcharge = totalNightSurcharge,
      baseFareEffective = if (isNightApplied) baseFareDay * NIGHT_MULTIPLIER else baseFareDay,
      extraKmRateEffective =
        if (isNightApplied) EXTRA_KM_RATE_DAY * NIGHT_MULTIPLIER else EXTRA_KM_RATE_DAY,
      waitingBlockRateEffective =
        if (isNightApplied) WAITING_CHARGE_PER_BLOCK_DAY * NIGHT_MULTIPLIER
        else WAITING_CHARGE_PER_BLOCK_DAY,
      totalFare = totalFare,
      roundedTotalFare = totalFare.roundToInt(),
    )
  }

  fun formatRupees(amount: Double): String {
    val rounded = amount.roundToInt()
    return if (kotlin.math.abs(amount - rounded.toDouble()) < 0.01) {
      "₹$rounded"
    } else {
      "₹%.2f".format(amount)
    }
  }

  fun formatKm(km: Double): String = "%.2f km".format(km)
}
