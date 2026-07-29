package com.schednd.domain.usecase.session

import com.schednd.domain.repository.SessionReminderScheduler
import javax.inject.Inject

class CancelSessionReminderUseCase @Inject constructor(
    private val reminderScheduler: SessionReminderScheduler
) {
    operator fun invoke(code: String) = reminderScheduler.cancel(code)
}
