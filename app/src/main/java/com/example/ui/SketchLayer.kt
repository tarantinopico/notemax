package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch

@Composable
fun SketchLayer(
    modifier: Modifier = Modifier,
    strokes: List<DrawingStroke>,
    isEditable: Boolean,
    onStrokeAdded: (DrawingStroke) -> Unit,
    activeTool: ToolType,
    activeColor: Color,
    activeStrokeWidth: Float
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var scale by remember { mutableStateOf(1f) } // Assuming 1f for simplicity

    Canvas(
        modifier = modifier
            .pointerInput(isEditable, activeTool, activeColor, activeStrokeWidth) {
                if (!isEditable) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints = listOf(offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        if (currentPoints.isNotEmpty()) {
                            onStrokeAdded(
                                DrawingStroke(
                                    points = currentPoints.toList(),
                                    color = activeColor.value.toLong(),
                                    strokeWidth = activeStrokeWidth,
                                    toolType = activeTool
                                )
                            )
                            currentPoints = emptyList()
                        }
                    },
                    onDragCancel = {
                        currentPoints = emptyList()
                    }
                )
            }
    ) {
        val drawStroke = { path: Path, color: Color, width: Float, tool: ToolType ->
            val strokeAlpha = if (tool == ToolType.HIGHLIGHTER) 0.4f else 1f
            if (tool == ToolType.ERASER) {
                drawPath(
                    path = path,
                    color = Color.Transparent,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = BlendMode.Clear
                )
            } else {
                drawPath(
                    path = path,
                    color = color.copy(alpha = strokeAlpha),
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        
        strokes.forEach { stroke ->
            drawStroke(stroke.toPath(), Color(stroke.color.toULong()), stroke.strokeWidth, stroke.toolType)
        }

        if (currentPoints.isNotEmpty()) {
            val path = Path()
            path.moveTo(currentPoints.first().x, currentPoints.first().y)
            for (i in 1 until currentPoints.size) {
                path.lineTo(currentPoints[i].x, currentPoints[i].y)
            }
            drawStroke(path, activeColor, activeStrokeWidth, activeTool)
        }
    }
}

@Composable
fun DrawingToolbar(
    activeTool: ToolType,
    onToolSelected: (ToolType) -> Unit,
    activeColor: Color,
    onColorSelected: (Color) -> Unit,
    activeStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    onClearAll: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val colors = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFA500), Color.Magenta
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton("Pen", ToolType.PEN == activeTool) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToolSelected(ToolType.PEN) 
            }
            ToolButton("Highlight", ToolType.HIGHLIGHTER == activeTool) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToolSelected(ToolType.HIGHLIGHTER) 
            }
            ToolButton("Eraser", ToolType.ERASER == activeTool) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToolSelected(ToolType.ERASER) 
            }
            IconButton(onClick = onClearAll) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEach { color ->
                val isSelected = activeColor == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onColorSelected(color) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
    }
}
