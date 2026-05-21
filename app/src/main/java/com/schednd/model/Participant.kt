package com.schednd.model

import com.google.firebase.Timestamp

data class Participant(
    val userId: String = "",
    val name: String = "",
    val availableDates: List<Timestamp> = emptyList(),
    val notes: List<String> = emptyList()
)
