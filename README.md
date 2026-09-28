# WSnow
Word Search Now: an Android word search app that makes new puzzles with any OpenAI-compatible AI endpoint.

## How it works
- **The AI picks the words.** You give a theme (or leave it blank to be surprised), and the model
  returns a themed word list as JSON.
- **The app builds the grid.** A placement algorithm hides the words, preferring spots that share
  letters. It fills the gaps with look-alike letters and checks that every word appears exactly once.
  Models can't reliably lay out a letter grid themselves, so this way every puzzle can be solved.
- **You solve it on your phone.** Drag across letters in a straight line. The selection snaps to the
  nearest direction. Your progress and time save automatically.

| Difficulty | Grid | Words | Directions |
|---|---|---|---|
| Easy | 10×10 | 8 | across, down |
| Medium | 13×13 | 12 | + diagonals |
| Hard | 15×15 | 16 | all 8, including backwards |

A few built-in themes also work offline, with no AI needed.

## Install
1. Open the repository's **Releases** page on your phone (or the latest run under **Actions**)
   and download `WSnow.apk`.
2. Open it. Android asks you to allow installs from your browser/files app the first time.
3. New builds install over the old one and keep your puzzles.

## Connect an AI
Open **Settings** (gear icon):
1. **Base URL**: the API root, usually ending in `/v1`. Tap a preset or type your own:
   - OpenAI `https://api.openai.com/v1`
   - OpenRouter `https://openrouter.ai/api/v1`
   - Groq `https://api.groq.com/openai/v1`
   - Ollama on your network `http://<computer-ip>:11434/v1`
   - LM Studio on your network `http://<computer-ip>:1234/v1`
2. **API key**: leave it blank for local servers that don't need one.
3. Tap **Fetch models** and pick one from the list (type to filter). If a server doesn't list its
   models, type the model ID yourself.
4. Tap **Test connection** to check it.

The key is stored only in the app's private storage on your phone.

## Development
- `engine/` is plain Kotlin, with no Android dependency: grid generation, selection logic, the
  OpenAI-compatible client and word-list parsing. Tests: `./gradlew :engine:test`.
- `app/` is the Jetpack Compose UI.
- `./gradlew :app:assembleRelease` builds `app/build/outputs/apk/release/app-release.apk` (needs the Android SDK).
- GitHub Actions builds and tests every push. Pushes to `main` (and `claude/*` branches) publish the APK as a release.

### Signing
Builds are signed with the development key in `app/wsnow-dev.keystore`, so updates install over each
other. To use your own private key, add these repository secrets: `WSNOW_KEYSTORE_BASE64`
(base64 of the .jks), `WSNOW_KEYSTORE_PASSWORD`, `WSNOW_KEY_ALIAS` and `WSNOW_KEY_PASSWORD`.
If you switch keys, uninstall the old build once first.
