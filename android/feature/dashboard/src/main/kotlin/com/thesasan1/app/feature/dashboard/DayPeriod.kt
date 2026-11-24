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
        /** 05:00–11:59 morning, 12:00–16:59 afternoon, 17:00–21:59 evening, otherwise night. */
        fun from(time: LocalTime): DayPeriod = when (time.hour) {
            in 5..11 -> Morning
            in 12..16 -> Afternoon
            in 17..21 -> Evening
            else -> Night
        }
    }
}
