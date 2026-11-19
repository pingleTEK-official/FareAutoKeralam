package com.example

import com.example.domain.FareCalculator
import com.example.domain.TariffMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FareCalculatorTest {

  @Test
  fun dayMinimumFareUpTo2KmIs30() {
    val under2Km = FareCalculator.calculate(1.2, 0, TariffMode.AUTO_TIME, 10)
    assertFalse(under2Km.isNightApplied)
    assertEquals(30.0, under2Km.totalFare, 0.001)

    val exact2Km = FareCalculator.calculate(2.0, 0, TariffMode.AUTO_TIME, 14)
    assertEquals(30.0, exact2Km.totalFare, 0.001)
  }

  @Test
  fun dayExtraKmIs15PerKm() {
    val result = FareCalculator.calculate(5.0, 0, TariffMode.MORNING_DAY, 22)
    assertFalse(result.isNightApplied)
    assertEquals(3.0, result.extraDistanceKm, 0.001)
    assertEquals(45.0, result.extraDistanceFareDay, 0.001)
    assertEquals(75.0, result.totalFare, 0.001)
  }

  @Test
  fun dayWaitingCharges10Per15Mins() {
    val wait15 = FareCalculator.calculate(2.0, 15, TariffMode.MORNING_DAY, 11)
    assertEquals(1, wait15.waitingBlocks)
    assertEquals(10.0, wait15.waitingFareDay, 0.001)
    assertEquals(40.0, wait15.totalFare, 0.001)

    val wait20 = FareCalculator.calculate(4.0, 20, TariffMode.MORNING_DAY, 11)
    assertEquals(2, wait20.waitingBlocks)
    assertEquals(20.0, wait20.waitingFareDay, 0.001)
    // Base 30 + Extra 2*15 (30) + Wait 20 = 80
    assertEquals(80.0, wait20.totalFare, 0.001)
  }

  @Test
  fun nightBoundaryHours7PmTo6Am() {
    assertFalse(FareCalculator.isNightHour(6))  // 6:00 AM -> Day
    assertFalse(FareCalculator.isNightHour(18)) // 6:59 PM -> Day
    assertTrue(FareCalculator.isNightHour(19))  // 7:00 PM -> Night
    assertTrue(FareCalculator.isNightHour(23))  // 11:00 PM -> Night
    assertTrue(FareCalculator.isNightHour(0))   // 12:00 AM -> Night
    assertTrue(FareCalculator.isNightHour(5))   // 5:59 AM -> Night
  }

  @Test
  fun nightApplies50PercentToBothDistanceAndWaiting() {
    // 5 km, 15 mins wait at 9 PM (21:00):
    // Day base = 30, Day extra = 3 * 15 = 45, Day wait = 10 -> Day subtotal = 85
    // Night +50% on distance (75 * 0.5 = 37.5) + Night +50% on waiting (10 * 0.5 = 5.0) -> Total = 127.5
    val result = FareCalculator.calculate(5.0, 15, TariffMode.AUTO_TIME, 21)
    assertTrue(result.isNightApplied)
    assertEquals(37.5, result.nightFareSurcharge, 0.001)
    assertEquals(5.0, result.nightWaitingSurcharge, 0.001)
    assertEquals(127.5, result.totalFare, 0.001)
    assertEquals(45.0, result.baseFareEffective, 0.001)
    assertEquals(22.5, result.extraKmRateEffective, 0.001)
    assertEquals(15.0, result.waitingBlockRateEffective, 0.001)
  }
}
