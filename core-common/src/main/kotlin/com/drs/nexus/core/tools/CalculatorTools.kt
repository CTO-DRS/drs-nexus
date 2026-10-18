package com.drs.nexus.core.tools

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Four real calculators sharing one file because each engine is small but fully validated:
 * percentage, loan EMI, BMI (WHO classification) and date difference.
 */
object PercentageCalculator {

    sealed class Result {
        data class Success(val text: String, val value: Double) : Result()
        data class Failure(val message: String) : Result()
    }

    /** mode 0: X% of Y  |  mode 1: X is what % of Y  |  mode 2: % change from X to Y. */
    fun calculate(mode: Int, x: Double, y: Double): Result {
        return when (mode) {
            0 -> Result.Success("$x% of $y = ${fmt(x / 100.0 * y)}", x / 100.0 * y)
            1 -> {
                if (y == 0.0) return Result.Failure("Cannot divide by zero (Y = 0)")
                val v = x / y * 100.0
                Result.Success("$x is ${fmt(v)}% of $y", v)
            }
            2 -> {
                if (x == 0.0) return Result.Failure("Cannot compute change from zero (X = 0)")
                val v = (y - x) / x * 100.0
                val arrow = if (v >= 0) "+" else ""
                Result.Success("Change from $x to $y = $arrow${fmt(v)}%", v)
            }
            else -> Result.Failure("Unknown mode")
        }
    }

    fun fmt(v: Double): String {
        val rounded = (v * 1e6).roundToLong() / 1e6
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }
}

object LoanCalculator {

    data class Amortization(
        val monthlyPayment: Double,
        val totalPayment: Double,
        val totalInterest: Double,
        val months: Int
    )

    sealed class Result {
        data class Success(val plan: Amortization) : Result()
        data class Failure(val message: String) : Result()
    }

    fun calculate(amount: Double, annualRatePercent: Double, years: Double): Result {
        if (amount <= 0) return Result.Failure("Loan amount must be greater than 0")
        if (years <= 0) return Result.Failure("Term must be greater than 0")
        if (annualRatePercent < 0) return Result.Failure("Interest rate cannot be negative")
        val months = (years * 12).toInt()
        if (months == 0) return Result.Failure("Term too short")
        if (annualRatePercent == 0.0) {
            val monthly = amount / months
            return Result.Success(Amortization(monthly, amount, 0.0, months))
        }
        val r = annualRatePercent / 100.0 / 12.0
        val monthly = amount * r * (1 + r).pow(months) / ((1 + r).pow(months) - 1)
        val total = monthly * months
        return Result.Success(Amortization(monthly, total, total - amount, months))
    }

    fun fmt(v: Double): String = String.format("%.2f", v)
}

object BmiCalculator {

    enum class Category(val key: String) {
        UNDERWEIGHT("bmi_underweight"),
        NORMAL("bmi_normal"),
        OVERWEIGHT("bmi_overweight"),
        OBESE("bmi_obese")
    }

    data class Reading(val bmi: Double, val category: Category, val healthyWeightMinKg: Double, val healthyWeightMaxKg: Double)

    sealed class Result {
        data class Success(val reading: Reading) : Result()
        data class Failure(val message: String) : Result()
    }

    /** Metric input: kg + cm. */
    fun calculate(kg: Double, cm: Double): Result {
        if (kg <= 0) return Result.Failure("Weight must be greater than 0")
        if (cm <= 0) return Result.Failure("Height must be greater than 0")
        val m = cm / 100.0
        val bmi = kg / (m * m)
        return Result.Success(reading(bmi, m))
    }

    /** Imperial input: pounds + total inches. */
    fun imperial(pounds: Double, inches: Double): Result {
        if (pounds <= 0 || inches <= 0) return Result.Failure("Weight and height must be greater than 0")
        val kg = pounds * 0.45359237
        val cm = inches * 2.54
        return calculate(kg, cm)
    }

    private fun reading(bmi: Double, meters: Double): Reading {
        val category = when {
            bmi < 18.5 -> Category.UNDERWEIGHT
            bmi < 25.0 -> Category.NORMAL
            bmi < 30.0 -> Category.OVERWEIGHT
            else -> Category.OBESE
        }
        return Reading(
            bmi = (bmi * 100).roundToLong() / 100.0,
            category = category,
            healthyWeightMinKg = (18.5 * meters * meters * 10).roundToLong() / 10.0,
            healthyWeightMaxKg = (24.9 * meters * meters * 10).roundToLong() / 10.0
        )
    }
}

object DateDifferenceTool {

    data class Diff(val days: Long, val weeks: Long, val months: Long, val years: Long, val weekdays: Long)

    sealed class Result {
        data class Success(val diff: Diff) : Result()
        data class Failure(val message: String) : Result()
    }

    fun between(startIso: String, endIso: String): Result {
        val start = parse(startIso) ?: return Result.Failure("Start date must be YYYY-MM-DD")
        val end = parse(endIso) ?: return Result.Failure("End date must be YYYY-MM-DD")
        if (end.isBefore(start)) return Result.Failure("End date is before start date")
        return Result.Success(diff(start, end))
    }

    fun addDays(iso: String, days: Long): Result {
        val start = parse(iso) ?: return Result.Failure("Date must be YYYY-MM-DD")
        val target = start.plusDays(days)
        return Result.Success(
            if (days >= 0) diff(start, target) else diff(target, start)
        )
    }

    private fun diff(start: LocalDate, end: LocalDate): Diff {
        val days = ChronoUnit.DAYS.between(start, end)
        var weekdays = 0L
        var cursor = start
        while (cursor.isBefore(end)) {
            val dow = cursor.dayOfWeek.value
            if (dow <= 5) weekdays++
            cursor = cursor.plusDays(1)
        }
        return Diff(
            days = days,
            weeks = days / 7,
            months = ChronoUnit.MONTHS.between(start, end),
            years = ChronoUnit.YEARS.between(start, end),
            weekdays = weekdays
        )
    }

    fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso.trim()) }.getOrNull()
}
