package com.example.pastoral.data.model

import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Note(
    val id: String = generateShortId("n"),
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
    val createdAt: String = currentIsoDate(),
    val updatedAt: String = currentIsoDate()
)

@JsonClass(generateAdapter = true)
data class Event(
    val id: String = generateShortId("e"),
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val location: String = "",
    val category: String = "Otro", // Misa, Retiro, Reunión, Campaña, Formación, Convivencia, Otro
    val description: String = "",
    val attendance: List<String> = emptyList() // Names of attendees
)

@JsonClass(generateAdapter = true)
data class Task(
    val id: String = generateShortId("t"),
    val title: String,
    val due: String? = null, // YYYY-MM-DD
    val priority: String = "media", // baja, media, alta
    val notes: String = "",
    val done: Boolean = false,
    val createdAt: String = currentIsoDate()
)

@JsonClass(generateAdapter = true)
data class Group(
    val id: String = generateShortId("g"),
    val name: String,
    val leader: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class Person(
    val id: String = generateShortId("p"),
    val name: String,
    val role: String = "Alumno", // Docente, Alumno, Catequista, Coordinador, etc.
    val group: String = "",
    val contact: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class PastoralSettings(
    val pastorName: String = "",
    val schoolName: String = "",
    val theme: String = "light", // light, dark
    val isConfigured: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ChatItem(
    val role: String, // user, assistant
    val content: String,
    val actionType: String? = null, // "event_created", "task_created", "note_created", "task_completed", "person_added", "group_added", "settings_updated"
    val actionTitle: String? = null,
    val actionDetails: String? = null,
    val targetTab: Int? = null // 1: Eventos, 2: Tareas, 3: Notas, 4: Comunidad, 0: Inicio
)

@JsonClass(generateAdapter = true)
data class PastoralData(
    val notes: List<Note> = emptyList(),
    val events: List<Event> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val groups: List<Group> = emptyList(),
    val persons: List<Person> = emptyList(),
    val chat: List<ChatItem> = emptyList(),
    val settings: PastoralSettings = PastoralSettings()
)

object PastoralConstants {
    val EVENT_CATEGORIES = listOf(
        "Misa",
        "Retiro",
        "Reunión",
        "Campaña",
        "Formación",
        "Convivencia",
        "Otro"
    )

    val PRIORITIES = listOf(
        "alta",
        "media",
        "baja"
    )

    val ROLES = listOf(
        "Alumno",
        "Docente",
        "Catequista",
        "Coordinador",
        "Padre de familia",
        "Exalumno",
        "Sacerdote / Asesor"
    )

    val TEMPLATES = listOf(
        TemplateItem(
            id = "reunion",
            name = "Nota de reunión",
            icon = "people",
            description = "Fecha · Asistentes · Temas · Acuerdos · Próximos pasos",
            defaultTitle = "Reunión de Pastoral - ",
            defaultBody = """
                Fecha: [YYYY-MM-DD]
                Asistentes: 
                - 
                
                Temas tratados:
                1. 
                2. 
                
                Acuerdos:
                - 
                
                Próximos pasos:
                - 
            """.trimIndent(),
            suggestedTags = listOf("reunión", "acuerdos")
        ),
        TemplateItem(
            id = "ficha",
            name = "Ficha de persona",
            icon = "person",
            description = "Acompañamiento personal y observaciones (respetando privacidad)",
            defaultTitle = "Ficha pastoral: ",
            defaultBody = """
                Nombre: 
                Rol: [Alumno / Docente / Catequista]
                Grupo: 
                Contacto: 
                
                Situación / Acompañamiento:
                - 
                
                Observaciones pastorales:
                - 
            """.trimIndent(),
            suggestedTags = listOf("ficha", "acompañamiento")
        ),
        TemplateItem(
            id = "acta",
            name = "Acta de reunión",
            icon = "description",
            description = "Acta formal: Orden del día, desarrollo, acuerdos y firmas",
            defaultTitle = "Acta Nº - Reunión Equipo Pastoral",
            defaultBody = """
                Fecha: [YYYY-MM-DD]  Hora: [HH:MM]
                Lugar: 
                Asistentes: 
                
                Orden del día:
                1. Oración inicial
                2. Lectura del acta anterior
                3. Puntos centrales
                
                Desarrollo:
                - 
                
                Acuerdos y resoluciones:
                - 
                
                Firmas de conformidad:
                - Responsable: 
                - Secretario/a: 
            """.trimIndent(),
            suggestedTags = listOf("acta", "formal")
        ),
        TemplateItem(
            id = "retiro",
            name = "Planificación de retiro",
            icon = "self_improvement",
            description = "Tema, objetivos, cronograma (bienvenida, dinámicas, oración) y responsables",
            defaultTitle = "Planificación: Retiro ",
            defaultBody = """
                Tema central: 
                Fecha: [YYYY-MM-DD]
                Lugar: [Casa de retiros / Capilla]
                Destinatarios: 
                
                Objetivos del retiro:
                1. 
                2. 
                
                Cronograma:
                - 08:30 Bienvenida y acogida
                - 09:15 Dinámica de integración
                - 10:30 Charla y reflexión central
                - 11:45 Desierto personal y silencio
                - 13:00 Almuerzo fraterno
                - 14:30 Taller por comunidades
                - 16:00 Adoración / Celebración penitencial
                - 17:30 Misa de clausura y envío
                
                Materiales necesarios:
                - Guías, velas, música, cantoral, botiquín
                
                Responsables de comisiones:
                - Liturgia: 
                - Logística: 
                - Dinámicas: 
            """.trimIndent(),
            suggestedTags = listOf("retiro", "planificación", "espiritualidad")
        )
    )
}

data class TemplateItem(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val defaultTitle: String,
    val defaultBody: String,
    val suggestedTags: List<String>
)

fun generateShortId(prefix: String = "item"): String {
    val uuidPart = UUID.randomUUID().toString().replace("-", "").take(5).lowercase()
    return "$prefix$uuidPart"
}

fun currentIsoDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    return sdf.format(Date())
}

fun currentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

data class ImportSummary(
    val notesCount: Int,
    val eventsCount: Int,
    val tasksCount: Int,
    val groupsCount: Int,
    val personsCount: Int
)
