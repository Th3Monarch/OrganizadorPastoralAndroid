package com.example.pastoral.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pastoral.assistant.PastoralAssistantEngine
import com.example.pastoral.data.model.Event
import com.example.pastoral.data.model.Note
import com.example.pastoral.data.model.Task
import com.example.pastoral.data.repository.PastoralRepository
import com.example.pastoral.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class PastoralTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    INICIO("Inicio", Icons.Default.Home),
    EVENTOS("Eventos", Icons.Default.CalendarMonth),
    TAREAS("Tareas", Icons.Default.CheckCircle),
    NOTAS("Notas", Icons.Default.Lightbulb),
    ASISTENTE("Asistente", Icons.Default.AutoAwesome),
    COMUNIDAD("Comunidad", Icons.Default.People),
    AJUSTES("Ajustes", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPastoralApp(
    repository: PastoralRepository,
    assistantEngine: PastoralAssistantEngine,
    authManager: com.example.pastoral.auth.AuthManager? = null
) {
    val pastoralData by repository.data.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    var isDarkTheme by remember(pastoralData.settings.theme) {
        mutableStateOf(pastoralData.settings.theme == "dark" || (pastoralData.settings.theme == "system" && systemDark))
    }

    var currentTab by remember { mutableStateOf(PastoralTab.INICIO) }

    // Quick add dialog triggers from Dashboard
    var showQuickAddIdeaDialog by remember { mutableStateOf(false) }
    var showQuickAddEventDialog by remember { mutableStateOf(false) }
    var showQuickAddTaskDialog by remember { mutableStateOf(false) }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "✝ ",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = when (currentTab) {
                                    PastoralTab.INICIO -> "Organizador Pastoral"
                                    PastoralTab.EVENTOS -> "Agenda y Calendario"
                                    PastoralTab.TAREAS -> "Tareas Pastorales"
                                    PastoralTab.NOTAS -> "Ideas y Plantillas"
                                    PastoralTab.ASISTENTE -> "Asistente Gemini"
                                    PastoralTab.COMUNIDAD -> "Comunidad y Grupos"
                                    PastoralTab.AJUSTES -> "Ajustes y Respaldos"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentTab = PastoralTab.COMUNIDAD },
                            modifier = Modifier.testTag("nav_to_community_top")
                        ) {
                            Icon(
                                imageVector = if (currentTab == PastoralTab.COMUNIDAD) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Comunidad",
                                tint = if (currentTab == PastoralTab.COMUNIDAD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { currentTab = PastoralTab.AJUSTES },
                            modifier = Modifier.testTag("nav_to_settings_top")
                        ) {
                            Icon(
                                imageVector = if (currentTab == PastoralTab.AJUSTES) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Ajustes",
                                tint = if (currentTab == PastoralTab.AJUSTES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val primaryTabs = listOf(
                        PastoralTab.INICIO,
                        PastoralTab.EVENTOS,
                        PastoralTab.TAREAS,
                        PastoralTab.NOTAS,
                        PastoralTab.ASISTENTE
                    )

                    primaryTabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = if (tab == PastoralTab.ASISTENTE) 10.sp else 11.sp,
                                    letterSpacing = if (tab == PastoralTab.ASISTENTE) (-0.4).sp else (-0.1).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContentTransition"
                ) { tab ->
                    when (tab) {
                        PastoralTab.INICIO -> DashboardScreen(
                            data = pastoralData,
                            onNavigateToTab = { index ->
                                currentTab = when (index) {
                                    1 -> PastoralTab.EVENTOS
                                    2 -> PastoralTab.TAREAS
                                    3 -> PastoralTab.NOTAS
                                    4 -> PastoralTab.COMUNIDAD
                                    5 -> PastoralTab.ASISTENTE
                                    6 -> PastoralTab.AJUSTES
                                    else -> PastoralTab.INICIO
                                }
                            },
                            onQuickAddIdea = { showQuickAddIdeaDialog = true },
                            onQuickAddEvent = { showQuickAddEventDialog = true },
                            onQuickAddTask = { showQuickAddTaskDialog = true },
                            onToggleTask = { taskId -> repository.toggleTaskDone(taskId) }
                        )

                        PastoralTab.EVENTOS -> EventsScreen(
                            events = pastoralData.events,
                            persons = pastoralData.persons,
                            onAddEvent = { repository.addEvent(it) },
                            onUpdateEvent = { repository.updateEvent(it) },
                            onDeleteEvent = { repository.deleteEvent(it) }
                        )

                        PastoralTab.TAREAS -> TasksScreen(
                            tasks = pastoralData.tasks,
                            onAddTask = { repository.addTask(it) },
                            onUpdateTask = { repository.updateTask(it) },
                            onToggleTaskDone = { repository.toggleTaskDone(it) },
                            onDeleteTask = { repository.deleteTask(it) }
                        )

                        PastoralTab.NOTAS -> NotesScreen(
                            notes = pastoralData.notes,
                            onAddNote = { repository.addNote(it) },
                            onUpdateNote = { repository.updateNote(it) },
                            onDeleteNote = { repository.deleteNote(it) }
                        )

                        PastoralTab.ASISTENTE -> AssistantScreen(
                            data = pastoralData,
                            repository = repository,
                            assistantEngine = assistantEngine,
                            onNavigateToTab = { index ->
                                currentTab = when (index) {
                                    1 -> PastoralTab.EVENTOS
                                    2 -> PastoralTab.TAREAS
                                    3 -> PastoralTab.NOTAS
                                    4 -> PastoralTab.COMUNIDAD
                                    else -> PastoralTab.INICIO
                                }
                            }
                        )

                        PastoralTab.COMUNIDAD -> CommunityScreen(
                            groups = pastoralData.groups,
                            persons = pastoralData.persons,
                            onAddGroup = { repository.addGroup(it) },
                            onUpdateGroup = { repository.updateGroup(it) },
                            onDeleteGroup = { repository.deleteGroup(it) },
                            onAddPerson = { repository.addPerson(it) },
                            onUpdatePerson = { repository.updatePerson(it) },
                            onDeletePerson = { repository.deletePerson(it) }
                        )

                        PastoralTab.AJUSTES -> SettingsScreen(
                            settings = pastoralData.settings,
                            repository = repository,
                            authManager = authManager,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { enabled ->
                                isDarkTheme = enabled
                                repository.updateSettings(pastoralData.settings.copy(theme = if (enabled) "dark" else "light"))
                            }
                        )
                    }
                }
            }
        }

        // Quick dialogs from Dashboard
        if (showQuickAddIdeaDialog) {
            NoteEditDialog(
                initialNote = null,
                onDismiss = { showQuickAddIdeaDialog = false },
                onSave = {
                    repository.addNote(it)
                    showQuickAddIdeaDialog = false
                }
            )
        }

        if (showQuickAddEventDialog) {
            EventEditDialog(
                initialEvent = null,
                defaultDate = com.example.pastoral.data.model.currentDateString(),
                availablePersons = pastoralData.persons,
                onDismiss = { showQuickAddEventDialog = false },
                onSave = {
                    repository.addEvent(it)
                    showQuickAddEventDialog = false
                }
            )
        }

        if (showQuickAddTaskDialog) {
            TaskEditDialog(
                initialTask = null,
                defaultDate = com.example.pastoral.data.model.currentDateString(),
                onDismiss = { showQuickAddTaskDialog = false },
                onSave = {
                    repository.addTask(it)
                    showQuickAddTaskDialog = false
                }
            )
        }

        // Onboarding dialog shown on fresh install
        if (!pastoralData.settings.isConfigured && pastoralData.settings.pastorName.isBlank()) {
            com.example.pastoral.ui.components.InitialSetupDialog(
                currentSettings = pastoralData.settings,
                onSaveSettings = { updated ->
                    repository.updateSettings(updated)
                },
                onLoadDemoData = {
                    repository.resetToDemoData()
                }
            )
        }
    }
}
