# Project Plan: GemmaEdge (On-Device LLM)

## 1. Overview
A private, offline-first AI assistant running **Gemma 2B** locally on the Android device. This project demonstrates high-performance memory management and modern reactive programming.

## 2. Tech Stack
*   **Language:** Kotlin
*   **AI Engine:** MediaPipe LLM Inference API
*   **Model:** Gemma 2B (4-bit Quantized)
*   **Architecture:** Multi-module Clean Architecture
*   **State Management:** MVVM with Kotlin Flow/StateFlow
*   **DI:** Hilt
*   **Local Storage:** Room (Chat History), DataStore (Settings)

## 3. System Architecture (Modules)
*   `:app` - Application class and navigation.
*   `:domain` - UseCases and Domain Models.
*   `:data` - LLM implementation, Room DAO, and Repository.
*   `:feature-chat` - Compose UI for the chat interface.
*   `:feature-settings` - Model configuration UI.
*   `:core-ui` - Design system and shared components.

## 4. Implementation Phases

### Phase 1: Foundation
1.  Set up multi-module structure and Gradle Version Catalog.
2.  Configure Hilt and basic dependency graph.
3.  Integrate MediaPipe LLM Inference dependencies.
4.  Implement model downloading/loading logic in the `:data` module.

### Phase 2: Core AI Engine
1.  Implement a `ChatRepository` that wraps the LLM Inference API.
2.  Create a `StreamResponseUseCase` to handle the `Flow<String>` coming from the model.
3.  Implement the `ChatViewModel` using MVI to handle `Idle`, `Loading`, `Generating`, and `Error` states.

### Phase 3: UI & UX
1.  Build the Chat Screen with Jetpack Compose (LazyColumn for messages).
2.  Implement "Typing" animations and auto-scroll logic.
3.  Add Chat History persistence using Room.

## 5. Testing & DevOps
*   **Unit Tests:** Test the `UseCase` logic and `Repository` mapping.
*   **CI/CD:** GitHub Action to run `./gradlew test` and `detekt` on PRs.
*   **Static Analysis:** Configure `ktlint` for code style enforcement.

## 6. Key Challenges
*   **OOM Prevention:** Managing the model lifecycle to release memory when the app is in the background.
*   **Token Streaming:** Ensuring the UI updates smoothly as tokens arrive without unnecessary recompositions.
