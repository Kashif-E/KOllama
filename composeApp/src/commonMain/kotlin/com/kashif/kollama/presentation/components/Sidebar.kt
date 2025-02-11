package com.kashif.kollama.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashif.kollama.ChatSession
import com.kashif.kollama.domain.model.ChatSessionDomainModel
import com.kashif.kollama.presentation.ButtonBackground
import com.kashif.kollama.presentation.state.ChatEvent
import com.kashif.kollama.presentation.state.ChatUiState
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Plus

@Composable
internal fun ChatSidebar(
    uiState: ChatUiState,
    onEvent: (ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        FeatherIcons.Cpu,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "kOlamma",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }


            Surface(
                onClick = { onEvent(ChatEvent.UserEvent.CreateNewChat) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        FeatherIcons.Plus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "New Chat",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))


            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.chatSessions) { session ->
                    ChatSessionItem(
                        session = session,
                        isSelected = session.id == uiState.currentSession?.id,
                        onClick = { onEvent(ChatEvent.UserEvent.SelectSession(session)) }
                    )
                }
            }

        }
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            ButtonBackground(
                modifier = Modifier.align(Alignment.BottomCenter).padding(vertical = 12.dp)
            ) {
                Text(
                    "Made with ❤️ by Kashif Mehmood",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatSessionItem(
    session: ChatSessionDomainModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else
            Color.Transparent,
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        else
            null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                FeatherIcons.MessageCircle,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                session.title,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
