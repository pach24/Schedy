package com.schednd.domain.model

import java.time.LocalDate

data class AvailabilitySlot(
    val date: LocalDate,
    val slot: DayTimeSlot
)

data class SlotCounts(
    val morning: Int = 0,
    val afternoon: Int = 0
) {
    val total: Int get() = maxOf(morning, afternoon)
}
