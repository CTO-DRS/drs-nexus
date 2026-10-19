package com.drs.nexus.core.tools

import kotlin.random.Random

/**
 * Random number/choice generator with explicit seeding support so results are
 * fully testable. Modes: integers in a range (optionally unique), dice, coin.
 */
class RandomGeneratorTool(private val random: Random = Random.Default) {

    enum class Mode { INTEGERS, DICE, COIN }

    data class Request(
        val mode: Mode = Mode.INTEGERS,
        val min: Long = 1,
        val max: Long = 100,
        val count: Int = 1,
        val unique: Boolean = false
    )

    sealed class Result {
        data class Success(val values: List<String>, val mode: Mode) : Result()
        data class Failure(val message: String) : Result()
    }

    fun generate(req: Request): Result {
        if (req.count < 1) return Result.Failure("Count must be at least 1")
        if (req.count > 100) return Result.Failure("Count cannot exceed 100")
        return when (req.mode) {
            Mode.INTEGERS -> generateIntegers(req)
            Mode.DICE -> {
                val values = List(req.count) { (random.nextLong(1, 7)).toString() }
                Result.Success(values, Mode.DICE)
            }
            Mode.COIN -> {
                val values = List(req.count) { if (random.nextBoolean()) "Heads" else "Tails" }
                Result.Success(values, Mode.COIN)
            }
        }
    }

    private fun generateIntegers(req: Request): Result {
        if (req.min > req.max) return Result.Failure("Min cannot be greater than max")
        val span = req.max - req.min
        // +1 because the range is inclusive on both ends; guard overflow for huge spans
        val available = if (span > Long.MAX_VALUE - 1) Long.MAX_VALUE else span + 1
        if (req.unique && req.count.toLong() > available) {
            return Result.Failure("Not enough unique values in range")
        }
        return if (req.unique) {
            val set = LinkedHashSet<Long>()
            while (set.size < req.count) {
                set.add(randomLong(req.min, req.max))
            }
            Result.Success(set.map { it.toString() }, Mode.INTEGERS)
        } else {
            Result.Success(List(req.count) { randomLong(req.min, req.max).toString() }, Mode.INTEGERS)
        }
    }

    private fun randomLong(min: Long, max: Long): Long {
        val span = max - min
        if (span < Int.MAX_VALUE) return random.nextLong(min, max + 1)
        // For spans beyond Int range, draw in two steps to stay unbiased enough for a utility tool
        val mid = min + span / 2
        return if (random.nextBoolean()) randomLongSafe(min, mid) else randomLongSafe(mid + 1, max)
    }

    private fun randomLongSafe(min: Long, max: Long): Long =
        if (min > max) min else random.nextLong(min, max + 1)

    fun formatResult(r: Result.Success): String = r.values.joinToString(", ")
}
