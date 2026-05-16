# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease

# Build a specific module
./gradlew :extensions:feishu:assembleDebug
./gradlew :self-control:assembleDebug

# Clean
./gradlew clean

# Run Android instrumentation tests (requires connected device/emulator)
./gradlew :app:connectedAndroidTest

# Run a single test class
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shijing.xomniclaw.ExampleTest
```

Requires JDK 17, Android SDK. Copy `local.properties.example` to `local.properties` and set `sdk.dir`. Use JDK 17 as JAVA_HOME (not JBR 21).

## Architecture

X-OmniClaw is an edge-native multimodal Android Agent that runs on physical devices. It follows an **Observation → Reasoning → Execution** loop: perceive the screen, decide the next action via LLM/VLM, execute via Android atomic actions (tap, swipe, type, app launch). It also supports speech-to-action (ASR) and camera-based context.

### Module structure (defined in `settings.gradle`)

| Module | Purpose |
|---|---|
| `:app` | Main APK — agent loop, UI, device control, LLM providers, skills, gateway |
| `:extensions:feishu` | Feishu/Lark chat channel integration (bot messaging, webhooks, tools for docs/tasks/drive/bitable) |
| `:extensions:discord` | Discord chat channel integration (gateway, messaging, sessions) |
| `:extensions:observer` | Accessibility service + screenshot/media-projection + audio preprocessing (AEC) |
| `:self-control` | Agent self-management — log queries, config read/write, service control, ADB interface |

### Agent loop: Kotlin bridge + Python logic

The agent loop is split across two layers:

- **`AgentLoop.kt`** (`app/.../agent/loop/`) — Thin Kotlin bridge. Manages coroutine scope, SharedFlow for progress events, tool execution via `ToolCallDispatcher`, LLM HTTP calls via `UnifiedLLMProvider`. Creates a `KotlinBridge` callback object and passes it to Python.
- **`agent_logic.py`** (`app/src/main/python/`) — Core business logic via Chaquopy. Handles iteration control, context budget enforcement, loop detection, incremental sensing, nudge injection, error tracking, and vision fallback decisions. Also `context_manager.py` (context pruning/compaction), `loop_detector.py` (drift/completion detection), `session_logger.py`, `utils.py`.

### Key packages under `com.shijing.xomniclaw`

- **`core/`** — Application class, foreground service, message queue, entry point
- **`agent/`** — The heart of the system:
  - `loop/` — AgentLoop (Kotlin bridge), ToolLoopDetection
  - `tools/` — All skills/tools: `device/` (tap, swipe, type, snapshot, etc.), `memory/`, `AndroidToolRegistry`, `ToolCallDispatcher`, `ToolRegistry`
  - `skills/` — SKILL.md parser, loader, installer, skill document model
  - `context/` — Context window management (budget enforcement, pruning)
  - `session/` — Session lifecycle, history sanitizer
  - `memory/` — Gallery memory indexing, memory evolution, personalization
- **`gateway/`** — NanoHTTPd-based local REST + WebSocket server. Methods in `gateway/methods/`: AgentMethods, ConfigMethods, SessionMethods, SkillsMethods, CronMethods, etc.
- **`providers/`** — LLM provider abstraction. `UnifiedLLMProvider` handles multiple backends (OpenAI-compatible, Anthropic, OpenRouter, Ollama, Moonshot, MiniMax). Provider config persisted to `/sdcard/.xomniclaw/xomniclaw.json`.
- **`channel/`** — External channel abstraction for Feishu/Discord integration
- **`scheduler/`** — WorkManager-based scheduled task execution (wake screen, run agent on schedule)
- **`accessibility/`** — Accessibility service proxy for UI tree capture and screen interaction
- **`voice/`** — ASR (speech-to-text), TTS, voice recording managers
- **`vision/`** — Camera frame push, screen frame sampling, vision frame buffering
- **`ui/`** — Jetpack Compose UI (MainActivityCompose is the launcher activity), alongside some legacy XML views
- **`behavior/`** — Behavior cloning / skill recording (record user actions → replayable skill)

### Skills system

Skills are defined as `SKILL.md` files in `app/src/main/assets/skills/<skill-id>/SKILL.md`. Each skill exposes tools registered via `ToolRegistry`. Bundled skills include: `app-search`, `taobao-search`, `gallery-qa`, `gallery-memory`, `capcut-theme-video`, `clipboard-to-shortcut`, `model-config`, `channel-config`, `scheduled-automation`, `skill-creator`, `memory-evolution`.

The bootstrap assets (`app/src/main/assets/bootstrap/`) include `AGENTS.md` (safety rules and interaction guidelines injected into the LLM system prompt), `APP_CONFIG.json`, voice/vision prompts, and memory templates.

### Device interaction

All device actions go through the unified `device` tool (defined in `agent/tools/device/`). Actions: `snapshot` (accessibility tree + screenshot), `tap`, `swipe`, `type`, `launch_app`, `screenshot`, `long_press`, `home`, `back`, etc. Snapshot provides UI element `ref` values for targeting; vision fallback (`use_dual_track`) activates when structural understanding is insufficient.

### Config persistence

Runtime config stored at `/sdcard/.xomniclaw/xomniclaw.json`. Uses MMKV for fast key-value storage. ConfigLoader reads/writes both. The `model-config` skill lets the agent modify its own provider/model settings at runtime.
