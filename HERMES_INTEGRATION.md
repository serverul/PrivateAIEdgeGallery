# Hermes AI Chat Integration

## Summary

Hermes Chat has been integrated as a new built-in task in the Edge Gallery Android app. It provides a streamlined LLM chat experience using the `LlmChatScreen` composable with image and audio support.

---

## Files Created

| File | Purpose |
|------|---------|
| `app/src/main/java/com/hartagis/edgear/customtasks/hermes/HermesScreen.kt` | HermesScreen composable + HermesTask + HermesTaskModule (Hilt) |

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/hartagis/edgear/data/Tasks.kt` | Added `BuiltInTaskId.HERMES_CHAT = "hermes_chat"` |
| `gradle/libs.versions.toml` | Added `room = "2.6.1"` version + Room library entries |
| `app/build.gradle.kts` | Added Room dependencies (`room-runtime`, `room-ktx`, `room-compiler` via KSP) |

---

## Architecture

```
Home Screen (LLM category)
  └── "Hermes Chat" task tile (robot icon, "new feature" badge)
        └── HermesScreen
              └── LlmChatScreen (taskId=HERMES_CHAT)
                    - Image picker enabled
                    - Audio picker enabled
                    - Editable system prompt
```

### Key Design Decisions

1. **Uses `LlmChatScreen` directly** — simpler than `AgentChatScreen` which has skills, JS execution, and WebView complexity.
2. **No `firebaseAnalytics` calls** — consistent with the no-tracker approach.
3. **`runtimeHelper.initialize()`** — same approach as `LlmChatTask`, with `supportImage=true` and `supportAudio=true`.
4. **Default system prompt** — lightweight personality for Hermes.
5. **Registered via Hilt `@IntoSet`** — automatically discovered by the app's home screen via `SingletonComponent`.


## Build Instructions

```bash
# Ensure you have Android SDK 35, NDK (if needed), and JDK 11+

cd /tmp/gallery-no-tracker/Android/src

# Clean and build debug APK
./gradlew clean assembleDebug

# To run directly on a connected device/emulator
./gradlew installDebug

# Run tests (if any)
./gradlew testDebug
```

### Build Requirements

- **AGP**: 8.8.2
- **Kotlin**: 2.2.0
- **Compile SDK**: 35
- **Min SDK**: 31
- **JVM Target**: 11
- **KSP plugin**: already present in build.gradle.kts

### Room Database

Room 2.6.1 dependencies are now available for use:

- `implementation(libs.androidx.room.runtime)` — runtime
- `implementation(libs.androidx.room.ktx)` — Kotlin extensions/coroutines
- `ksp(libs.androidx.room.compiler)` — annotation processor

**Note**: No Room entities/DAOs/database have been created yet — the dependencies are in place for future use (e.g., chat history persistence). This avoids compile errors from unused `@Database` classes.

### Troubleshooting

1. **`BuiltInTaskId.HERMES_CHAT` unresolved**: Make sure Tasks.kt was saved with the new constant.
2. **KSP errors**: Verify `alias(libs.plugins.ksp)` is in the `plugins {}` block of `app/build.gradle.kts` — it already is.
3. **Room errors**: If no `@Database` class exists, Room won't have anything to process. That's expected at this stage. No entities need to be defined yet.
4. **Hermes not appearing on home screen**: Check logcat for Hilt injection errors. The `HermesTaskModule` uses `@Provides @IntoSet` which should automatically register it.
