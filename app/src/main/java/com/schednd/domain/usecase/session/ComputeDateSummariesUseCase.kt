package com.schednd.domain.usecase.session

import com.schednd.domain.model.DateSummary
import com.schednd.domain.model.computeAttendanceTier
import com.schednd.domain.model.Participant
import java.time.LocalDate
import javax.inject.Inject

class ComputeDateSummariesUseCase @Inject constructor() {

    operator fun invoke(
        dates: List<LocalDate>,
        participants: List<Participant>,
        availability: Map<String, Set<LocalDate>>
    ): List<DateSummary> {
        if (participants.isEmpty()) return emptyList()
        val total = participants.size
        val today = LocalDate.now()

        return dates.filter { !it.isBefore(today) }.map { date ->
            val count = participants.count { p -> date in (availability[p.userId] ?: emptySet()) }
            val absentNames = participants
                .filter { p -> date !in (availability[p.userId] ?: emptySet()) }
                .map { it.name }
            val tier = computeAttendanceTier(count, total)
            DateSummary(date, count, total, absentNames, tier)
        }.sortedByDescending { it.count }
    }
}
