package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

sealed class MdBlock {
    data class TextBlock(val text: String) : MdBlock()
    data class TableBlock(val rows: List<List<String>>) : MdBlock()
}

fun parseMarkdownBlocks(text: String): List<MdBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MdBlock>()
    var currentText = StringBuilder()
    var inTable = false
    val currentTable = mutableListOf<List<String>>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            if (!inTable) {
                if (currentText.isNotEmpty()) {
                    blocks.add(MdBlock.TextBlock(currentText.toString()))
                    currentText = StringBuilder()
                }
                inTable = true
            }
            val row = trimmed.removeSurrounding("|", "|").split("|").map { it.trim() }
            if (row.all { it.replace("-", "").isBlank() }) {
                // skip separator row
            } else {
                currentTable.add(row)
            }
        } else {
            if (inTable) {
                blocks.add(MdBlock.TableBlock(currentTable.toList()))
                currentTable.clear()
                inTable = false
            }
            currentText.append(line).append("\n")
        }
    }
    
    if (inTable) {
        blocks.add(MdBlock.TableBlock(currentTable.toList()))
    } else if (currentText.isNotEmpty()) {
        blocks.add(MdBlock.TextBlock(currentText.toString()))
    }
    
    return blocks
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, onNoteLinkClick: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val blocks = parseMarkdownBlocks(text)
    
    val inlineContent = mapOf(
        "important" to InlineTextContent(
            Placeholder(120.sp, 26.sp, PlaceholderVerticalAlign.TextCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("IMPORTANT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        "todo" to InlineTextContent(
            Placeholder(80.sp, 26.sp, PlaceholderVerticalAlign.TextCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("TODO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }
        },
        "arrow" to InlineTextContent(
            Placeholder(44.sp, 24.sp, PlaceholderVerticalAlign.TextCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    )

    val tagRegex = Regex("[$]{2}TAG_BEGIN[$]{2}color:([0-9A-Fa-f]{6}),textColor:([0-9A-Fa-f]{6}),text:(.*?)[$]{2}TAG_END[$]{2}")
    val contentMap = mutableMapOf<String, InlineTextContent>()
    contentMap.putAll(inlineContent)
    
    tagRegex.findAll(text).forEach { match ->
        val colorStr = match.groupValues[1]
        val textColorStr = match.groupValues[2]
        val textStr = match.groupValues[3]
        
        val bgColor = Color(android.graphics.Color.parseColor("#$colorStr"))
        val fgColor = Color(android.graphics.Color.parseColor("#$textColorStr"))
        
        val id = "tag_${colorStr}_${textColorStr}_${textStr}"
        contentMap[id] = InlineTextContent(
            Placeholder(
                width = (textStr.length * 8 + 24).sp,
                height = 24.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(textStr, color = fgColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.TextBlock -> {
                    val annotatedString = parseMarkdownImproved(block.text, colorScheme.primary)
                    var layoutResult by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, lineHeight = 28.sp),
                        inlineContent = contentMap,
                        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                            detectTapGestures { pos ->
                                layoutResult?.let { layout ->
                                    val offset = layout.getOffsetForPosition(pos)
                                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            try {
                                                val uri = Uri.parse(annotation.item)
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                if (annotation.item.startsWith("file://") || annotation.item.startsWith("content://")) {
                                                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    annotatedString.getStringAnnotations(tag = "WIKILINK", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            onNoteLinkClick?.invoke(annotation.item)
                                        }
                                }
                            }
                        },
                        onTextLayout = { layoutResult = it }
                    )
                }
                is MdBlock.TableBlock -> {
                    MarkdownTable(block)
                }
            }
        }
    }
}

@Composable
fun MarkdownTable(block: MdBlock.TableBlock) {
    if (block.rows.isEmpty()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Column {
                block.rows.forEachIndexed { rowIndex, row ->
                    val isHeader = rowIndex == 0
                    Row(
                        modifier = Modifier.background(
                            if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            else if (rowIndex % 2 == 0) Color.Transparent
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .widthIn(min = 100.dp, max = 300.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun parseMarkdownImproved(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        
        for ((index, line) in lines.withIndex()) {
            if (line.isEmpty()) {
                append("\n")
                continue
            }
            
            var currentLineStyle = SpanStyle()
            var textToProcess = line
            
            when {
                textToProcess.startsWith("### ") -> {
                    currentLineStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    textToProcess = textToProcess.removePrefix("### ")
                }
                textToProcess.startsWith("## ") -> {
                    currentLineStyle = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    textToProcess = textToProcess.removePrefix("## ")
                }
                textToProcess.startsWith("# ") -> {
                    currentLineStyle = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    textToProcess = textToProcess.removePrefix("# ")
                }
                textToProcess.matches(Regex("^\\d+\\.\\s+(.*)")) -> {
                    currentLineStyle = SpanStyle(fontWeight = FontWeight.Medium)
                }
                textToProcess.matches(Regex("^[\\-\\*]\\s+(.*)")) -> {
                    textToProcess = textToProcess.replaceFirst(Regex("^[\\-\\*]\\s+"), "•  ")
                }
            }
            
            withStyle(currentLineStyle) {
                parseInlineMarkdown(this@buildAnnotatedString, textToProcess, primaryColor)
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

fun parseInlineMarkdown(builder: AnnotatedString.Builder, text: String, primaryColor: Color) {
    val regex = Regex("(#important)|(@todo)|(==>)|([$]{2}TAG_BEGIN[$]{2}.*?[$]{2}TAG_END[$]{2})|(\\[\\[.*?\\]\\])|(\\*\\*.*?\\*\\*)|(<u>.*?</u>)|(\\*.*?\\*)|(\\[.*?\\]\\(.*?\\))")
    val tagInnerRegex = Regex("[$]{2}TAG_BEGIN[$]{2}color:([0-9A-Fa-f]{6}),textColor:([0-9A-Fa-f]{6}),text:(.*?)[$]{2}TAG_END[$]{2}")
    val matches = regex.findAll(text)
    
    var currentIndex = 0
    for (match in matches) {
        if (currentIndex < match.range.first) {
            builder.append(text.substring(currentIndex, match.range.first))
        }
        
        val matchText = match.value
        when {
            matchText.startsWith("$$" + "TAG_BEGIN" + "$$") -> {
                val tagMatch = tagInnerRegex.matchEntire(matchText)
                if (tagMatch != null) {
                    val colorStr = tagMatch.groupValues[1]
                    val textColorStr = tagMatch.groupValues[2]
                    val textStr = tagMatch.groupValues[3]
                    val id = "tag_${colorStr}_${textColorStr}_${textStr}"
                    builder.appendInlineContent(id, textStr)
                } else {
                    builder.append(matchText)
                }
            }
            matchText == "#important" -> {
                builder.appendInlineContent("important", "[Important]")
            }
            matchText == "@todo" -> {
                builder.appendInlineContent("todo", "[Todo]")
            }
            matchText == "==>" -> {
                builder.appendInlineContent("arrow", "==>")
            }
            matchText.startsWith("[[") && matchText.endsWith("]]") -> {
                val title = matchText.substring(2, matchText.length - 2)
                builder.pushStringAnnotation(tag = "WIKILINK", annotation = title)
                builder.withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append(title)
                }
                builder.pop()
            }
            matchText.startsWith("**") && matchText.endsWith("**") -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(matchText.removeSurrounding("**"))
                }
            }
            matchText.startsWith("*") && matchText.endsWith("*") -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(matchText.removeSurrounding("*"))
                }
            }
            matchText.startsWith("<u>") && matchText.endsWith("</u>") -> {
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(matchText.removeSurrounding("<u>", "</u>"))
                }
            }
            matchText.matches(Regex("\\[.*?\\]\\(.*?\\)")) -> {
                val title = matchText.substringAfter("[").substringBefore("]")
                val url = matchText.substringAfter("(").substringBefore(")")
                
                builder.pushStringAnnotation(tag = "URL", annotation = url)
                builder.withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                    append(title)
                }
                builder.pop()
            }
            else -> builder.append(matchText)
        }
        currentIndex = match.range.last + 1
    }
    
    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }
}

