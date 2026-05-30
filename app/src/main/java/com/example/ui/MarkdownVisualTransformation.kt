package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

class MarkdownVisualTransformation(private val primaryColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            append(text.text)
            
            val lines = text.text.split("\n")
            var currentIndex = 0
            for (line in lines) {
                when {
                    line.startsWith("### ") -> addStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), currentIndex, currentIndex + line.length)
                    line.startsWith("## ") -> addStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), currentIndex, currentIndex + line.length)
                    line.startsWith("# ") -> addStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold), currentIndex, currentIndex + line.length)
                }
                currentIndex += line.length + 1
            }
            
            val regex = Regex("(#important)|(@todo)|(==>)|([$]{2}TAG_BEGIN[$]{2}.*?[$]{2}TAG_END[$]{2})|(\\[\\[.*?\\]\\])|(\\*\\*.*?\\*\\*)|(<u>.*?</u>)|(\\*.*?\\*)|(\\[.*?\\]\\(.*?\\))")
            val tagInnerRegex = Regex("[$]{2}TAG_BEGIN[$]{2}color:([0-9A-Fa-f]{6}),textColor:([0-9A-Fa-f]{6}),text:(.*?)[$]{2}TAG_END[$]{2}")
            val matches = regex.findAll(text.text)
            for (match in matches) {
                val matchText = match.value
                when {
                    matchText.startsWith("$$" + "TAG_BEGIN" + "$$") -> {
                        val tagMatch = tagInnerRegex.matchEntire(matchText)
                        if (tagMatch != null) {
                            val bgColor = Color(android.graphics.Color.parseColor("#" + tagMatch.groupValues[1]))
                            val fgColor = Color(android.graphics.Color.parseColor("#" + tagMatch.groupValues[2]))
                            addStyle(SpanStyle(background = bgColor.copy(alpha = 0.2f), color = fgColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                        } else {
                            addStyle(SpanStyle(color = Color.Gray), match.range.first, match.range.last + 1)
                        }
                    }
                    matchText == "#important" -> addStyle(SpanStyle(background = Color(0x33FF0000), color = Color(0xFFFF0000), fontWeight = FontWeight.Bold, fontSize = 12.sp), match.range.first, match.range.last + 1)
                    matchText == "@todo" -> addStyle(SpanStyle(background = Color(0x330000FF), color = Color(0xFF0000FF), fontWeight = FontWeight.Bold, fontSize = 12.sp), match.range.first, match.range.last + 1)
                    matchText == "==>" -> addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                    matchText.startsWith("[[") && matchText.endsWith("]]") -> addStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
                    matchText.startsWith("**") && matchText.endsWith("**") -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                    matchText.startsWith("*") && matchText.endsWith("*") -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                    matchText.startsWith("<u>") && matchText.endsWith("</u>") -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
                    matchText.matches(Regex("\\[.*?\\]\\(.*?\\)")) -> addStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
                }
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
