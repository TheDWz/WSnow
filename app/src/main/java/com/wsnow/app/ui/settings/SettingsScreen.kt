package com.wsnow.app.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wsnow.app.WSnowApp
import com.wsnow.engine.ai.ApiConfig
import com.wsnow.engine.ai.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val loaded: Boolean = false,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val models: List<String> = emptyList(),
    val loadingModels: Boolean = false,
    val testing: Boolean = false,
    /** Result of the last fetch/test: true = success, false = error. */
    val status: Pair<Boolean, String>? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WSnowApp
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = app.settings.current()
            _state.update { it.copy(loaded = true, baseUrl = s.baseUrl, apiKey = s.apiKey, model = s.model) }
        }
    }

    fun setBaseUrl(v: String) {
        _state.update { it.copy(baseUrl = v, models = emptyList()) }
        viewModelScope.launch { app.settings.setBaseUrl(v.trim()) }
    }

    fun setApiKey(v: String) {
        _state.update { it.copy(apiKey = v) }
        viewModelScope.launch { app.settings.setApiKey(v.trim()) }
    }

    fun setModel(v: String) {
        _state.update { it.copy(model = v) }
        viewModelScope.launch { app.settings.setModel(v.trim()) }
    }

    private fun currentConfig() = _state.value.let { ApiConfig(it.baseUrl.trim(), it.apiKey.trim()) }

    fun fetchModels() {
        _state.update { it.copy(loadingModels = true, status = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { app.aiClient.listModels(currentConfig()) }
            }
            _state.update { s ->
                result.fold(
                    onSuccess = { models ->
                        s.copy(
                            loadingModels = false,
                            models = models,
                            status = if (models.isEmpty()) false to "The server returned no models. You can type a model ID instead."
                            else true to "Found ${models.size} model${if (models.size == 1) "" else "s"}.",
                        )
                    },
                    onFailure = { e -> s.copy(loadingModels = false, status = false to (e.message ?: "Failed")) },
                )
            }
            val models = result.getOrNull().orEmpty()
            if (_state.value.model.isBlank() && models.size == 1) setModel(models.first())
        }
    }

    fun testConnection() {
        _state.update { it.copy(testing = true, status = null) }
        viewModelScope.launch {
            val model = _state.value.model.trim()
            val result = runCatching {
                require(model.isNotBlank()) { "Pick a model first." }
                withContext(Dispatchers.IO) {
                    app.aiClient.chat(
                        currentConfig(),
                        model,
                        listOf(ChatMessage("user", "Reply with the single word: ready")),
                    )
                }
            }
            _state.update { s ->
                s.copy(
                    testing = false,
                    status = result.fold(
                        onSuccess = { true to "Connected. $model replied: \"${it.trim().take(80)}\"" },
                        onFailure = { false to (it.message ?: "Failed") },
                    ),
                )
            }
        }
    }
}

private val presets = listOf(
    "OpenAI" to "https://api.openai.com/v1",
    "OpenRouter" to "https://openrouter.ai/api/v1",
    "Groq" to "https://api.groq.com/openai/v1",
    "Ollama (LAN)" to "http://192.168.1.100:11434/v1",
    "LM Studio (LAN)" to "http://192.168.1.100:1234/v1",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Any OpenAI-compatible server works. The AI picks the themed words; " +
                    "WSnow builds the grid itself.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = vm::setBaseUrl,
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (name, url) ->
                    AssistChip(onClick = { vm.setBaseUrl(url) }, label = { Text(name) })
                }
            }

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = vm::setApiKey,
                label = { Text("API key") },
                placeholder = { Text("Leave blank for local servers") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (showKey) "Hide key" else "Show key",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = vm::fetchModels,
                enabled = state.baseUrl.isNotBlank() && !state.loadingModels,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loadingModels) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text("Fetch models")
            }

            ModelPicker(state.model, state.models, vm::setModel)

            OutlinedButton(
                onClick = vm::testConnection,
                enabled = state.model.isNotBlank() && state.baseUrl.isNotBlank() && !state.testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.testing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text("Test connection")
            }

            state.status?.let { (ok, message) ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Settings save automatically. Your API key is stored only on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A text field that filters the fetched model list as you type, and also accepts any typed ID. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(model: String, models: List<String>, onModel: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val shown = if (filtering && model.isNotBlank()) {
        models.filter { it.contains(model.trim(), ignoreCase = true) }
    } else {
        models
    }

    ExposedDropdownMenuBox(
        expanded = expanded && shown.isNotEmpty(),
        onExpandedChange = { expanded = it; filtering = false },
    ) {
        OutlinedTextField(
            value = model,
            onValueChange = { onModel(it); filtering = true; expanded = true },
            label = { Text("Model") },
            placeholder = { Text(if (models.isEmpty()) "Fetch models, or type an ID" else "Choose a model") },
            supportingText = if (models.isNotEmpty()) {
                { Text("${models.size} available — type to filter") }
            } else null,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && shown.isNotEmpty()) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && shown.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            shown.take(200).forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = { onModel(id); expanded = false; filtering = false },
                )
            }
        }
    }
}
