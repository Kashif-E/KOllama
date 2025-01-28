package com.kashif.deepseek.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kashif.deepseek.presentation.modifier.animatedBorder
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu

@Composable
fun ModelSelector(
    selectedModel: String?,
    availableModels: List<String>,
    onModelSelect: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                FeatherIcons.Cpu,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Text(
                selectedModel ?: "Select Model",
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

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier.animatedBorder(
                    borderColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = colorScheme.surface,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surface,
            ) {
                Box {
                    Column(
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            "Select AI Model",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        availableModels.forEach { model ->
                            Surface(
                                onClick = {
                                    onModelSelect(model)
                                    showDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (model == selectedModel)
                                    colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                border = if (model == selectedModel)
                                    BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                                else
                                    null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            model,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = if (model == selectedModel)
                                                    colorScheme.primary
                                                else
                                                    colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            getModelDescription(model),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (model == selectedModel) {

                                        PulsatingDot(color = colorScheme.primary)

                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}