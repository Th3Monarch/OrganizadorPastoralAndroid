package com.example.pastoral.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.*
import org.commonmark.node.*
import org.commonmark.parser.Parser

/**
 * Singleton CommonMark parser configured with GFM Tables and Strikethrough extensions.
 */
private val commonMarkParser: Parser by lazy {
    Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create()
            )
        )
        .build()
}

/**
 * Returns an immutable list of direct children of this Node.
 */
fun Node.children(): List<Node> {
    val list = mutableListOf<Node>()
    var cur = this.firstChild
    while (cur != null) {
        list.add(cur)
        cur = cur.next
    }
    return list
}

/**
 * Removes internal code action blocks (e.g. ```action ... ```) or JSON payloads
 * from AI responses before presenting to the user.
 */
fun cleanRawAiContent(text: String): String {
    val withoutActions = text
        .replace(Regex("""```(?:action|json)\s*[\s\S]*?\s*```""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""```action\s*[\s\S]*$""", RegexOption.IGNORE_CASE), "")
        .trim()

    // 1. Normalize unicode bullets '• ' at start of lines into standard Markdown '- '
    val withBullets = withoutActions.replace(Regex("""(?m)^([ \t]*)•[ \t]+"""), "$1- ")

    // 2. Ensure table blocks have a preceding newline so GFM tables extension parses reliably
    val withTableSpacing = withBullets.replace(
        Regex("""(?m)([^\n| \t].*)\n([ \t]*\|[^\n]+\|[ \t]*\n[ \t]*\|[-: |]+\|)"""),
        "$1\n\n$2"
    )

    return withTableSpacing
}

/**
 * Parses inline CommonMark nodes (Text, Bold, Italic, Strikethrough, Code, Links)
 * into a rich Compose AnnotatedString, completely stripping raw asterisks and delimiters.
 */
fun buildAnnotatedStringFromNodes(
    startNode: Node?,
    baseColor: Color,
    linkColor: Color,
    codeBgColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        appendInlineNode(startNode, this, baseColor, linkColor, codeBgColor)
    }
}

private fun appendInlineNode(
    node: Node?,
    builder: AnnotatedString.Builder,
    baseColor: Color,
    linkColor: Color,
    codeBgColor: Color
) {
    var current = node
    while (current != null) {
        when (current) {
            is Text -> {
                // Strip escaping backslashes if present (\* -> *)
                val literal = current.literal.replace("\\*", "*").replace("\\_", "_")
                builder.append(literal)
            }
            is StrongEmphasis -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                    appendInlineNode(current.firstChild, builder, baseColor, linkColor, codeBgColor)
                }
            }
            is Emphasis -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                    appendInlineNode(current.firstChild, builder, baseColor, linkColor, codeBgColor)
                }
            }
            is Strikethrough -> {
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = baseColor)) {
                    appendInlineNode(current.firstChild, builder, baseColor, linkColor, codeBgColor)
                }
            }
            is Code -> {
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        fontSize = 13.sp,
                        color = baseColor
                    )
                ) {
                    builder.append(" ${current.literal} ")
                }
            }
            is Link -> {
                builder.withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    appendInlineNode(current.firstChild, builder, linkColor, linkColor, codeBgColor)
                }
            }
            is HardLineBreak -> {
                builder.append("\n")
            }
            is SoftLineBreak -> {
                builder.append(" ")
            }
            else -> {
                // Fallback for custom or nested nodes
                if (current.firstChild != null) {
                    appendInlineNode(current.firstChild, builder, baseColor, linkColor, codeBgColor)
                }
            }
        }
        current = current.next
    }
}

/**
 * Full Markdown viewer component that interprets headings, tables, bullet/numbered lists,
 * quotes, bold, italics, dividers, and code blocks with polished Material 3 styling.
 */
@Composable
fun PastoralMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val cleanedText = remember(text) { cleanRawAiContent(text) }
    if (cleanedText.isBlank()) return

    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val document = remember(cleanedText) {
        try {
            commonMarkParser.parse(cleanedText)
        } catch (_: Exception) {
            null
        }
    }

    if (document == null) {
        Text(text = cleanedText, modifier = modifier, style = style, color = resolvedColor)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        document.children().forEach { child ->
            RenderMarkdownBlock(
                node = child,
                style = style,
                textColor = resolvedColor,
                linkColor = linkColor,
                codeBgColor = codeBgColor
            )
        }
    }
}

/**
 * Renders individual CommonMark AST block nodes.
 */
@Composable
private fun RenderMarkdownBlock(
    node: Node,
    style: TextStyle,
    textColor: Color,
    linkColor: Color,
    codeBgColor: Color
) {
    when (node) {
        is Heading -> {
            val headingStyle = when (node.level) {
                1 -> MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                2 -> MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                3 -> MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor
                )
                else -> MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
            }
            val annotated = remember(node, textColor) {
                buildAnnotatedStringFromNodes(node.firstChild, textColor, linkColor, codeBgColor)
            }
            Text(
                text = annotated,
                style = headingStyle,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        is Paragraph -> {
            val annotated = remember(node, textColor) {
                buildAnnotatedStringFromNodes(node.firstChild, textColor, linkColor, codeBgColor)
            }
            Text(
                text = annotated,
                style = style,
                color = textColor
            )
        }

        is TableBlock -> {
            MarkdownTable(
                tableBlock = node,
                textColor = textColor,
                linkColor = linkColor,
                codeBgColor = codeBgColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        is BulletList -> {
            MarkdownBulletList(
                listNode = node,
                style = style,
                textColor = textColor,
                linkColor = linkColor,
                codeBgColor = codeBgColor
            )
        }

        is OrderedList -> {
            MarkdownOrderedList(
                listNode = node,
                style = style,
                textColor = textColor,
                linkColor = linkColor,
                codeBgColor = codeBgColor
            )
        }

        is BlockQuote -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (quoteChild in node.children()) {
                            if (quoteChild is Paragraph) {
                                val quoteAnnotated = remember(quoteChild, textColor) {
                                    buildAnnotatedStringFromNodes(
                                        quoteChild.firstChild,
                                        textColor,
                                        linkColor,
                                        codeBgColor
                                    )
                                }
                                Text(
                                    text = quoteAnnotated,
                                    style = style.copy(fontStyle = FontStyle.Italic, fontSize = 13.5.sp),
                                    color = textColor
                                )
                            } else {
                                RenderMarkdownBlock(
                                    node = quoteChild,
                                    style = style,
                                    textColor = textColor,
                                    linkColor = linkColor,
                                    codeBgColor = codeBgColor
                                )
                            }
                        }
                    }
                }
            }
        }

        is FencedCodeBlock -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (!node.info.isNullOrBlank()) {
                        Text(
                            text = node.info.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    val scrollState = rememberScrollState()
                    Text(
                        text = node.literal.trimEnd(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = textColor
                        ),
                        modifier = Modifier.horizontalScroll(scrollState)
                    )
                }
            }
        }

        is ThematicBreak -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        else -> {
            // Render any child nodes
            var otherChild: Node? = node.firstChild
            while (otherChild != null) {
                RenderMarkdownBlock(
                    node = otherChild,
                    style = style,
                    textColor = textColor,
                    linkColor = linkColor,
                    codeBgColor = codeBgColor
                )
                otherChild = otherChild.next
            }
        }
    }
}

/**
 * Formats and renders a Markdown Table with horizontal scrolling, subtle borders,
 * distinct header styling, and rich cell contents.
 */
@Composable
private fun MarkdownTable(
    tableBlock: TableBlock,
    textColor: Color,
    linkColor: Color,
    codeBgColor: Color,
    modifier: Modifier = Modifier
) {
    // Extract headers and rows
    val headerCells = mutableListOf<TableCell>()
    val bodyRows = mutableListOf<List<TableCell>>()

    for (section in tableBlock.children()) {
        when (section) {
            is TableHead -> {
                for (row in section.children().filterIsInstance<TableRow>()) {
                    headerCells.addAll(row.children().filterIsInstance<TableCell>())
                }
            }
            is TableBody -> {
                for (row in section.children().filterIsInstance<TableRow>()) {
                    val rowCells = row.children().filterIsInstance<TableCell>()
                    if (rowCells.isNotEmpty()) bodyRows.add(rowCells)
                }
            }
        }
    }

    if (headerCells.isEmpty() && bodyRows.isEmpty()) return

    val totalColumns = maxOf(headerCells.size, bodyRows.maxOfOrNull { it.size } ?: 0)
    val horizontalScroll = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            Column {
                // 1. Header Row
                if (headerCells.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            headerCells.forEachIndexed { colIndex, cell ->
                                val annotated = remember(cell, textColor) {
                                    buildAnnotatedStringFromNodes(cell.firstChild, textColor, linkColor, codeBgColor)
                                }
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 90.dp, max = 220.dp)
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = annotated,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        textAlign = when (cell.alignment) {
                                            TableCell.Alignment.CENTER -> TextAlign.Center
                                            TableCell.Alignment.RIGHT -> TextAlign.End
                                            else -> TextAlign.Start
                                        }
                                    )
                                }
                                if (colIndex < headerCells.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                }

                // 2. Body Rows
                bodyRows.forEachIndexed { rowIndex, row ->
                    val isAlt = rowIndex % 2 == 1
                    Surface(
                        color = if (isAlt) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (colIndex in 0 until totalColumns) {
                                val cell = row.getOrNull(colIndex)
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 90.dp, max = 220.dp)
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    if (cell != null) {
                                        val annotated = remember(cell, textColor) {
                                            buildAnnotatedStringFromNodes(cell.firstChild, textColor, linkColor, codeBgColor)
                                        }
                                        Text(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                            color = textColor,
                                            textAlign = when (cell.alignment) {
                                                TableCell.Alignment.CENTER -> TextAlign.Center
                                                TableCell.Alignment.RIGHT -> TextAlign.End
                                                else -> TextAlign.Start
                                            }
                                        )
                                    }
                                }
                                if (colIndex < totalColumns - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    )
                                }
                            }
                        }
                    }
                    if (rowIndex < bodyRows.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

/**
 * Renders bullet lists and interactive checklist items.
 */
@Composable
private fun MarkdownBulletList(
    listNode: BulletList,
    style: TextStyle,
    textColor: Color,
    linkColor: Color,
    codeBgColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (item in listNode.children().filterIsInstance<ListItem>()) {
            // Check if this item is a task checklist: [ ] or [x]
            val firstParagraph = item.firstChild as? Paragraph
            val firstText = firstParagraph?.firstChild as? Text
            val literal = firstText?.literal ?: ""

            val isTaskItem = literal.startsWith("[ ] ") || literal.startsWith("[x] ") || literal.startsWith("[X] ")
            val isChecked = literal.startsWith("[x] ") || literal.startsWith("[X] ")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (isTaskItem) {
                    Icon(
                        imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = if (isChecked) "Completado" else "Pendiente",
                        tint = if (isChecked) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp, top = 2.dp)
                    )
                } else {
                    Text(
                        text = "•",
                        style = style.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp, start = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (child in item.children()) {
                        if (child is Paragraph) {
                            val itemAnnotated = remember(child, textColor, isTaskItem) {
                                val cleaned = buildAnnotatedStringFromNodes(
                                    child.firstChild,
                                    textColor,
                                    linkColor,
                                    codeBgColor
                                )
                                if (isTaskItem) {
                                    // Remove the leading "[ ] " or "[x] " marker from rendered text
                                    cleaned.subSequence(4, cleaned.length)
                                } else {
                                    cleaned
                                }
                            }
                            Text(
                                text = itemAnnotated,
                                style = style,
                                color = textColor
                            )
                        } else {
                            RenderMarkdownBlock(
                                node = child,
                                style = style,
                                textColor = textColor,
                                linkColor = linkColor,
                                codeBgColor = codeBgColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders numbered lists with proper number prefixes and nesting.
 */
@Composable
private fun MarkdownOrderedList(
    listNode: OrderedList,
    style: TextStyle,
    textColor: Color,
    linkColor: Color,
    codeBgColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val items = listNode.children().filterIsInstance<ListItem>()
        items.forEachIndexed { i, item ->
            val index = listNode.startNumber + i
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$index.",
                    style = style.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (child in item.children()) {
                        if (child is Paragraph) {
                            val itemAnnotated = remember(child, textColor) {
                                buildAnnotatedStringFromNodes(
                                    child.firstChild,
                                    textColor,
                                    linkColor,
                                    codeBgColor
                                )
                            }
                            Text(
                                text = itemAnnotated,
                                style = style,
                                color = textColor
                            )
                        } else {
                            RenderMarkdownBlock(
                                node = child,
                                style = style,
                                textColor = textColor,
                                linkColor = linkColor,
                                codeBgColor = codeBgColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact inline Markdown text composable, ideal for card previews, lists, and titles.
 * Eliminates asterisks and formats inline bold, italic, code, and strikethrough.
 */
@Composable
fun PastoralInlineMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val cleanedText = remember(text) { cleanRawAiContent(text) }
    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val document = remember(cleanedText) {
        try {
            commonMarkParser.parse(cleanedText)
        } catch (_: Exception) {
            null
        }
    }

    val annotated = remember(document, resolvedColor) {
        if (document != null) {
            // Find first block with inline nodes (e.g. Paragraph or Heading)
            var n: Node? = document.firstChild
            var inlineStart: Node? = null
            while (n != null) {
                if (n is Paragraph || n is Heading) {
                    inlineStart = n.firstChild
                    break
                }
                n = n.next
            }
            buildAnnotatedStringFromNodes(inlineStart ?: document.firstChild, resolvedColor, linkColor, codeBgColor)
        } else {
            AnnotatedString(cleanedText)
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = resolvedColor,
        maxLines = maxLines,
        overflow = overflow
    )
}
