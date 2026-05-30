package com.example.ui

import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.abs

class EditorHistory {
    private val history = mutableListOf<TextFieldValue>()
    private var currentIndex = -1

    val canUndo get() = currentIndex > 0
    val canRedo get() = currentIndex < history.lastIndex

    fun push(value: TextFieldValue) {
        if (currentIndex >= 0 && history[currentIndex].text == value.text) {
            history[currentIndex] = value
            return
        }

        if (currentIndex < history.lastIndex) {
            val elementsToKeep = history.subList(0, currentIndex + 1)
            history.clear()
            history.addAll(elementsToKeep)
        }

        history.add(value)
        if (history.size > 200) {
            history.removeAt(0)
        } else {
            currentIndex++
        }
    }

    fun undo(): TextFieldValue? {
        if (canUndo) {
            currentIndex--
            return history[currentIndex]
        }
        return null
    }

    fun redo(): TextFieldValue? {
        if (canRedo) {
            currentIndex++
            return history[currentIndex]
        }
        return null
    }
}
