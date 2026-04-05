# Private Edge Gallery 🔒

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**On-Device AI — Privacy First. No Tracking, No Telemetry, Fully Offline.**

Private Edge Gallery is a privacy-focused fork of Google's [AI Edge Gallery](https://github.com/google-ai-edge/gallery), stripped of all Firebase analytics, Google tracking, and external telemetry. Run powerful on-device Large Language Models (LLMs) — 100% offline, 100% private.

> **Built from Google's Apache 2.0-licensed AI Edge Gallery.**
> Original: https://github.com/google-ai-edge/gallery
> Licensed under the same Apache License 2.0. See [LICENSE](LICENSE) for details.

## What's Different from the Original

| Feature | Original Gallery | Private Edge Gallery |
|---------|-----------------|---------------------|
| Firebase Analytics | ✅ Included | ❌ Removed |
| Google Services / OSS Licenses | ✅ Included | ❌ Removed |
| Telemetry & Tracking | ✅ Present | ❌ Fully stripped |
| Internet required | Often | Never |
| Package name | `com.google.ai.edge.gallery` | `com.hartagis.edgear` |

## ✨ Core Features

* **100% On-Device** — All AI inference happens on your hardware. No cloud, no data leaving the device.
* **AI Chat with Thinking Mode** — Multi-turn conversations with visible step-by-step reasoning.
* **Ask Image** — Multimodal support: identify objects, analyze photos, solve visual problems.
* **Audio Scribe** — Transcribe and translate voice recordings on-device.
* **Agent Skills** — Extend LLM capabilities with modular tools (Wikipedia, maps, custom skills).
* **Mobile Actions** — Offline device controls and automated tasks.
* **Tiny Garden** — Mini-game powered entirely by on-device model.
* **Model Management & Benchmark** — Download, load, and benchmark models on your hardware.
* **Prompt Lab** — Test prompts with full control over temperature, top-k, and parameters.
* **Zero Telemetry** — Network calls are only for Hugging Face model downloads. Nothing else.

## 📱 Screenshots

> *Screenshots from the app running with local models*

## 🏁 Get Started

### Pre-built APK
Download the latest APK from [Releases](https://github.com/google-ai-edge/gallery/releases/latest/) or clone and build from source.

### Build from Source

1. **Requirements:** Android Studio, Gradle 8+, Android SDK (API 31+)
2. **Clone:**
   ```bash
   git clone https://github.com/hartagis/private-edge-gallery.git
   cd private-edge-gallery/Android/src
   ```
3. **Build:**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio → **Build → Rebuild Project**

See [DEVELOPMENT.md](Android/src/DEVELOPMENT.md) for full build instructions.

## 🛠️ Technology

* **Google AI Edge** — Core on-device ML APIs
* **LiteRT** — Lightweight runtime for optimized model execution
* **Hugging Face** — Model download integration
* **Compose / Material 3** — Modern UI
* **Hilt** — Dependency injection

## 📄 License

This project is licensed under the **Apache License, Version 2.0**.

Original work Copyright © 2025 Google LLC.
Modifications Copyright © 2026 HartaGIS.

See [LICENSE](LICENSE) for the full license text.

## 🔗 Links

* [Original Project (Google AI Edge Gallery)](https://github.com/google-ai-edge/gallery)
* [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
* [Hugging Face LiteRT Community](https://huggingface.co/litert-community)
* [Google AI Edge Documentation](https://ai.google.dev/edge)
