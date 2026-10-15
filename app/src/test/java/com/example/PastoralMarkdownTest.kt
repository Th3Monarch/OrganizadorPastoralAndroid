package com.example

import androidx.compose.ui.graphics.Color
import com.example.pastoral.ui.components.buildAnnotatedStringFromNodes
import com.example.pastoral.ui.components.cleanRawAiContent
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.junit.Assert.*
import org.junit.Test

class PastoralMarkdownTest {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .build()

    @Test
    fun testParseInlineBoldAndItalicStripsAsterisks() {
        val input = "✝ **Guía Pastoral** y *oración comunitaria*"
        val doc = parser.parse(input)
        val paragraph = doc.firstChild as Paragraph
        val annotated = buildAnnotatedStringFromNodes(
            paragraph.firstChild,
            Color.Black,
            Color.Blue,
            Color.LightGray
        )
        val plainText = annotated.text

        // Verify asterisks are stripped
        assertFalse("Should not contain raw double asterisks", plainText.contains("**"))
        assertFalse("Should not contain raw single asterisks", plainText.contains("*"))
        assertEquals("✝ Guía Pastoral y oración comunitaria", plainText)

        // Verify span styles were added
        assertTrue("Should have span styles for bold and italic", annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun testParseInlineCodeAndStrikethrough() {
        val input = "Usa el comando `hoy` y no ~~ayer~~"
        val doc = parser.parse(input)
        val paragraph = doc.firstChild as Paragraph
        val annotated = buildAnnotatedStringFromNodes(
            paragraph.firstChild,
            Color.Black,
            Color.Blue,
            Color.LightGray
        )
        val plainText = annotated.text

        assertFalse("Should not contain raw backticks", plainText.contains("`"))
        assertFalse("Should not contain raw tildes", plainText.contains("~~"))
        assertTrue("Plain text should contain code word", plainText.contains("hoy"))
        assertTrue("Plain text should contain struck word", plainText.contains("ayer"))
    }

    @Test
    fun testParseMarkdownBlocks() {
        val markdown = """
            # Título Principal Pastoral
            
            > «La paz os dejo, mi paz os doy»
            
            ## Tareas del día:
            • Comprar flores para el altar
            • Preparar catequesis
            1. Primer paso
            2. Segundo paso
            
            ---
            
            ```kotlin
            val amor = "infinito"
            ```
            
            | Actividad | Hora |
            |---|---|
            | Misa | 10:00 |
            
            Texto regular final.
        """.trimIndent()

        val cleaned = cleanRawAiContent(markdown)
        val doc = parser.parse(cleaned)
        assertNotNull(doc)

        var hasH1 = false
        var hasH2 = false
        var hasQuote = false
        var hasBullet = false
        var hasOrdered = false
        var hasDivider = false
        var hasCode = false
        var hasTable = false
        var hasParagraph = false

        var node = doc.firstChild
        while (node != null) {
            when (node) {
                is Heading -> if (node.level == 1) hasH1 = true else if (node.level == 2) hasH2 = true
                is BlockQuote -> hasQuote = true
                is BulletList -> hasBullet = true
                is OrderedList -> hasOrdered = true
                is ThematicBreak -> hasDivider = true
                is FencedCodeBlock -> hasCode = true
                is TableBlock -> hasTable = true
                is Paragraph -> hasParagraph = true
            }
            node = node.next
        }

        assertTrue("Should parse H1", hasH1)
        assertTrue("Should parse H2", hasH2)
        assertTrue("Should parse Blockquote", hasQuote)
        assertTrue("Should parse BulletList", hasBullet)
        assertTrue("Should parse OrderedList", hasOrdered)
        assertTrue("Should parse ThematicBreak", hasDivider)
        assertTrue("Should parse FencedCodeBlock", hasCode)
        assertTrue("Should parse TableBlock", hasTable)
        assertTrue("Should parse Paragraph", hasParagraph)
    }

    @Test
    fun testCleanRawAiContent() {
        val dirty = "Mensaje previo\n```action\n[{\"action\":\"create_note\",\"title\":\"Test\"}]\n```\nMensaje posterior"
        val cleaned = cleanRawAiContent(dirty)
        assertFalse("Should strip action block", cleaned.contains("```action"))
        assertTrue("Should keep conversational text", cleaned.contains("Mensaje previo"))
        assertTrue("Should keep conversational text", cleaned.contains("Mensaje posterior"))
    }
}
