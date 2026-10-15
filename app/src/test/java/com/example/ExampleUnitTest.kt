package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDefaultCategories() {
    assertTrue(com.example.pastoral.data.model.PastoralConstants.EVENT_CATEGORIES.contains("Retiro"))
    assertTrue(com.example.pastoral.data.model.PastoralConstants.EVENT_CATEGORIES.contains("Misa"))
    assertEquals(4, com.example.pastoral.data.model.PastoralConstants.TEMPLATES.size)
  }

  @Test
  fun testCleanRawAiContent() {
    val textWithAction = """
      ¡Paz y bien! He organizado tu agenda.
      ```action
      [{"action":"create_event","title":"Misa"}]
      ```
      ¿Deseas algo más?
    """.trimIndent()
    val cleaned = com.example.pastoral.ui.components.cleanRawAiContent(textWithAction)
    assertFalse(cleaned.contains("```action"))
    assertTrue(cleaned.contains("¡Paz y bien!"))
    assertTrue(cleaned.contains("¿Deseas algo más?"))
  }

  @Test
  fun testCommonMarkTablesAndListsParsing() {
    val sampleMarkdown = """
      # Guía Pastoral
      Aquí tienes el horario:
      | Horario | Actividad |
      |---|---|
      | 08:00 | Laudes |
      | 10:00 | **Misa Mayor** |

      - [x] Cirios preparados
      - [ ] Coro ensayado
    """.trimIndent()

    val parser = org.commonmark.parser.Parser.builder()
        .extensions(listOf(
            org.commonmark.ext.gfm.tables.TablesExtension.create(),
            org.commonmark.ext.gfm.strikethrough.StrikethroughExtension.create()
        ))
        .build()
    val cleaned = com.example.pastoral.ui.components.cleanRawAiContent(sampleMarkdown)
    val doc = parser.parse(cleaned)
    assertNotNull(doc)

    var hasTable = false
    var hasList = false
    var node = doc.firstChild
    while (node != null) {
      if (node is org.commonmark.ext.gfm.tables.TableBlock) hasTable = true
      if (node is org.commonmark.node.BulletList) hasList = true
      node = node.next
    }
    assertTrue("Should parse markdown table", hasTable)
    assertTrue("Should parse bullet list", hasList)
  }
}
