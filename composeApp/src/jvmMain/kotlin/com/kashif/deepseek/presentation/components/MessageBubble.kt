package com.kashif.deepseek.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.MessageStatus
import com.kashif.deepseek.presentation.rememberMarkdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownPadding
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Copy
import compose.icons.feathericons.RefreshCw

@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = rememberMarkdownTypography(colorScheme = colorScheme)
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            border = BorderStroke(
                width = 1.dp,
                color = if (message.isUser)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            ),
            modifier = Modifier.align(if (message.isUser) Alignment.End else Alignment.Start)
        ) {
            Box(
                modifier = Modifier
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    (if (message.isUser)
                                        colorScheme.primary
                                    else
                                        colorScheme.secondary)
                                        .copy(alpha = 0.05f),
                                    Color.Transparent,
                                )
                            )
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!message.isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {


                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(message.content))
                            }) {
                                Icon(
                                    imageVector = FeatherIcons.Copy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    SelectionContainer {
                        Markdown(
                            message.content,
                            modifier = Modifier.wrapContentWidth(),
                            components = markdownComponents(
                                codeBlock = highlightedCodeBlock,
                                codeFence = highlightedCodeFence
                            ),
                            dimens = markdownDimens(
                                dividerThickness = 1.dp,
                                codeBackgroundCornerSize = 12.dp,
                                blockQuoteThickness = 2.dp,
                                tableMaxWidth = Dp.Unspecified,
                                tableCellWidth = 160.dp,
                                tableCellPadding = 16.dp,
                                tableCornerSize = 8.dp,
                            ),
                            padding = markdownPadding(
                                block = 8.dp,
                                list = 12.dp,
                                listItemBottom = 8.dp,
                                indentList = 8.dp,
                                codeBlock = PaddingValues(16.dp),
                                blockQuote = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                blockQuoteText = PaddingValues(vertical = 4.dp),
                                blockQuoteBar = PaddingValues.Absolute(
                                    left = 4.dp,
                                    top = 2.dp,
                                    right = 4.dp,
                                    bottom = 2.dp
                                ),
                            ),
                        )
                    }

                    when (message.status) {
                        MessageStatus.SENDING -> {
                            Box(modifier = Modifier.padding(12.dp)) {
                                PulsatingDot(
                                    color = MaterialTheme.colorScheme.primary,
                                    size = 8.dp
                                )
                            }
                        }

                        MessageStatus.ERROR -> {
                            Surface(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        FeatherIcons.RefreshCw,
                                        contentDescription = "Retry",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Retry",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        MessageStatus.SENT -> {
                            Row(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    FeatherIcons.Check,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
