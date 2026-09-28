package com.wsnow.app.ui.create

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wsnow.app.WSnowApp
import com.wsnow.app.data.FallbackWords
import com.wsnow.app.data.SavedGame
import com.wsnow.app.data.Settings
import com.wsnow.engine.Difficulty
import com.wsnow.engine.PuzzleGenerator
import com.wsnow.engine.WordSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateUiState(
    val working: String? = null,
    val error: String? = null,
    val createdId: String? = null,
)

class CreateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WSnowApp
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _state = MutableStateFlow(CreateUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun setDifficulty(d: Difficulty) {
        viewModelScope.launch { app.settings.setDifficulty(d) }
    }

    fun generateWithAi(theme: String) {
        val s: Settings = settings.value ?: return
        start("Asking ${s.model} for words…") {
            val recent = app.puzzles.games.value.take(10).map { it.puzzle.title }
            val words = app.wordListGenerator.generate(s.apiConfig, s.model, theme, s.difficulty, recent)
            _state.update { it.copy(working = "Hiding ${words.words.size} words in the grid…") }
            PuzzleGenerator().generate(words.words, s.difficulty, words.title, words.theme)
        }
    }

    fun generateOffline() {
        val difficulty = settings.value?.difficulty ?: Difficulty.MEDIUM
        start("Building puzzle…") {
            val recent = app.puzzles.games.value.take(3).map { it.puzzle.theme }.toSet()
            val (theme, raw) = FallbackWords.themes.entries.filter { it.key !in recent }
                .ifEmpty { FallbackWords.themes.entries.toList() }
                .random().toPair()
            val words = WordSanitizer.sanitize(raw.shuffled(), difficulty.maxWordLength)
            PuzzleGenerator().generate(words, difficulty, theme, theme)
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(working = null) }
    }

    fun consumeCreated() = _state.update { it.copy(createdId = null) }

    private fun start(message: String, build: suspend () -> com.wsnow.engine.Puzzle) {
        if (_state.value.working != null) return
        _state.value = CreateUiState(working = message)
        job = viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { build() } }
            result.onSuccess { puzzle ->
                app.puzzles.save(SavedGame(puzzle))
                _state.value = CreateUiState(createdId = puzzle.id)
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) return@onFailure
                _state.value = CreateUiState(error = e.message ?: e.javaClass.simpleName)
            }
        }
    }
}

private val themeIdeas = listOf(
    "Ocean animals", "Space exploration", "80s movies", "Baking", "Dinosaurs",
    "Board games", "National parks", "Weather", "Harry Potter", "Coffee",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onCreated: (String) -> Unit,
    vm: CreateViewModel = viewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var theme by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.createdId) {
        state.createdId?.let { vm.consumeCreated(); onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New puzzle") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "AI settings") }
                },
            )
        },
    ) { padding ->
        val s = settings ?: return@Scaffold
        val busy = state.working != null
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!s.isAiConfigured) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connect an AI model", style = MaterialTheme.typography.titleMedium)
                        Text("Set a base URL and pick a model to generate puzzles on any theme. " +
                            "You can still play the built-in themes offline.")
                        Button(onClick = onSettings) { Text("Open AI settings") }
                    }
                }
            }

            Text("Difficulty", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = s.difficulty == d,
                        onClick = { vm.setDifficulty(d) },
                        shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                        enabled = !busy,
                    ) { Text(d.label) }
                }
            }
            Text(
                describe(s.difficulty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = theme,
                onValueChange = { theme = it },
                label = { Text("Theme") },
                placeholder = { Text("Anything! Leave blank to be surprised") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (s.isAiConfigured) vm.generateWithAi(theme) }),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeIdeas.forEach { idea ->
                    SuggestionChip(onClick = { theme = idea }, label = { Text(idea) }, enabled = !busy)
                }
            }

            Button(
                onClick = { vm.generateWithAi(theme) },
                enabled = s.isAiConfigured && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (theme.isBlank()) "Surprise me" else "Generate with AI")
            }
            OutlinedButton(
                onClick = vm::generateOffline,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Casino, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Random built-in theme (offline)")
            }

            state.working?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(message, Modifier.weight(1f))
                    TextButton(onClick = vm::cancel) { Text("Cancel") }
                }
            }
            state.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Couldn't create the puzzle", style = MaterialTheme.typography.titleSmall)
                        Text(error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun describe(d: Difficulty): String = when (d) {
    Difficulty.EASY -> "${d.size}×${d.size} grid, ${d.wordCount} words, across and down only"
    Difficulty.MEDIUM -> "${d.size}×${d.size} grid, ${d.wordCount} words, adds diagonals"
    Difficulty.HARD -> "${d.size}×${d.size} grid, ${d.wordCount} words, any direction including backwards"
}
