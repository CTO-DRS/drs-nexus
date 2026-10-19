package com.drs.nexus.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomGeneratorToolTest {

    private fun gen(seed: Long = 42) = RandomGeneratorTool(Random(seed))

    @Test
    fun `integers respect range and count`() {
        val r = gen().generate(RandomGeneratorTool.Request(min = 5, max = 10, count = 7))
            as RandomGeneratorTool.Result.Success
        assertEquals(7, r.values.size)
        assertTrue(r.values.all { it.toLong() in 5..10 })
    }

    @Test
    fun `unique mode returns distinct in-range values`() {
        val r = gen().generate(
            RandomGeneratorTool.Request(min = 1, max = 50, count = 10, unique = true)
        ) as RandomGeneratorTool.Result.Success
        assertEquals(10, r.values.distinct().size)
        assertTrue(r.values.all { it.toLong() in 1..50 })
    }

    @Test
    fun `unique impossible request fails`() {
        val r = gen().generate(RandomGeneratorTool.Request(min = 1, max = 5, count = 6, unique = true))
        assertTrue(r is RandomGeneratorTool.Result.Failure)
        assertTrue((r as RandomGeneratorTool.Result.Failure).message.contains("unique"))
    }

    @Test
    fun `inverted range fails`() {
        assertTrue(gen().generate(RandomGeneratorTool.Request(min = 9, max = 2)) is RandomGeneratorTool.Result.Failure)
    }

    @Test
    fun `count bounds enforced`() {
        assertTrue(gen().generate(RandomGeneratorTool.Request(count = 0)) is RandomGeneratorTool.Result.Failure)
        assertTrue(gen().generate(RandomGeneratorTool.Request(count = 101)) is RandomGeneratorTool.Result.Failure)
    }

    @Test
    fun `dice values are one to six`() {
        val r = gen().generate(RandomGeneratorTool.Request(mode = RandomGeneratorTool.Mode.DICE, count = 12))
            as RandomGeneratorTool.Result.Success
        assertEquals(12, r.values.size)
        assertTrue(r.values.all { it.toLong() in 1..6 })
    }

    @Test
    fun `coin returns heads or tails`() {
        val r = gen().generate(RandomGeneratorTool.Request(mode = RandomGeneratorTool.Mode.COIN, count = 1))
            as RandomGeneratorTool.Result.Success
        assertTrue(r.values.single() == "Heads" || r.values.single() == "Tails")
    }

    @Test
    fun `same seed reproduces same sequence`() {
        val req = RandomGeneratorTool.Request(min = 0, max = 1_000_000, count = 5)
        val a = gen(7).generate(req) as RandomGeneratorTool.Result.Success
        val b = gen(7).generate(req) as RandomGeneratorTool.Result.Success
        assertEquals(a.values, b.values)
    }

    @Test
    fun `format result joins with commas`() {
        val r = RandomGeneratorTool.Result.Success(listOf("1", "2", "3"), RandomGeneratorTool.Mode.INTEGERS)
        assertEquals("1, 2, 3", gen().formatResult(r))
    }
}
