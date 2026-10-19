package com.drs.nexus.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolRunDao {
    @Insert
    suspend fun insert(run: ToolRunEntity): Long

    @Query("SELECT * FROM tool_runs ORDER BY executedAt DESC LIMIT :limit")
    fun recentRuns(limit: Int = 100): Flow<List<ToolRunEntity>>

    @Query("SELECT * FROM tool_runs WHERE toolId = :toolId ORDER BY executedAt DESC LIMIT :limit")
    fun runsForTool(toolId: String, limit: Int = 50): Flow<List<ToolRunEntity>>

    @Query("DELETE FROM tool_runs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tool_runs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tool_runs")
    suspend fun count(): Int

    @Query("SELECT * FROM tool_runs ORDER BY executedAt DESC")
    suspend fun allRunsOnce(): List<ToolRunEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM tool_runs WHERE toolId = :toolId AND executedAt = :executedAt AND inputPreview = :inputPreview)")
    suspend fun existsRun(toolId: String, executedAt: Long, inputPreview: String): Boolean
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Delete
    suspend fun remove(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites ORDER BY pinnedAt DESC")
    fun all(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE toolId = :toolId)")
    fun isFavorite(toolId: String): Flow<Boolean>

    @Query("DELETE FROM favorites WHERE toolId = :toolId")
    suspend fun removeById(toolId: String)

    @Query("SELECT * FROM favorites ORDER BY pinnedAt DESC")
    suspend fun allOnce(): List<FavoriteEntity>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Query("UPDATE favorites SET pinnedAt = :pinnedAt WHERE toolId = :toolId")
    suspend fun updatePinnedAt(toolId: String, pinnedAt: Long)
}
