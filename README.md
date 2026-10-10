# 🛡️ RakshaAI

> **On-Device AI-Powered Scam & Phishing Detection Assistant for Android**

RakshaAI is a real-time, privacy-first Android security application designed to protect users from SMS smishing, messaging fraud, phishing URLs, malicious QR codes, scam screenshots, and fraudulent app notifications. Built with an **offline-first** architecture, all threat detection models, OCR pipelines, and threat intelligence lookups run **100% on-device** without transmitting any private user data to cloud servers.

---

## 🚀 Key Features

- **🔒 100% Offline & Privacy-First**: Operates entirely locally. No internet permission is requested or required; your personal messages, notifications, and screenshots never leave your device.
- **⚡ Real-Time Notification Protection**: Continuously monitors incoming notifications across all messaging, SMS, banking, payment, and shopping apps in real-time, instantly issuing heads-up security warnings for high-risk threats.
- **🤖 Multi-Modal AI Detection**:
  - **Message NLP Classifier**: TF-IDF vectorization paired with a custom Linear SVM trained to detect financial fraud, KYC scams, OTP-sharing scams, lottery fraud, and job scams across English, Hindi, Gujarati, and Hinglish.
  - **URL Phishing Analysis**: Combines deterministic structural rules, a lightweight LightGBM ML classifier, and an offline local PhishTank threat intelligence store (`~72,000+` verified phishing URLs).
  - **Screenshot & OCR Scanner**: Powered by ML Kit OCR to extract text and URLs from user screenshots and images.
- **💾 Persistent Scan History**: Leverages Room SQLite database persistence so your scan history securely survives app restarts and process death.
- **🌐 Multi-Language Support**: Fully localized for English, ગુજરાતી (Gujarati), and हिन्दी (Hindi).

---

## 🏗️ Architecture & Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Concurrency**: Kotlin Coroutines & Flow (`StateFlow`)
- **Local Persistence**: Android Room (`2.6.1`) with KSP
- **Machine Learning & Vision**: ML Kit OCR, custom TF-IDF + Linear SVM, LightGBM tree evaluator

---

## 📱 Core Detection Pipelines

```
[ User Input: Message / URL / Screenshot / Notification ]
                      │
                      ▼
            [ Local Preprocessing ]
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
   [ Message NLP ]  [ URL ML ]  [ PhishTank ]
         └────────────┬────────────┘
                      │
                      ▼
             [ Evidence Fusion ]
                      │
                      ▼
           [ Risk Engine (0-100) ]
                      │
         ┌────────────┴────────────┐
         ▼                         ▼
   [ ScanResult ]          [ Room Database ]
         │
         ▼
   [ UI & Alerts ]
```

---

## 🛠️ Getting Started & Building

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/yashtarapara25/RaskshaAI.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open** -> Navigate to the cloned `RaskshaAI` folder.
3. **Build the Project**:
   - Run Gradle sync and execute the build task:
     ```bash
     ./gradlew clean assembleDebug
     ```

---

## 📦 Project Structure

- `app/src/main/java/com/rakshaai/security/`
  - `data/local/` — Room database, entities, DAOs, and type converters for persistent scan history.
  - `domain/engine/` — Core detection engine and scoring fusion.
  - `domain/url/` — URL normalizer, parser, feature extractor, LightGBM model wrapper, and PhishTank store.
  - `notification/` — `RakshaNotificationListenerService` for real-time notification protection.
  - `ui/` — Jetpack Compose UI screens (Dashboard, Scan, History, Result, Privacy Settings, Language Selection).

---

## 📄 License

This project is developed as an advanced security research and product implementation. All rights reserved.
