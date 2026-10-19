package com.drs.nexus.core.tools

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Precise age calculator working purely on [LocalDate] inputs.
 * Computes calendar-exact years/months/days (the human way), plus totals,
 * next-birthday countdown and the weekday of birth. Leap-year aware.
 */
class AgeCalculatorTool {

    data class AgeResult(
        val years: Long,
        val months: Long,
        val days: Long,
        val totalMonths: Long,
        val totalDays: Long,
        val totalWeeks: Long,
        val nextBirthdayInDays: Long,
        val nextBirthdayDate: LocalDate,
        val isBirthdayToday: Boolean,
        val birthDayOfWeek: DayOfWeek
    )

    sealed class Result {
        data class Success(val age: AgeResult) : Result()
        data class Failure(val message: String) : Result()
    }

    fun compute(birthDate: LocalDate, today: LocalDate): Result {
        if (birthDate.isAfter(today)) {
            return Result.Failure("Birth date is in the future")
        }
        // Calendar-exact decomposition anchored on the last celebrated birthday.
        var years = ChronoUnit.YEARS.between(birthDate, today)
        // Clamped-anniversary rule: e.g. born Feb 29 2000, on Feb 28 of a common
        // year the 26th anniversary IS today even though YEARS.between says 25
        // (it subtracts the missing day-of-month). Bump years when the next
        // anniversary date (which LocalDate clamps) lands exactly on today.
        val nextAnniversary = birthDate.plusYears(years + 1)
        if (nextAnniversary == today) {
            years += 1
        }
        val lastBirthday = birthDate.plusYears(years)
        var months = ChronoUnit.MONTHS.between(lastBirthday, today)
        var dayAnchor = lastBirthday.plusMonths(months)
        if (dayAnchor.isAfter(today)) {
            months -= 1
            dayAnchor = lastBirthday.plusMonths(months)
        }
        val days = ChronoUnit.DAYS.between(dayAnchor, today)

        val totalMonths = ChronoUnit.MONTHS.between(birthDate, today)
        val totalDays = ChronoUnit.DAYS.between(birthDate, today)
        val totalWeeks = totalDays / 7

        val isBirthdayToday = lastBirthday == today
        // Next birthday: the following anniversary; zero when celebrating today.
        val nextBirthday = birthDate.plusYears(years + 1)
        val nextBirthdayInDays = if (isBirthdayToday) 0L else ChronoUnit.DAYS.between(today, nextBirthday)

        return Result.Success(
            AgeResult(
                years = years,
                months = months,
                days = days,
                totalMonths = totalMonths,
                totalDays = totalDays,
                totalWeeks = totalWeeks,
                nextBirthdayInDays = nextBirthdayInDays,
                nextBirthdayDate = if (isBirthdayToday) today else nextBirthday,
                isBirthdayToday = isBirthdayToday,
                birthDayOfWeek = birthDate.dayOfWeek
            )
        )
    }

    fun formatResult(r: AgeResult): String = "${r.years}y ${r.months}m ${r.days}d"
}
