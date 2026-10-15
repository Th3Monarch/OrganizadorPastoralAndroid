package com.example.pastoral.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.data.model.*
import com.example.pastoral.ui.components.CategoryBadge
import com.example.pastoral.ui.components.PastoralInlineMarkdown
import com.example.ui.theme.CrimsonRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    events: List<Event>,
    persons: List<Person>,
    onAddEvent: (Event) -> Unit,
    onUpdateEvent: (Event) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Calendar state: Selected date (YYYY-MM-DD)
    var selectedDate by remember { mutableStateOf(currentDateString()) }
    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    val currentMonthLabel = remember(currentMonthCalendar) {
        monthFormat.format(currentMonthCalendar.time).replaceFirstChar { it.uppercase() }
    }

    // Days in current month
    val daysInMonth = remember(currentMonthCalendar) {
        val cal = currentMonthCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        (1..maxDay).map { day ->
            val dayStr = if (day < 10) "0$day" else "$day"
            "$monthStr-$dayStr"
        }
    }

    val eventDatesSet = remember(events) {
        events.map { it.date }.toSet()
    }

    // Filter events
    val filteredEvents = remember(events, selectedCategory, selectedDate) {
        events.filter { event ->
            val matchCat = selectedCategory == null || event.category.equals(selectedCategory, ignoreCase = true)
            matchCat
        }.sortedBy { it.date + (it.time ?: "00:00") }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_event_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Evento")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("events_screen")
        ) {
            // Month Header & Navigation
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val nextCal = currentMonthCalendar.clone() as Calendar
                            nextCal.add(Calendar.MONTH, -1)
                            currentMonthCalendar = nextCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                        }

                        Text(
                            text = currentMonthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = {
                                val nextCal = currentMonthCalendar.clone() as Calendar
                                nextCal.add(Calendar.MONTH, 1)
                                currentMonthCalendar = nextCal
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                            }

                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir agenda")
                            }
                        }
                    }

                    // Days carousel
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(daysInMonth) { dateStr ->
                            val isSelected = dateStr == selectedDate
                            val hasEvent = eventDatesSet.contains(dateStr)
                            val dayNumber = dateStr.takeLast(2)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 56.dp)
                                    .clickable { selectedDate = dateStr }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (hasEvent) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todas") }
                )
                PastoralConstants.EVENT_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Events List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selectedDayEvents = filteredEvents.filter { it.date == selectedDate }
                val otherEvents = filteredEvents.filter { it.date != selectedDate }

                if (selectedDayEvents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Eventos del día seleccionado ($selectedDate)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(selectedDayEvents, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onEdit = { eventToEdit = event },
                            onDelete = { onDeleteEvent(event.id) },
                            onShare = { shareEventText(context, event) }
                        )
                    }
                }

                item {
                    Text(
                        text = "Todos los eventos agendados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (otherEvents.isEmpty() && selectedDayEvents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay eventos en esta categoría",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(otherEvents, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onEdit = { eventToEdit = event },
                            onDelete = { onDeleteEvent(event.id) },
                            onShare = { shareEventText(context, event) }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Adding or Editing Event
    if (showAddDialog || eventToEdit != null) {
        val editing = eventToEdit
        EventEditDialog(
            initialEvent = editing,
            defaultDate = selectedDate,
            availablePersons = persons,
            onDismiss = {
                showAddDialog = false
                eventToEdit = null
            },
            onSave = { savedEvent ->
                if (editing != null) {
                    onUpdateEvent(savedEvent)
                } else {
                    onAddEvent(savedEvent)
                }
                showAddDialog = false
                eventToEdit = null
            }
        )
    }

    // Share / Export Agenda Dialog
    if (showExportDialog) {
        AgendaShareDialog(
            events = events,
            currentMonthLabel = currentMonthLabel,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                CategoryBadge(category = event.category)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📅 ${event.date}${event.time?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.location.isNotBlank()) {
                    Text(
                        text = "📍 ${event.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                PastoralInlineMarkdown(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (event.attendance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.attendance.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = CrimsonRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditDialog(
    initialEvent: Event?,
    defaultDate: String,
    availablePersons: List<Person>,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var date by remember { mutableStateOf(initialEvent?.date ?: defaultDate) }
    var time by remember { mutableStateOf(initialEvent?.time ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var category by remember { mutableStateOf(initialEvent?.category ?: "Misa") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var selectedAttendees by remember { mutableStateOf(initialEvent?.attendance?.toSet() ?: emptySet()) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEvent != null) "Editar Evento" else "Nuevo Evento Pastoral")
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
                    label = { Text("Título del Evento *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Hora (HH:MM)") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        PastoralConstants.EVENT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lugar (ej. Capilla, Patio Central)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o lema") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Attendance multi-select
                if (availablePersons.isNotEmpty()) {
                    Text(
                        text = "Lista de Asistencia (Comunidad):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    availablePersons.forEach { person ->
                        val isChecked = selectedAttendees.contains(person.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAttendees = if (isChecked) {
                                        selectedAttendees - person.name
                                    } else {
                                        selectedAttendees + person.name
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedAttendees = if (checked) {
                                        selectedAttendees + person.name
                                    } else {
                                        selectedAttendees - person.name
                                    }
                                }
                            )
                            Text(text = "${person.name} (${person.role})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && date.isNotBlank()) {
                        val event = Event(
                            id = initialEvent?.id ?: generateShortId("e"),
                            title = title.trim(),
                            date = date.trim(),
                            time = if (time.isNotBlank()) time.trim() else null,
                            location = location.trim(),
                            category = category,
                            description = description.trim(),
                            attendance = selectedAttendees.toList()
                        )
                        onSave(event)
                    }
                },
                enabled = title.isNotBlank() && date.isNotBlank()
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

@Composable
fun AgendaShareDialog(
    events: List<Event>,
    currentMonthLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val agendaText = remember(events, currentMonthLabel) {
        val sb = StringBuilder()
        sb.append("✝ AGENDA PASTORAL · $currentMonthLabel\n\n")
        events.forEach { ev ->
            val timeStr = ev.time?.let { " - $it" } ?: ""
            val locStr = if (ev.location.isNotBlank()) " | Lugar: ${ev.location}" else ""
            sb.append("• ${ev.date}$timeStr: «${ev.title}» [${ev.category}]$locStr\n")
            if (ev.description.isNotBlank()) {
                sb.append("  ${ev.description}\n")
            }
            if (ev.attendance.isNotEmpty()) {
                sb.append("  Asistencia: ${ev.attendance.joinToString(", ")}\n")
            }
            sb.append("\n")
        }
        sb.append("Generado por Organizador Pastoral.")
        sb.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir Agenda Pastoral") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Texto formateado listo para compartir por WhatsApp, correo o guardar:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = agendaText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Agenda Pastoral - $currentMonthLabel")
                    putExtra(Intent.EXTRA_TEXT, agendaText)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir Agenda"))
                onDismiss()
            }) {
                Text("Compartir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

fun shareEventText(context: Context, event: Event) {
    val text = buildString {
        append("✝ Evento Pastoral: «${event.title}»\n")
        append("📅 Fecha: ${event.date}${event.time?.let { " $it" } ?: ""}\n")
        append("🏷 Categoría: ${event.category}\n")
        if (event.location.isNotBlank()) append("📍 Lugar: ${event.location}\n")
        if (event.description.isNotBlank()) append("📝 ${event.description}\n")
        if (event.attendance.isNotEmpty()) append("👥 Asistentes: ${event.attendance.joinToString(", ")}\n")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir Evento"))
}
