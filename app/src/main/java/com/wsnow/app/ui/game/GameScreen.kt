package com.wsnow.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wsnow.app.ui.theme.HighlightColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    puzzleId: String,
    onBack: () -> Unit,
    onNewPuzzle: () -> Unit,
    vm: GameViewModel = viewModel(),
) {
    LaunchedEffect(puzzleId) { vm.load(puzzleId) }
    val game by vm.game.collectAsStateWithLifecycle()
    val hint by vm.hint.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The clock only runs while the screen is visible.
    LaunchedEffect(game?.puzzle?.id) {
        if (game == null) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(1000)
                vm.tick(1000)
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.persist() }

    var confirmRestart by remember { mutableStateOf(false) }
    // Only celebrate a puzzle finished in this session, not when reopening a solved one.
    var wasCompleteOnOpen by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var celebrated by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(game?.puzzle?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    val g = game
                    IconButton(onClick = vm::hint, enabled = g != null && !g.isComplete) {
                        Icon(Icons.Filled.Lightbulb, "Hint")
                    }
                    IconButton(onClick = { confirmRestart = true }, enabled = g != null) {
                        Icon(Icons.Filled.RestartAlt, "Restart")
                    }
                },
            )
        },
    ) { padding ->
        val g = game
        if (g == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (wasCompleteOnOpen == null) wasCompleteOnOpen = g.isComplete

        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Found ${g.found.size} / ${g.puzzle.words.size}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    formatTime(g.elapsedMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            WordGrid(
                puzzle = g.puzzle,
                found = g.found,
                hint = hint,
                onSelect = { line ->
                    if (vm.submit(line) != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                g.puzzle.words.sortedBy { it.display }.forEach { word ->
                    val isFound = word.letters in g.found
                    val index = g.puzzle.words.indexOf(word)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (isFound) HighlightColors[index % HighlightColors.size] else Color.Transparent,
                                    CircleShape,
                                ),
                        )
                        Text(
                            word.display,
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (isFound) TextDecoration.LineThrough else null,
                            color = if (isFound) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (g.isComplete && wasCompleteOnOpen == false && !celebrated) {
            AlertDialog(
                onDismissRequest = { celebrated = true },
                title = { Text("Puzzle solved!") },
                text = {
                    Text(
                        "You found all ${g.puzzle.words.size} words in ${formatTime(g.elapsedMs)}" +
                            if (g.hintsUsed > 0) " using ${g.hintsUsed} hint${if (g.hintsUsed == 1) "" else "s"}." else ".",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { celebrated = true; onNewPuzzle() }) { Text("New puzzle") }
                },
                dismissButton = {
                    TextButton(onClick = { celebrated = true }) { Text("Admire grid") }
                },
            )
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Restart puzzle?") },
            text = { Text("This clears the words you've found and resets the timer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    wasCompleteOnOpen = false
                    celebrated = false
                    vm.restart()
                }) { Text("Restart") }
            },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancel") } },
        )
    }
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
