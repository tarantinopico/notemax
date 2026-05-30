package com.example.ui

object StringUtils {
    private val markdownRegex = Regex("[#*_\\-\\[\\]()]")

    fun extractPreviewText(content: String): String {
        val cleanedLines = content.lines()
            .map { it.replace(markdownRegex, "").trim() }
            .filter { it.isNotBlank() }
            .take(3)
        return cleanedLines.joinToString(" ").take(150)
    }
}
