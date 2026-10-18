package com.drs.nexus.core.tools

/**
 * Fully OFFLINE currency converter: a built-in static reference table (base USD)
 * with an explicit rates date. No network calls — the app stays 100% on-device.
 * Rates are indicative reference values, clearly labeled as static in the UI.
 */
class CurrencyConverterTool {

    data class Currency(val code: String, val nameEn: String, val ratePerUsd: Double)

    data class Conversion(val amount: Double, val from: String, val to: String, val result: Double, val unitRate: Double)

    sealed class Result {
        data class Success(val conversion: Conversion) : Result()
        data class Failure(val message: String) : Result()
    }

    companion object {
        /** Reference date of the embedded static table (UTC). */
        const val RATES_DATE = "2026-10-01"

        // ratePerUsd = how many units of the currency 1 USD buys
        val CURRENCIES = listOf(
            Currency("USD", "US Dollar", 1.0),
            Currency("SAR", "Saudi Riyal", 3.75),
            Currency("AED", "UAE Dirham", 3.6725),
            Currency("KWD", "Kuwaiti Dinar", 0.3065),
            Currency("QAR", "Qatari Riyal", 3.64),
            Currency("EGP", "Egyptian Pound", 48.35),
            Currency("JOD", "Jordanian Dinar", 0.709),
            Currency("MAD", "Moroccan Dirham", 9.92),
            Currency("EUR", "Euro", 0.86),
            Currency("GBP", "British Pound", 0.745),
            Currency("CHF", "Swiss Franc", 0.795),
            Currency("TRY", "Turkish Lira", 42.1),
            Currency("JPY", "Japanese Yen", 151.8),
            Currency("CNY", "Chinese Yuan", 7.12),
            Currency("INR", "Indian Rupee", 88.9),
            Currency("PKR", "Pakistani Rupee", 281.5),
            Currency("IDR", "Indonesian Rupiah", 16480.0),
            Currency("MYR", "Malaysian Ringgit", 4.21),
            Currency("CAD", "Canadian Dollar", 1.395),
            Currency("AUD", "Australian Dollar", 1.52),
            Currency("RUB", "Russian Ruble", 81.5),
            Currency("BRL", "Brazilian Real", 5.42)
        )

        fun byCode(code: String): Currency? = CURRENCIES.firstOrNull { it.code == code.uppercase().trim() }
    }

    fun convert(amount: Double, from: String, to: String): Result {
        if (amount.isNaN() || amount.isInfinite()) return Result.Failure("Amount is not a finite number")
        if (amount < 0) return Result.Failure("Amount cannot be negative")
        val src = byCode(from) ?: return Result.Failure("Unknown currency code: $from")
        val dst = byCode(to) ?: return Result.Failure("Unknown currency code: $to")
        // normalize through USD, then round to sensible decimals
        val inUsd = amount / src.ratePerUsd
        val out = inUsd * dst.ratePerUsd
        val rounded = if (dst.ratePerUsd > 100) (out * 100).toLong() / 100.0 else (out * 1e6).toLong() / 1e6
        val unitRate = (dst.ratePerUsd / src.ratePerUsd * 1e6).toLong() / 1e6
        return Result.Success(Conversion(amount, src.code, dst.code, rounded, unitRate))
    }

    fun formatResult(c: Conversion): String {
        val value = if (c.result == c.result.toLong().toDouble()) c.result.toLong().toString() else c.result.toString()
        return "$value ${c.to}"
    }
}
