package com.kashif.kollama.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kashif.kollama.data.model.OllamaModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu

@Composable
fun ModelSelector(
    selectedModel: OllamaModel?,
    showDialog: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.clickable {
                println("ModelSelector clicked")
                showDialog()
            }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                FeatherIcons.Cpu,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Text(
                selectedModel?.name ?: "Select Model",
                color = colorScheme.primary
            )
            PulsatingDot(
                color = if (selectedModel != null)
                    colorScheme.primary
                else
                    colorScheme.onSurfaceVariant
            )
        }
    }

}