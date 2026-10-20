package com.drs.nexus.core.util

/**
 * Pure usage-statistics engine — no Android dependencies.
 * Consumes lightweight run stamps (tool id + timestamp) so it stays testable
 * on the JVM while the app layer feeds it real Room rows.
 */
data class RunStamp(val toolId: String, val executedAt: Long)

data class ToolUsage(
    val toolId: String,
    val runs: Int,
    val lastUsedAt: Long
)

data class UsageSummary(
    val totalRuns: Int,
    val distinctTools: Int,
    val last7Days: Int,
    val previous7Days: Int,
    /** Most-used tools, strongest first, capped by the requested limit. */
    val top: List<ToolUsage>
) {
    /** last7Days - previous7Days; positive means the trend is growing. */
    val weeklyDelta: Int get() = last7Days - previous7Days
}

object UsageStats {

    const val DAY_MS: Long = 86_400_000L
    const val WEEK_MS: Long = 7 * DAY_MS

    /**
     * Builds a full summary from raw run stamps.
     *
     * Windows are anchored at [nowMs] and roll backwards: "last 7 days" is
     * (now - 7d, now] and "previous 7 days" is (now - 14d, now - 7d].
     *
     * Ties in the top list are broken deterministically: more runs first,
     * then more recent use, then lexicographic tool id — so the UI can never
     * flicker between equal candidates.
     */
    fun summarize(runs: List<RunStamp>, nowMs: Long, topN: Int = 5): UsageSummary {
        if (runs.isEmpty()) return UsageSummary(0, 0, 0, 0, emptyList())

        val last7Start = nowMs - WEEK_MS
        val prev7Start = nowMs - 2 * WEEK_MS

        var last7 = 0
        var prev7 = 0
        val perTool = LinkedHashMap<String, MutableUsage>()

        for (run in runs) {
            when {
                run.executedAt > last7Start -> last7++
                run.executedAt > prev7Start -> prev7++
            }
            val entry = perTool.getOrPut(run.toolId) { MutableUsage() }
            entry.count++
            if (run.executedAt > entry.lastUsedAt) entry.lastUsedAt = run.executedAt
        }

        val top = perTool.entries
            .map { ToolUsage(it.key, it.value.count, it.value.lastUsedAt) }
            .sortedWith(
                compareByDescending<ToolUsage> { it.runs }
                    .thenByDescending { it.lastUsedAt }
                    .thenBy { it.toolId }
            )
            .take(topN.coerceAtLeast(0))

        return UsageSummary(
            totalRuns = runs.size,
            distinctTools = perTool.size,
            last7Days = last7,
            previous7Days = prev7,
            top = top
        )
    }

    private class MutableUsage {
        var count: Int = 0
        var lastUsedAt: Long = Long.MIN_VALUE
    }
}
