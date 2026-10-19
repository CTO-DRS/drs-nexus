package com.drs.nexus.core.tools

/**
 * Single source of truth for the tool catalog shared across UI and data layers.
 * `id` is stable and used for Room history/favorites persistence.
 */
object ToolRegistry {

    enum class Category(val id: String) { DEVELOPER("developer"), CONVERTERS("converters"), GENERATORS("generators"), CALCULATORS("calculators"), TEXT("text") }

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
        ToolDef("url_parser", "tool_urlparser_name", "tool_urlparser_desc", "travel_explore", Category.DEVELOPER),
        ToolDef("jwt_decoder", "tool_jwt_name", "tool_jwt_desc", "vpn_key", Category.DEVELOPER),
        ToolDef("timestamp", "tool_ts_name", "tool_ts_desc", "schedule", Category.CONVERTERS),
        ToolDef("unit_converter", "tool_unit_name", "tool_unit_desc", "straighten", Category.CONVERTERS),
        ToolDef("base_converter", "tool_base_name", "tool_base_desc", "calculate", Category.CONVERTERS),
        ToolDef("color_converter", "tool_color_name", "tool_color_desc", "palette", Category.CONVERTERS),
        ToolDef("currency", "tool_currency_name", "tool_currency_desc", "payments", Category.CONVERTERS),
        ToolDef("uuid", "tool_uuid_name", "tool_uuid_desc", "tag", Category.GENERATORS),
        ToolDef("password", "tool_pass_name", "tool_pass_desc", "lock", Category.GENERATORS),
        ToolDef("random_gen", "tool_random_name", "tool_random_desc", "casino", Category.GENERATORS),
        ToolDef("lorem", "tool_lorem_name", "tool_lorem_desc", "notes", Category.GENERATORS),
        ToolDef("qr_generator", "tool_qr_name", "tool_qr_desc", "qr_code", Category.GENERATORS),
        ToolDef("percentage", "tool_percent_name", "tool_percent_desc", "percent", Category.CALCULATORS),
        ToolDef("loan", "tool_loan_name", "tool_loan_desc", "account_balance", Category.CALCULATORS),
        ToolDef("bmi", "tool_bmi_name", "tool_bmi_desc", "monitor_weight", Category.CALCULATORS),
        ToolDef("date_diff", "tool_datediff_name", "tool_datediff_desc", "event", Category.CALCULATORS),
        ToolDef("age_calc", "tool_age_name", "tool_age_desc", "cake", Category.CALCULATORS),
        ToolDef("text_utils", "tool_text_name", "tool_text_desc", "text_fields", Category.TEXT)
    )

    fun byId(id: String): ToolDef? = all.firstOrNull { it.id == id }
}
