package com.thesasan1.app.feature.dashboard

import java.time.LocalTime

/** Part of the day used to choose the dashboard greeting (DASH-01). */
enum class DayPeriod {
    Morning,
    Afternoon,
    Evening,
    Night,
    ;

    companion object {
        private const val MORNING_START_HOUR = 5
        private const val AFTERNOON_START_HOUR = 12
        private const val EVENING_START_HOUR = 17
        private const val NIGHT_START_HOUR = 22

        /** 05:00–11:59 morning, 12:00–16:59 afternoon, 17:00–21:59 evening, otherwise night. */
        fun from(time: LocalTime): DayPeriod = when (time.hour) {
            in MORNING_START_HOUR until AFTERNOON_START_HOUR -> Morning
            in AFTERNOON_START_HOUR until EVENING_START_HOUR -> Afternoon
            in EVENING_START_HOUR until NIGHT_START_HOUR -> Evening
            else -> Night
        }
    }
}
