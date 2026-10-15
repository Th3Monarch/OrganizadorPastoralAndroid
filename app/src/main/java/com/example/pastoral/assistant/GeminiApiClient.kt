package com.example.pastoral.assistant

import com.example.BuildConfig
import com.example.pastoral.data.model.PastoralData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val googleSearch: Map<String, String>? = null,
    val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTool>? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebChunk(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    val web: GeminiWebChunk? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

class GeminiApiClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Mandated gemini-3.8-flash model with Google Search & Google Maps Grounding
    private val modelName = "gemini-3.8-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    suspend fun generatePastoralResponse(
        userInput: String,
        pastoralData: PastoralData
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No API key configured"))
        }

        val systemPrompt = buildSystemPrompt(pastoralData)

        // Try first with full Google Search and Google Maps grounding tools
        val toolsWithSearchAndMaps = listOf(
            GeminiTool(googleSearch = emptyMap()),
            GeminiTool(googleMaps = emptyMap())
        )

        val fullRequest = GeminiRequest(
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userInput))
                )
            ),
            tools = toolsWithSearchAndMaps,
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        val primaryResult = executeRequest(fullRequest, apiKey)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // Fallback with Google Search grounding alone if combined tools error
        val searchOnlyRequest = fullRequest.copy(
            tools = listOf(GeminiTool(googleSearch = emptyMap()))
        )
        val searchOnlyResult = executeRequest(searchOnlyRequest, apiKey)
        if (searchOnlyResult.isSuccess) {
            return@withContext searchOnlyResult
        }

        // Direct request fallback
        val directRequest = fullRequest.copy(tools = null)
        executeRequest(directRequest, apiKey)
    }

    private fun executeRequest(requestObj: GeminiRequest, apiKey: String): Result<String> {
        return try {
            val jsonBody = requestAdapter.toJson(requestObj)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    return Result.failure(Exception("HTTP error ${response.code}: $bodyString"))
                }

                val geminiResponse = responseAdapter.fromJson(bodyString)
                val candidate = geminiResponse?.candidates?.firstOrNull()
                val replyText = candidate?.content?.parts?.firstOrNull()?.text

                if (!replyText.isNullOrBlank()) {
                    var finalReply = replyText.trim()
                    val webSources = candidate.groundingMetadata?.groundingChunks
                        ?.mapNotNull { it.web }
                        ?.filter { !it.uri.isNullOrBlank() }

                    if (!webSources.isNullOrEmpty()) {
                        val distinctSources = webSources.distinctBy { it.uri }.take(3)
                        val sourcesText = "\n\n> 🌐 **Fuentes verificadas (Google Search & Maps)**:\n" +
                                distinctSources.joinToString("\n") { "• [${it.title ?: it.uri}](${it.uri})" }
                        finalReply += sourcesText
                    }
                    Result.success(finalReply)
                } else {
                    Result.failure(Exception("Respuesta vacía de Gemini"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(data: PastoralData): String {
        val settings = data.settings
        val pastor = if (settings.pastorName.isNotBlank()) settings.pastorName else "Responsable"
        val school = if (settings.schoolName.isNotBlank()) settings.schoolName else "Institución Pastoral"

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd (EEEE)", java.util.Locale("es", "ES"))
        val nowStr = sdf.format(java.util.Date())

        val eventsSummary = if (data.events.isEmpty()) "Ninguno registrado aún." else {
            data.events.take(8).joinToString("; ") { "${it.date} (${it.category}): ${it.title} en ${it.location}" }
        }

        val tasksSummary = if (data.tasks.isEmpty()) "Ninguna pendiente." else {
            data.tasks.take(8).joinToString("; ") { "${it.title} [${it.priority}]${if (it.isCompleted) " (Hecho)" else " (Pendiente)"}" }
        }

        val notesSummary = if (data.notes.isEmpty()) "Ninguna nota guardada." else {
            data.notes.take(5).joinToString("; ") { it.title }
        }

        val contactsSummary = if (data.persons.isEmpty()) "Ningún contacto registrado." else {
            data.persons.take(6).joinToString("; ") { "${it.name} (${it.role} - ${it.group})" }
        }

        return """
            Eres el Asistente Pastoral inteligente de la institución "$school", al servicio del pastoralista "$pastor".
            Fecha actual del dispositivo: $nowStr.
            
            TIENES ACCESO A HERRAMIENTAS DE CONEXIÓN CON EL MUNDO REAL:
            1. GOOGLE SEARCH GROUNDING: Para consultar datos actualizados del santoral, lecturas bíblicas del día, tiempos litúrgicos, documentos de la Iglesia y noticias.
            2. GOOGLE MAPS GROUNDING: Para buscar direcciones de parroquias, catedrales, capillas, centros de retiro, conventos y tiendas religiosas locales cuando el usuario lo solicite.
            
            TIENES PODER Y CONTROL TOTAL SOBRE LA APLICACIÓN.
            El usuario te pedirá organizar eventos, escribir tareas, guardar notas o dinámicas, agregar contactos y actualizar el estado de las cosas.
            
            DATOS ACTUALES EN LA APLICACIÓN:
            • Eventos próximos: $eventsSummary
            • Tareas: $tasksSummary
            • Notas/Ideas: $notesSummary
            • Contactos comunitarios: $contactsSummary
            
            INSTRUCCIÓN CRÍTICA PARA EJECUTAR ACCIONES:
            Cuando el usuario te pida explícita o implícitamente realizar una acción de gestión (crear evento, agregar tarea, crear nota, etc.), DEBES incluir al final de tu respuesta un bloque de código EXACTAMENTE con la etiqueta ` ```action ` conteniendo una lista JSON de acciones a ejecutar.
            
            FORMATO DEL BLOQUE DE ACCIÓN:
            ```action
            [
              {
                "action": "create_event",
                "title": "Retiro Espiritual de Pascua",
                "date": "2026-11-15",
                "time": "09:00",
                "location": "Capilla del Colegio",
                "category": "Retiro",
                "description": "Jornada de silencio y oración para jóvenes.",
                "attendance": ["Juan Perez", "Maria Gomez"]
              }
            ]
            ```
            
            ACCIONES DISPONIBLES:
            1. "create_event": Requiere "title", "date" (YYYY-MM-DD), opcionales "time", "location", "category" (Misa, Retiro, Catequesis, Reunión, Celebración, Formación, Servicio, Convivencia, Otro), "description", "attendance" (lista de nombres).
            2. "delete_event": Requiere "title" (título aproximado o exacto) o "id".
            3. "create_task": Requiere "title", opcionales "dueDate" (YYYY-MM-DD), "priority" (Alta, Media, Baja), "notes".
            4. "toggle_task": Requiere "title" (para buscar la tarea) y opcional "isCompleted" (boolean).
            5. "delete_task": Requiere "title" o "id".
            6. "create_note": Requiere "title", "body", opcional "tags" (lista de tags como Dinámica, Homilía, Oración, Reunión).
            7. "delete_note": Requiere "title" o "id".
            8. "create_contact": Requiere "name", opcionales "role", "group", "phone", "email", "notes".
            9. "delete_contact": Requiere "name" o "id".
            10. "update_settings": Requiere "pastorName" o "schoolName" o "theme" ("light" o "dark").
            
            PAUTAS:
            1. Si el usuario te pide crear o modificar algo, SIEMPRE ejecuta la acción incluyendo el bloque ` ```action ` y acompáñalo de un texto cálido, fraterno y pastoral confirmando lo que has realizado.
            2. Si te pide redactar una oración, dinámica, homilía o reflexión, escribe el contenido con riqueza pastoral y si te dice "guárdala", "crea la nota" o "anótala", agrega la acción `create_note`.
            3. Si el usuario pide varias cosas a la vez, incluye TODAS las acciones en la lista.
            4. Tono cálido, servicial, respetuoso y organizado.
            5. FORMATO VISUAL, TABLAS Y SINTAXIS MARKDOWN LIMPIA:
               - La aplicación interpreta formato Markdown de manera nativa mediante parser AST (negrita, cursiva, tablas, listas, citas, encabezados).
               - Utiliza **negrita** para resaltar conceptos clave, títulos o nombres importantes.
               - Utiliza listas organizadas con viñetas (`- ` o `• `) o listas numeradas (`1. `, `2. `) para pasos ordenados o listas de cotejo (`- [ ] ` y `- [x] `).
               - Cuando organices cronogramas, horarios, comparativas de grupos o presupuestos, utiliza TABLAS Markdown con encabezados estándar (`| Columna 1 | Columna 2 | \n |---|---| \n | Valor 1 | Valor 2 |`).
               - Utiliza citas de bloque (`> `) para versículos bíblicos, oraciones o reflexiones destacadas.
               - NUNCA generes asteriscos sueltos, caracteres de escape con barra invertida (`\*`), ni signos raros repetitivos.
        """.trimIndent()
    }
}
