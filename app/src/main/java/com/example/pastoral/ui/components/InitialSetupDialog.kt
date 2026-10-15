package com.example.pastoral.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pastoral.data.model.PastoralSettings

@Composable
fun InitialSetupDialog(
    currentSettings: PastoralSettings,
    onSaveSettings: (PastoralSettings) -> Unit,
    onLoadDemoData: () -> Unit
) {
    var pastorName by remember { mutableStateOf(currentSettings.pastorName) }
    var schoolName by remember { mutableStateOf(currentSettings.schoolName) }

    AlertDialog(
        onDismissRequest = { /* Modal must be completed or skipped */ },
        modifier = Modifier.testTag("initial_setup_dialog"),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✝", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Bienvenido a tu Organizador Pastoral",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Configura tu institución para comenzar con tu base de datos limpia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = pastorName,
                    onValueChange = { pastorName = it },
                    label = { Text("Tu nombre como responsable *") },
                    placeholder = { Text("Ej. Gabriel Morales / Hna. Teresa") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pastor_name_input")
                )

                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("Colegio, Parroquia o Institución *") },
                    placeholder = { Text("Ej. Colegio San José / Parroquia") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_school_name_input")
                )

                Text(
                    text = "La aplicación se iniciará completamente limpia para que tú y tu equipo la llenen con sus propios eventos, tareas, notas y personas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (pastorName.isNotBlank()) pastorName.trim() else "Responsable Pastoral"
                    val finalSchool = if (schoolName.isNotBlank()) schoolName.trim() else "Pastoral Escolar"
                    onSaveSettings(
                        currentSettings.copy(
                            pastorName = finalName,
                            schoolName = finalSchool,
                            isConfigured = true
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_confirm_clean_button")
            ) {
                Text("Comenzar con Pastoral Limpia", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onLoadDemoData()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_load_demo_button")
            ) {
                Text("¿Quieres explorar con datos de ejemplo? (Opcional)")
            }
        }
    )
}
