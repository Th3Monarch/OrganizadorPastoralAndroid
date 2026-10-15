package com.example.pastoral.assistant

import com.example.pastoral.data.model.*
import com.example.pastoral.data.repository.PastoralRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class PastoralActionItem(
    val action: String? = null,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val category: String? = null,
    val description: String? = null,
    val due: String? = null,
    val priority: String? = null,
    val notes: String? = null,
    val body: String? = null,
    val tags: List<String>? = null,
    val query: String? = null,
    val name: String? = null,
    val role: String? = null,
    val group: String? = null,
    val contact: String? = null,
    val leader: String? = null,
    val pastorName: String? = null,
    val schoolName: String? = null,
    val theme: String? = null
)

data class AssistantExecutionResult(
    val replyText: String,
    val actionType: String? = null,
    val actionTitle: String? = null,
    val actionDetails: String? = null,
    val targetTab: Int? = null
)

class PastoralAssistantEngine(private val repository: PastoralRepository) {

    private val geminiClient = GeminiApiClient()
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val sdfDisplay = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
    private val sdfDayMonth = SimpleDateFormat("EEE d MMM", Locale("es", "ES"))

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listActionType = Types.newParameterizedType(List::class.java, PastoralActionItem::class.java)
    private val actionsAdapter = moshi.adapter<List<PastoralActionItem>>(listActionType)

    suspend fun processMessage(userInput: String): AssistantExecutionResult {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()
        val data = repository.data.value
        val settings = data.settings

        // 1. Command: /ayuda
        if (lower == "/ayuda" || lower == "ayuda" || lower == "¿qué comandos hay?" || lower == "que comandos hay") {
            return AssistantExecutionResult(
                replyText = """
                    ✝ **Guía y Comandos de tu Asistente Pastoral**:
                    
                    ⚡ **¡Tengo control total sobre la aplicación! Puedes pedirme directamente**:
                    • *"Haz un evento [nombre] el [fecha/día] a las [hora] en [lugar]"*
                    • *"Escríbeme una tarea: [título], para el [viernes/mañana], prioridad alta"*
                    • *"Crea una nota con una dinámica de acogida para jóvenes"*
                    • *"Marca como completada la tarea de comprar cirios"*
                    • *"Agrega a María López como catequista al grupo de Confirmación"*
                    • *"Borra la tarea / el evento [nombre]"*
                    
                    📋 **Comandos rápidos oficiales**:
                    • **/hoy** o "buenos días": Panel del día con eventos, vencidos y pendientes.
                    • **/semana**: Planificación semanal con 3 prioridades sugeridas.
                    • **/lista**: Conteos y estado general de la pastoral.
                    • **/buscar [término]**: Búsqueda global en eventos, tareas, notas y comunidad.
                    • **/plantillas**: Ver las 4 plantillas oficiales (reunión, ficha, acta, retiro).
                    • **/exportar**: Generar respaldo JSON oficial compatible con PC.
                    • **/limpiar**: Limpiar la conversación actual.
                    
                    «Lo poco y fiel: que la herramienta sirva a las personas y no al revés.»
                """.trimIndent()
            )
        }

        // 2. Intent: Identity registration ("Hola, me llamo X y mi colegio es Y")
        if (lower.contains("me llamo") || lower.contains("mi nombre es") || lower.contains("mi colegio es")) {
            val nameMatch = Regex("""(?:me llamo|mi nombre es)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+?)(?:,| y mi colegio| y el colegio| del colegio|\.|$|\n)""", RegexOption.IGNORE_CASE).find(userInput)
            val schoolMatch = Regex("""(?:mi colegio es|el colegio es|del colegio)\s+(?:el\s+)?([a-zA-ZáéíóúÁÉÍÓÚñÑ\s0-9]+?)(?:\.|$|\n)""", RegexOption.IGNORE_CASE).find(userInput)

            val parsedName = nameMatch?.groupValues?.getOrNull(1)?.trim()?.replaceFirstChar { it.uppercase() }
            val parsedSchool = schoolMatch?.groupValues?.getOrNull(1)?.trim()?.replaceFirstChar { it.uppercase() }

            if (!parsedName.isNullOrBlank() || !parsedSchool.isNullOrBlank()) {
                val updatedSettings = settings.copy(
                    pastorName = if (!parsedName.isNullOrBlank()) parsedName else (if (settings.pastorName.isNotBlank()) settings.pastorName else "Responsable"),
                    schoolName = if (!parsedSchool.isNullOrBlank()) parsedSchool else (if (settings.schoolName.isNotBlank()) settings.schoolName else "Institución"),
                    isConfigured = true
                )
                repository.updateSettings(updatedSettings)
                return AssistantExecutionResult(
                    replyText = "✔ **¡Un saludo fraternal, ${updatedSettings.pastorName}!** He guardado tus datos para **${updatedSettings.schoolName}**. Los utilizaré en tus saludos diarios, panel y respaldos oficiales.\n\n¿Deseas consultar tu agenda con **/hoy** o planificar con **/semana**?",
                    actionType = "settings_updated",
                    actionTitle = updatedSettings.pastorName,
                    actionDetails = "Institución: ${updatedSettings.schoolName}",
                    targetTab = 0
                )
            }
        }

        // 3. Command: /hoy
        if (lower == "/hoy" || lower == "buenos días" || lower == "buen dia" || (lower.startsWith("hola") && trimmed.length < 15)) {
            return AssistantExecutionResult(replyText = generateDailyBriefing(data))
        }

        // 4. Command: /semana
        if (lower == "/semana" || lower.contains("resumen de la semana") || lower.contains("semana pastoral")) {
            return AssistantExecutionResult(replyText = generateWeeklyBriefing(data))
        }

        // 5. Command: /lista
        if (lower == "/lista" || lower == "estadísticas" || lower == "estado general") {
            val pendingTasks = data.tasks.count { !it.done }
            val futureEvents = data.events.count { it.date >= currentDateString() }
            return AssistantExecutionResult(
                replyText = """
                    📊 **Estado General · ${settings.schoolName}**
                    Responsable: ${settings.pastorName}
                    
                    • 📅 **Eventos agendados**: ${data.events.size} ($futureEvents próximos)
                    • ✅ **Tareas activas**: $pendingTasks pendientes (${data.tasks.size} en total)
                    • 💡 **Ideas y notas**: ${data.notes.size} guardadas
                    • 👥 **Comunidad**: ${data.persons.size} personas en ${data.groups.size} grupos
                    
                    ¿Deseas revisar los eventos de hoy con **/hoy** o planificar con **/semana**?
                """.trimIndent()
            )
        }

        // 6. Command: /plantillas
        if (lower == "/plantillas" || lower == "plantillas") {
            return AssistantExecutionResult(
                replyText = """
                    📋 **Plantillas Pastorales Oficiales Disponibles**:
                    
                    1. **Nota de reunión**: Fecha · Asistentes · Temas · Acuerdos · Próximos pasos.
                       *(Escribe `/plantilla reunion` o pídeme "crea una nota de reunión")*
                       
                    2. **Ficha de persona**: Acompañamiento personal y pastoral (respetando datos sensibles).
                       *(Escribe `/plantilla ficha`)*
                       
                    3. **Acta de reunión**: Fecha, hora, lugar, orden del día, desarrollo, acuerdos y firmas.
                       *(Escribe `/plantilla acta`)*
                       
                    4. **Planificación de retiro**: Tema, destinatarios, objetivos, cronograma por horas y comisiones.
                       *(Escribe `/plantilla retiro`)*
                       
                    Pídeme directamente crear una nota con cualquiera de estas plantillas y la guardaré en tu sección de Notas.
                """.trimIndent()
            )
        }

        // Specific template command
        if (lower.startsWith("/plantilla") || lower.startsWith("plantilla ")) {
            val templateType = lower.removePrefix("/plantilla").removePrefix("plantilla").trim()
            val template = PastoralConstants.TEMPLATES.find { 
                it.id.contains(templateType) || templateType.contains(it.id) ||
                (templateType.contains("reuni") && it.id == "reunion")
            } ?: PastoralConstants.TEMPLATES.first()

            return AssistantExecutionResult(
                replyText = """
                    📄 **Plantilla: ${template.name}**
                    ${template.description}
                    
                    ```text
                    ${template.defaultBody}
                    ```
                    
                    ¿Quieres que cree una nota directamente con esta plantilla? Solo dime: *"Crea una nota con la plantilla de ${template.name}"*.
                """.trimIndent()
            )
        }

        // 7. Command: /exportar
        if (lower == "/exportar" || lower == "exportar json" || lower == "respaldo") {
            val json = repository.exportJson()
            return AssistantExecutionResult(
                replyText = """
                    Aquí tienes tu respaldo JSON completo oficial para la app de escritorio o Google Drive:
                    
                    ```json
                    $json
                    ```
                    
                    Tip: También puedes ir a **Ajustes** para copiarlo o compartirlo como archivo `.json`.
                """.trimIndent()
            )
        }

        // 8. Command: /buscar [query]
        if (lower.startsWith("/buscar") || lower.startsWith("buscar ")) {
            val query = lower.removePrefix("/buscar").removePrefix("buscar").trim()
            return AssistantExecutionResult(replyText = executeSearch(query, data))
        }

        // 9. Check for Community Leader Query
        if (lower.contains("quién coordina") || lower.contains("quien coordina") || lower.contains("coordinador de") || lower.contains("líder de")) {
            val foundGroup = data.groups.find { lower.contains(it.name.lowercase()) }
            if (foundGroup != null) {
                return AssistantExecutionResult(
                    replyText = "👥 El grupo **${foundGroup.name}** está coordinado por **${foundGroup.leader}**.\n\nDescripción: ${foundGroup.description}"
                )
            }
            val leaders = data.persons.filter { it.role.equals("Coordinador", ignoreCase = true) }
            if (leaders.isNotEmpty()) {
                val list = leaders.joinToString("\n") { "• **${it.name}** (coordina: ${it.group}, contacto: ${it.contact})" }
                return AssistantExecutionResult(replyText = "👥 Coordinadores registrados en la comunidad:\n$list")
            }
        }

        // 10. Check for Task Query
        if ((lower.contains("qué tareas") || lower.contains("que tareas") || lower.contains("mis tareas")) && !isTaskCreationIntent(lower)) {
            val pending = data.tasks.filter { !it.done }
            if (pending.isEmpty()) {
                return AssistantExecutionResult(replyText = "✅ ¡No tienes tareas pendientes en este momento! Todo al día.")
            }
            val list = pending.joinToString("\n") { t ->
                val dueStr = t.due?.let { " · Vence: $it" } ?: ""
                val pBadge = when (t.priority) {
                    "alta" -> "🔴 Alta"
                    "media" -> "🟡 Media"
                    else -> "🔵 Baja"
                }
                "• [${pBadge}] **${t.title}**$dueStr"
            }
            return AssistantExecutionResult(
                replyText = "📋 **Tareas pendientes (${pending.size})**:\n\n$list\n\nPuedes decirme: *«Marca como completada la tarea [nombre]»* y lo haré de inmediato.",
                targetTab = 2
            )
        }

        // 11. Check for Event Query
        if ((lower.contains("qué eventos") || lower.contains("que eventos") || lower.contains("proximos eventos") || lower.contains("próximos eventos")) && !isEventCreationIntent(lower)) {
            val today = currentDateString()
            val upcoming = data.events.filter { it.date >= today }.take(5)
            if (upcoming.isEmpty()) {
                return AssistantExecutionResult(replyText = "📅 No hay eventos próximos programados a partir de hoy.")
            }
            val list = upcoming.joinToString("\n") { ev ->
                val timeStr = ev.time?.let { " a las $it" } ?: ""
                val locStr = if (ev.location.isNotBlank()) " (${ev.location})" else ""
                "• **${ev.date}$timeStr**: «${ev.title}» [${ev.category}]$locStr"
            }
            return AssistantExecutionResult(replyText = "📅 **Próximos eventos agendados**:\n\n$list", targetTab = 1)
        }

        // --- ATTEMPT GEMINI GENERATIVE CALL FIRST IF ONLINE / CONFIGURED ---
        val geminiResult = geminiClient.generatePastoralResponse(userInput, data)
        if (geminiResult.isSuccess) {
            val rawGeminiReply = geminiResult.getOrNull()
            if (!rawGeminiReply.isNullOrBlank()) {
                // Check if Gemini returned an action block
                val extractedActions = extractActionBlock(rawGeminiReply)
                if (extractedActions.isNotEmpty()) {
                    val executionResult = executeActionList(extractedActions, data)
                    val cleanedText = cleanActionBlockFromText(rawGeminiReply)
                    return AssistantExecutionResult(
                        replyText = cleanedText.ifBlank { executionResult.replyText },
                        actionType = executionResult.actionType,
                        actionTitle = executionResult.actionTitle,
                        actionDetails = executionResult.actionDetails,
                        targetTab = executionResult.targetTab
                    )
                } else {
                    // Check if the user clearly had an action intent (e.g. "haz un evento", "escríbeme una tarea", "crea una nota")
                    // that Gemini answered conversationally without writing an action tag:
                    val localActionFallback = tryExecuteLocalIntent(userInput, lower, data)
                    if (localActionFallback != null) {
                        return AssistantExecutionResult(
                            replyText = "$rawGeminiReply\n\n${localActionFallback.replyText}",
                            actionType = localActionFallback.actionType,
                            actionTitle = localActionFallback.actionTitle,
                            actionDetails = localActionFallback.actionDetails,
                            targetTab = localActionFallback.targetTab
                        )
                    }
                    return AssistantExecutionResult(replyText = rawGeminiReply)
                }
            }
        }

        // --- LOCAL NLP ENGINE (Guarantees 100% offline action execution) ---
        val localExecution = tryExecuteLocalIntent(userInput, lower, data)
        if (localExecution != null) {
            return localExecution
        }

        // Fallback friendly reply
        val pName = if (settings.pastorName.isNotBlank()) "Estimado/a ${settings.pastorName}," else "Estimado responsable pastoral,"
        val sName = if (settings.schoolName.isNotBlank()) " para **${settings.schoolName}**" else ""
        return AssistantExecutionResult(
            replyText = """
                ✝ $pName he recibido tu mensaje.
                
                Tengo control completo sobre tu aplicación pastoral$sName:
                • Si me dices: **«Haz un evento [título] el [fecha/hora]»**, lo agregaré a tu calendario.
                • Si me dices: **«Escríbeme una tarea [título]»**, la crearé en tus tareas pendientes.
                • Si me dices: **«Crea una nota [título/idea]»**, la guardaré en tus ideas.
                • Si me dices: **«Marca completada la tarea [nombre]»**, la daré por lista.
                • También puedes consultar **/hoy**, **/semana** o pedirme redacción pastoral.
                
                ¿En qué puedo ayudarte en este momento?
            """.trimIndent()
        )
    }

    private fun isTaskCreationIntent(lower: String): Boolean {
        return lower.startsWith("/tarea") ||
                lower.contains("crear tarea") || lower.contains("crea una tarea") || lower.contains("crea tarea") ||
                lower.contains("escribeme una tarea") || lower.contains("escríbeme una tarea") || lower.contains("escribe una tarea") ||
                lower.contains("haz una tarea") || lower.contains("hazme una tarea") || lower.contains("hacer tarea") ||
                lower.contains("pon una tarea") || lower.contains("nueva tarea") || lower.contains("agrega tarea") ||
                lower.contains("agrega una tarea") || lower.contains("añade tarea") || lower.contains("añade una tarea") ||
                lower.contains("anota la tarea") || lower.contains("anota tarea") || lower.contains("ponme una tarea")
    }

    private fun isEventCreationIntent(lower: String): Boolean {
        return lower.startsWith("/evento") ||
                lower.contains("haz un evento") || lower.contains("hazme un evento") || lower.contains("hacer un evento") ||
                lower.contains("crea un evento") || lower.contains("crear evento") || lower.contains("crea evento") ||
                lower.contains("crear un evento") || lower.contains("agenda un evento") || lower.contains("agenda evento") ||
                lower.contains("agendar evento") || lower.contains("programa un evento") || lower.contains("programar evento") ||
                lower.contains("nuevo evento") || lower.contains("anota el evento") || lower.contains("anota un evento") ||
                lower.contains("agrega un evento") || lower.contains("añade un evento")
    }

    private fun isNoteCreationIntent(lower: String): Boolean {
        return lower.startsWith("/nota") ||
                lower.contains("crea una nota") || lower.contains("crear una nota") || lower.contains("crear nota") || lower.contains("crea la nota") ||
                lower.contains("escribeme una nota") || lower.contains("escríbeme una nota") || lower.contains("escribe una nota") ||
                lower.contains("haz una nota") || lower.contains("hazme una nota") || lower.contains("hacer una nota") ||
                lower.contains("tengo una idea") || lower.contains("guardar idea") || lower.contains("guarda esta idea") ||
                lower.contains("guarda la idea") || lower.contains("guardar nota") || lower.contains("guarda esta nota") ||
                lower.contains("anota esto") || lower.contains("anota:") || lower.contains("nota:")
    }

    private fun isTaskCompletionIntent(lower: String): Boolean {
        return lower.contains("marca como completada") || lower.contains("marcar como completada") ||
                lower.contains("marca como hecha") || lower.contains("marcar como hecha") ||
                lower.contains("marca como lista") || lower.contains("marcar como lista") ||
                lower.contains("completa la tarea") || lower.contains("completar la tarea") || lower.contains("completar tarea") ||
                lower.contains("terminé la tarea") || lower.contains("termine la tarea") || lower.contains("ya hice la tarea") ||
                lower.contains("tarea hecha") || lower.contains("tarea completada")
    }

    private fun isDeletionIntent(lower: String): Boolean {
        return lower.contains("elimina el evento") || lower.contains("borra el evento") || lower.contains("cancela el evento") ||
                lower.contains("elimina la tarea") || lower.contains("borra la tarea") || lower.contains("cancela la tarea") ||
                lower.contains("elimina la nota") || lower.contains("borra la nota")
    }

    private fun isCommunityIntent(lower: String): Boolean {
        return lower.contains("agrega a") && (lower.contains("a la comunidad") || lower.contains("como catequista") || lower.contains("como docente") || lower.contains("como alumno") || lower.contains("como coordinador")) ||
                lower.contains("registra a") || lower.contains("nuevo catequista") || lower.contains("nueva persona") ||
                lower.contains("crea el grupo") || lower.contains("crear grupo") || lower.contains("nuevo grupo")
    }

    private fun tryExecuteLocalIntent(userInput: String, lower: String, data: PastoralData): AssistantExecutionResult? {
        if (isTaskCreationIntent(lower)) {
            return parseAndCreateTask(userInput, data)
        }
        if (isEventCreationIntent(lower)) {
            return parseAndCreateEvent(userInput, data)
        }
        if (isNoteCreationIntent(lower)) {
            return parseAndCreateNote(userInput, data)
        }
        if (isTaskCompletionIntent(lower)) {
            return parseAndCompleteTask(userInput, data)
        }
        if (isDeletionIntent(lower)) {
            return parseAndDeleteItem(userInput, lower, data)
        }
        if (isCommunityIntent(lower)) {
            return parseAndManageCommunity(userInput, lower, data)
        }
        return null
    }

    // --- NLP ACTION PARSERS ---

    private fun parseAndCreateTask(input: String, data: PastoralData): AssistantExecutionResult {
        var clean = input
        val prefixes = listOf(
            "/tarea", "escribeme una tarea:", "escribeme una tarea", "escríbeme una tarea:", "escríbeme una tarea",
            "escribe una tarea:", "escribe una tarea", "crea una tarea:", "crea una tarea", "crear tarea:", "crear tarea",
            "haz una tarea:", "haz una tarea", "hazme una tarea:", "hazme una tarea", "pon una tarea:", "pon una tarea",
            "nueva tarea:", "nueva tarea", "agrega una tarea:", "agrega una tarea", "agrega tarea:", "agrega tarea",
            "añade una tarea:", "añade una tarea", "añade tarea:", "añade tarea", "anota la tarea:", "anota la tarea",
            "anota tarea:", "anota tarea"
        )
        for (pref in prefixes) {
            if (clean.startsWith(pref, ignoreCase = true)) {
                clean = clean.substring(pref.length).trim()
                break
            } else if (clean.contains(pref, ignoreCase = true)) {
                clean = clean.replace(pref, "", ignoreCase = true).trim()
                break
            }
        }

        var priority = "media"
        if (clean.contains("prioridad alta", ignoreCase = true) || clean.contains("urgente", ignoreCase = true) || clean.contains("alta prioridad", ignoreCase = true)) {
            priority = "alta"
            clean = clean.replace("prioridad alta", "", ignoreCase = true)
                .replace("alta prioridad", "", ignoreCase = true)
                .replace("urgente", "", ignoreCase = true)
        } else if (clean.contains("prioridad baja", ignoreCase = true) || clean.contains("baja prioridad", ignoreCase = true)) {
            priority = "baja"
            clean = clean.replace("prioridad baja", "", ignoreCase = true)
                .replace("baja prioridad", "", ignoreCase = true)
        }

        val parsedDate = extractDateFromString(clean)
        val due = parsedDate?.first
        if (parsedDate != null) {
            clean = parsedDate.second
        }

        val title = clean.trim(':', ' ', ',', '-', '.', ';')
        val finalTitle = if (title.isBlank()) "Tarea Pastoral" else title.replaceFirstChar { it.uppercase() }

        val task = Task(
            title = finalTitle,
            due = due,
            priority = priority
        )
        repository.addTask(task)

        val dueText = due?.let { " · Vence: $it" } ?: ""
        val pBadge = when (priority) {
            "alta" -> "🔴 Alta"
            "baja" -> "🔵 Baja"
            else -> "🟡 Media"
        }

        return AssistantExecutionResult(
            replyText = "✔ **¡Tarea creada con éxito!**\nHe añadido «**${task.title}**» a tus tareas con prioridad $pBadge$dueText.",
            actionType = "task_created",
            actionTitle = task.title,
            actionDetails = "Prioridad: $pBadge$dueText",
            targetTab = 2
        )
    }

    private fun parseAndCreateEvent(input: String, data: PastoralData): AssistantExecutionResult {
        var clean = input
        val prefixes = listOf(
            "/evento", "haz un evento:", "haz un evento", "hazme un evento:", "hazme un evento",
            "crea un evento:", "crea un evento", "crear un evento:", "crear un evento", "crear evento:", "crear evento",
            "crea evento:", "crea evento", "agenda un evento:", "agenda un evento", "agenda evento:", "agenda evento",
            "agendar evento:", "agendar evento", "programa un evento:", "programa un evento", "nuevo evento:", "nuevo evento",
            "anota el evento:", "anota el evento", "agrega un evento:", "agrega un evento", "añade un evento:", "añade un evento"
        )
        for (pref in prefixes) {
            if (clean.startsWith(pref, ignoreCase = true)) {
                clean = clean.substring(pref.length).trim()
                break
            } else if (clean.contains(pref, ignoreCase = true)) {
                clean = clean.replace(pref, "", ignoreCase = true).trim()
                break
            }
        }

        // 1. Explicit category extraction: e.g. "categoría Retiro" or "categoria Misa"
        var category = "Otro"
        val explicitCatMatch = Regex("""\b(?:categor[íi]a|tipo)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)""", RegexOption.IGNORE_CASE).find(clean)
        if (explicitCatMatch != null) {
            val catWord = explicitCatMatch.groupValues[1].lowercase()
            for (cat in PastoralConstants.EVENT_CATEGORIES) {
                if (cat.lowercase() == catWord || catWord.contains(cat.lowercase())) {
                    category = cat
                    break
                }
            }
            clean = clean.replace(explicitCatMatch.value, " ")
        }

        // 2. Extract time (e.g. "a las 09:00", "a las 9am")
        val parsedTime = extractTimeFromString(clean)
        val time = parsedTime?.first
        if (parsedTime != null) {
            clean = parsedTime.second
        }

        // 3. Extract date (e.g. "el 2026-11-20", "el próximo viernes")
        val parsedDate = extractDateFromString(clean)
        val date = parsedDate?.first ?: currentDateString()
        if (parsedDate != null) {
            clean = parsedDate.second
        }

        // 4. Extract location (e.g. "en la capilla", "en el salón parroquial")
        var location = ""
        val locMatch = Regex("""\ben\s+(?:el\s+|la\s+)?([a-zA-ZáéíóúÁÉÍÓÚñÑ0-9\s]{3,30}?)(?=\s+(?:categor[íi]a|tipo|a\s+las|el\s+\d|\d{4}-\d{2}-\d{2}|,|\.|$))""", RegexOption.IGNORE_CASE).find(clean)
            ?: Regex("""\ben\s+(?:el\s+|la\s+)?(capilla|sal[oó]n(?:\s+parroquial)?|auditorio|patio|templo|parroquia|colegio|iglesia|cancha|casa\s+de\s+retiros|zoom|virtual)\b""", RegexOption.IGNORE_CASE).find(clean)
        if (locMatch != null) {
            location = locMatch.groupValues.getOrNull(1)?.trim()?.replaceFirstChar { it.uppercase() } ?: ""
            clean = clean.replace(locMatch.value, " ")
        }

        // Clean extra remnants like trailing "el" or "para el"
        clean = clean.replace(Regex("""\b(?:para\s+el|el|para|a\s+las)\s*$"""), " ")
            .replace(Regex("""^\s*(?:para\s+el|el|para|a\s+las)\s+"""), " ")

        val title = clean.trim(':', ' ', ',', '-', '.', ';')
        val finalTitle = if (title.isBlank()) "Evento Pastoral" else title.replaceFirstChar { it.uppercase() }

        // If category was not explicitly given, infer from title without removing it from title
        if (category == "Otro") {
            for (cat in PastoralConstants.EVENT_CATEGORIES) {
                if (finalTitle.contains(cat, ignoreCase = true)) {
                    category = cat
                    break
                }
            }
            if (category == "Otro") {
                if (finalTitle.contains("reunion", ignoreCase = true) || finalTitle.contains("reunión", ignoreCase = true)) category = "Reunión"
                else if (finalTitle.contains("retiro", ignoreCase = true)) category = "Retiro"
                else if (finalTitle.contains("misa", ignoreCase = true) || finalTitle.contains("eucaristia", ignoreCase = true) || finalTitle.contains("eucaristía", ignoreCase = true)) category = "Misa"
                else if (finalTitle.contains("campaña", ignoreCase = true) || finalTitle.contains("campana", ignoreCase = true)) category = "Campaña"
                else if (finalTitle.contains("catequesis", ignoreCase = true) || finalTitle.contains("formacion", ignoreCase = true) || finalTitle.contains("formación", ignoreCase = true)) category = "Formación"
                else if (finalTitle.contains("convivencia", ignoreCase = true)) category = "Convivencia"
            }
        }

        val event = Event(
            title = finalTitle,
            date = date,
            time = time,
            location = location,
            category = category
        )
        repository.addEvent(event)

        val timeStr = time?.let { " a las $it" } ?: ""
        val locStr = if (location.isNotBlank()) " en $location" else ""

        return AssistantExecutionResult(
            replyText = "✔ **¡Evento programado con éxito!**\nHe agendado «**${event.title}**» para el **$date**$timeStr$locStr [Categoría: $category].",
            actionType = "event_created",
            actionTitle = event.title,
            actionDetails = "Fecha: $date$timeStr · $category",
            targetTab = 1
        )
    }

    private fun parseAndCreateNote(input: String, data: PastoralData): AssistantExecutionResult {
        var clean = input
        val prefixes = listOf(
            "/nota", "crea una nota:", "crea una nota", "crear una nota:", "crear una nota", "crear nota:", "crear nota",
            "crea la nota:", "crea la nota", "escribeme una nota:", "escribeme una nota", "escríbeme una nota:", "escríbeme una nota",
            "escribe una nota:", "escribe una nota", "haz una nota:", "haz una nota", "hazme una nota:", "hazme una nota",
            "tengo una idea:", "tengo una idea", "guardar idea:", "guardar idea", "guarda esta idea:", "guarda esta idea",
            "guarda la idea:", "guarda la idea", "guardar nota:", "guardar nota", "guarda esta nota:", "guarda esta nota",
            "anota esto:", "anota esto", "anota:", "nota:"
        )
        for (pref in prefixes) {
            if (clean.startsWith(pref, ignoreCase = true)) {
                clean = clean.substring(pref.length).trim()
                break
            } else if (clean.contains(pref, ignoreCase = true)) {
                clean = clean.replace(pref, "", ignoreCase = true).trim()
                break
            }
        }

        // Check for template mention
        var body = clean
        var tags = listOf("idea", "pastoral")
        var title = if (clean.length > 40) clean.take(37) + "..." else clean
        if (title.isBlank()) title = "Nota Pastoral"

        for (tmpl in PastoralConstants.TEMPLATES) {
            if (clean.contains(tmpl.id, ignoreCase = true) || clean.contains(tmpl.name, ignoreCase = true)) {
                title = tmpl.defaultTitle + (if (clean.length > 50) clean.take(30) else "")
                body = "${tmpl.defaultBody}\n\nDetalles adicionales:\n$clean"
                tags = tmpl.suggestedTags
                break
            }
        }

        val note = Note(
            title = title.replaceFirstChar { it.uppercase() },
            body = body,
            tags = tags
        )
        repository.addNote(note)

        return AssistantExecutionResult(
            replyText = "✔ **¡Nota guardada con éxito!**\nHe registrado «**${note.title}**» en tu sección de Notas e Ideas con las etiquetas: ${note.tags.joinToString(", ")}.",
            actionType = "note_created",
            actionTitle = note.title,
            actionDetails = "Etiquetas: ${note.tags.joinToString(", ")}",
            targetTab = 3
        )
    }

    private fun parseAndCompleteTask(input: String, data: PastoralData): AssistantExecutionResult {
        var clean = input
        val prefixes = listOf(
            "marca como completada la tarea", "marca como completada", "marcar como completada la tarea", "marcar como completada",
            "marca como hecha la tarea", "marca como hecha", "marca como lista la tarea", "marca como lista",
            "completa la tarea", "completar la tarea", "completar tarea", "terminé la tarea", "termine la tarea",
            "ya hice la tarea", "tarea hecha", "tarea completada"
        )
        for (p in prefixes) {
            if (clean.contains(p, ignoreCase = true)) {
                clean = clean.replace(p, "", ignoreCase = true).trim()
            }
        }
        val query = clean.trim(':', ' ', ',', '-', '"', '«', '»').lowercase()

        val taskToComplete = if (query.isNotBlank()) {
            data.tasks.find { !it.done && (it.title.lowercase().contains(query) || query.contains(it.title.lowercase())) }
                ?: data.tasks.find { !it.done }
        } else {
            data.tasks.find { !it.done }
        }

        return if (taskToComplete != null) {
            repository.updateTask(taskToComplete.copy(done = true))
            AssistantExecutionResult(
                replyText = "✔ **¡Tarea completada!**\nHe marcado «**${taskToComplete.title}**» como realizada en tu lista de tareas pastorales.",
                actionType = "task_completed",
                actionTitle = taskToComplete.title,
                actionDetails = "Estado: Completada",
                targetTab = 2
            )
        } else {
            AssistantExecutionResult(
                replyText = "No encontré ninguna tarea pendiente que coincida con «$query». Puedes revisar tus tareas activas escribiendo **/hoy**."
            )
        }
    }

    private fun parseAndDeleteItem(input: String, lower: String, data: PastoralData): AssistantExecutionResult {
        if (lower.contains("tarea")) {
            val query = lower.replace("elimina la tarea", "")
                .replace("borra la tarea", "")
                .replace("cancela la tarea", "")
                .trim(':', ' ', ',', '-', '"', '«', '»')
            val found = data.tasks.find { it.title.lowercase().contains(query) || query.contains(it.title.lowercase()) }
            return if (found != null) {
                repository.deleteTask(found.id)
                AssistantExecutionResult(
                    replyText = "✔ **Tarea eliminada**: «${found.title}» ha sido retirada de tu lista.",
                    actionType = "item_deleted",
                    actionTitle = found.title,
                    actionDetails = "Tarea eliminada",
                    targetTab = 2
                )
            } else {
                AssistantExecutionResult(replyText = "No encontré ninguna tarea que coincida con «$query».")
            }
        } else if (lower.contains("evento")) {
            val query = lower.replace("elimina el evento", "")
                .replace("borra el evento", "")
                .replace("cancela el evento", "")
                .trim(':', ' ', ',', '-', '"', '«', '»')
            val found = data.events.find { it.title.lowercase().contains(query) || query.contains(it.title.lowercase()) }
            return if (found != null) {
                repository.deleteEvent(found.id)
                AssistantExecutionResult(
                    replyText = "✔ **Evento cancelado/eliminado**: «${found.title}» (${found.date}) ha sido eliminado de tu agenda.",
                    actionType = "item_deleted",
                    actionTitle = found.title,
                    actionDetails = "Evento eliminado",
                    targetTab = 1
                )
            } else {
                AssistantExecutionResult(replyText = "No encontré ningún evento que coincida con «$query».")
            }
        }
        return AssistantExecutionResult(replyText = "Por favor indícame exactamente el título de la tarea o evento que deseas eliminar.")
    }

    private fun parseAndManageCommunity(input: String, lower: String, data: PastoralData): AssistantExecutionResult {
        if (lower.contains("grupo")) {
            val groupNameMatch = Regex("""(?:crea el grupo|crear grupo|nuevo grupo)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s0-9]+?)(?:,| con líder| coordinado por|\.|$|\n)""", RegexOption.IGNORE_CASE).find(input)
            val leaderMatch = Regex("""(?:con líder|coordinado por|líder|lider)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+?)(?:,|\.|$|\n)""", RegexOption.IGNORE_CASE).find(input)
            val groupName = groupNameMatch?.groupValues?.getOrNull(1)?.trim() ?: "Nuevo Grupo"
            val leader = leaderMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

            val group = Group(
                name = groupName.replaceFirstChar { it.uppercase() },
                leader = leader.replaceFirstChar { it.uppercase() },
                description = "Grupo pastoral de la comunidad"
            )
            repository.addGroup(group)
            return AssistantExecutionResult(
                replyText = "✔ **¡Grupo creado en la comunidad!**\nHe registrado el grupo «**${group.name}**»${if (leader.isNotBlank()) " coordinado por **$leader**" else ""}.",
                actionType = "group_added",
                actionTitle = group.name,
                actionDetails = if (leader.isNotBlank()) "Líder: $leader" else "Grupo pastoral",
                targetTab = 4
            )
        } else {
            // Person addition
            val nameMatch = Regex("""(?:agrega a|registra a)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+?)(?:,| como| al grupo|\.|$|\n)""", RegexOption.IGNORE_CASE).find(input)
            val roleMatch = Regex("""(?:como|rol)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+?)(?:,| al grupo| en el grupo|\.|$|\n)""", RegexOption.IGNORE_CASE).find(input)
            val groupMatch = Regex("""(?:al grupo|en el grupo)\s+([a-zA-ZáéíóúÁÉÍÓÚñÑ\s0-9]+?)(?:,|\.|$|\n)""", RegexOption.IGNORE_CASE).find(input)

            val name = nameMatch?.groupValues?.getOrNull(1)?.trim() ?: "Nuevo Miembro"
            val role = roleMatch?.groupValues?.getOrNull(1)?.trim() ?: "Alumno"
            val group = groupMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

            val person = Person(
                name = name.replaceFirstChar { it.uppercase() },
                role = role.replaceFirstChar { it.uppercase() },
                group = group.replaceFirstChar { it.uppercase() }
            )
            repository.addPerson(person)

            return AssistantExecutionResult(
                replyText = "✔ **¡Persona agregada a la comunidad!**\nHe registrado a **${person.name}** (${person.role}${if (group.isNotBlank()) " · Grupo: $group" else ""}).",
                actionType = "person_added",
                actionTitle = person.name,
                actionDetails = "${person.role} ${if (group.isNotBlank()) "· $group" else ""}",
                targetTab = 4
            )
        }
    }

    // --- GEMINI ACTION EXECUTION ---

    private fun extractActionBlock(text: String): List<PastoralActionItem> {
        val pattern = Regex("""```action\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        val json = match?.groupValues?.getOrNull(1)?.trim()
        if (!json.isNullOrBlank()) {
            try {
                val list = actionsAdapter.fromJson(json)
                if (list != null) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    private fun cleanActionBlockFromText(text: String): String {
        return text
            .replace(Regex("""```(?:action|json)\s*[\s\S]*?\s*```""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""```action\s*[\s\S]*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun executeActionList(actions: List<PastoralActionItem>, data: PastoralData): AssistantExecutionResult {
        var primaryActionType: String? = null
        var primaryActionTitle: String? = null
        var primaryActionDetails: String? = null
        var primaryTargetTab: Int? = null
        val summaryList = mutableListOf<String>()

        for (item in actions) {
            when (item.action?.lowercase()) {
                "create_event", "event" -> {
                    val title = item.title ?: "Evento Pastoral"
                    val date = item.date ?: currentDateString()
                    val event = Event(
                        title = title.replaceFirstChar { it.uppercase() },
                        date = date,
                        time = item.time,
                        location = item.location ?: "",
                        category = item.category ?: "Otro",
                        description = item.description ?: ""
                    )
                    repository.addEvent(event)
                    summaryList.add("📅 Evento «${event.title}» ($date)")
                    if (primaryActionType == null) {
                        primaryActionType = "event_created"
                        primaryActionTitle = event.title
                        primaryActionDetails = "Fecha: $date${event.time?.let { " · $it" } ?: ""} [${event.category}]"
                        primaryTargetTab = 1
                    }
                }
                "create_task", "task" -> {
                    val title = item.title ?: "Tarea Pastoral"
                    val task = Task(
                        title = title.replaceFirstChar { it.uppercase() },
                        due = item.due,
                        priority = item.priority ?: "media",
                        notes = item.notes ?: ""
                    )
                    repository.addTask(task)
                    summaryList.add("✅ Tarea «${task.title}»")
                    if (primaryActionType == null) {
                        primaryActionType = "task_created"
                        primaryActionTitle = task.title
                        primaryActionDetails = "Prioridad: ${task.priority.replaceFirstChar { it.uppercase() }}${task.due?.let { " · Vence: $it" } ?: ""}"
                        primaryTargetTab = 2
                    }
                }
                "create_note", "note" -> {
                    val title = item.title ?: "Nota Pastoral"
                    val note = Note(
                        title = title.replaceFirstChar { it.uppercase() },
                        body = item.body ?: "",
                        tags = item.tags ?: listOf("pastoral")
                    )
                    repository.addNote(note)
                    summaryList.add("💡 Nota «${note.title}»")
                    if (primaryActionType == null) {
                        primaryActionType = "note_created"
                        primaryActionTitle = note.title
                        primaryActionDetails = "Etiquetas: ${note.tags.joinToString(", ")}"
                        primaryTargetTab = 3
                    }
                }
                "complete_task" -> {
                    val q = (item.query ?: item.title ?: "").lowercase()
                    val found = data.tasks.find { !it.done && (it.title.lowercase().contains(q) || q.contains(it.title.lowercase())) }
                        ?: data.tasks.find { !it.done }
                    if (found != null) {
                        repository.updateTask(found.copy(done = true))
                        summaryList.add("✔ Completada tarea «${found.title}»")
                        if (primaryActionType == null) {
                            primaryActionType = "task_completed"
                            primaryActionTitle = found.title
                            primaryActionDetails = "Marcada como completada"
                            primaryTargetTab = 2
                        }
                    }
                }
                "delete_task" -> {
                    val q = (item.query ?: item.title ?: "").lowercase()
                    val found = data.tasks.find { it.title.lowercase().contains(q) || q.contains(it.title.lowercase()) }
                    if (found != null) {
                        repository.deleteTask(found.id)
                        summaryList.add("🗑 Eliminada tarea «${found.title}»")
                    }
                }
                "delete_event" -> {
                    val q = (item.query ?: item.title ?: "").lowercase()
                    val found = data.events.find { it.title.lowercase().contains(q) || q.contains(it.title.lowercase()) }
                    if (found != null) {
                        repository.deleteEvent(found.id)
                        summaryList.add("🗑 Eliminado evento «${found.title}»")
                    }
                }
                "add_person" -> {
                    val name = item.name ?: "Miembro"
                    val person = Person(
                        name = name.replaceFirstChar { it.uppercase() },
                        role = item.role ?: "Alumno",
                        group = item.group ?: "",
                        contact = item.contact ?: "",
                        notes = item.notes ?: ""
                    )
                    repository.addPerson(person)
                    summaryList.add("👤 Persona «${person.name}»")
                    if (primaryActionType == null) {
                        primaryActionType = "person_added"
                        primaryActionTitle = person.name
                        primaryActionDetails = "${person.role} · ${person.group}"
                        primaryTargetTab = 4
                    }
                }
                "add_group" -> {
                    val name = item.name ?: "Grupo"
                    val group = Group(
                        name = name.replaceFirstChar { it.uppercase() },
                        leader = item.leader ?: "",
                        description = item.description ?: ""
                    )
                    repository.addGroup(group)
                    summaryList.add("👥 Grupo «${group.name}»")
                    if (primaryActionType == null) {
                        primaryActionType = "group_added"
                        primaryActionTitle = group.name
                        primaryActionDetails = "Líder: ${group.leader}"
                        primaryTargetTab = 4
                    }
                }
                "update_settings" -> {
                    val updated = data.settings.copy(
                        pastorName = item.pastorName ?: data.settings.pastorName,
                        schoolName = item.schoolName ?: data.settings.schoolName,
                        theme = item.theme ?: data.settings.theme,
                        isConfigured = true
                    )
                    repository.updateSettings(updated)
                    summaryList.add("⚙ Ajustes actualizados")
                }
            }
        }

        val reply = "⚡ **Acciones ejecutadas en tu app**:\n" + summaryList.joinToString("\n") { "• $it" }
        return AssistantExecutionResult(
            replyText = reply,
            actionType = primaryActionType,
            actionTitle = primaryActionTitle,
            actionDetails = primaryActionDetails,
            targetTab = primaryTargetTab
        )
    }

    // --- DATE & TIME PARSING HELPERS ---

    private fun extractDateFromString(text: String): Pair<String, String>? {
        var clean = text

        // 1. ISO format: YYYY-MM-DD (optionally preceded by 'el' or 'para el')
        val isoRegex = Regex("""\b(?:el\s+|para\s+el\s+)?(\d{4}-\d{2}-\d{2})\b""")
        val isoMatch = isoRegex.find(clean)
        if (isoMatch != null) {
            val date = isoMatch.groupValues[1]
            return Pair(date, clean.replace(isoMatch.value, " "))
        }

        // 2. Natural expressions: hoy, mañana, pasado mañana
        if (clean.contains("pasado mañana", ignoreCase = true)) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, 2)
            return Pair(sdfDate.format(c.time), clean.replace("pasado mañana", " ", ignoreCase = true).replace("para pasado mañana", " ", ignoreCase = true))
        }
        if (clean.contains("mañana", ignoreCase = true)) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, 1)
            return Pair(sdfDate.format(c.time), clean.replace("para mañana", " ", ignoreCase = true).replace("mañana", " ", ignoreCase = true))
        }
        if (clean.contains("hoy", ignoreCase = true)) {
            return Pair(currentDateString(), clean.replace("para hoy", " ", ignoreCase = true).replace("hoy", " ", ignoreCase = true))
        }

        // 3. Days of the week (lunes, martes, etc.)
        val weekdays = mapOf(
            "lunes" to Calendar.MONDAY,
            "martes" to Calendar.TUESDAY,
            "miércoles" to Calendar.WEDNESDAY,
            "miercoles" to Calendar.WEDNESDAY,
            "jueves" to Calendar.THURSDAY,
            "viernes" to Calendar.FRIDAY,
            "sábado" to Calendar.SATURDAY,
            "sabado" to Calendar.SATURDAY,
            "domingo" to Calendar.SUNDAY
        )

        for ((dayName, calDay) in weekdays) {
            val regex = Regex("""\b(?:el\s+|para\s+el\s+|este\s+|próximo\s+)?$dayName\b""", RegexOption.IGNORE_CASE)
            val match = regex.find(clean)
            if (match != null) {
                val c = Calendar.getInstance()
                val currentDow = c.get(Calendar.DAY_OF_WEEK)
                var diff = (calDay - currentDow + 7) % 7
                if (diff == 0) diff = 7
                c.add(Calendar.DAY_OF_YEAR, diff)
                return Pair(sdfDate.format(c.time), clean.replace(match.value, " "))
            }
        }

        // 4. "15 de noviembre", "20 de octubre", etc.
        val months = mapOf(
            "enero" to 0, "febrero" to 1, "marzo" to 2, "abril" to 3,
            "mayo" to 4, "junio" to 5, "julio" to 6, "agosto" to 7,
            "septiembre" to 8, "setiembre" to 8, "octubre" to 9,
            "noviembre" to 10, "diciembre" to 11
        )
        for ((mName, mIdx) in months) {
            val dateTextRegex = Regex("""\b(?:el\s+)?(\d{1,2})\s+de\s+$mName(?:\s+de\s+(\d{4}))?\b""", RegexOption.IGNORE_CASE)
            val match = dateTextRegex.find(clean)
            if (match != null) {
                val day = match.groupValues[1].toIntOrNull() ?: 1
                val year = match.groupValues.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                val c = Calendar.getInstance()
                c.set(year, mIdx, day)
                return Pair(sdfDate.format(c.time), clean.replace(match.value, " "))
            }
        }

        return null
    }

    private fun extractTimeFromString(text: String): Pair<String, String>? {
        var clean = text

        // 1. "a las 15:30", "15:30", "09:00"
        val timeRegex = Regex("""\b(?:a\s+las\s+)?(\d{1,2}):(\d{2})\b""", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(clean)
        if (match != null) {
            val hh = match.groupValues[1].padStart(2, '0')
            val mm = match.groupValues[2]
            return Pair("$hh:$mm", clean.replace(match.value, " "))
        }

        // 2. "a las 10am", "a las 4pm", "a las 10 de la mañana", "a las 3 de la tarde"
        val amPmRegex = Regex("""\b(?:a\s+las\s+)?(\d{1,2})\s*(am|pm|de la mañana|de la tarde|de la noche)\b""", RegexOption.IGNORE_CASE)
        val amPmMatch = amPmRegex.find(clean)
        if (amPmMatch != null) {
            var h = amPmMatch.groupValues[1].toIntOrNull() ?: 9
            val period = amPmMatch.groupValues[2].lowercase()
            if ((period.contains("pm") || period.contains("tarde") || period.contains("noche")) && h < 12) {
                h += 12
            } else if (period.contains("am") && h == 12) {
                h = 0
            }
            val hh = h.toString().padStart(2, '0')
            return Pair("$hh:00", clean.replace(amPmMatch.value, " "))
        }

        // 3. "a las 10", "a las 9"
        val hourRegex = Regex("""\ba\s+las\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
        val hourMatch = hourRegex.find(clean)
        if (hourMatch != null) {
            val h = hourMatch.groupValues[1].toIntOrNull() ?: 9
            val hh = h.toString().padStart(2, '0')
            return Pair("$hh:00", clean.replace(hourMatch.value, " "))
        }

        return null
    }

    // --- BRIEFINGS ---

    private fun generateDailyBriefing(data: PastoralData): String {
        val today = currentDateString()
        val calendar = Calendar.getInstance()
        val todayDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = sdfDate.format(calendar.time)

        val settings = data.settings
        val formattedToday = sdfDisplay.format(todayDate).replaceFirstChar { it.uppercase() }

        val todayEvents = data.events.filter { it.date == today }
        val tomorrowEvents = data.events.filter { it.date == tomorrow }
        val upcomingEvents = data.events.filter { it.date > tomorrow }.take(3)

        val overdueTasks = data.tasks.filter { !it.done && it.due != null && it.due < today }
        val todayTasks = data.tasks.filter { !it.done && it.due == today }

        val sb = StringBuilder()
        val pastor = if (settings.pastorName.isNotBlank()) settings.pastorName else "Responsable"
        val school = if (settings.schoolName.isNotBlank()) " · ${settings.schoolName}" else ""
        sb.append("✝ **Buenos días, $pastor**$school\n")
        sb.append("🗓 **$formattedToday**\n\n")

        if (overdueTasks.isNotEmpty()) {
            sb.append("⚠️ **Tareas vencidas (${overdueTasks.size})**:\n")
            overdueTasks.forEach { sb.append("• 🔴 ${it.title} (venció el ${it.due})\n") }
            sb.append("\n")
        }

        sb.append("📅 **Eventos de HOY**:\n")
        if (todayEvents.isEmpty()) {
            sb.append("• *Sin eventos agendados para hoy.*\n")
        } else {
            todayEvents.forEach { ev ->
                val timeStr = ev.time?.let { "$it - " } ?: ""
                val locStr = if (ev.location.isNotBlank()) " (${ev.location})" else ""
                sb.append("• 🕒 $timeStr**${ev.title}** [${ev.category}]$locStr\n")
            }
        }
        sb.append("\n")

        sb.append("✅ **Tareas para HOY**:\n")
        if (todayTasks.isEmpty()) {
            sb.append("• *No tienes tareas con vencimiento hoy.*\n")
        } else {
            todayTasks.forEach { t ->
                val prio = when (t.priority) {
                    "alta" -> "🔴 Alta"
                    "media" -> "🟡 Media"
                    else -> "🔵 Baja"
                }
                sb.append("• [$prio] ${t.title}\n")
            }
        }
        sb.append("\n")

        if (tomorrowEvents.isNotEmpty()) {
            sb.append("🌅 **Mañana (${tomorrow})**:\n")
            tomorrowEvents.forEach { ev ->
                val timeStr = ev.time?.let { " $it" } ?: ""
                sb.append("• ${ev.title}$timeStr [${ev.category}]\n")
            }
            sb.append("\n")
        }

        if (upcomingEvents.isNotEmpty()) {
            sb.append("🔮 **Próximos días**:\n")
            upcomingEvents.forEach { ev ->
                sb.append("• ${ev.date}: ${ev.title} (${ev.category})\n")
            }
            sb.append("\n")
        }

        sb.append("«Todo lo puedo en Cristo que me fortalece.» (Flp 4,13)")
        return sb.toString()
    }

    private fun generateWeeklyBriefing(data: PastoralData): String {
        val today = currentDateString()
        val calendar = Calendar.getInstance()
        val startDay = sdfDayMonth.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDay = sdfDayMonth.format(calendar.time)
        val endIso = sdfDate.format(calendar.time)

        val weekEvents = data.events.filter { it.date in today..endIso }
        val pendingTasks = data.tasks.filter { !it.done }
        val highPriority = pendingTasks.filter { it.priority == "alta" }

        val sb = StringBuilder()
        sb.append("📅 **Tu semana pastoral ($startDay - $endDay)**:\n\n")

        if (weekEvents.isEmpty()) {
            sb.append("• No hay eventos programados en los próximos 7 días.\n\n")
        } else {
            weekEvents.forEach { ev ->
                val timeStr = ev.time?.let { " $it" } ?: ""
                val locStr = if (ev.location.isNotBlank()) " (${ev.location})" else ""
                sb.append("• **${ev.date}$timeStr**: ${ev.title} [${ev.category}]$locStr\n")
            }
            sb.append("\n")
        }

        sb.append("✅ **3 Prioridades pastorales sugeridas**:\n")
        var count = 1

        if (highPriority.isNotEmpty()) {
            sb.append("1. **${highPriority.first().title}** (Vence: ${highPriority.first().due ?: "Pronto"}, prioridad alta).\n")
            count++
        }

        val nearestRetiroOrMisa = weekEvents.find { it.category == "Retiro" || it.category == "Misa" || it.category == "Campaña" }
        if (nearestRetiroOrMisa != null) {
            sb.append("$count. **Preparar ${nearestRetiroOrMisa.title}** el ${nearestRetiroOrMisa.date}: revisar lecturas, guías y responsables.\n")
            count++
        } else if (pendingTasks.size >= count) {
            sb.append("$count. **${pendingTasks[count - 1].title}**: avanzar con la coordinación de la semana.\n")
            count++
        }

        val youthGroup = data.groups.find { it.name.contains("Juvenil", ignoreCase = true) }
        val leader = youthGroup?.leader ?: "los coordinadores"
        sb.append("$count. **Acompañamiento pastoral**: contactar a $leader para la animación del próximo encuentro.\n\n")

        sb.append("¿Quieres que te recuerde alguna de estas acciones ahora?")
        return sb.toString()
    }

    private fun executeSearch(query: String, data: PastoralData): String {
        if (query.isBlank()) {
            return "Por favor indica qué deseas buscar. Ejemplo: `/buscar retiro`"
        }
        val q = query.lowercase()
        val results = mutableListOf<String>()

        data.events.filter { it.title.lowercase().contains(q) || it.location.lowercase().contains(q) || it.category.lowercase().contains(q) }
            .forEach { results.add("📅 **Evento**: «${it.title}» (${it.date} ${it.time ?: ""}) - Lugar: ${it.location}") }

        data.tasks.filter { it.title.lowercase().contains(q) || it.notes.lowercase().contains(q) }
            .forEach { results.add("✅ **Tarea**: «${it.title}» - Estado: ${if (it.done) "Completada" else "Pendiente"}") }

        data.notes.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) || it.tags.any { t -> t.lowercase().contains(q) } }
            .forEach { results.add("💡 **Nota**: «${it.title}» [${it.tags.joinToString(", ")}]") }

        data.persons.filter { it.name.lowercase().contains(q) || it.role.lowercase().contains(q) || it.group.lowercase().contains(q) }
            .forEach { results.add("👤 **Persona**: ${it.name} (${it.role} · ${it.group})") }

        data.groups.filter { it.name.lowercase().contains(q) || it.leader.lowercase().contains(q) }
            .forEach { results.add("👥 **Grupo**: ${it.name} (Coordina: ${it.leader})") }

        return if (results.isEmpty()) {
            "🔍 No se encontraron coincidencias para «$query» en notas, eventos, tareas ni personas."
        } else {
            "🔍 **Coincidencias encontradas para «$query» (${results.size})**:\n\n" + results.joinToString("\n")
        }
    }
}
