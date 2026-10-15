package com.example.pastoral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.assistant.PastoralAssistantEngine
import com.example.pastoral.data.model.ChatItem
import com.example.pastoral.data.model.PastoralData
import com.example.pastoral.data.repository.PastoralRepository
import com.example.pastoral.ui.components.PastoralMarkdownText
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(
    data: PastoralData,
    repository: PastoralRepository,
    assistantEngine: PastoralAssistantEngine,
    onNavigateToTab: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Local chat session state (with default greeting if empty)
    val chatMessages = remember(data.chat, data.settings) {
        if (data.chat.isEmpty()) {
            val name = if (data.settings.pastorName.isNotBlank()) data.settings.pastorName else "Responsable"
            val school = if (data.settings.schoolName.isNotBlank()) " del **${data.settings.schoolName}**" else ""
            listOf(
                ChatItem(
                    role = "assistant",
                    content = "✝ **¡Paz y bien, $name!**\n\nSoy tu Asistente Pastoral con poder completo sobre tu aplicación$school.\n\n⚡ **Dime lo que necesitas y lo haré de inmediato**:\n• «*Haz un evento: Retiro de Jóvenes el próximo viernes a las 10:00*»\n• «*Escríbeme una tarea: Comprar cirios para mañana, prioridad alta*»\n• «*Crea una nota con una dinámica para catequesis*»\n• «*Marca como completada la tarea de velas*»\n\n¿Qué deseas organizar hoy?"
                )
            )
        } else {
            data.chat
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val actionPromptChips = listOf(
        "📅 Haz un evento" to "Haz un evento: Retiro de Jóvenes el próximo viernes a las 10:00 en la capilla",
        "✅ Escríbeme una tarea" to "Escríbeme una tarea: Comprar cirios para mañana, prioridad alta",
        "💡 Crea una nota" to "Crea una nota con ideas para el inicio del año pastoral escolar",
        "🌅 /hoy" to "/hoy",
        "📅 /semana" to "/semana",
        "📋 /plantillas" to "/plantillas",
        "❓ /ayuda" to "/ayuda"
    )

    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("assistant_screen")
    ) {
        // Top Toolbar with Adaptive Layout for Control Total IA (Never wraps vertically)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pastoral Cross Icon
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✝", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title and Subtitle with Control Total IA Badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Asistente Pastoral",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Badge: Control Total IA (Always horizontal, never wraps vertically)
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF16A34A), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Control Total IA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "• Eventos, notas y tareas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { repository.clearChat() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Limpiar chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Prompts & Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actionPromptChips.forEach { (label, commandOrPrompt) ->
                SuggestionChip(
                    onClick = {
                        if (!isGenerating) {
                            if (commandOrPrompt.startsWith("/")) {
                                // Execute command immediately
                                scope.launch {
                                    isGenerating = true
                                    repository.addChatMessage(role = "user", content = commandOrPrompt)
                                    val result = assistantEngine.processMessage(commandOrPrompt)
                                    repository.addChatMessage(
                                        role = "assistant",
                                        content = result.replyText,
                                        actionType = result.actionType,
                                        actionTitle = result.actionTitle,
                                        actionDetails = result.actionDetails,
                                        targetTab = result.targetTab
                                    )
                                    isGenerating = false
                                }
                            } else {
                                // Fill input field so user can customize or tap send
                                inputText = commandOrPrompt
                            }
                        }
                    },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("action_chip_${label.filter { it.isLetter() }.lowercase()}")
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chatMessages) { msg ->
                ChatBubble(
                    message = msg,
                    onNavigateToTab = onNavigateToTab
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ejecutando acción y reflexionando respuesta pastoral...",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ej: Haz un evento..., Escríbeme una tarea...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input_field"),
                    maxLines = 3,
                    enabled = !isGenerating
                )

                IconButton(
                    onClick = {
                        val textToSend = inputText.trim()
                        if (textToSend.isNotEmpty() && !isGenerating) {
                            inputText = ""
                            scope.launch {
                                isGenerating = true
                                repository.addChatMessage(role = "user", content = textToSend)
                                val result = assistantEngine.processMessage(textToSend)
                                repository.addChatMessage(
                                    role = "assistant",
                                    content = result.replyText,
                                    actionType = result.actionType,
                                    actionTitle = result.actionTitle,
                                    actionDetails = result.actionDetails,
                                    targetTab = result.targetTab
                                )
                                isGenerating = false
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("assistant_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatItem,
    onNavigateToTab: (Int) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✝", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                PastoralMarkdownText(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Render Action Execution Card if this message executed a database action
                if (!isUser && message.actionType != null && message.actionTitle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = CircleShape,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (message.actionType) {
                                                "event_created" -> Icons.Default.CalendarMonth
                                                "task_created" -> Icons.Default.CheckCircle
                                                "note_created" -> Icons.Default.Lightbulb
                                                "task_completed" -> Icons.Default.DoneAll
                                                "person_added" -> Icons.Default.Person
                                                "group_added" -> Icons.Default.People
                                                else -> Icons.Default.Check
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "ACCIÓN REALIZADA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.actionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!message.actionDetails.isNullOrBlank()) {
                                Text(
                                    text = message.actionDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (message.targetTab != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onNavigateToTab(message.targetTab) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("action_view_button_${message.actionType}")
                                ) {
                                    val buttonLabel = when (message.targetTab) {
                                        1 -> "Ver en Agenda ↗"
                                        2 -> "Ver en Tareas ↗"
                                        3 -> "Ver en Notas ↗"
                                        4 -> "Ver en Comunidad ↗"
                                        else -> "Ver en la App ↗"
                                    }
                                    Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
