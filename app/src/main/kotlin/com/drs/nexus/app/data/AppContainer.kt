package com.drs.nexus.app.data

import android.content.Context
import com.drs.nexus.core.data.NexusDatabase
import com.drs.nexus.core.data.SettingsRepository
import com.drs.nexus.core.data.ToolRepository

/** Hand-rolled DI container: single source of real dependencies. */
class AppContainer(context: Context) {
    private val db = NexusDatabase.get(context)

    val settingsRepository = SettingsRepository(context)
    val toolRepository = ToolRepository(db.toolRunDao(), db.favoriteDao())
}
