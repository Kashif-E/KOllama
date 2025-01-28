package com.kashif.deepseek.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.presentation.components.ChatSidebar
import com.kashif.deepseek.presentation.components.MessageBubble
import com.kashif.deepseek.presentation.components.ModelSelectionScreen
import com.kashif.deepseek.presentation.components.ModelSelector
import com.kashif.deepseek.presentation.components.ThinkingIndicator
import com.kashif.deepseek.presentation.components.WelcomeScreen
import com.kashif.deepseek.presentation.modifier.animatedBorder
import com.kashif.deepseek.presentation.state.ChatEvent
import com.kashif.deepseek.presentation.state.ChatUiState
import com.kashif.deepseek.presentation.state.ChatViewModel
import com.kashif.deepseek.presentation.theme.LocalThemeIsDark
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import compose.icons.FeatherIcons
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Send
import compose.icons.feathericons.Sun

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.state.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            ChatSidebar(
                uiState = uiState,
                onEvent = viewModel::handleEvent,
                modifier = Modifier.width(280.dp)
            )


            MainContent(
                uiState = uiState,
                messages = viewModel.messages,
                onEvent = viewModel::handleEvent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun MainContent(
    uiState: ChatUiState,
    messages: List<ChatMessage>,
    onEvent: (ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModelSelector(
                    selectedModel = uiState.selectedModel,
                    availableModels = uiState.availableModels,
                    onModelSelect = { onEvent(ChatEvent.UserEvent.SelectModel(it)) }
                )

                var isDark by LocalThemeIsDark.current
                IconButton(onClick = { isDark = !isDark }) {
                    Icon(
                        if (isDark) FeatherIcons.Sun else FeatherIcons.Moon,
                        contentDescription = "Toggle theme"
                    )
                }
            }
        }


        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            when {
                uiState.currentSession == null -> WelcomeScreen(onEvent)
                uiState.selectedModel == null -> ModelSelectionScreen(
                    availableModels = uiState.availableModels,
                    onModelSelected = { onEvent(ChatEvent.UserEvent.SelectModel(it)) }
                )

                else -> ChatMessages(
                    messages = messages,
                    thinkingMessage = uiState.thinkingMessage,
                    onRetryMessage = { onEvent(ChatEvent.UserEvent.RetryMessage(it)) }
                )
            }
        }


        if (uiState.currentSession != null && uiState.selectedModel != null) {
            ChatInput(
                onSendMessage = { onEvent(ChatEvent.UserEvent.SendMessage(it)) },
                enabled = !uiState.isLoading && uiState.thinkingMessage == null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


@Composable
private fun ChatMessages(
    messages: List<ChatMessage>,
    thinkingMessage: ChatMessage?,
    onRetryMessage: (ChatMessage) -> Unit
) {

    val state = rememberLazyListState()

    LaunchedEffect(messages.size) {
        state.animateScrollToItem(messages.size)
    }
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(messages) { message ->
            if (message.content.isNotEmpty()) {
                MessageBubble(message = message, onRetry = { onRetryMessage(message) })
            }
        }

        thinkingMessage?.let {
            item {
                ThinkingIndicator()
                MessageBubble(message = it, onRetry = {})
            }
        }
    }
}


@Composable
fun ChatInput(
    onSendMessage: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Box {

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isFocused)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier.then(
                    if (isFocused) Modifier.animatedBorder(
                        borderColors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderWidth = 2.dp
                    ) else Modifier
                )
                    .padding(12.dp)
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        )
                    ),
                    maxLines = 4,
                    placeholder = {
                        Text(
                            "Type your message...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (text.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendMessage(text)
                            text = ""
                        },
                        enabled = enabled
                    ) {
                        Icon(
                            FeatherIcons.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun rememberMarkdownTypography(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography
): MarkdownTypography = remember(colorScheme, typography) {
    DefaultMarkdownTypography(
        h1 = typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            lineHeight = 40.sp,
            fontSize = 32.sp
        ),
        h2 = typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            lineHeight = 36.sp,
            fontSize = 28.sp
        ),
        h3 = typography.headlineSmall.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 32.sp,
            fontSize = 24.sp
        ),
        h4 = typography.titleLarge.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 28.sp,
            fontSize = 22.sp
        ),
        h5 = typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 20.sp
        ),
        h6 = typography.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 18.sp
        ),
        text = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        code = typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = colorScheme.onSurfaceVariant,
            background = colorScheme.surfaceVariant.copy(alpha = 0.5f),
            lineHeight = 20.sp,
            fontSize = 14.sp
        ),
        inlineCode = typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = colorScheme.primary,
            background = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            lineHeight = 20.sp,
            fontSize = 14.sp
        ),
        quote = typography.bodyLarge.copy(
            color = colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        paragraph = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        ordered = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        bullet = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        list = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 16.sp
        ),
        link = typography.bodyLarge.copy(
            color = colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            lineHeight = 24.sp,
            fontSize = 16.sp
        )
    )
}

@Composable
fun ButtonBackground(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        content()
    }
}