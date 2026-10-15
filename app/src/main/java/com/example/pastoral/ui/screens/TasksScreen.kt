package com.example.pastoral.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.data.model.PastoralConstants
import com.example.pastoral.data.model.Task
import com.example.pastoral.data.model.currentDateString
import com.example.pastoral.data.model.generateShortId
import com.example.pastoral.ui.components.PastoralInlineMarkdown
import com.example.pastoral.ui.components.PriorityBadge
import com.example.ui.theme.CrimsonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    onAddTask: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onToggleTaskDone: (String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var selectedFilterTab by remember { mutableIntStateOf(1) } // 0: Todas, 1: Pendientes, 2: Completadas
    var quickTaskTitle by remember { mutableStateOf("") }
    var quickTaskPriority by remember { mutableStateOf("media") }

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val today = currentDateString()

    val filteredTasks = remember(tasks, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> tasks.filter { !it.done }
            2 -> tasks.filter { it.done }
            else -> tasks
        }.sortedWith(compareBy<Task> { it.done }
            .thenBy {
                when (it.priority) {
                    "alta" -> 0
                    "media" -> 1
                    else -> 2
                }
            }
            .thenBy { it.due ?: "9999-99-99" }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva Tarea")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("tasks_screen")
        ) {
            // Filter Tabs (Todas, Pendientes, Completadas)
            TabRow(selectedTabIndex = selectedFilterTab) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = {
                        Text(
                            text = "Todas (${tasks.size})",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = {
                        Text(
                            text = "Pendientes (${tasks.count { !it.done }})",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.5.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                )
                Tab(
                    selected = selectedFilterTab == 2,
                    onClick = { selectedFilterTab = 2 },
                    text = {
                        Text(
                            text = "Hechas (${tasks.count { it.done }})",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            // Quick task inline bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickTaskTitle,
                        onValueChange = { quickTaskTitle = it },
                        placeholder = { Text("Añadir tarea rápida...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_task_input")
                    )

                    IconButton(
                        onClick = {
                            if (quickTaskTitle.isNotBlank()) {
                                onAddTask(
                                    Task(
                                        title = quickTaskTitle.trim(),
                                        priority = quickTaskPriority,
                                        due = today
                                    )
                                )
                                quickTaskTitle = ""
                            }
                        },
                        enabled = quickTaskTitle.isNotBlank(),
                        modifier = Modifier.testTag("quick_task_submit")
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Guardar",
                            tint = if (quickTaskTitle.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Task List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedFilterTab == 1) "¡No hay tareas pendientes!" else "Sin tareas registradas.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            today = today,
                            onToggle = { onToggleTaskDone(task.id) },
                            onEdit = { taskToEdit = task },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // Add or Edit Dialog
    if (showAddDialog || taskToEdit != null) {
        val editing = taskToEdit
        TaskEditDialog(
            initialTask = editing,
            defaultDate = today,
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onSave = { savedTask ->
                if (editing != null) {
                    onUpdateTask(savedTask)
                } else {
                    onAddTask(savedTask)
                }
                showAddDialog = false
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    today: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    PastoralInlineMarkdown(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.due != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isOverdue = !task.done && task.due < today
                    val isToday = task.due == today
                    Text(
                        text = when {
                            isOverdue -> "⚠️ Venció el ${task.due}"
                            isToday -> "⏰ Vence HOY"
                            else -> "📅 Vence: ${task.due}"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isOverdue || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) CrimsonRed else if (isToday) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                PriorityBadge(priority = task.priority)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = CrimsonRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    initialTask: Task?,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var due by remember { mutableStateOf(initialTask?.due ?: defaultDate) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "media") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var hasDueDate by remember { mutableStateOf(initialTask?.due != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTask != null) "Editar Tarea" else "Nueva Tarea")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Prioridad:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PastoralConstants.PRIORITIES.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasDueDate = !hasDueDate }
                ) {
                    Checkbox(checked = hasDueDate, onCheckedChange = { hasDueDate = it })
                    Text("Asignar fecha límite", style = MaterialTheme.typography.bodyMedium)
                }

                if (hasDueDate) {
                    OutlinedTextField(
                        value = due,
                        onValueChange = { due = it },
                        label = { Text("Fecha de vencimiento (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o detalles") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = Task(
                            id = initialTask?.id ?: generateShortId("t"),
                            title = title.trim(),
                            due = if (hasDueDate && due.isNotBlank()) due.trim() else null,
                            priority = priority,
                            notes = notes.trim(),
                            done = initialTask?.done ?: false
                        )
                        onSave(task)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
