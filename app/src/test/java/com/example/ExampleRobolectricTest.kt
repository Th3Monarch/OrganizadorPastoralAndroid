package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pastoral.assistant.PastoralAssistantEngine
import com.example.pastoral.data.repository.PastoralRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Organizador Pastoral", appName)
    }

    @Test
    fun `AI engine creates task when told escribeme una tarea`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PastoralRepository(context)
        val engine = PastoralAssistantEngine(repository)

        val initialTasksCount = repository.data.value.tasks.size
        val result = engine.processMessage("Escríbeme una tarea: Comprar cirios para mañana, prioridad alta")

        assertEquals("task_created", result.actionType)
        val updatedTasks = repository.data.value.tasks
        assertEquals(initialTasksCount + 1, updatedTasks.size)
        val created = updatedTasks.first()
        assertTrue(created.title.contains("Comprar cirios", ignoreCase = true))
        assertEquals("alta", created.priority)
        assertNotNull(created.due)
    }

    @Test
    fun `AI engine creates event when told haz un evento`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PastoralRepository(context)
        val engine = PastoralAssistantEngine(repository)

        val initialEventsCount = repository.data.value.events.size
        val result = engine.processMessage("Haz un evento: Retiro de Adviento el 2026-11-20 a las 09:00 en la capilla categoría Retiro")

        assertEquals("event_created", result.actionType)
        val updatedEvents = repository.data.value.events
        assertEquals(initialEventsCount + 1, updatedEvents.size)
        val created = updatedEvents.find { it.title.contains("Retiro de Adviento", ignoreCase = true) }
        assertNotNull(created)
        assertEquals("2026-11-20", created?.date)
        assertEquals("09:00", created?.time)
        assertEquals("Retiro", created?.category)
    }

    @Test
    fun `AI engine creates note when told crea una nota`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PastoralRepository(context)
        val engine = PastoralAssistantEngine(repository)

        val initialNotesCount = repository.data.value.notes.size
        val result = engine.processMessage("Crea una nota con las ideas de acogida para los catequistas nuevos")

        assertEquals("note_created", result.actionType)
        val updatedNotes = repository.data.value.notes
        assertEquals(initialNotesCount + 1, updatedNotes.size)
        val created = updatedNotes.first()
        assertTrue(created.title.contains("acogida", ignoreCase = true) || created.body.contains("acogida", ignoreCase = true))
    }
}
