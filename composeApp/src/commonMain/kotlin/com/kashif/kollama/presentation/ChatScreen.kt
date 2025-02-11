package com.kashif.kollama.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.kashif.kollama.data.model.OllamaModel
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.presentation.components.AnimatedBorderBox
import com.kashif.kollama.presentation.components.ChatSidebar
import com.kashif.kollama.presentation.components.MessageBubble
import com.kashif.kollama.presentation.components.ModelMarquee
import com.kashif.kollama.presentation.components.ModelSelectionScreen
import com.kashif.kollama.presentation.components.ModelSelector
import com.kashif.kollama.presentation.components.PulsatingDot
import com.kashif.kollama.presentation.components.RightSideSheet
import com.kashif.kollama.presentation.components.ThinkingIndicator
import com.kashif.kollama.presentation.components.WelcomeScreen
import com.kashif.kollama.presentation.modifier.animatedBorder
import com.kashif.kollama.presentation.modifier.horizontalOffsetLayout
import com.kashif.kollama.presentation.state.ChatEvent
import com.kashif.kollama.presentation.state.ChatUiState
import com.kashif.kollama.presentation.state.ChatViewModel
import com.kashif.kollama.theme.LocalThemeIsDark
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import compose.icons.FeatherIcons
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Send
import compose.icons.feathericons.Sun
import kotlinx.coroutines.flow.collectLatest

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
        Box {
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
            if (uiState.showModelList) {
                ModelListSheet(
                    showDialog = true,
                    availableModels = uiState.availableModels,
                    selectedModel = uiState.selectedModel,
                    onModelSelected = { viewModel.handleEvent(ChatEvent.UserEvent.SelectModel(it)) },
                    onDismiss = { viewModel.handleEvent(ChatEvent.UserEvent.DismissModelList) }
                )
            }
        }
    }
}


@Composable
fun ModelListSheet(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    availableModels: List<OllamaModel>,
    selectedModel: OllamaModel?,
    onModelSelected: (OllamaModel) -> Unit,
    onDismiss: () -> Unit
) {

    val colorScheme = MaterialTheme.colorScheme
    RightSideSheet(
        isVisible = showDialog, onDismiss = onDismiss
    ) {
        val marqueeOffset = with(LocalDensity.current) {
            16.dp.roundToPx()
        }
        AnimatedBorderBox(
            borderColors = listOf(
                colorScheme.primary,
                colorScheme.secondary,
                colorScheme.tertiary
            ),
            backgroundColor = colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            borderWidth = 2.dp
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surface,
            ) {

                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Select AI Model",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    availableModels.fastForEach { model ->
                        Surface(
                            onClick = {
                                onModelSelected(model)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (model == selectedModel)
                                colorScheme.primary.copy(alpha = 0.1f)
                            else
                                colorScheme.primary.copy(0.035f),
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
                                        model.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (model == selectedModel)
                                                colorScheme.primary
                                            else
                                                colorScheme.onSurface
                                        )
                                    )
                                    ModelMarquee(
                                        model,
                                        modifier = Modifier.fillMaxWidth()
                                            .horizontalOffsetLayout(marqueeOffset)
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

@Composable
private fun MainContent(
    uiState: ChatUiState,
    messages: List<ChatMessageDomainModel>,
    onEvent: (ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    LaunchedEffect(messages.size) {
        state.animateScrollToItem(messages.size)
    }
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
                    showDialog = {
                        println("Show model list clicked")
                        onEvent(ChatEvent.UserEvent.ShowModelList)
                    }
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
                    state = state,
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
    state: LazyListState,
    messages: List<ChatMessageDomainModel>,
    thinkingMessage: ChatMessageDomainModel?,
    onRetryMessage: (ChatMessageDomainModel) -> Unit
) {
    LaunchedEffect(messages.size, thinkingMessage) {
        val lastIndex = messages.size + if (thinkingMessage != null) 1 else 0
        state.animateScrollToItem(lastIndex)
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
        if (thinkingMessage != null) {
            item {
                ThinkingIndicator()
                MessageBubble(
                    message = thinkingMessage, onRetry = {})
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
    val borderColor by animateColorAsState(
        if (isFocused)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    )



    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
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
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused }
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter) {
                            if (event.isShiftPressed) {
                                text += "\n"
                                true
                            } else if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
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
                maxLines = 10,
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

@Composable
fun rememberMarkdownTypography(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography
): MarkdownTypography = remember(colorScheme, typography) {
    DefaultMarkdownTypography(
        h1 = typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            lineHeight = 28.sp,
            fontSize = 24.sp,
            background = Color.Transparent
        ),
        h2 = typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            lineHeight = 26.sp,
            fontSize = 22.sp,
            background = Color.Transparent
        ),
        h3 = typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 24.sp,
            fontSize = 20.sp,
            background = Color.Transparent
        ),
        h4 = typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 18.sp,
            background = Color.Transparent
        ),
        h5 = typography.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 20.sp,
            fontSize = 17.sp,
            background = Color.Transparent
        ),
        h6 = typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            lineHeight = 20.sp,
            fontSize = 16.sp,
            background = Color.Transparent
        ),
        text = typography.bodySmall.copy(
            color = colorScheme.onSurface,
            lineHeight = 20.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        code = typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            fontSize = 14.sp,
            background = colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        inlineCode = typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = colorScheme.primary,
            lineHeight = 20.sp,
            fontSize = 14.sp,
            background = colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        quote = typography.bodyLarge.copy(
            color = colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        paragraph = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        ordered = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        bullet = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        list = typography.bodyLarge.copy(
            color = colorScheme.onSurface,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
        ),
        link = typography.bodyLarge.copy(
            color = colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            lineHeight = 22.sp,
            fontSize = 15.sp,
            background = Color.Transparent
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