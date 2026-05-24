package com.schednd.model

enum class DayTimeSlot {
    MORNING,
    AFTERNOON,
    BOTH;

    val hasMorning: Boolean get() = this == MORNING || this == BOTH
    val hasAfternoon: Boolean get() = this == AFTERNOON || this == BOTH

    companion object {
        fun fromNameOrBoth(name: String?): DayTimeSlot =
            entries.firstOrNull { it.name == name } ?: BOTH
    }
}
