package com.drs.nexus.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.drs.nexus.app.R

/**
 * Single source of truth for localized tool names/descriptions.
 * Covers EVERY tool in ToolRegistry — older screens previously carried
 * partial copies that leaked raw keys like "tool_currency_name" into the UI.
 */
@Composable
fun localizedToolName(nameKey: String): String = when (nameKey) {
    "tool_json_name" -> stringResource(R.string.tool_json_name)
    "tool_base64_name" -> stringResource(R.string.tool_base64_name)
    "tool_hash_name" -> stringResource(R.string.tool_hash_name)
    "tool_regex_name" -> stringResource(R.string.tool_regex_name)
    "tool_url_name" -> stringResource(R.string.tool_url_name)
    "tool_urlparser_name" -> stringResource(R.string.tool_urlparser_name)
    "tool_jwt_name" -> stringResource(R.string.tool_jwt_name)
    "tool_ts_name" -> stringResource(R.string.tool_ts_name)
    "tool_unit_name" -> stringResource(R.string.tool_unit_name)
    "tool_base_name" -> stringResource(R.string.tool_base_name)
    "tool_color_name" -> stringResource(R.string.tool_color_name)
    "tool_currency_name" -> stringResource(R.string.tool_currency_name)
    "tool_uuid_name" -> stringResource(R.string.tool_uuid_name)
    "tool_pass_name" -> stringResource(R.string.tool_pass_name)
    "tool_random_name" -> stringResource(R.string.tool_random_name)
    "tool_lorem_name" -> stringResource(R.string.tool_lorem_name)
    "tool_qr_name" -> stringResource(R.string.tool_qr_name)
    "tool_percent_name" -> stringResource(R.string.tool_percent_name)
    "tool_loan_name" -> stringResource(R.string.tool_loan_name)
    "tool_bmi_name" -> stringResource(R.string.tool_bmi_name)
    "tool_datediff_name" -> stringResource(R.string.tool_datediff_name)
    "tool_age_name" -> stringResource(R.string.tool_age_name)
    "tool_text_name" -> stringResource(R.string.tool_text_name)
    else -> nameKey
}

@Composable
fun localizedToolDesc(descKey: String): String = when (descKey) {
    "tool_json_desc" -> stringResource(R.string.tool_json_desc)
    "tool_base64_desc" -> stringResource(R.string.tool_base64_desc)
    "tool_hash_desc" -> stringResource(R.string.tool_hash_desc)
    "tool_regex_desc" -> stringResource(R.string.tool_regex_desc)
    "tool_url_desc" -> stringResource(R.string.tool_url_desc)
    "tool_urlparser_desc" -> stringResource(R.string.tool_urlparser_desc)
    "tool_jwt_desc" -> stringResource(R.string.tool_jwt_desc)
    "tool_ts_desc" -> stringResource(R.string.tool_ts_desc)
    "tool_unit_desc" -> stringResource(R.string.tool_unit_desc)
    "tool_base_desc" -> stringResource(R.string.tool_base_desc)
    "tool_color_desc" -> stringResource(R.string.tool_color_desc)
    "tool_currency_desc" -> stringResource(R.string.tool_currency_desc)
    "tool_uuid_desc" -> stringResource(R.string.tool_uuid_desc)
    "tool_pass_desc" -> stringResource(R.string.tool_pass_desc)
    "tool_random_desc" -> stringResource(R.string.tool_random_desc)
    "tool_lorem_desc" -> stringResource(R.string.tool_lorem_desc)
    "tool_qr_desc" -> stringResource(R.string.tool_qr_desc)
    "tool_percent_desc" -> stringResource(R.string.tool_percent_desc)
    "tool_loan_desc" -> stringResource(R.string.tool_loan_desc)
    "tool_bmi_desc" -> stringResource(R.string.tool_bmi_desc)
    "tool_datediff_desc" -> stringResource(R.string.tool_datediff_desc)
    "tool_age_desc" -> stringResource(R.string.tool_age_desc)
    "tool_text_desc" -> stringResource(R.string.tool_text_desc)
    else -> ""
}
