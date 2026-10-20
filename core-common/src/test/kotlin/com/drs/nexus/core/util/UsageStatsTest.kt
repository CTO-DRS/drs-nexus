package com.drs.nexus.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsTest {

    private val day = UsageStats.DAY_MS
    private val now = 1_700_000_000_000L

    @Test
    fun `empty runs produce zeroed summary`() {
        val s = UsageStats.summarize(emptyList(), now)
        assertEquals(0, s.totalRuns)
        assertEquals(0, s.distinctTools)
        assertEquals(0, s.last7Days)
        assertEquals(0, s.previous7Days)
        assertEquals(0, s.weeklyDelta)
        assertEquals(true, s.top.isEmpty())
    }

    @Test
    fun `windows are anchored at now`() {
        val runs = listOf(
            RunStamp("a", now - day * 1),          // last week
            RunStamp("a", now - day * 6),          // last week
            RunStamp("b", now - day * 7),          // exactly 7d ago -> previous window
            RunStamp("b", now - day * 8),          // previous week
            RunStamp("c", now - day * 13),         // previous week
            RunStamp("c", now - day * 14)          // exactly 14d ago -> out of both
        )
        val s = UsageStats.summarize(runs, now)
        assertEquals(2, s.last7Days)
        assertEquals(3, s.previous7Days)
        assertEquals(-1, s.weeklyDelta)
        assertEquals(6, s.totalRuns)
        assertEquals(3, s.distinctTools)
    }

    @Test
    fun `top list sorts by runs then recency then id`() {
        val base = now - day
        val runs = listOf(
            RunStamp("b", base),          // 3 runs
            RunStamp("b", base - 1),
            RunStamp("b", base - 2),
            RunStamp("a", base),          // 3 runs, same recency -> id tie-break (a before b)
            RunStamp("a", base - 1),
            RunStamp("a", base - 2),
            RunStamp("c", base),          // 2 runs, most recent of the 2-run group
            RunStamp("c", base - 1),
            RunStamp("d", base - day * 3) // 1 run, older
        )
        val s = UsageStats.summarize(runs, now, topN = 10)
        assertEquals(listOf("a", "b", "c", "d"), s.top.map { it.toolId })
        assertEquals(3, s.top[0].runs)
        assertEquals(base, s.top[0].lastUsedAt)
    }

    @Test
    fun `topN caps the list`() {
        val runs = (1..8).map { RunStamp("tool$it", now - day) }
        val s = UsageStats.summarize(runs, now, topN = 5)
        assertEquals(5, s.top.size)
        assertEquals(8, s.distinctTools)
    }

    @Test
    fun `lastUsedAt is the maximum timestamp per tool`() {
        val runs = listOf(
            RunStamp("x", now - day * 5),
            RunStamp("x", now - day),
            RunStamp("x", now - day * 3)
        )
        val s = UsageStats.summarize(runs, now)
        assertEquals(now - day, s.top.single().lastUsedAt)
    }

    @Test
    fun `topN zero yields empty top but keeps counters`() {
        val runs = listOf(RunStamp("a", now - day))
        val s = UsageStats.summarize(runs, now, topN = 0)
        assertEquals(true, s.top.isEmpty())
        assertEquals(1, s.totalRuns)
    }
}
