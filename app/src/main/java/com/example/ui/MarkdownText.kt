package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, onNoteLinkClick: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val annotatedString = parseMarkdownImproved(text, colorScheme.primary)
    
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, lineHeight = 28.sp),
        onClick = { offset ->
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
    )
}

@Composable
fun parseMarkdownImproved(text: String, primaryColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        
        for ((index, line) in lines.withIndex()) {
            if (line.isBlank() || line.isEmpty()) {
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
                    // Bullet point: transform asterisk/dash to •
                    textToProcess = textToProcess.replaceFirst(Regex("^[\\-\\*]\\s+"), "•  ")
                }
                textToProcess.trim().startsWith("|") && textToProcess.trim().endsWith("|") -> {
                    currentLineStyle = SpanStyle(
                        background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
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

fun parseInlineMarkdown(builder: AnnotatedString.Builder, text: String, primaryColor: androidx.compose.ui.graphics.Color) {
    val regex = Regex("(\\[\\[.*?\\]\\])|(\\*\\*.*?\\*\\*)|(<u>.*?</u>)|(\\*.*?\\*)|(\\[.*?\\]\\(.*?\\))")
    val matches = regex.findAll(text)
    
    var currentIndex = 0
    for (match in matches) {
        if (currentIndex < match.range.first) {
            builder.append(text.substring(currentIndex, match.range.first))
        }
        
        val matchText = match.value
        when {
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
