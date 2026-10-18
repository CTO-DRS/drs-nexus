package com.drs.nexus.core.tools

/**
 * Single source of truth for the tool catalog shared across UI and data layers.
 * `id` is stable and used for Room history/favorites persistence.
 */
object ToolRegistry {

    enum class Category(val id: String) { DEVELOPER("developer"), CONVERTERS("converters"), GENERATORS("generators"), TEXT("text") }

    data class ToolDef(
        val id: String,
        val nameKey: String,
        val descKey: String,
        val iconKey: String,
        val category: Category
    )

    val all: List<ToolDef> = listOf(
        ToolDef("json_formatter", "tool_json_name", "tool_json_desc", "code", Category.DEVELOPER),
        ToolDef("base64", "tool_base64_name", "tool_base64_desc", "swap", Category.DEVELOPER),
        ToolDef("hash", "tool_hash_name", "tool_hash_desc", "fingerprint", Category.DEVELOPER),
        ToolDef("regex_tester", "tool_regex_name", "tool_regex_desc", "search", Category.DEVELOPER),
        ToolDef("url_codec", "tool_url_name", "tool_url_desc", "link", Category.DEVELOPER),
        ToolDef("timestamp", "tool_ts_name", "tool_ts_desc", "schedule", Category.CONVERTERS),
        ToolDef("unit_converter", "tool_unit_name", "tool_unit_desc", "straighten", Category.CONVERTERS),
        ToolDef("uuid", "tool_uuid_name", "tool_uuid_desc", "tag", Category.GENERATORS),
        ToolDef("password", "tool_pass_name", "tool_pass_desc", "lock", Category.GENERATORS),
        ToolDef("lorem", "tool_lorem_name", "tool_lorem_desc", "notes", Category.GENERATORS),
        ToolDef("text_utils", "tool_text_name", "tool_text_desc", "text_fields", Category.TEXT)
    )

    fun byId(id: String): ToolDef? = all.firstOrNull { it.id == id }
}
