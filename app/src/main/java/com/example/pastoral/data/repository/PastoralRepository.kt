package com.example.pastoral.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.pastoral.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PastoralRepository"

class PastoralRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    ),
    private val auth: FirebaseAuth = Firebase.auth
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(PastoralData::class.java)

    private val dataFile: File
        get() = File(context.filesDir, "pastoral-data.json")

    private val _data = MutableStateFlow(PastoralData())
    val data: StateFlow<PastoralData> = _data.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestoreListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    init {
        loadData()
        setupAuthAndFirestore()
    }

    private fun setupAuthAndFirestore() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startListeningToFirestore(user.uid)
            } else {
                stopListeningToFirestore()
            }
        }
    }

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("User must be authenticated for Firestore operations")
    }

    private fun startListeningToFirestore(userId: String) {
        if (activeUserId == userId && firestoreListener != null) return
        stopListeningToFirestore()
        activeUserId = userId

        val userDocRef = db.collection("users").document(userId)
        firestoreListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error listening to Firestore updates: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val dataMap = snapshot.data
                if (dataMap != null) {
                    val remoteData = parsePastoralDataFromMap(dataMap)
                    // If remote data has content, merge with local state
                    if (remoteData.events.isNotEmpty() || remoteData.tasks.isNotEmpty() ||
                        remoteData.notes.isNotEmpty() || remoteData.persons.isNotEmpty() ||
                        remoteData.settings.isConfigured
                    ) {
                        _data.value = remoteData
                        saveDataInternal(remoteData)
                    }
                }
            } else {
                // New user or document not created yet on Firestore: push local data
                uploadDataToFirestore(userId, _data.value)
            }
        }
    }

    private fun stopListeningToFirestore() {
        firestoreListener?.remove()
        firestoreListener = null
        activeUserId = null
    }

    private fun uploadDataToFirestore(userId: String, data: PastoralData) {
        scope.launch {
            try {
                val map = data.toFirestoreMap(userId)
                db.collection("users").document(userId).set(map)
                Log.d(TAG, "Successfully synced pastoral data to Firestore for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload pastoral data to Firestore: ${e.message}", e)
            }
        }
    }

    fun syncWithFirestore() {
        val user = auth.currentUser
        if (user != null) {
            uploadDataToFirestore(user.uid, _data.value)
        }
    }

    private fun loadData() {
        try {
            if (dataFile.exists()) {
                val json = dataFile.readText()
                val loaded = adapter.fromJson(json)
                if (loaded != null) {
                    _data.value = loaded
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val initial = getCleanInitialData()
        _data.value = initial
        saveDataInternal(initial)
    }

    fun getCleanInitialData(): PastoralData {
        return PastoralData(
            notes = emptyList(),
            events = emptyList(),
            tasks = emptyList(),
            groups = emptyList(),
            persons = emptyList(),
            chat = emptyList(),
            settings = PastoralSettings(
                pastorName = "",
                schoolName = "",
                theme = "light",
                isConfigured = false
            )
        )
    }

    private fun persist(updated: PastoralData) {
        _data.value = updated
        scope.launch {
            saveDataInternal(updated)
            val user = auth.currentUser
            if (user != null) {
                uploadDataToFirestore(user.uid, updated)
            }
        }
    }

    private fun saveDataInternal(toSave: PastoralData) {
        try {
            val json = adapter.indent("  ").toJson(toSave)
            dataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Notes CRUD ---
    fun addNote(note: Note) {
        val updatedNotes = listOf(note) + _data.value.notes
        persist(_data.value.copy(notes = updatedNotes))
    }

    fun updateNote(note: Note) {
        val updatedNotes = _data.value.notes.map { if (it.id == note.id) note.copy(updatedAt = currentIsoDate()) else it }
        persist(_data.value.copy(notes = updatedNotes))
    }

    fun deleteNote(id: String) {
        persist(_data.value.copy(notes = _data.value.notes.filterNot { it.id == id }))
    }

    // --- Events CRUD ---
    fun addEvent(event: Event) {
        val updatedEvents = (_data.value.events + event).sortedBy { it.date + (it.time ?: "00:00") }
        persist(_data.value.copy(events = updatedEvents))
    }

    fun updateEvent(event: Event) {
        val updatedEvents = _data.value.events.map { if (it.id == event.id) event else it }
            .sortedBy { it.date + (it.time ?: "00:00") }
        persist(_data.value.copy(events = updatedEvents))
    }

    fun deleteEvent(id: String) {
        persist(_data.value.copy(events = _data.value.events.filterNot { it.id == id }))
    }

    // --- Tasks CRUD ---
    fun addTask(task: Task) {
        val updatedTasks = listOf(task) + _data.value.tasks
        persist(_data.value.copy(tasks = updatedTasks))
    }

    fun updateTask(task: Task) {
        val updatedTasks = _data.value.tasks.map { if (it.id == task.id) task else it }
        persist(_data.value.copy(tasks = updatedTasks))
    }

    fun toggleTaskDone(id: String) {
        val updatedTasks = _data.value.tasks.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }
        persist(_data.value.copy(tasks = updatedTasks))
    }

    fun deleteTask(id: String) {
        persist(_data.value.copy(tasks = _data.value.tasks.filterNot { it.id == id }))
    }

    // --- Groups CRUD ---
    fun addGroup(group: Group) {
        persist(_data.value.copy(groups = _data.value.groups + group))
    }

    fun updateGroup(group: Group) {
        val updated = _data.value.groups.map { if (it.id == group.id) group else it }
        persist(_data.value.copy(groups = updated))
    }

    fun deleteGroup(id: String) {
        persist(_data.value.copy(groups = _data.value.groups.filterNot { it.id == id }))
    }

    // --- Persons CRUD ---
    fun addPerson(person: Person) {
        persist(_data.value.copy(persons = _data.value.persons + person))
    }

    fun updatePerson(person: Person) {
        val updated = _data.value.persons.map { if (it.id == person.id) person else it }
        persist(_data.value.copy(persons = updated))
    }

    fun deletePerson(id: String) {
        persist(_data.value.copy(persons = _data.value.persons.filterNot { it.id == id }))
    }

    // --- Settings ---
    fun updateSettings(settings: PastoralSettings) {
        persist(_data.value.copy(settings = settings))
    }

    // --- Chat Messages ---
    fun addChatMessage(
        role: String,
        content: String,
        actionType: String? = null,
        actionTitle: String? = null,
        actionDetails: String? = null,
        targetTab: Int? = null
    ) {
        val updatedChat = _data.value.chat + ChatItem(
            role = role,
            content = content,
            actionType = actionType,
            actionTitle = actionTitle,
            actionDetails = actionDetails,
            targetTab = targetTab
        )
        persist(_data.value.copy(chat = updatedChat))
    }

    fun clearChat() {
        persist(_data.value.copy(chat = emptyList()))
    }

    // --- Import / Export ---
    fun exportJson(): String {
        val cleanData = _data.value.copy(chat = emptyList())
        return adapter.indent("  ").toJson(cleanData)
    }

    suspend fun importJson(jsonContent: String): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val parsed = adapter.fromJson(jsonContent)
                ?: return@withContext Result.failure(IllegalArgumentException("El formato del JSON no es válido"))

            val finalData = parsed.copy(
                notes = parsed.notes ?: emptyList(),
                events = parsed.events ?: emptyList(),
                tasks = parsed.tasks ?: emptyList(),
                groups = parsed.groups ?: emptyList(),
                persons = parsed.persons ?: emptyList(),
                chat = emptyList(),
                settings = parsed.settings ?: _data.value.settings
            )

            persist(finalData)

            val summary = ImportSummary(
                notesCount = finalData.notes.size,
                eventsCount = finalData.events.size,
                tasksCount = finalData.tasks.size,
                groupsCount = finalData.groups.size,
                personsCount = finalData.persons.size
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetToDemoData() {
        val sample = getSampleData()
        persist(sample)
    }

    fun clearAllData() {
        val cleared = PastoralData(
            notes = emptyList(),
            events = emptyList(),
            tasks = emptyList(),
            groups = emptyList(),
            persons = emptyList(),
            chat = emptyList(),
            settings = _data.value.settings
        )
        persist(cleared)
    }

    private fun getSampleData(): PastoralData {
        return PastoralData(
            notes = listOf(
                Note(
                    id = "n-1",
                    title = "Dinámica: La Red de Pescadores",
                    body = "Dinámica para inicio de curso con jóvenes de 3º y 4º de Secundaria.\nObjetivo: Reconocer los talentos individuales puestos al servicio de la comunidad pastoral.\n\nMateriales:\n- Madeja de lana de color vivo\n- Cartulinas con citas de Mateo 4, 18-22\n\nPasos:\n1. Colocar al grupo en círculo silencioso.\n2. El animador lanza la madeja diciendo un valor personal.\n3. Al final todos sostienen la red y se reflexiona sobre la fuerza de la comunidad.",
                    tags = listOf("Dinámica", "Jóvenes", "Comunidad")
                ),
                Note(
                    id = "n-2",
                    title = "Esquema: Homilía San Francisco de Asís",
                    body = "Tema: «Señor, hazme un instrumento de tu paz».\n\nPuntos principales:\n1. La llamada a reconstruir la Iglesia viva.\n2. La sencillez frente a la cultura del descarte.\n3. Vivir en fraternidad en el ámbito escolar y familiar.",
                    tags = listOf("Homilía", "San Francisco", "Paz")
                )
            ),
            events = listOf(
                Event(
                    id = "e-1",
                    title = "Misa de Envío Pastoral",
                    date = "2026-10-20",
                    time = "10:30",
                    location = "Capilla Mayor",
                    category = "Misa",
                    description = "Celebración eucarística con toda la comunidad educativa para bendecir los proyectos del trimestre.",
                    attendance = listOf("Hno. Director", "Prof. Carlos", "Catequistas 1º Bachillerato")
                ),
                Event(
                    id = "e-2",
                    title = "Reunión de Coordinación Catequética",
                    date = "2026-10-23",
                    time = "16:00",
                    location = "Sala de Pastoral",
                    category = "Reunión",
                    description = "Planificación del calendario de Primeras Comuniones y Confirmación.",
                    attendance = listOf("Equipo de Catequistas", "Responsables de Nivel")
                )
            ),
            tasks = listOf(
                Task(
                    id = "t-1",
                    title = "Revisar cancionero litúrgico para la Misa de Envío",
                    due = "2026-10-18",
                    priority = "alta",
                    notes = "Verificar que el coro tenga las partituras y acordes impresos.",
                    done = true
                ),
                Task(
                    id = "t-2",
                    title = "Comprar cirios y flores para el altar",
                    due = "2026-10-19",
                    priority = "alta",
                    notes = "2 cirios blancos y arreglo de flores para la capilla mayor.",
                    done = false
                )
            ),
            groups = listOf(
                Group(id = "g-1", name = "Equipo de Catequistas", leader = "Hna. Teresa", description = "Catequesis de Comunión y Confirmación"),
                Group(id = "g-2", name = "Coro Pastoral Juvenil", leader = "Prof. Mario", description = "Animación musical de misas y vigilias")
            ),
            persons = listOf(
                Person(id = "p-1", name = "Hna. Teresa López", role = "Catequista", group = "Equipo de Catequistas", contact = "600112233", notes = "Coordinadora de 2º año"),
                Person(id = "p-2", name = "Prof. Mario Soria", role = "Docente", group = "Coro Pastoral Juvenil", contact = "611223344", notes = "Guitarrista y director")
            ),
            chat = emptyList(),
            settings = PastoralSettings(
                pastorName = "Hno. Andrés",
                schoolName = "Colegio San José",
                theme = "light",
                isConfigured = true
            )
        )
    }
}

/**
 * Maps PastoralData into a Firestore-friendly Map for zero-trust cloud persistence.
 */
fun PastoralData.toFirestoreMap(userId: String): Map<String, Any> {
    val eventsList = events.map { event ->
        mapOf(
            "id" to event.id,
            "title" to event.title,
            "date" to event.date,
            "time" to (event.time ?: ""),
            "location" to event.location,
            "category" to event.category,
            "description" to event.description,
            "attendance" to event.attendance,
            "userId" to userId
        )
    }

    val tasksList = tasks.map { task ->
        mapOf(
            "id" to task.id,
            "title" to task.title,
            "due" to (task.due ?: ""),
            "priority" to task.priority,
            "notes" to task.notes,
            "done" to task.done,
            "createdAt" to task.createdAt,
            "userId" to userId
        )
    }

    val notesList = notes.map { note ->
        mapOf(
            "id" to note.id,
            "title" to note.title,
            "body" to note.body,
            "tags" to note.tags,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt,
            "userId" to userId
        )
    }

    val groupsList = groups.map { g ->
        mapOf(
            "id" to g.id,
            "name" to g.name,
            "leader" to g.leader,
            "description" to g.description,
            "userId" to userId
        )
    }

    val personsList = persons.map { p ->
        mapOf(
            "id" to p.id,
            "name" to p.name,
            "role" to p.role,
            "group" to p.group,
            "contact" to p.contact,
            "notes" to p.notes,
            "userId" to userId
        )
    }

    val settingsMap = mapOf(
        "pastorName" to settings.pastorName,
        "schoolName" to settings.schoolName,
        "theme" to settings.theme,
        "isConfigured" to settings.isConfigured,
        "userId" to userId
    )

    return mapOf(
        "userId" to userId,
        "updatedAt" to currentIsoDate(),
        "events" to eventsList,
        "tasks" to tasksList,
        "notes" to notesList,
        "groups" to groupsList,
        "persons" to personsList,
        "settings" to settingsMap
    )
}

/**
 * Safely parses a PastoralData object from a Firestore document map.
 */
@Suppress("UNCHECKED_CAST")
fun parsePastoralDataFromMap(map: Map<String, Any?>): PastoralData {
    val eventsRaw = (map["events"] as? List<Map<String, Any?>>) ?: emptyList()
    val events = eventsRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        val title = m["title"] as? String ?: return@mapNotNull null
        val date = m["date"] as? String ?: return@mapNotNull null
        val time = (m["time"] as? String)?.ifBlank { null }
        val location = m["location"] as? String ?: ""
        val category = m["category"] as? String ?: "Otro"
        val description = m["description"] as? String ?: ""
        val attendance = (m["attendance"] as? List<String>) ?: emptyList()
        Event(id, title, date, time, location, category, description, attendance)
    }

    val tasksRaw = (map["tasks"] as? List<Map<String, Any?>>) ?: emptyList()
    val tasks = tasksRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        val title = m["title"] as? String ?: return@mapNotNull null
        val due = (m["due"] as? String)?.ifBlank { null }
        val priority = m["priority"] as? String ?: "media"
        val notes = m["notes"] as? String ?: ""
        val done = (m["done"] as? Boolean) ?: (m["isCompleted"] as? Boolean) ?: false
        val createdAt = m["createdAt"] as? String ?: currentIsoDate()
        Task(id, title, due, priority, notes, done, createdAt)
    }

    val notesRaw = (map["notes"] as? List<Map<String, Any?>>) ?: emptyList()
    val notes = notesRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        val title = m["title"] as? String ?: return@mapNotNull null
        val body = m["body"] as? String ?: ""
        val tags = (m["tags"] as? List<String>) ?: emptyList()
        val createdAt = m["createdAt"] as? String ?: currentIsoDate()
        val updatedAt = m["updatedAt"] as? String ?: currentIsoDate()
        Note(id, title, body, tags, createdAt, updatedAt)
    }

    val groupsRaw = (map["groups"] as? List<Map<String, Any?>>) ?: emptyList()
    val groups = groupsRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        val name = m["name"] as? String ?: return@mapNotNull null
        val leader = m["leader"] as? String ?: ""
        val description = m["description"] as? String ?: ""
        Group(id, name, leader, description)
    }

    val personsRaw = (map["persons"] as? List<Map<String, Any?>>) ?: emptyList()
    val persons = personsRaw.mapNotNull { m ->
        val id = m["id"] as? String ?: return@mapNotNull null
        val name = m["name"] as? String ?: return@mapNotNull null
        val role = m["role"] as? String ?: "Alumno"
        val group = m["group"] as? String ?: ""
        val contact = m["contact"] as? String ?: ""
        val notes = m["notes"] as? String ?: ""
        Person(id, name, role, group, contact, notes)
    }

    val settingsRaw = map["settings"] as? Map<String, Any?>
    val settings = if (settingsRaw != null) {
        PastoralSettings(
            pastorName = settingsRaw["pastorName"] as? String ?: "",
            schoolName = settingsRaw["schoolName"] as? String ?: "",
            theme = settingsRaw["theme"] as? String ?: "light",
            isConfigured = (settingsRaw["isConfigured"] as? Boolean) ?: false
        )
    } else {
        PastoralSettings()
    }

    return PastoralData(
        notes = notes,
        events = events,
        tasks = tasks,
        groups = groups,
        persons = persons,
        chat = emptyList(),
        settings = settings
    )
}
