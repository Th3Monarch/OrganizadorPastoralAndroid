package com.example.pastoral.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.data.model.Group
import com.example.pastoral.data.model.PastoralConstants
import com.example.pastoral.data.model.Person
import com.example.pastoral.data.model.generateShortId
import com.example.pastoral.ui.components.RoleBadge
import com.example.ui.theme.CrimsonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    groups: List<Group>,
    persons: List<Person>,
    onAddGroup: (Group) -> Unit,
    onUpdateGroup: (Group) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onAddPerson: (Person) -> Unit,
    onUpdatePerson: (Person) -> Unit,
    onDeletePerson: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Personas, 1: Grupos
    var searchQuery by remember { mutableStateOf("") }

    var showPersonDialog by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<Person?>(null) }

    var showGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }

    val filteredPersons = remember(persons, searchQuery) {
        if (searchQuery.isBlank()) persons
        else persons.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.role.contains(searchQuery, ignoreCase = true) ||
            it.group.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.leader.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showPersonDialog = true else showGroupDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_community_fab")
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.PersonAdd else Icons.Default.GroupAdd,
                    contentDescription = "Agregar"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("community_screen")
        ) {
            // Tabs: Personas vs Grupos
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Personas (${persons.size})",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Grupos (${groups.size})",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            // Search bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (selectedTab == 0) "Buscar persona, rol o grupo..." else "Buscar grupo o líder...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Privacy guidance banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = "Privacidad",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Protección pastoral: Minimiza datos sensibles de menores. Con nombre, rol y grupo basta para la gestión pastoral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content List
            if (selectedTab == 0) {
                // Persons List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredPersons.isEmpty()) {
                        item {
                            EmptyCommunityCard(message = "No se encontraron personas.")
                        }
                    } else {
                        items(filteredPersons, key = { it.id }) { person ->
                            PersonCard(
                                person = person,
                                onEdit = { personToEdit = person },
                                onDelete = { onDeletePerson(person.id) },
                                onContact = {
                                    if (person.contact.isNotBlank()) {
                                        openContactIntent(context, person.contact)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Groups List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredGroups.isEmpty()) {
                        item {
                            EmptyCommunityCard(message = "No hay grupos pastorales registrados.")
                        }
                    } else {
                        items(filteredGroups, key = { it.id }) { group ->
                            val memberCount = persons.count { it.group.equals(group.name, ignoreCase = true) }
                            GroupCard(
                                group = group,
                                memberCount = memberCount,
                                onEdit = { groupToEdit = group },
                                onDelete = { onDeleteGroup(group.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Person Dialog
    if (showPersonDialog || personToEdit != null) {
        val editing = personToEdit
        PersonEditDialog(
            initialPerson = editing,
            availableGroups = groups.map { it.name },
            onDismiss = {
                showPersonDialog = false
                personToEdit = null
            },
            onSave = { savedPerson ->
                if (editing != null) {
                    onUpdatePerson(savedPerson)
                } else {
                    onAddPerson(savedPerson)
                }
                showPersonDialog = false
                personToEdit = null
            }
        )
    }

    // Group Dialog
    if (showGroupDialog || groupToEdit != null) {
        val editing = groupToEdit
        GroupEditDialog(
            initialGroup = editing,
            onDismiss = {
                showGroupDialog = false
                groupToEdit = null
            },
            onSave = { savedGroup ->
                if (editing != null) {
                    onUpdateGroup(savedGroup)
                } else {
                    onAddGroup(savedGroup)
                }
                showGroupDialog = false
                groupToEdit = null
            }
        )
    }
}

@Composable
fun PersonCard(
    person: Person,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onContact: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
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
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                RoleBadge(role = person.role)
            }

            if (person.group.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👥 Grupo: ${person.group}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (person.contact.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onContact() }
                ) {
                    Icon(
                        Icons.Default.ContactPhone,
                        contentDescription = "Contacto",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = person.contact,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (person.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = person.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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

@Composable
fun GroupCard(
    group: Group,
    memberCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
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
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$memberCount integrantes",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (group.leader.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🌟 Coordinador/a: ${group.leader}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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

@Composable
fun EmptyCommunityCard(message: String) {
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
                Icons.Outlined.People,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditDialog(
    initialPerson: Person?,
    availableGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (Person) -> Unit
) {
    var name by remember { mutableStateOf(initialPerson?.name ?: "") }
    var role by remember { mutableStateOf(initialPerson?.role ?: "Alumno") }
    var group by remember { mutableStateOf(initialPerson?.group ?: "") }
    var contact by remember { mutableStateOf(initialPerson?.contact ?: "") }
    var notes by remember { mutableStateOf(initialPerson?.notes ?: "") }

    var roleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPerson != null) "Editar Persona" else "Registrar Persona") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo *") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol pastoral") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        PastoralConstants.ROLES.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Grupo o Comunidad asignada") },
                    placeholder = { Text(if (availableGroups.isNotEmpty()) availableGroups.first() else "Pastoral Juvenil") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contacto (correo o teléfono)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Acompañamiento") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val person = Person(
                            id = initialPerson?.id ?: generateShortId("p"),
                            name = name.trim(),
                            role = role,
                            group = group.trim(),
                            contact = contact.trim(),
                            notes = notes.trim()
                        )
                        onSave(person)
                    }
                },
                enabled = name.isNotBlank()
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
fun GroupEditDialog(
    initialGroup: Group?,
    onDismiss: () -> Unit,
    onSave: (Group) -> Unit
) {
    var name by remember { mutableStateOf(initialGroup?.name ?: "") }
    var leader by remember { mutableStateOf(initialGroup?.leader ?: "") }
    var description by remember { mutableStateOf(initialGroup?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGroup != null) "Editar Grupo" else "Nuevo Grupo Pastoral") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Grupo *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = leader,
                    onValueChange = { leader = it },
                    label = { Text("Coordinador/a del Grupo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del apostolado") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val group = Group(
                            id = initialGroup?.id ?: generateShortId("g"),
                            name = name.trim(),
                            leader = leader.trim(),
                            description = description.trim()
                        )
                        onSave(group)
                    }
                },
                enabled = name.isNotBlank()
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

fun openContactIntent(context: Context, contact: String) {
    try {
        if (contact.contains("@")) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contact"))
            context.startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contact"))
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback share text
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, contact)
        }
        context.startActivity(Intent.createChooser(intent, "Contactar"))
    }
}
