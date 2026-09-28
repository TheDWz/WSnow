package com.wsnow.app.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.wsnow.app.ui.theme.HighlightColors
import com.wsnow.engine.Cell
import com.wsnow.engine.Line
import com.wsnow.engine.Puzzle
import com.wsnow.engine.Selection

/**
 * The letter grid. Press on a letter and drag in any straight line to select; the selection snaps
 * to the nearest of the eight directions. [onSelect] is called when the finger lifts.
 */
@Composable
fun WordGrid(
    puzzle: Puzzle,
    found: Map<String, Line>,
    hint: Cell?,
    onSelect: (Line) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = puzzle.size
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    var selection by remember { mutableStateOf<Line?>(null) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val wordIndex = remember(puzzle) { puzzle.words.withIndex().associate { (i, w) -> w.letters to i } }

    BoxWithConstraints(modifier) {
        val side = minOf(maxWidth, maxHeight)
        val cellPx = with(LocalDensity.current) { side.toPx() } / n
        val fontSize = with(LocalDensity.current) { (cellPx * 0.55f).toSp() }
        val letterStyle = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Medium, color = colors.onSurface)
        val letters = remember(puzzle, fontSize, colors.onSurface) {
            ('A'..'Z').associateWith { textMeasurer.measure(it.toString(), letterStyle) }
        }

        Canvas(
            Modifier
                .size(side)
                .semantics { contentDescription = "Word search grid, $n by $n" }
                .pointerInput(puzzle.id) {
                    fun cellAt(p: Offset): Cell {
                        val cell = size.width.toFloat() / n
                        return Cell(
                            (p.y / cell).toInt().coerceIn(0, n - 1),
                            (p.x / cell).toInt().coerceIn(0, n - 1),
                        )
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val start = cellAt(down.position)
                        selection = Line(start, start)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            selection = Selection.snap(start, cellAt(change.position), n)
                            change.consume()
                        }
                        selection?.let { if (it.start != it.end) currentOnSelect(it) }
                        selection = null
                    }
                },
        ) {
            val cell = size.width / n
            drawRoundRect(colors.surfaceContainerHighest, cornerRadius = CornerRadius(cell * 0.4f))

            found.forEach { (word, line) ->
                val color = HighlightColors[(wordIndex[word] ?: 0) % HighlightColors.size]
                drawCapsule(line, cell, color.copy(alpha = 0.45f))
            }
            selection?.let { drawCapsule(it, cell, colors.primary.copy(alpha = 0.35f)) }
            hint?.let {
                drawCircle(
                    colors.tertiary,
                    radius = cell * 0.46f,
                    center = centerOf(it, cell),
                    style = Stroke(width = cell * 0.1f),
                )
            }

            for (r in 0 until n) for (c in 0 until n) {
                val layout = letters[puzzle.rows[r][c]] ?: continue
                val center = centerOf(Cell(r, c), cell)
                drawText(
                    layout,
                    topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
                )
            }
        }
    }
}

private fun centerOf(cell: Cell, size: Float) = Offset((cell.col + 0.5f) * size, (cell.row + 0.5f) * size)

private fun DrawScope.drawCapsule(line: Line, cell: Float, color: Color) {
    drawLine(
        color = color,
        start = centerOf(line.start, cell),
        end = centerOf(line.end, cell),
        strokeWidth = cell * 0.8f,
        cap = StrokeCap.Round,
    )
}
