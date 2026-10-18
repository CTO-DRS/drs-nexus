package com.drs.nexus.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drs.nexus.app.R
import com.drs.nexus.app.ui.theme.NexusPalette
import com.drs.nexus.app.ui.theme.NexusShapes
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: (onboarded: Boolean) -> Unit) {
    var scale by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.7f) }
    val animated by animateFloatAsState(targetValue = scale, animationSpec = tween(650), label = "logo")
    LaunchedEffect(Unit) {
        scale = 1f
        delay(950)
        onFinished(false)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(animated)
                    .background(MaterialTheme.colorScheme.primary, NexusShapes.card),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.TextFields,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "DRS Nexus",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private data class OnbPage(val titleRes: Int, val bodyRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: androidx.compose.ui.graphics.Color)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnbPage(R.string.onb_title_1, R.string.onb_body_1, Icons.Outlined.TextFields, NexusPalette.Blue),
        OnbPage(R.string.onb_title_2, R.string.onb_body_2, Icons.Outlined.Lock, NexusPalette.Green),
        OnbPage(R.string.onb_title_3, R.string.onb_body_3, Icons.Outlined.Fingerprint, NexusPalette.Purple)
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(page.tint.copy(alpha = 0.14f), NexusShapes.card),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(page.icon, contentDescription = null, tint = page.tint, modifier = Modifier.size(52.dp))
                }
                Text(
                    stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(if (active) 10.dp else 7.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            }
        }
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = NexusShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                if (pagerState.currentPage < pages.size - 1) stringResource(R.string.next) else stringResource(R.string.get_started),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
