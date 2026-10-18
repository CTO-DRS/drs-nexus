package com.drs.nexus.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ToolRunEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun toolRunDao(): ToolRunDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var instance: NexusDatabase? = null

        fun get(context: Context): NexusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "drs_nexus.db"
                ).build().also { instance = it }
            }
    }
}
