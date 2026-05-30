package com.example.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val annotatedString = parseMarkdown(text)
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    )
}

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // simple regex for bold, italics, underline, links, basic headings, lists
        val regex = Regex("(?m)(^###?\\s.*$)|(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(<u>.*?</u>)|(\\[.*?\\]\\(.*?\\))|(^\\d+\\.\\s.*$)|(^[\\-\\*]\\s.*$)")
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
                matchValue.startsWith("<u>") && matchValue.endsWith("</u>") -> {
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(matchValue.removeSurrounding("<u>", "</u>"))
                    }
                }
                matchValue.matches(Regex("\\[.*?\\]\\(.*?\\)")) -> {
                    val linkTitle = matchValue.substringAfter("[").substringBefore("]")
                    val linkUrl = matchValue.substringAfter("(").substringBefore(")")
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(linkTitle)
                    }
                    pop()
                }
                matchValue.matches(Regex("(?m)^\\d+\\.\\s.*$")) -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        val num = Regex("^\\d+\\.").find(matchValue)?.value ?: "1."
                        append(num + " " + matchValue.replaceFirst(Regex("^\\d+\\.\\s"), ""))
                    }
                }
                matchValue.matches(Regex("(?m)^[\\-\\*]\\s.*$")) -> {
                    withStyle(style = SpanStyle()) {
                        append("•  " + matchValue.replaceFirst(Regex("^[\\-\\*]\\s"), ""))
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
