package com.drs.nexus.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.drs.nexus.app.R
import com.drs.nexus.app.data.AppContainer
import com.drs.nexus.app.ui.components.ResultActionsRow
import com.drs.nexus.app.ui.components.ResultCard
import com.drs.nexus.app.ui.theme.NexusShapes
import com.drs.nexus.core.tools.BmiCalculator
import com.drs.nexus.core.tools.DateDifferenceTool
import com.drs.nexus.core.tools.LoanCalculator
import com.drs.nexus.core.tools.PercentageCalculator
import kotlinx.coroutines.launch

/** Compact numeric input following the tool design language. */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = NexusShapes.input,
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

// ---------------- Percentage Calculator ----------------

@Composable
fun PercentageToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("percentage")!!
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(0) }
    var x by remember { mutableStateOf("25") }
    var y by remember { mutableStateOf("200") }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val modes = listOf("X% of Y", "X is ?% of Y", "change X → Y")

    ToolScaffold(tool, container, navController) {
        OptionChipRow(options = modes, selectedIndex = mode, onSelect = { mode = it; output = null; error = null })
        NumberField(value = x, onValueChange = { x = it; output = null; error = null }, label = if (mode == 0) "X (%)" else "X")
        NumberField(value = y, onValueChange = { y = it; output = null; error = null }, label = if (mode == 0) "Y" else "Y")
        RunButton(stringResource(R.string.action_calculate)) {
            val xv = x.trim().toDoubleOrNull()
            val yv = y.trim().toDoubleOrNull()
            if (xv == null || yv == null) {
                error = "Please enter valid numbers in both fields"
            } else {
                when (val r = PercentageCalculator.calculate(mode, xv, yv)) {
                    is PercentageCalculator.Result.Success -> { output = r.text; error = null }
                    is PercentageCalculator.Result.Failure -> { error = r.message; output = null }
                }
                scope.launch { container.toolRepository.logRun(tool.id, "mode=$mode x=$xv y=$yv", output ?: "error") }
            }
        }
        error?.let { ErrorBanner(it) }
        output?.let {
            ResultCard(title = stringResource(R.string.label_result), content = it)
            ResultActionsRow(result = it)
        }
    }
}

// ---------------- Loan / EMI Calculator ----------------

@Composable
fun LoanToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("loan")!!
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("300000") }
    var rate by remember { mutableStateOf("6") }
    var years by remember { mutableStateOf("30") }
    var output by remember { mutableStateOf<LoanCalculator.Amortization?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ToolScaffold(tool, container, navController) {
        NumberField(value = amount, onValueChange = { amount = it; output = null; error = null }, label = "Loan amount")
        NumberField(value = rate, onValueChange = { rate = it; output = null; error = null }, label = "Annual interest rate (%)")
        NumberField(value = years, onValueChange = { years = it; output = null; error = null }, label = "Term (years)")
        RunButton(stringResource(R.string.action_calculate)) {
            val a = amount.trim().toDoubleOrNull()
            val r = rate.trim().toDoubleOrNull()
            val t = years.trim().toDoubleOrNull()
            if (a == null || r == null || t == null) {
                error = "Please fill all fields with valid numbers"
            } else {
                when (val res = LoanCalculator.calculate(a, r, t)) {
                    is LoanCalculator.Result.Success -> { output = res.plan; error = null }
                    is LoanCalculator.Result.Failure -> { error = res.message; output = null }
                }
                scope.launch { container.toolRepository.logRun(tool.id, "amount=$a rate=$r years=$t", output?.monthlyPayment?.toString() ?: "error") }
            }
        }
        error?.let { ErrorBanner(it) }
        output?.let { plan ->
            ResultCard(
                title = stringResource(R.string.loan_monthly),
                content = LoanCalculator.fmt(plan.monthlyPayment)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.loan_total_paid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(LoanCalculator.fmt(plan.totalPayment), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.loan_total_interest), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(LoanCalculator.fmt(plan.totalInterest), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.loan_months), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${plan.months}", style = MaterialTheme.typography.bodySmall)
            }
            ResultActionsRow(result = "${stringResource(R.string.loan_monthly)}: ${LoanCalculator.fmt(plan.monthlyPayment)}")
        }
    }
}

// ---------------- BMI Calculator ----------------

@Composable
fun BmiToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("bmi")!!
    val scope = rememberCoroutineScope()
    var unit by remember { mutableStateOf(0) } // 0 metric, 1 imperial
    var kg by remember { mutableStateOf("70") }
    var cm by remember { mutableStateOf("175") }
    var lb by remember { mutableStateOf("154") }
    var inch by remember { mutableStateOf("69") }
    var reading by remember { mutableStateOf<BmiCalculator.Reading?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val catLabels = mapOf(
        BmiCalculator.Category.UNDERWEIGHT to stringResource(R.string.bmi_underweight),
        BmiCalculator.Category.NORMAL to stringResource(R.string.bmi_normal),
        BmiCalculator.Category.OVERWEIGHT to stringResource(R.string.bmi_overweight),
        BmiCalculator.Category.OBESE to stringResource(R.string.bmi_obese)
    )

    ToolScaffold(tool, container, navController) {
        OptionChipRow(
            options = listOf(stringResource(R.string.bmi_metric), stringResource(R.string.bmi_imperial)),
            selectedIndex = unit,
            onSelect = { unit = it; reading = null; error = null }
        )
        if (unit == 0) {
            NumberField(value = kg, onValueChange = { kg = it; reading = null; error = null }, label = "Weight (kg)")
            NumberField(value = cm, onValueChange = { cm = it; reading = null; error = null }, label = "Height (cm)")
        } else {
            NumberField(value = lb, onValueChange = { lb = it; reading = null; error = null }, label = "Weight (lb)")
            NumberField(value = inch, onValueChange = { inch = it; reading = null; error = null }, label = "Height (inches)")
        }
        RunButton(stringResource(R.string.action_calculate)) {
            val res = if (unit == 0) {
                val k = kg.trim().toDoubleOrNull(); val c = cm.trim().toDoubleOrNull()
                if (k == null || c == null) BmiCalculator.Result.Failure("Please enter valid numbers")
                else BmiCalculator.calculate(k, c)
            } else {
                val p = lb.trim().toDoubleOrNull(); val i = inch.trim().toDoubleOrNull()
                if (p == null || i == null) BmiCalculator.Result.Failure("Please enter valid numbers")
                else BmiCalculator.imperial(p, i)
            }
            when (res) {
                is BmiCalculator.Result.Success -> { reading = res.reading; error = null }
                is BmiCalculator.Result.Failure -> { error = res.message; reading = null }
            }
            reading?.let { scope.launch { container.toolRepository.logRun(tool.id, "unit=$unit", "BMI ${it.bmi}") } }
        }
        error?.let { ErrorBanner(it) }
        reading?.let { r ->
            ResultCard(
                title = "BMI",
                content = "${r.bmi} — ${catLabels[r.category]}"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.bmi_healthy_range), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${r.healthyWeightMinKg}–${r.healthyWeightMaxKg} kg", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------- Date Difference ----------------

@Composable
fun DateDiffToolScreen(container: AppContainer, navController: NavHostController) {
    val tool = com.drs.nexus.core.tools.ToolRegistry.byId("date_diff")!!
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(0) } // 0 = between, 1 = add days
    var start by remember { mutableStateOf("2026-01-01") }
    var end by remember { mutableStateOf("2026-12-31") }
    var days by remember { mutableStateOf("30") }
    var output by remember { mutableStateOf<DateDifferenceTool.Diff?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ToolScaffold(tool, container, navController) {
        OptionChipRow(
            options = listOf("between dates", "add days"),
            selectedIndex = mode,
            onSelect = { mode = it; output = null; error = null }
        )
        NumberField(value = start, onValueChange = { start = it; output = null; error = null }, label = "Start date (YYYY-MM-DD)")
        if (mode == 0) {
            NumberField(value = end, onValueChange = { end = it; output = null; error = null }, label = "End date (YYYY-MM-DD)")
        } else {
            NumberField(value = days, onValueChange = { days = it; output = null; error = null }, label = "Days to add (can be negative)")
        }
        RunButton(stringResource(R.string.action_calculate)) {
            val res = if (mode == 0) {
                DateDifferenceTool.between(start, end)
            } else {
                val d = days.trim().toLongOrNull()
                if (d == null) DateDifferenceTool.Result.Failure("Days must be a whole number")
                else DateDifferenceTool.addDays(start, d)
            }
            when (res) {
                is DateDifferenceTool.Result.Success -> { output = res.diff; error = null }
                is DateDifferenceTool.Result.Failure -> { error = res.message; output = null }
            }
            output?.let { scope.launch { container.toolRepository.logRun(tool.id, "mode=$mode", "${it.days} days") } }
        }
        error?.let { ErrorBanner(it) }
        output?.let { d ->
            ResultCard(
                title = stringResource(R.string.datediff_result),
                content = "${d.days} days"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.datediff_weeks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${d.weeks}", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.datediff_months), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${d.months}", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.datediff_years), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${d.years}", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.datediff_weekdays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${d.weekdays}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
