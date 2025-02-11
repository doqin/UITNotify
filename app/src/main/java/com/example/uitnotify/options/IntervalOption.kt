package com.example.uitnotify.options

enum class IntervalOption(val minutes: Long, val label: String) {
    ONE_MINUTE(1, "1 minute"),
    FIVE_MINUTES(5, "5 minutes"),
    FIFTEEN_MINUTES(15, "15 minutes"),
    THIRTY_MINUTES(30, "30 minutes"),
    ONE_HOUR(60, "1 hour"),
    TWO_HOURS(120, "2 hours"),
    SIX_HOURS(360, "6 hours"),
    TWELVE_HOURS(720, "12 hours"),
    ONE_DAY(1440, "1 day")
}