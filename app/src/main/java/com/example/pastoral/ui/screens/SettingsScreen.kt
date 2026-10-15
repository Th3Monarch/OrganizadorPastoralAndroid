package com.example.pastoral.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.data.model.PastoralSettings
import com.example.pastoral.data.repository.PastoralRepository
import com.example.ui.theme.CrimsonRed
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: PastoralSettings,
    repository: PastoralRepository,
    authManager: com.example.pastoral.auth.AuthManager? = null,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = authManager?.currentUser

    var pastorNameInput by remember(settings.pastorName) { mutableStateOf(settings.pastorName) }
    var schoolNameInput by remember(settings.schoolName) { mutableStateOf(settings.schoolName) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPromptMaestroDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ajustes y Configuración",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 0. Cuenta Google y Nube Firebase Firestore
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cuenta y Nube Firestore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF16A34A), androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sincronizado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currentUser?.displayName?.take(1)?.uppercase() ?: "✝",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Usuario Pastoral",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "Conectado mediante Google",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            repository.syncWithFirestore()
                            Toast.makeText(context, "Sincronización con Firestore completada", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sincronizar")
                    }

                    if (authManager != null) {
                        Button(
                            onClick = {
                                authManager.signOut()
                                Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión")
                        }
                    }
                }
            }
        }

        // 1. Personalización pastoral
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Identidad Pastoral",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = pastorNameInput,
                    onValueChange = { pastorNameInput = it },
                    label = { Text("Nombre del Responsable Pastoral") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = schoolNameInput,
                    onValueChange = { schoolNameInput = it },
                    label = { Text("Nombre del Colegio o Institución") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        repository.updateSettings(
                            settings.copy(
                                pastorName = pastorNameInput.trim(),
                                schoolName = schoolNameInput.trim()
                            )
                        )
                        Toast.makeText(context, "Ajustes guardados correctamente", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Cambios")
                }
            }
        }

        // 2. Apariencia
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tema Oscuro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isDarkTheme) "Activado (nocturno)" else "Desactivado (modo claro)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme(it) }
                )
            }
        }

        // 3. Respaldo y Migración JSON (100% compatible con Windows app y Gemini Gem)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Respaldos y Migración (pastoral-data.json)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "El archivo JSON es compatible de ida y vuelta al 100% con la app de escritorio de Windows y las instrucciones del Gem de Gemini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar JSON")
                    }
                }

                FilledTonalButton(
                    onClick = { showPromptMaestroDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Prompt Maestro para Gemini Gem")
                }
            }
        }

        // 4. Zona de Datos de Ejemplo y Zona de Riesgo
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Gestión de Datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar Datos de Ejemplo Oficiales")
                }

                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar Todos los Datos (Zona de Riesgo)")
                }
            }
        }

        // Footer info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "« Lo poco y fiel » · Organizador Pastoral v1.0.0\nAdaptación para Android · Octubre de 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // Export Dialog
    if (showExportDialog) {
        val exportedJson = remember { repository.exportJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar pastoral-data.json") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Copia este bloque o compártelo para guardarlo en Google Drive o abrirlo en Windows:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportedJson,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("pastoral-data.json", exportedJson))
                    Toast.makeText(context, "JSON copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("Copiar al portapapeles")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "pastoral-data.json")
                        putExtra(Intent.EXTRA_TEXT, exportedJson)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Guardar o Enviar Respaldo"))
                    showExportDialog = false
                }) {
                    Text("Compartir")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var inputJson by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Respaldo JSON") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Pega aquí el contenido de tu archivo pastoral-data.json:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputJson,
                        onValueChange = {
                            inputJson = it
                            errorMsg = null
                        },
                        placeholder = { Text("{\"notes\": [...], \"events\": [...], ...}") },
                        minLines = 8,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = CrimsonRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = repository.importJson(inputJson.trim())
                            result.onSuccess { summary ->
                                Toast.makeText(
                                    context,
                                    "Importados: ${summary.notesCount} notas, ${summary.eventsCount} eventos, ${summary.tasksCount} tareas, ${summary.groupsCount} grupos, ${summary.personsCount} personas",
                                    Toast.LENGTH_LONG
                                ).show()
                                showImportDialog = false
                            }.onFailure { err ->
                                errorMsg = "Error al importar: ${err.localizedMessage}"
                            }
                        }
                    },
                    enabled = inputJson.isNotBlank()
                ) {
                    Text("Validar e Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Reset Demo Data Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Restaurar Datos de Ejemplo") },
            text = {
                Text("Esta acción recargará los datos modelo de la pastoral (Colegio San José, retiro de 4to año, campaña solidaria, grupos y personas).")
            },
            confirmButton = {
                Button(onClick = {
                    repository.resetToDemoData()
                    Toast.makeText(context, "Datos de ejemplo restaurados", Toast.LENGTH_SHORT).show()
                    showResetConfirmDialog = false
                }) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("¿Borrar todos los datos?") },
            text = {
                Text("Se eliminarán todas las notas, eventos, tareas y personas registradas. Esta acción no se puede deshacer a menos que tengas un respaldo JSON exportado.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearAllData()
                        Toast.makeText(context, "Se borraron todos los datos", Toast.LENGTH_SHORT).show()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Sí, borrar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Prompt Maestro Dialog (Section 6)
    if (showPromptMaestroDialog) {
        val promptMaestroText = PROMPT_MAESTRO_TEXT
        AlertDialog(
            onDismissRequest = { showPromptMaestroDialog = false },
            title = { Text("Prompt Maestro para Gemini Gem") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Este es el prompt oficial de la Sección 6 para crear tu Gem pastoral en Google Gemini. Puedes copiarlo con un toque:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = promptMaestroText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt Maestro Pastoral", promptMaestroText))
                    Toast.makeText(context, "Prompt maestro copiado al portapapeles", Toast.LENGTH_LONG).show()
                    showPromptMaestroDialog = false
                }) {
                    Text("Copiar Prompt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromptMaestroDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

val PROMPT_MAESTRO_TEXT = """
[ROL]
Eres "Organizador Pastoral", el asistente personal de la pastoral escolar de tu usuario. Trabajas en español. Si aún no lo sabes, pregunta UNA sola vez al empezar: (1) tu nombre como responsable, (2) el nombre del colegio. Guárdalos y úsalos en el saludo y en los respaldos.

[PERSONALIDAD Y ESTILO]
- Cálido, práctico, respetuoso y organizado. Lenguaje apto para pastoral escolar/católica.
- Respuestas breves: lista corta o párrafo. Máximo UNA pregunta por mensaje.
- No prediques ni des sermones largos a menos que te lo pidan. Puedes cerrar con una frase breve de ánimo.
- Nunca inventes nombres, fechas ni datos. Si falta algo indispensable, pídelo antes de guardar.

[ENTIDADES Y ESQUEMA]
notes: id, title, body, tags[]
events: id, title, date (YYYY-MM-DD), time (HH:MM, opcional), location, category, description, attendance (nombres en texto, lista)
tasks: id, title, due (YYYY-MM-DD, opcional), priority (baja | media | alta), notes, done (boolean)
groups: id, name, leader, description
persons: id, name, role, group, contact, notes
Categorías válidas de evento: Misa, Retiro, Reunión, Campaña, Formación, Convivencia, Otro.

[COMANDOS RÁPIDOS]
/nota /evento /tarea /grupo /persona → captura guiada paso a paso
/hoy /semana /buscar X /lista → consultas y panel
/exportar /importar → respaldo JSON para la app de escritorio
/plantillas /limpiar /ayuda → utilidades
""".trimIndent()
