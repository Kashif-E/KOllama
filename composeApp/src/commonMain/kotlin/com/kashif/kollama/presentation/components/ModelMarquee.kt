package com.kashif.kollama.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kashif.kollama.data.model.OllamaModel
import kotlin.math.roundToInt


@Composable
fun ModelMarquee(
    model: OllamaModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                spacing = MarqueeSpacing(16.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModelInfoChip(
            label = "Size",
            value = formatSize(model.size)
        )
        ModelInfoChip(
            label = "Parameters",
            value = model.details.parameterSize
        )
        ModelInfoChip(
            label = "Format",
            value = model.details.format.uppercase()
        )
        ModelInfoChip(
            label = "Family",
            value = model.details.family
        )
        ModelInfoChip(
            label = "Quantization",
            value = model.details.quantizationLevel
        )
    }
}

@Composable
private fun ModelInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return "${(gb * 100).roundToInt() / 100.0}GB"
}
