package com.drs.nexus.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drs.nexus.app.R
import com.drs.nexus.app.ui.theme.NexusShapes

/** Copies text to the system clipboard and confirms with a toast. */
fun copyToClipboard(context: Context, text: String, label: String = "DRS Nexus") {
    if (text.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
}

/** Shares text through the Android share sheet. */
fun shareText(context: Context, text: String, subject: String = "DRS Nexus result") {
    if (text.isEmpty()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
fun NexusTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, NexusShapes.cardSmall)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.14f), NexusShapes.cardSmall),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ResultActionsRow(
    result: String,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionButton(
            icon = Icons.Filled.ContentCopy,
            label = stringResource(R.string.action_copy),
            onClick = { copyToClipboard(context, result) }
        )
        ActionButton(
            icon = Icons.Filled.Share,
            label = stringResource(R.string.action_share),
            onClick = { shareText(context, result) }
        )
        extra?.invoke()
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.primary) {
    FilledIconButton(
        onClick = onClick,
        shape = NexusShapes.cardSmall,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ResultCard(title: String, content: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NexusShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 14,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DeleteIconButton(onClick: () -> Unit) {
    val danger = androidx.compose.foundation.isSystemInDarkTheme().let { dark ->
        if (dark) Color(0xFFFF453A) else Color(0xFFE5484D)
    }
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = danger.copy(alpha = 0.1f),
            contentColor = danger
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(17.dp))
    }
}
