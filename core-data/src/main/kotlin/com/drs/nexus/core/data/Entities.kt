package com.drs.nexus.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One execution of any tool — persisted real usage history. */
@Entity(tableName = "tool_runs")
data class ToolRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: String,
    val inputPreview: String,
    val outputPreview: String,
    val executedAt: Long = System.currentTimeMillis()
)

/** User-pinned tools. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val toolId: String,
    val pinnedAt: Long = System.currentTimeMillis()
)
