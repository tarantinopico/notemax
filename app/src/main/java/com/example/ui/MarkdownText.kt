package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val annotatedString = parseMarkdown(text)
    Text(
        text = annotatedString,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Very basic markdown parsing for bold, italics, headings
        // Format: **bold**, *italics*, # Heading 1, ## Heading 2, ### Heading 3
        val regex = Regex("(?m)(^#{1,3}\\s.*$)|(\\*\\*.*?\\*\\*)|(\\*.*?\\*)")
        val matches = regex.findAll(text)

        for (match in matches) {
            val startIndex = match.range.first
            val endIndex = match.range.last + 1
            
            if (startIndex > currentIndex) {
                append(text.substring(currentIndex, startIndex))
            }
            
            val matchValue = match.value
            when {
                matchValue.startsWith("### ") -> {
                    withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                        append(matchValue.removePrefix("### "))
                    }
                }
                matchValue.startsWith("## ") -> {
                    withStyle(style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                        append(matchValue.removePrefix("## "))
                    }
                }
                matchValue.startsWith("# ") -> {
                    withStyle(style = SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                        append(matchValue.removePrefix("# "))
                    }
                }
                matchValue.startsWith("**") && matchValue.endsWith("**") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchValue.removeSurrounding("**"))
                    }
                }
                matchValue.startsWith("*") && matchValue.endsWith("*") -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchValue.removeSurrounding("*"))
                    }
                }
                else -> append(matchValue)
            }
            currentIndex = endIndex
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
